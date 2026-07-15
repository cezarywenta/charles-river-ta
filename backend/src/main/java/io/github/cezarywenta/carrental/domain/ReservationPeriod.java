package io.github.cezarywenta.carrental.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a half-open interval [start, end).
 * A period ending exactly when another begins does not overlap it.
 */
public record ReservationPeriod(LocalDateTime start, LocalDateTime end) {

    public ReservationPeriod {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start");
        }
    }

    public static ReservationPeriod startingAt(LocalDateTime start, int numberOfDays) {
        Objects.requireNonNull(start, "start must not be null");
        if (numberOfDays <= 0) {
            throw new IllegalArgumentException("numberOfDays must be positive, was " + numberOfDays);
        }
        return new ReservationPeriod(start, start.plusDays(numberOfDays));
    }

    public boolean overlaps(ReservationPeriod other) {
        Objects.requireNonNull(other, "other must not be null");

        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
