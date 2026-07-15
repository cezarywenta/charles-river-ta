package io.github.cezarywenta.carrental.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.cezarywenta.carrental.domain.CarType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FleetCapacityTest {

    @Test
    void capacityForReturnsConfiguredValue() {
        FleetCapacity fleet = new FleetCapacity(Map.of(CarType.SEDAN, 3, CarType.SUV, 2, CarType.VAN, 1));

        assertEquals(2, fleet.capacityFor(CarType.SUV));
    }

    @Test
    void capacityForRejectsNullCarType() {
        FleetCapacity fleet = new FleetCapacity(Map.of(CarType.SEDAN, 3, CarType.SUV, 2, CarType.VAN, 1));

        assertThrows(NullPointerException.class, () -> fleet.capacityFor(null));
    }
}
