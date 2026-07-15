package io.github.cezarywenta.carrental.domain;

/**
 * A car type is available for a period when the number of active
 * reservations overlapping that period is below the fleet capacity for that
 * type. Availability is always a function of capacity and the reservations
 * overlapping a specific period, not a single decrementing counter.
 */
public final class AvailabilityPolicy {

    private AvailabilityPolicy() {
    }

    public static boolean isAvailable(int capacity, long overlappingActiveReservations) {
        validate(capacity, overlappingActiveReservations);
        return overlappingActiveReservations < capacity;
    }

    public static int availableCount(int capacity, long overlappingActiveReservations) {
        validate(capacity, overlappingActiveReservations);
        return (int) Math.max(0, capacity - overlappingActiveReservations);
    }

    private static void validate(int capacity, long overlappingActiveReservations) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        if (overlappingActiveReservations < 0) {
            throw new IllegalArgumentException("overlappingActiveReservations must not be negative");
        }
    }
}
