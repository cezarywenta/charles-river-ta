package io.github.cezarywenta.carrental.adapter.web;

import io.github.cezarywenta.carrental.application.ReservationQueryService;
import io.github.cezarywenta.carrental.domain.ReservationPeriod;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Advisory only: reflects current availability but reserves nothing. The
 * only authoritative capacity check happens inside ReservationService.reserve().
 */
@RestController
public class AvailabilityController {

    private final ReservationQueryService reservationQueryService;

    public AvailabilityController(ReservationQueryService reservationQueryService) {
        this.reservationQueryService = reservationQueryService;
    }

    @GetMapping("/api/availability")
    public AvailabilityResponse availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam int numberOfDays) {
        ReservationPeriod period = ReservationPeriod.startingAt(startAt, numberOfDays);

        var availability = reservationQueryService.availabilityFor(period).stream()
                .map(a -> new AvailabilityResponse.CarTypeAvailability(a.carType(), a.availableCount()))
                .toList();

        return new AvailabilityResponse(period.start(), period.end(), availability);
    }
}
