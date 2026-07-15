package io.github.cezarywenta.carrental.configuration;

import io.github.cezarywenta.carrental.adapter.inmem.InMemoryReservationRepository;
import io.github.cezarywenta.carrental.adapter.inmem.PerCarTypeLockManager;
import io.github.cezarywenta.carrental.application.FleetCapacity;
import io.github.cezarywenta.carrental.application.LockManager;
import io.github.cezarywenta.carrental.application.ReservationQueryService;
import io.github.cezarywenta.carrental.application.ReservationRepository;
import io.github.cezarywenta.carrental.application.ReservationService;
import io.github.cezarywenta.carrental.domain.CarType;
import java.time.Clock;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires each adapter as a single Spring-managed singleton. ReservationRepository
 * and LockManager in particular must stay singletons: two InMemoryReservationRepository
 * instances would each hold separate reservations, and two PerCarTypeLockManager
 * instances would each hold a separate set of locks, silently defeating the
 * concurrency guarantee ReservationService relies on.
 */
@Configuration
@EnableConfigurationProperties(FleetProperties.class)
public class RentalConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    FleetCapacity fleetCapacity(FleetProperties properties) {
        return new FleetCapacity(Map.of(
                CarType.SEDAN, properties.sedan(),
                CarType.SUV, properties.suv(),
                CarType.VAN, properties.van()));
    }

    @Bean
    ReservationRepository reservationRepository() {
        return new InMemoryReservationRepository();
    }

    @Bean
    LockManager lockManager() {
        return new PerCarTypeLockManager();
    }

    @Bean
    ReservationService reservationService(
            ReservationRepository reservationRepository,
            FleetCapacity fleetCapacity,
            LockManager lockManager,
            Clock clock) {
        return new ReservationService(reservationRepository, fleetCapacity, lockManager, clock);
    }

    @Bean
    ReservationQueryService reservationQueryService(
            ReservationRepository reservationRepository, FleetCapacity fleetCapacity) {
        return new ReservationQueryService(reservationRepository, fleetCapacity);
    }
}
