package io.github.cezarywenta.carrental.adapter.inmem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cezarywenta.carrental.domain.CarType;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PerCarTypeLockManagerTest {

    private final PerCarTypeLockManager lockManager = new PerCarTypeLockManager();

    @Test
    void executesActionAndReturnsItsResult() {
        String result = lockManager.executeLocked(CarType.SUV, () -> "ok");

        assertEquals("ok", result);
    }

    @Test
    void releasesLockEvenWhenActionThrows() throws Exception {
        assertThrows(RuntimeException.class, () -> lockManager.executeLocked(CarType.SUV, () -> {
            throw new RuntimeException("boom");
        }));

        // Must run on a different thread: ReentrantLock is reentrant, so the
        // same thread could re-enter a leaked lock and hide a missing unlock().
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> result = executor.submit(() -> lockManager.executeLocked(CarType.SUV, () -> "ok"));

            assertEquals("ok", result.get(2, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void differentCarTypesDoNotBlockEachOther() throws Exception {
        CountDownLatch suvHoldingLock = new CountDownLatch(1);
        CountDownLatch releaseSuv = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> suvTask = executor.submit(() -> lockManager.executeLocked(CarType.SUV, () -> {
                suvHoldingLock.countDown();
                awaitUninterruptibly(releaseSuv);
                return "suv-done";
            }));

            assertTrue(suvHoldingLock.await(2, TimeUnit.SECONDS));

            Future<String> sedanTask =
                    executor.submit(() -> lockManager.executeLocked(CarType.SEDAN, () -> "sedan-done"));
            assertEquals("sedan-done", sedanTask.get(2, TimeUnit.SECONDS));

            releaseSuv.countDown();
            assertEquals("suv-done", suvTask.get(2, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
