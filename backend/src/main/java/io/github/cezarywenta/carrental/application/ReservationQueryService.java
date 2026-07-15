package io.github.cezarywenta.carrental.application;

import io.github.cezarywenta.carrental.domain.AvailabilityPolicy;
import io.github.cezarywenta.carrental.domain.CarType;
import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationId;
import io.github.cezarywenta.carrental.domain.ReservationPeriod;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only use cases, kept separate from ReservationService so synchronized
 * commands and unsynchronized queries are not mixed in one class.
 */
public class ReservationQueryService {

    private final ReservationRepository repository;
    private final FleetCapacity fleetCapacity;

    public ReservationQueryService(ReservationRepository repository, FleetCapacity fleetCapacity) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.fleetCapacity = Objects.requireNonNull(fleetCapacity, "fleetCapacity must not be null");
    }

    public List<Reservation> findByCustomerId(String customerId) {
        requireCustomerId(customerId);
        return repository.findByCustomerId(customerId).stream()
                .sorted(Comparator.comparing(reservation -> reservation.period().start()))
                .toList();
    }

    public Optional<Reservation> findForCustomer(ReservationId reservationId, String customerId) {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        requireCustomerId(customerId);
        return repository.findById(reservationId).filter(reservation -> reservation.customerId().equals(customerId));
    }

    public List<CarTypeAvailability> availabilityFor(ReservationPeriod period) {
        Objects.requireNonNull(period, "period must not be null");
        return Arrays.stream(CarType.values())
                .map(carType -> {
                    long activeOverlapping = repository.findOverlapping(carType, period).stream()
                            .filter(Reservation::isActive)
                            .count();
                    int availableCount =
                            AvailabilityPolicy.availableCount(fleetCapacity.capacityFor(carType), activeOverlapping);
                    return new CarTypeAvailability(carType, availableCount);
                })
                .toList();
    }

    public record CarTypeAvailability(CarType carType, int availableCount) {
    }

    private static void requireCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
    }
}
