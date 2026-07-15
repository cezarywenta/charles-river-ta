package io.github.cezarywenta.carrental.adapter.web;

import io.github.cezarywenta.carrental.application.CancelReservationCommand;
import io.github.cezarywenta.carrental.application.CancellationResult;
import io.github.cezarywenta.carrental.application.ReservationQueryService;
import io.github.cezarywenta.carrental.application.ReservationResult;
import io.github.cezarywenta.carrental.application.ReservationService;
import io.github.cezarywenta.carrental.application.ReserveCarCommand;
import io.github.cezarywenta.carrental.domain.Reservation;
import io.github.cezarywenta.carrental.domain.ReservationId;
import io.github.cezarywenta.carrental.domain.ReservationPeriod;
import io.github.cezarywenta.carrental.domain.ReservationStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private static final URI CAR_UNAVAILABLE = URI.create("urn:problem:car-unavailable");
    private static final URI CANCELLATION_NOT_ALLOWED = URI.create("urn:problem:cancellation-not-allowed");
    private static final URI RESERVATION_NOT_FOUND = URI.create("urn:problem:reservation-not-found");

    private final ReservationService reservationService;
    private final ReservationQueryService reservationQueryService;

    public ReservationController(
            ReservationService reservationService, ReservationQueryService reservationQueryService) {
        this.reservationService = reservationService;
        this.reservationQueryService = reservationQueryService;
    }

    @PostMapping
    public ResponseEntity<Object> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        ReservationPeriod period = ReservationPeriod.startingAt(request.startAt(), request.numberOfDays());
        ReserveCarCommand command = new ReserveCarCommand(DemoCustomer.ID, request.carType(), period);

        ReservationResult result = reservationService.reserve(command);

        return switch (result) {
            case ReservationResult.ReservationConfirmed confirmed -> {
                URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(confirmed.reservation().id())
                        .toUri();
                yield ResponseEntity.created(location).body(ReservationResponse.from(confirmed.reservation()));
            }
            case ReservationResult.CarUnavailable unavailable ->
                    ResponseEntity.status(HttpStatus.CONFLICT).body(carUnavailableProblem(unavailable));
        };
    }

    @GetMapping
    public List<ReservationResponse> listReservations() {
        return reservationQueryService.findByCustomerId(DemoCustomer.ID).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<Object> getReservation(@PathVariable String reservationId) {
        Optional<Reservation> found =
                reservationQueryService.findForCustomer(parseReservationId(reservationId), DemoCustomer.ID);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(reservationNotFoundProblem());
        }
        return ResponseEntity.ok(ReservationResponse.from(found.get()));
    }

    /**
     * Only {"status": "CANCELLED"} is a supported transition; any other
     * status value is rejected as a 400, not silently ignored.
     */
    @PatchMapping("/{reservationId}")
    public ResponseEntity<Object> updateReservationStatus(
            @PathVariable String reservationId, @Valid @RequestBody UpdateReservationStatusRequest request) {
        if (request.status() != ReservationStatus.CANCELLED) {
            throw new InvalidRequestException(
                    "status must be CANCELLED; no other transition is supported via this endpoint");
        }

        CancelReservationCommand command = new CancelReservationCommand(parseReservationId(reservationId), DemoCustomer.ID);
        CancellationResult result = reservationService.cancel(command);

        return switch (result) {
            case CancellationResult.CancellationConfirmed confirmed ->
                    ResponseEntity.ok(ReservationResponse.from(confirmed.reservation()));
            case CancellationResult.CancellationNotAllowed notAllowed ->
                    ResponseEntity.status(HttpStatus.CONFLICT).body(cancellationNotAllowedProblem());
            case CancellationResult.ReservationNotFound notFound ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body(reservationNotFoundProblem());
        };
    }

    private static ReservationId parseReservationId(String value) {
        try {
            return ReservationId.of(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("reservationId must be a valid UUID");
        }
    }

    private static ProblemDetail carUnavailableProblem(ReservationResult.CarUnavailable unavailable) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "No %s is available for the selected period".formatted(unavailable.carType()));
        problem.setTitle("Car unavailable");
        problem.setType(CAR_UNAVAILABLE);
        return problem;
    }

    private static ProblemDetail cancellationNotAllowedProblem() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The reservation can no longer be cancelled");
        problem.setTitle("Cancellation not allowed");
        problem.setType(CANCELLATION_NOT_ALLOWED);
        return problem;
    }

    private static ProblemDetail reservationNotFoundProblem() {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No reservation found with the given id");
        problem.setTitle("Reservation not found");
        problem.setType(RESERVATION_NOT_FOUND);
        return problem;
    }
}
