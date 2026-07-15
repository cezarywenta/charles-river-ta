package io.github.cezarywenta.carrental.application;

import io.github.cezarywenta.carrental.domain.AvailabilityPolicy;
import io.github.cezarywenta.carrental.domain.CarType;
import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationId;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates reservation creation and cancellation use cases. The
 * find-check-save sequence for both use cases runs inside the LockManager's
 * per-car-type critical section so concurrent requests cannot both observe
 * capacity as free and overbook the same car type.
 */
public class ReservationService {

    private final ReservationRepository repository;
    private final FleetCapacity fleetCapacity;
    private final LockManager lockManager;
    private final Clock clock;

    public ReservationService(
            ReservationRepository repository,
            FleetCapacity fleetCapacity,
            LockManager lockManager,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.fleetCapacity = Objects.requireNonNull(fleetCapacity, "fleetCapacity must not be null");
        this.lockManager = Objects.requireNonNull(lockManager, "lockManager must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ReservationResult reserve(ReserveCarCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        if (command.period().start().isBefore(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("startAt must not be in the past");
        }

        return lockManager.executeLocked(command.carType(), () -> {
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
        });
    }

    /**
     * Requires the requesting customerId to match the reservation's owner.
     * A mismatch is reported as ReservationNotFound rather than a separate
     * "forbidden" outcome, so a caller cannot use this endpoint to probe
     * whether a given reservation id belongs to someone else.
     *
     * <p>The first lookup only determines which car type's lock to acquire;
     * the repository port makes no guarantee that a reservation cannot
     * disappear or change owner between two reads, so existence and
     * ownership are re-checked authoritatively inside the critical section,
     * alongside the cancellation transition and save.
     */
    public CancellationResult cancel(CancelReservationCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Optional<Reservation> initial = repository.findById(command.reservationId());
        if (initial.isEmpty() || !belongsTo(initial.get(), command.customerId())) {
            return new CancellationResult.ReservationNotFound(command.reservationId());
        }

        CarType carType = initial.get().carType();
        return lockManager.executeLocked(carType, () -> {
            Optional<Reservation> current = repository.findById(command.reservationId());
            if (current.isEmpty() || !belongsTo(current.get(), command.customerId())) {
                return new CancellationResult.ReservationNotFound(command.reservationId());
            }

            Reservation reservation = current.get();
            LocalDateTime now = LocalDateTime.now(clock);
            if (!reservation.isCancellable(now)) {
                return new CancellationResult.CancellationNotAllowed(reservation.id());
            }

            Reservation cancelled = reservation.cancelAt(now);
            repository.save(cancelled);
            return new CancellationResult.CancellationConfirmed(cancelled);
        });
    }

    private static boolean belongsTo(Reservation reservation, String customerId) {
        return reservation.customerId().equals(customerId);
    }
}
