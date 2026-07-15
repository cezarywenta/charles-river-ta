package io.github.cezarywenta.carrental.adapter.inmem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cezarywenta.carrental.domain.CarType;
import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationId;
import io.github.cezarywenta.carrental.domain.ReservationPeriod;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryReservationRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 9, 0);
    private static final Instant CREATED_AT = Instant.parse("2026-07-01T00:00:00Z");

    private final InMemoryReservationRepository repository = new InMemoryReservationRepository();

    private Reservation confirmed(CarType carType, ReservationPeriod period, String customerId) {
        return Reservation.confirmed(ReservationId.generate(), customerId, carType, period, CREATED_AT);
    }

    @Test
    void savedReservationCanBeFoundById() {
        Reservation reservation = confirmed(CarType.SUV, new ReservationPeriod(NOW, NOW.plusDays(1)), "customer-1");

        repository.save(reservation);

        assertEquals(Optional.of(reservation), repository.findById(reservation.id()));
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertEquals(Optional.empty(), repository.findById(ReservationId.generate()));
    }

    @Test
    void savingWithSameIdUpsertsInsteadOfDuplicating() {
        Reservation reservation = confirmed(CarType.SUV, new ReservationPeriod(NOW, NOW.plusDays(1)), "customer-1");
        repository.save(reservation);

        Reservation cancelled = reservation.cancelAt(NOW.minusMinutes(1));
        repository.save(cancelled);

        assertEquals(Optional.of(cancelled), repository.findById(reservation.id()));
        assertEquals(1, repository.findByCustomerId("customer-1").size());
    }

    @Test
    void findOverlappingFiltersByCarTypeAndPeriod() {
        ReservationPeriod period = new ReservationPeriod(NOW, NOW.plusDays(1));
        Reservation suv = confirmed(CarType.SUV, period, "customer-1");
        Reservation sedanSamePeriod = confirmed(CarType.SEDAN, period, "customer-2");
        Reservation suvLaterPeriod = confirmed(CarType.SUV, new ReservationPeriod(NOW.plusDays(5), NOW.plusDays(6)), "customer-3");
        repository.save(suv);
        repository.save(sedanSamePeriod);
        repository.save(suvLaterPeriod);

        List<Reservation> overlapping = repository.findOverlapping(CarType.SUV, period);

        assertEquals(List.of(suv), overlapping);
    }

    @Test
    void findByCustomerIdReturnsOnlyThatCustomersReservations() {
        Reservation mine = confirmed(CarType.SUV, new ReservationPeriod(NOW, NOW.plusDays(1)), "customer-1");
        Reservation someoneElses = confirmed(CarType.SEDAN, new ReservationPeriod(NOW, NOW.plusDays(1)), "customer-2");
        repository.save(mine);
        repository.save(someoneElses);

        List<Reservation> found = repository.findByCustomerId("customer-1");

        assertEquals(1, found.size());
        assertTrue(found.contains(mine));
    }
}
