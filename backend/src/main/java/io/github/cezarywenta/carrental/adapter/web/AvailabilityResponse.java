package io.github.cezarywenta.carrental.adapter.web;

import io.github.cezarywenta.carrental.domain.CarType;
import java.time.LocalDateTime;
import java.util.List;

public record AvailabilityResponse(LocalDateTime startAt, LocalDateTime endAt, List<CarTypeAvailability> availability) {

    public record CarTypeAvailability(CarType carType, int availableCount) {
    }
}
