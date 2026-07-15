package io.github.cezarywenta.carrental.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReservationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 9, 0);
    private static final Instant CREATED_AT = Instant.parse("2026-07-01T00:00:00Z");

    private Reservation confirmedStartingAt(LocalDateTime start) {
        ReservationPeriod period = new ReservationPeriod(start, start.plusDays(1));
        return Reservation.confirmed(ReservationId.generate(), "customer-123", CarType.SUV, period, CREATED_AT);
    }

    @Test
    void confirmedFutureReservationCanBeCancelled() {
        Reservation reservation = confirmedStartingAt(NOW.plusDays(1));

        assertTrue(reservation.isCancellable(NOW));
    }

    @Test
    void startedReservationCannotBeCancelled() {
        Reservation reservation = confirmedStartingAt(NOW.minusHours(1));

        assertFalse(reservation.isCancellable(NOW));
    }

    @Test
    void reservationStartingExactlyNowCannotBeCancelled() {
        Reservation reservation = confirmedStartingAt(NOW);

        assertFalse(reservation.isCancellable(NOW));
    }

    @Test
    void alreadyCancelledReservationCannotBeCancelledAgain() {
        Reservation cancelled = confirmedStartingAt(NOW.plusDays(1)).cancelAt(NOW);

        assertFalse(cancelled.isCancellable(NOW));
    }

    @Test
    void startedReservationCannotBeTransitionedToCancelled() {
        Reservation reservation = confirmedStartingAt(NOW.minusHours(1));

        assertThrows(IllegalStateException.class, () -> reservation.cancelAt(NOW));
    }

    @Test
    void cancelledReservationCannotBeCancelledAgainViaCancelAt() {
        Reservation cancelled = confirmedStartingAt(NOW.plusDays(1)).cancelAt(NOW);

        assertThrows(IllegalStateException.class, () -> cancelled.cancelAt(NOW));
    }

    @Test
    void cancellingChangesStatusWithoutDeletingHistory() {
        Reservation reservation = confirmedStartingAt(NOW.plusDays(1));

        Reservation cancelled = reservation.cancelAt(NOW);

        assertTrue(reservation.isActive());
        assertFalse(cancelled.isActive());
        assertEquals(reservation.id(), cancelled.id());
        assertEquals(reservation.customerId(), cancelled.customerId());
        assertEquals(reservation.carType(), cancelled.carType());
        assertEquals(reservation.period(), cancelled.period());
        assertEquals(reservation.createdAt(), cancelled.createdAt());
    }

    @Test
    void blankCustomerIdIsRejected() {
        ReservationPeriod period = new ReservationPeriod(NOW, NOW.plusDays(1));

        assertThrows(IllegalArgumentException.class,
                () -> Reservation.confirmed(ReservationId.generate(), " ", CarType.SEDAN, period, CREATED_AT));
    }
}
