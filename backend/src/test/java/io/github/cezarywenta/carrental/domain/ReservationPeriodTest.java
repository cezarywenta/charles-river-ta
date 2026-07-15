package io.github.cezarywenta.carrental.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReservationPeriodTest {

    private static final LocalDateTime AUG_10_10 = LocalDateTime.of(2026, 8, 10, 10, 0);

    @Test
    void startingAtComputesEndFromStartPlusDays() {
        ReservationPeriod period = ReservationPeriod.startingAt(AUG_10_10, 3);

        assertEquals(AUG_10_10, period.start());
        assertEquals(AUG_10_10.plusDays(3), period.end());
    }

    @Test
    void zeroDaysIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ReservationPeriod.startingAt(AUG_10_10, 0));
    }

    @Test
    void negativeDaysIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ReservationPeriod.startingAt(AUG_10_10, -1));
    }

    @Test
    void endNotAfterStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReservationPeriod(AUG_10_10, AUG_10_10));
        assertThrows(IllegalArgumentException.class,
                () -> new ReservationPeriod(AUG_10_10, AUG_10_10.minusDays(1)));
    }

    @Test
    void adjacentPeriodsDoNotOverlap() {
        // A: Aug 10 10:00 -> Aug 12 10:00, B: Aug 12 10:00 -> Aug 14 10:00
        ReservationPeriod a = new ReservationPeriod(AUG_10_10, AUG_10_10.plusDays(2));
        ReservationPeriod b = new ReservationPeriod(AUG_10_10.plusDays(2), AUG_10_10.plusDays(4));

        assertFalse(a.overlaps(b));
        assertFalse(b.overlaps(a));
    }

    @Test
    void partialOverlapIsDetected() {
        ReservationPeriod a = new ReservationPeriod(AUG_10_10, AUG_10_10.plusDays(3));
        ReservationPeriod b = new ReservationPeriod(AUG_10_10.plusDays(1), AUG_10_10.plusDays(4));

        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
    }

    @Test
    void periodContainingAnExistingPeriodOverlaps() {
        ReservationPeriod existing = new ReservationPeriod(AUG_10_10.plusDays(1), AUG_10_10.plusDays(2));
        ReservationPeriod requested = new ReservationPeriod(AUG_10_10, AUG_10_10.plusDays(4));

        assertTrue(requested.overlaps(existing));
        assertTrue(existing.overlaps(requested));
    }

    @Test
    void periodInsideAnExistingPeriodOverlaps() {
        ReservationPeriod existing = new ReservationPeriod(AUG_10_10, AUG_10_10.plusDays(5));
        ReservationPeriod requested = new ReservationPeriod(AUG_10_10.plusDays(1), AUG_10_10.plusDays(2));

        assertTrue(requested.overlaps(existing));
        assertTrue(existing.overlaps(requested));
    }

    @Test
    void identicalPeriodsOverlap() {
        ReservationPeriod a = new ReservationPeriod(AUG_10_10, AUG_10_10.plusDays(2));
        ReservationPeriod b = new ReservationPeriod(AUG_10_10, AUG_10_10.plusDays(2));

        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
    }

    @Test
    void separatePeriodsDoNotOverlap() {
        ReservationPeriod a = new ReservationPeriod(AUG_10_10, AUG_10_10.plusDays(1));
        ReservationPeriod b = new ReservationPeriod(AUG_10_10.plusDays(5), AUG_10_10.plusDays(6));

        assertFalse(a.overlaps(b));
        assertFalse(b.overlaps(a));
    }

    @Test
    void nullPeriodIsRejectedWhenCheckingOverlap() {
        ReservationPeriod period = new ReservationPeriod(AUG_10_10, AUG_10_10.plusDays(1));

        assertThrows(NullPointerException.class, () -> period.overlaps(null));
    }
}
