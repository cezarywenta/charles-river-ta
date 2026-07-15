package io.github.cezarywenta.carrental.application;

import io.github.cezarywenta.carrental.domain.AvailabilityPolicy;
import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationId;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Synchronous use cases only: find overlapping -> check capacity -> save.
 * Concurrent requests can both observe capacity as free before either saves,
 * causing overbooking; that atomicity guarantee is added on top of this
 * class in a later step rather than baked into it here.
 */
public class ReservationService {

    private final ReservationRepository repository;
    private final FleetCapacity fleetCapacity;
    private final Clock clock;

    public ReservationService(
            ReservationRepository repository,
            FleetCapacity fleetCapacity,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.fleetCapacity = Objects.requireNonNull(fleetCapacity, "fleetCapacity must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ReservationResult reserve(ReserveCarCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        List<Reservation> overlapping = repository.findOverlapping(command.carType(), command.period());
        long activeOverlapping = overlapping.stream().filter(Reservation::isActive).count();
        int capacity = fleetCapacity.capacityFor(command.carType());

        if (!AvailabilityPolicy.isAvailable(capacity, activeOverlapping)) {
            return new ReservationResult.CarUnavailable(command.carType(), command.period());
        }

        Reservation reservation = Reservation.confirmed(
                ReservationId.generate(),
                command.customerId(),
                command.carType(),
                command.period(),
                clock.instant());
        repository.save(reservation);
        return new ReservationResult.ReservationConfirmed(reservation);
    }

    /**
     * Requires the requesting customerId to match the reservation's owner.
     * A mismatch is reported as ReservationNotFound rather than a separate
     * "forbidden" outcome, so a caller cannot use this endpoint to probe
     * whether a given reservation id belongs to someone else.
     */
    public CancellationResult cancel(CancelReservationCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Optional<Reservation> found = repository.findById(command.reservationId());
        if (found.isEmpty() || !found.get().customerId().equals(command.customerId())) {
            return new CancellationResult.ReservationNotFound(command.reservationId());
        }

        Reservation reservation = found.get();
        LocalDateTime now = LocalDateTime.now(clock);
        if (!reservation.isCancellable(now)) {
            return new CancellationResult.CancellationNotAllowed(reservation.id());
        }

        Reservation cancelled = reservation.cancelAt(now);
        repository.save(cancelled);
        return new CancellationResult.CancellationConfirmed(cancelled);
    }
}
