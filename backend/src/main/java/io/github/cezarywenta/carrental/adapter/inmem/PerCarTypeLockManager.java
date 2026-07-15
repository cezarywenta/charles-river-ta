package io.github.cezarywenta.carrental.adapter.inmem;

import io.github.cezarywenta.carrental.application.LockManager;
import io.github.cezarywenta.carrental.domain.CarType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * One lock per car type, so a reservation attempt for one type never blocks
 * an attempt for another. Locks only within this JVM; see LockManager.
 */
public class PerCarTypeLockManager implements LockManager {

    private final Map<CarType, ReentrantLock> locks;

    public PerCarTypeLockManager() {
        Map<CarType, ReentrantLock> initial = new EnumMap<>(CarType.class);
        for (CarType carType : CarType.values()) {
            initial.put(carType, new ReentrantLock());
        }
        this.locks = Map.copyOf(initial);
    }

    @Override
    public <T> T executeLocked(CarType carType, Supplier<T> action) {
        Objects.requireNonNull(carType, "carType must not be null");
        Objects.requireNonNull(action, "action must not be null");

        ReentrantLock lock = locks.get(carType);
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
