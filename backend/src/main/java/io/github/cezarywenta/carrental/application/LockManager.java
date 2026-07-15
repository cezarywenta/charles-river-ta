package io.github.cezarywenta.carrental.application;

import io.github.cezarywenta.carrental.domain.CarType;
import java.util.function.Supplier;

/**
 * Coordinates exclusive access per car type around a critical section. The
 * in-memory implementation works only within a single JVM; a production
 * evolution would move this guarantee into the persistence layer
 * (transactions, pessimistic/optimistic locking) instead.
 */
public interface LockManager {

    <T> T executeLocked(CarType carType, Supplier<T> action);
}
