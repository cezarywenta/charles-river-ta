package io.github.cezarywenta.carrental.adapter.web;

import io.github.cezarywenta.carrental.domain.ReservationStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Only a transition to CANCELLED is currently supported; any other status
 * value is syntactically valid but rejected as a 400 by the controller.
 */
public record UpdateReservationStatusRequest(@NotNull(message = "status must not be null") ReservationStatus status) {
}
