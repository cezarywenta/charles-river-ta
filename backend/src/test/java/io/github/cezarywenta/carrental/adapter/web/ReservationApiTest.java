package io.github.cezarywenta.carrental.adapter.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Exercises the real wired application (real ReservationService, real
 * InMemoryReservationRepository, real PerCarTypeLockManager) through the
 * HTTP layer, rather than mocking collaborators, so these tests double as a
 * check on the Spring wiring itself. A fixed Clock keeps "now" stable
 * regardless of the day the suite runs, so reservations dated in 2027 stay
 * reliably in the future. Each test also uses a distinct time window so
 * tests avoid interfering with each other via the shared singleton
 * repository, though this is not a hard isolation guarantee.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ReservationApiTest.FixedClockConfiguration.class)
class ReservationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReservationReturns201WithConfirmedReservationAndLocationHeader() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carType":"SEDAN","startAt":"2027-01-10T10:00:00","numberOfDays":2}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/reservations/")))
                .andExpect(jsonPath("$.carType").value("SEDAN"))
                .andExpect(jsonPath("$.startAt").value("2027-01-10T10:00:00"))
                .andExpect(jsonPath("$.endAt").value("2027-01-12T10:00:00"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.reservationId").exists());
    }

    @Test
    void createReservationReturns409WithProblemBodyWhenCarTypeIsFullyBooked() throws Exception {
        String requestBody = """
                {"carType":"VAN","startAt":"2027-02-10T10:00:00","numberOfDays":2}""";

        // default VAN capacity is 1
        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem:car-unavailable"))
                .andExpect(jsonPath("$.title").value("Car unavailable"))
                .andExpect(jsonPath("$.detail").value("No VAN is available for the selected period"));
    }

    @Test
    void createReservationReturns400ForNonPositiveNumberOfDays() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carType":"SEDAN","startAt":"2027-03-10T10:00:00","numberOfDays":0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"))
                .andExpect(jsonPath("$.detail").value("numberOfDays must be positive"));
    }

    @Test
    void createReservationReturns400ForStartInThePast() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carType":"SUV","startAt":"2025-01-01T10:00:00","numberOfDays":2}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"))
                .andExpect(jsonPath("$.detail").value("startAt must not be in the past"));
    }

    @Test
    void createReservationReturns400ForMissingCarType() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startAt":"2027-03-15T10:00:00","numberOfDays":1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"));
    }

    @Test
    void availabilityReturnsCountsForAllCarTypes() throws Exception {
        mockMvc.perform(get("/api/availability").param("startAt", "2027-04-01T10:00:00").param("numberOfDays", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startAt").value("2027-04-01T10:00:00"))
                .andExpect(jsonPath("$.endAt").value("2027-04-02T10:00:00"))
                .andExpect(jsonPath("$.availability", hasSize(3)));
    }

    @Test
    void availabilityReturns400ForNonPositiveNumberOfDays() throws Exception {
        mockMvc.perform(get("/api/availability").param("startAt", "2027-04-10T10:00:00").param("numberOfDays", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"));
    }

    @Test
    void malformedAvailabilityDateReturns400() throws Exception {
        mockMvc.perform(get("/api/availability").param("startAt", "not-a-date").param("numberOfDays", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"));
    }

    @Test
    void missingAvailabilityParameterReturns400() throws Exception {
        mockMvc.perform(get("/api/availability").param("numberOfDays", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"));
    }

    @Test
    void getUnknownReservationReturns404WithProblemBody() throws Exception {
        mockMvc.perform(get("/api/reservations/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:reservation-not-found"));
    }

    @Test
    void getReservationByIdReturnsItsCurrentState() throws Exception {
        String reservationId = createReservation("SEDAN", "2027-06-01T10:00:00", 2);

        mockMvc.perform(get("/api/reservations/{id}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void cancelUnknownReservationReturns404WithProblemBody() throws Exception {
        mockMvc.perform(patch("/api/reservations/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:reservation-not-found"))
                .andExpect(jsonPath("$.title").value("Reservation not found"));
    }

    @Test
    void patchReservationWithMalformedIdReturns400() throws Exception {
        mockMvc.perform(patch("/api/reservations/{id}", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"));
    }

    @Test
    void patchReservationWithUnsupportedStatusReturns400() throws Exception {
        String reservationId = createReservation("SEDAN", "2027-06-10T10:00:00", 2);

        mockMvc.perform(patch("/api/reservations/{id}", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"));
    }

    @Test
    void unknownReservationStatusValueReturns400() throws Exception {
        mockMvc.perform(patch("/api/reservations/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"UNKNOWN"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"));
    }

    @Test
    void cancelReservationSucceedsAndIsReflectedInListAndGet() throws Exception {
        String reservationId = createReservation("SUV", "2027-05-01T10:00:00", 2);

        mockMvc.perform(patch("/api/reservations/{id}", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(patch("/api/reservations/{id}", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem:cancellation-not-allowed"));

        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reservationId == '" + reservationId + "')].status")
                        .value("CANCELLED"));

        mockMvc.perform(get("/api/reservations/{id}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void listReservationsIsSortedByStartDate() throws Exception {
        String laterId = createReservation("SEDAN", "2027-07-20T10:00:00", 1);
        String earlierId = createReservation("SEDAN", "2027-07-10T10:00:00", 1);

        String response = mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode reservations = objectMapper.readTree(response);
        int earlierIndex = indexOfReservation(reservations, earlierId);
        int laterIndex = indexOfReservation(reservations, laterId);

        assertTrue(earlierIndex >= 0 && laterIndex >= 0);
        assertTrue(earlierIndex < laterIndex);
    }

    private static int indexOfReservation(JsonNode reservations, String reservationId) {
        for (int i = 0; i < reservations.size(); i++) {
            if (reservations.get(i).get("reservationId").asString().equals(reservationId)) {
                return i;
            }
        }
        return -1;
    }

    private String createReservation(String carType, String startAt, int numberOfDays) throws Exception {
        String response = mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carType\":\"%s\",\"startAt\":\"%s\",\"numberOfDays\":%d}"
                                .formatted(carType, startAt, numberOfDays)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("reservationId").asString();
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
