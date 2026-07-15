package io.github.cezarywenta.carrental.adapter.inmem;

import io.github.cezarywenta.carrental.application.ReservationRepository;
import io.github.cezarywenta.carrental.domain.CarType;
import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationId;
import io.github.cezarywenta.carrental.domain.ReservationPeriod;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backed by a ConcurrentHashMap so individual reads/writes are thread-safe.
 * That alone does not make find-then-save sequences atomic; the caller
 * (ReservationService, via LockManager) is responsible for that boundary.
 */
public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<ReservationId, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public List<Reservation> findOverlapping(CarType carType, ReservationPeriod period) {
        Objects.requireNonNull(carType, "carType must not be null");
        Objects.requireNonNull(period, "period must not be null");
        return reservations.values().stream()
                .filter(reservation -> reservation.carType() == carType && reservation.period().overlaps(period))
                .toList();
    }

    @Override
    public List<Reservation> findByCustomerId(String customerId) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        return reservations.values().stream()
                .filter(reservation -> reservation.customerId().equals(customerId))
                .toList();
    }

    @Override
    public Optional<Reservation> findById(ReservationId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(reservations.get(id));
    }

    @Override
    public void save(Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation must not be null");
        reservations.put(reservation.id(), reservation);
    }
}
