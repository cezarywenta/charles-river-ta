package io.github.cezarywenta.carrental.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cezarywenta.carrental.domain.CarType;
import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationId;
import io.github.cezarywenta.carrental.domain.ReservationPeriod;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReservationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 9, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final FakeReservationRepository repository = new FakeReservationRepository();

    private FleetCapacity fleetCapacity(int sedan, int suv, int van) {
        return new FleetCapacity(Map.of(CarType.SEDAN, sedan, CarType.SUV, suv, CarType.VAN, van));
    }

    private ReservationService service(FleetCapacity fleetCapacity) {
        return new ReservationService(repository, fleetCapacity, new FakeLockManager(), FIXED_CLOCK);
    }

    private Reservation seedConfirmed(CarType carType, ReservationPeriod period, String customerId) {
        Reservation reservation = Reservation.confirmed(
                ReservationId.generate(), customerId, carType, period, FIXED_CLOCK.instant());
        repository.save(reservation);
        return reservation;
    }

    @Test
    void reserveSucceedsWhenCarIsAvailable() {
        ReservationService service = service(fleetCapacity(1, 1, 1));
        ReservationPeriod period = new ReservationPeriod(NOW.plusDays(1), NOW.plusDays(2));

        ReservationResult result =
                service.reserve(new ReserveCarCommand("customer-1", CarType.SUV, period));

        ReservationResult.ReservationConfirmed confirmed =
                assertInstanceOf(ReservationResult.ReservationConfirmed.class, result);
        assertEquals("customer-1", confirmed.reservation().customerId());
        assertEquals(CarType.SUV, confirmed.reservation().carType());
        assertTrue(confirmed.reservation().isActive());
        assertEquals(1, repository.all().size());
    }

    @Test
    void reserveFailsWhenNoCapacityAvailable() {
        ReservationPeriod period = new ReservationPeriod(NOW.plusDays(1), NOW.plusDays(2));
        seedConfirmed(CarType.SUV, period, "customer-1");
        ReservationService service = service(fleetCapacity(1, 1, 1));

        ReservationResult result =
                service.reserve(new ReserveCarCommand("customer-2", CarType.SUV, period));

        ReservationResult.CarUnavailable unavailable =
                assertInstanceOf(ReservationResult.CarUnavailable.class, result);
        assertEquals(CarType.SUV, unavailable.carType());
    }

    @Test
    void carTypesHaveIndependentCapacity() {
        ReservationPeriod period = new ReservationPeriod(NOW.plusDays(1), NOW.plusDays(2));
        seedConfirmed(CarType.SUV, period, "customer-1");
        ReservationService service = service(fleetCapacity(1, 1, 1));

        ReservationResult result =
                service.reserve(new ReserveCarCommand("customer-2", CarType.SEDAN, period));

        assertInstanceOf(ReservationResult.ReservationConfirmed.class, result);
    }

    @Test
    void cancelledReservationDoesNotOccupyCapacity() {
        ReservationPeriod period = new ReservationPeriod(NOW.plusDays(1), NOW.plusDays(2));
        Reservation cancelled = seedConfirmed(CarType.SUV, period, "customer-1").cancelAt(NOW);
        repository.save(cancelled);
        ReservationService service = service(fleetCapacity(1, 1, 1));

        ReservationResult result =
                service.reserve(new ReserveCarCommand("customer-2", CarType.SUV, period));

        assertInstanceOf(ReservationResult.ReservationConfirmed.class, result);
    }

    @Test
    void nonOverlappingReservationDoesNotOccupyCapacity() {
        ReservationPeriod existingPeriod = new ReservationPeriod(NOW.plusDays(1), NOW.plusDays(2));
        ReservationPeriod requestedPeriod = new ReservationPeriod(NOW.plusDays(2), NOW.plusDays(3));
        seedConfirmed(CarType.SUV, existingPeriod, "customer-1");
        ReservationService service = service(fleetCapacity(1, 1, 1));

        ReservationResult result =
                service.reserve(new ReserveCarCommand("customer-2", CarType.SUV, requestedPeriod));

        assertInstanceOf(ReservationResult.ReservationConfirmed.class, result);
    }

    @Test
    void reserveRejectsStartInThePast() {
        ReservationPeriod period = new ReservationPeriod(NOW.minusMinutes(1), NOW.plusDays(1));
        ReservationService service = service(fleetCapacity(1, 1, 1));

        assertThrows(IllegalArgumentException.class,
                () -> service.reserve(new ReserveCarCommand("customer-1", CarType.SUV, period)));
    }

    @Test
    void reserveAllowsStartExactlyAtNow() {
        ReservationPeriod period = new ReservationPeriod(NOW, NOW.plusDays(1));
        ReservationService service = service(fleetCapacity(1, 1, 1));

        ReservationResult result =
                service.reserve(new ReserveCarCommand("customer-1", CarType.SUV, period));

        assertInstanceOf(ReservationResult.ReservationConfirmed.class, result);
    }

    @Test
    void failedReservationDoesNotModifyRepository() {
        ReservationPeriod period = new ReservationPeriod(NOW.plusDays(1), NOW.plusDays(2));
        seedConfirmed(CarType.SUV, period, "customer-1");
        ReservationService service = service(fleetCapacity(1, 1, 1));

        service.reserve(new ReserveCarCommand("customer-2", CarType.SUV, period));

        assertEquals(1, repository.all().size());
    }

    @Test
    void cancelSucceedsForCancellableReservation() {
        ReservationPeriod period = new ReservationPeriod(NOW.plusDays(1), NOW.plusDays(2));
        Reservation reservation = seedConfirmed(CarType.SUV, period, "customer-1");
        ReservationService service = service(fleetCapacity(1, 1, 1));

        CancellationResult result =
                service.cancel(new CancelReservationCommand(reservation.id(), "customer-1"));

        CancellationResult.CancellationConfirmed confirmed =
                assertInstanceOf(CancellationResult.CancellationConfirmed.class, result);
        assertEquals(reservation.id(), confirmed.reservation().id());
        assertFalse(repository.findById(reservation.id()).orElseThrow().isActive());
    }

    @Test
    void cancelFailsForUnknownReservation() {
        ReservationService service = service(fleetCapacity(1, 1, 1));

        CancellationResult result =
                service.cancel(new CancelReservationCommand(ReservationId.generate(), "customer-1"));

        assertInstanceOf(CancellationResult.ReservationNotFound.class, result);
    }

    @Test
    void cancelFailsForStartedReservation() {
        ReservationPeriod period = new ReservationPeriod(NOW.minusHours(1), NOW.plusHours(1));
        Reservation reservation = seedConfirmed(CarType.SUV, period, "customer-1");
        ReservationService service = service(fleetCapacity(1, 1, 1));

        CancellationResult result =
                service.cancel(new CancelReservationCommand(reservation.id(), "customer-1"));

        assertInstanceOf(CancellationResult.CancellationNotAllowed.class, result);
    }

    @Test
    void cancelFailsForAlreadyCancelledReservation() {
        ReservationPeriod period = new ReservationPeriod(NOW.plusDays(1), NOW.plusDays(2));
        Reservation reservation = seedConfirmed(CarType.SUV, period, "customer-1");
        ReservationService service = service(fleetCapacity(1, 1, 1));
        service.cancel(new CancelReservationCommand(reservation.id(), "customer-1"));

        CancellationResult result =
                service.cancel(new CancelReservationCommand(reservation.id(), "customer-1"));

        assertInstanceOf(CancellationResult.CancellationNotAllowed.class, result);
    }

    @Test
    void cancelFailsForDifferentCustomer() {
        ReservationPeriod period = new ReservationPeriod(NOW.plusDays(1), NOW.plusDays(2));
        Reservation reservation = seedConfirmed(CarType.SUV, period, "customer-1");
        ReservationService service = service(fleetCapacity(1, 1, 1));

        CancellationResult result =
                service.cancel(new CancelReservationCommand(reservation.id(), "customer-2"));

        assertInstanceOf(CancellationResult.ReservationNotFound.class, result);
    }
}
