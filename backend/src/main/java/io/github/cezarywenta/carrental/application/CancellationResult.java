package io.github.cezarywenta.carrental.application;

import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationId;
import java.util.Objects;

/**
 * Result of ReservationService.cancel(). Mirrors ReservationResult: three
 * expected, non-exceptional outcomes for callers to handle explicitly.
 */
public sealed interface CancellationResult permits
        CancellationResult.CancellationConfirmed,
        CancellationResult.CancellationNotAllowed,
        CancellationResult.ReservationNotFound {

    record CancellationConfirmed(Reservation reservation) implements CancellationResult {
        public CancellationConfirmed {
            Objects.requireNonNull(reservation, "reservation must not be null");
        }
    }

    record CancellationNotAllowed(ReservationId reservationId) implements CancellationResult {
        public CancellationNotAllowed {
            Objects.requireNonNull(reservationId, "reservationId must not be null");
        }
    }

    record ReservationNotFound(ReservationId reservationId) implements CancellationResult {
        public ReservationNotFound {
            Objects.requireNonNull(reservationId, "reservationId must not be null");
        }
    }
}
