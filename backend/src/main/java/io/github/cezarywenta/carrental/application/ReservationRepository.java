package io.github.cezarywenta.carrental.application;

import io.github.cezarywenta.carrental.domain.CarType;
import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationId;
import io.github.cezarywenta.carrental.domain.ReservationPeriod;
import java.util.List;
import java.util.Optional;

/**
 * Port for reservation storage: in-memory today, potentially a JPA
 * repository in a production evolution.
 */
public interface ReservationRepository {

    List<Reservation> findOverlapping(CarType carType, ReservationPeriod period);

    List<Reservation> findByCustomerId(String customerId);

    Optional<Reservation> findById(ReservationId id);

    void save(Reservation reservation);
}
