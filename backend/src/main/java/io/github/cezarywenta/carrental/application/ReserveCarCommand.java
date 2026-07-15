package io.github.cezarywenta.carrental.application;

import io.github.cezarywenta.carrental.domain.CarType;
import io.github.cezarywenta.carrental.domain.ReservationPeriod;
import java.util.Objects;

public record ReserveCarCommand(String customerId, CarType carType, ReservationPeriod period) {

    public ReserveCarCommand {
        Objects.requireNonNull(carType, "carType must not be null");
        Objects.requireNonNull(period, "period must not be null");
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
    }
}
