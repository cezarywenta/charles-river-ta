package io.github.cezarywenta.carrental.adapter.inmem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cezarywenta.carrental.application.FleetCapacity;
import io.github.cezarywenta.carrental.application.ReservationResult;
import io.github.cezarywenta.carrental.application.ReservationService;
import io.github.cezarywenta.carrental.application.ReserveCarCommand;
import io.github.cezarywenta.carrental.domain.CarType;
import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationPeriod;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * A controlled behavioral test, not a formal proof of thread safety: it
 * demonstrates that under this specific contention pattern, the per-car-type
 * lock prevents overbooking. It does not exhaustively verify the locking
 * implementation under all possible interleavings.
 */
class ConcurrentReservationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 10, 10, 0);
    private static final Clock CLOCK = Clock.fixed(START.minusDays(1).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Test
    void concurrentReservationsNeverExceedCapacity() throws Exception {
        int suvCapacity = 2;
        int attempts = 10;

        InMemoryReservationRepository repository = new InMemoryReservationRepository();
        PerCarTypeLockManager lockManager = new PerCarTypeLockManager();
        FleetCapacity fleetCapacity =
                new FleetCapacity(Map.of(CarType.SEDAN, 5, CarType.SUV, suvCapacity, CarType.VAN, 5));
        ReservationService service = new ReservationService(repository, fleetCapacity, lockManager, CLOCK);
        ReservationPeriod period = new ReservationPeriod(START, START.plusDays(3));

        CyclicBarrier barrier = new CyclicBarrier(attempts);
        List<Callable<ReservationResult>> tasks = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            String customerId = "customer-" + i;
            tasks.add(() -> {
                barrier.await();
                return service.reserve(new ReserveCarCommand(customerId, CarType.SUV, period));
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        List<Future<ReservationResult>> futures;
        try {
            futures = executor.invokeAll(tasks);
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        long confirmed = 0;
        long unavailable = 0;
        for (Future<ReservationResult> future : futures) {
            ReservationResult result = future.get();
            if (result instanceof ReservationResult.ReservationConfirmed) {
                confirmed++;
            } else if (result instanceof ReservationResult.CarUnavailable) {
                unavailable++;
            }
        }

        assertEquals(suvCapacity, confirmed);
        assertEquals(attempts - suvCapacity, unavailable);

        long activeInRepository = repository.findOverlapping(CarType.SUV, period).stream()
                .filter(Reservation::isActive)
                .count();
        assertEquals(suvCapacity, activeInRepository);
    }
}
