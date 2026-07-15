package io.github.cezarywenta.carrental.application;

import io.github.cezarywenta.carrental.domain.CarType;
import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationId;
import io.github.cezarywenta.carrental.domain.ReservationPeriod;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Minimal, non-thread-safe ReservationRepository for exercising
 * ReservationService in isolation. The real, lock-protected in-memory
 * repository is built in a later step.
 */
class FakeReservationRepository implements ReservationRepository {

    private final List<Reservation> reservations = new ArrayList<>();

    @Override
    public List<Reservation> findOverlapping(CarType carType, ReservationPeriod period) {
        return reservations.stream()
                .filter(r -> r.carType() == carType && r.period().overlaps(period))
                .toList();
    }

    @Override
    public List<Reservation> findByCustomerId(String customerId) {
        return reservations.stream().filter(r -> r.customerId().equals(customerId)).toList();
    }

    @Override
    public Optional<Reservation> findById(ReservationId id) {
        return reservations.stream().filter(r -> r.id().equals(id)).findFirst();
    }

    @Override
    public void save(Reservation reservation) {
        reservations.removeIf(r -> r.id().equals(reservation.id()));
        reservations.add(reservation);
    }

    List<Reservation> all() {
        return List.copyOf(reservations);
    }
}
