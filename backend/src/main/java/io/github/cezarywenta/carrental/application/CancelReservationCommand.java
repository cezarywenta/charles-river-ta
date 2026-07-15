package io.github.cezarywenta.carrental.application;

import io.github.cezarywenta.carrental.domain.ReservationId;
import java.util.Objects;

public record CancelReservationCommand(ReservationId reservationId, String customerId) {

    public CancelReservationCommand {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
    }
}
