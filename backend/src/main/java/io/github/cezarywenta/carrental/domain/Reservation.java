package io.github.cezarywenta.carrental.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

public record Reservation(
        ReservationId id,
        String customerId,
        CarType carType,
        ReservationPeriod period,
        ReservationStatus status,
        Instant createdAt) {

    public Reservation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(carType, "carType must not be null");
        Objects.requireNonNull(period, "period must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
    }

    public static Reservation confirmed(
            ReservationId id, String customerId, CarType carType, ReservationPeriod period, Instant createdAt) {
        return new Reservation(id, customerId, carType, period, ReservationStatus.CONFIRMED, createdAt);
    }

    public boolean isActive() {
        return status == ReservationStatus.CONFIRMED;
    }

    /**
     * Cancellable only while confirmed and not yet started; a reservation
     * starting exactly at {@code asOf} is treated as already started.
     */
    public boolean isCancellable(LocalDateTime asOf) {
        Objects.requireNonNull(asOf, "asOf must not be null");
        return isActive() && period.start().isAfter(asOf);
    }

    /**
     * Performs the CONFIRMED -> CANCELLED transition, enforcing
     * {@link #isCancellable(LocalDateTime)} so callers cannot cancel an
     * already-cancelled or already-started reservation by skipping the check.
     */
    public Reservation cancelAt(LocalDateTime asOf) {
        if (!isCancellable(asOf)) {
            throw new IllegalStateException("reservation cannot be cancelled");
        }
        return new Reservation(id, customerId, carType, period, ReservationStatus.CANCELLED, createdAt);
    }
}
