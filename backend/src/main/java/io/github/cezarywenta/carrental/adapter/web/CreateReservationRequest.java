package io.github.cezarywenta.carrental.adapter.web;

import io.github.cezarywenta.carrental.domain.CarType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record CreateReservationRequest(
        @NotNull(message = "carType must not be null") CarType carType,
        @NotNull(message = "startAt must not be null") LocalDateTime startAt,
        @NotNull(message = "numberOfDays must not be null") @Positive(message = "numberOfDays must be positive")
                Integer numberOfDays) {
}
