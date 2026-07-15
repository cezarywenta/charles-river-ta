package io.github.cezarywenta.carrental.application;

import io.github.cezarywenta.carrental.domain.CarType;
import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationPeriod;
import java.util.Objects;

/**
 * Result of ReservationService.reserve(). "No car available" is an expected
 * business outcome, not an error, so it is modeled as a sealed result
 * instead of an exception.
 */
public sealed interface ReservationResult permits
        ReservationResult.ReservationConfirmed,
        ReservationResult.CarUnavailable {

    record ReservationConfirmed(Reservation reservation) implements ReservationResult {
        public ReservationConfirmed {
            Objects.requireNonNull(reservation, "reservation must not be null");
        }
    }

    record CarUnavailable(CarType carType, ReservationPeriod period) implements ReservationResult {
        public CarUnavailable {
            Objects.requireNonNull(carType, "carType must not be null");
            Objects.requireNonNull(period, "period must not be null");
        }
    }
}
