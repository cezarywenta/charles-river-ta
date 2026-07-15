package io.github.cezarywenta.carrental.adapter.web;

import io.github.cezarywenta.carrental.domain.CarType;
import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationStatus;
import java.time.Instant;
import java.time.LocalDateTime;

public record ReservationResponse(
        String reservationId,
        CarType carType,
        LocalDateTime startAt,
        LocalDateTime endAt,
        ReservationStatus status,
        Instant createdAt) {

    static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.id().toString(),
                reservation.carType(),
                reservation.period().start(),
                reservation.period().end(),
                reservation.status(),
                reservation.createdAt());
    }
}
