package io.github.cezarywenta.carrental.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AvailabilityPolicyTest {

    @Test
    void availableWhenOverlappingCountBelowCapacity() {
        assertTrue(AvailabilityPolicy.isAvailable(2, 0));
        assertTrue(AvailabilityPolicy.isAvailable(2, 1));
    }

    @Test
    void unavailableWhenOverlappingCountReachesCapacity() {
        assertFalse(AvailabilityPolicy.isAvailable(2, 2));
    }

    @Test
    void unavailableWhenCapacityIsZero() {
        assertFalse(AvailabilityPolicy.isAvailable(0, 0));
    }

    @Test
    void availableCountNeverGoesNegative() {
        assertEquals(0, AvailabilityPolicy.availableCount(2, 5));
    }

    @Test
    void availableCountIsCapacityMinusOverlapping() {
        assertEquals(1, AvailabilityPolicy.availableCount(3, 2));
    }

    @Test
    void negativeCapacityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> AvailabilityPolicy.isAvailable(-1, 0));
    }

    @Test
    void negativeReservationCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> AvailabilityPolicy.availableCount(2, -1));
    }
}
