package io.github.cezarywenta.carrental.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Wraps a UUID so reservation identity is a distinct type instead of a bare
 * UUID/String passed around by convention.
 */
public record ReservationId(UUID value) {

    public ReservationId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static ReservationId generate() {
        return new ReservationId(UUID.randomUUID());
    }

    public static ReservationId of(String value) {
        Objects.requireNonNull(value, "value must not be null");
        return new ReservationId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
