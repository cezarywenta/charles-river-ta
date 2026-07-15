package io.github.cezarywenta.carrental.application;

import io.github.cezarywenta.carrental.domain.CarType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The fleet is a configured count per car type, not individually tracked
 * vehicle records. Managing individual vehicles (registration number,
 * maintenance state, location, assignment policy) would be a separate,
 * larger domain and is out of scope here.
 */
public record FleetCapacity(Map<CarType, Integer> capacityByType) {

    public FleetCapacity {
        Objects.requireNonNull(capacityByType, "capacityByType must not be null");
        for (CarType carType : CarType.values()) {
            Integer capacity = capacityByType.get(carType);
            if (capacity == null) {
                throw new IllegalArgumentException("missing capacity for " + carType);
            }
            if (capacity < 0) {
                throw new IllegalArgumentException("capacity for " + carType + " must not be negative");
            }
        }
        capacityByType = Map.copyOf(new EnumMap<>(capacityByType));
    }

    public int capacityFor(CarType carType) {
        Objects.requireNonNull(carType, "carType must not be null");
        return capacityByType.get(carType);
    }
}
