package io.github.cezarywenta.carrental.application;

import io.github.cezarywenta.carrental.domain.CarType;
import java.util.function.Supplier;

/**
 * Pass-through LockManager for single-threaded ReservationService tests.
 * The real, thread-safe implementation is exercised by the concurrency test.
 */
class FakeLockManager implements LockManager {

    @Override
    public <T> T executeLocked(CarType carType, Supplier<T> action) {
        return action.get();
    }
}
