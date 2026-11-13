package ifsuldeminas.edu.br.ifsystem;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ifsuldeminas.edu.br.ifsystem.keycontrol.api.KeyDtos.*;
import ifsuldeminas.edu.br.ifsystem.keycontrol.repository.KeyMovementRepository;
import ifsuldeminas.edu.br.ifsystem.keycontrol.service.KeyService;
import ifsuldeminas.edu.br.ifsystem.reservation.api.ReservationDtos.*;
import ifsuldeminas.edu.br.ifsystem.reservation.domain.*;
import ifsuldeminas.edu.br.ifsystem.reservation.repository.ReservationRepository;
import ifsuldeminas.edu.br.ifsystem.reservation.service.ReservationService;
import ifsuldeminas.edu.br.ifsystem.room.service.RoomService;
import ifsuldeminas.edu.br.ifsystem.security.JwtService;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.*;
import ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest(properties = "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect")
@org.springframework.context.annotation.Import(ModulePostgresTest.DatabaseConfiguration.class)
@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext
class ModulePostgresTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class DatabaseConfiguration {
        @org.springframework.context.annotation.Bean(destroyMethod = "close")
        EmbeddedPostgres testPostgres() throws java.io.IOException {
            return EmbeddedPostgres.builder().setPort(0).start();
        }

        @org.springframework.context.annotation.Bean(destroyMethod = "close")
        com.zaxxer.hikari.HikariDataSource dataSource(EmbeddedPostgres testPostgres) {
            var source = new com.zaxxer.hikari.HikariDataSource();
            source.setJdbcUrl(testPostgres.getJdbcUrl("postgres", "postgres"));
            source.setUsername("postgres");
            source.setPassword("postgres");
            return source;
        }
    }

    @Autowired UsuarioRepository users;
    @Autowired ReservationRepository reservations;
    @Autowired KeyMovementRepository movements;
    @Autowired ReservationService service;
    @Autowired KeyService keys;
    @Autowired RoomService rooms;
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    Usuario student, teacher, operator;
    Instant start, end;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        movements.deleteAll();
        reservations.deleteAll();
        users.deleteAll();
        student = user("11111111111", Perfil.ALUNO);
        teacher = user("22222222222", Perfil.PROFESSOR);
        operator = user("33333333333", Perfil.SERVIDOR);
        start = Instant.now().minusSeconds(60);
        end = Instant.now().plusSeconds(3600);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private Usuario user(String cpf, Perfil role) {
        return users.save(
                new Usuario(
                        "Test",
                        cpf,
                        cpf + "@ifsuldeminas.edu.br",
                        cpf,
                        "unused-test-password",
                        null,
                        null,
                        null,
                        role));
    }

    private void login(Usuario user) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private UUID create(Usuario user, Instant from, Instant to) {
        login(user);
        return service.create(new CreateReservation("sala-114", from, to)).id();
    }

    private UUID approved() {
        UUID id = create(student, start, end);
        login(operator);
        service.approve(id);
        return id;
    }

    private void expectStatus(int expected, org.junit.jupiter.api.function.Executable action) {
        assertEquals(
                expected,
                assertThrows(ResponseStatusException.class, action).getStatusCode().value());
    }

    @Test
    void validRequestIsPendingAndReferencesAuthenticatedUser() {
        UUID id = create(student, start, end);
        var response = service.find(id);
        assertEquals(student.getId(), response.requesterId());
        assertEquals(ReservationStatus.PENDING, response.status());
    }

    @Test
    void missingRoomIsRejected() {
        login(student);
        expectStatus(404, () -> service.create(new CreateReservation("missing", start, end)));
    }

    @Test
    void invalidIntervalsAreRejected() {
        login(student);
        expectStatus(400, () -> service.create(new CreateReservation("sala-114", end, start)));
        expectStatus(400, () -> service.create(new CreateReservation("sala-114", start, start)));
        expectStatus(400, () -> service.create(new CreateReservation("sala-114", null, end)));
    }

    @Test
    void authorizedApprovalRecordsReviewer() {
        UUID id = approved();
        var response = service.find(id);
        assertEquals(ReservationStatus.APPROVED, response.status());
        assertEquals(operator.getId(), response.reviewedBy());
        assertNotNull(response.reviewedAt());
    }

    @Test
    void studentsCannotApproveOrReject() {
        UUID id = create(student, start, end);
        expectStatus(403, () -> service.approve(id));
        expectStatus(403, () -> service.reject(id, new RejectReservation("No")));
    }

    @Test
    void rejectionIsRecordedAndRequiresReason() {
        UUID id = create(student, start, end);
        login(operator);
        expectStatus(400, () -> service.reject(id, new RejectReservation(" ")));
        var rejected = service.reject(id, new RejectReservation("Room maintenance"));
        assertEquals(ReservationStatus.REJECTED, rejected.status());
        assertEquals("Room maintenance", rejected.rejectionReason());
        expectStatus(409, () -> service.approve(id));
    }

    @Test
    void teacherHasPriorityOverPendingStudent() {
        UUID studentId = create(student, start, end), teacherId = create(teacher, start, end);
        login(operator);
        expectStatus(409, () -> service.approve(studentId));
        service.approve(teacherId);
        assertEquals(ReservationStatus.PENDING, service.find(studentId).status());
    }

    @Test
    void teacherDoesNotDisplaceApprovedReservation() {
        UUID studentId = approved();
        UUID teacherId = create(teacher, start, end);
        login(operator);
        expectStatus(409, () -> service.approve(teacherId));
        assertEquals(ReservationStatus.APPROVED, service.find(studentId).status());
    }

    @Test
    void adjacentReservationsAreAllowed() {
        approved();
        UUID id = create(student, end, end.plusSeconds(3600));
        login(operator);
        assertEquals(ReservationStatus.APPROVED, service.approve(id).status());
    }

    @Test
    void availabilityUsesApprovedReservationsAndClipsAtDayBoundaries() {
        LocalDate date = LocalDate.of(2026, 11, 13);
        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        Instant midnight = date.atStartOfDay(zone).toInstant();
        UUID id = create(student, midnight.minusSeconds(3600), midnight.plusSeconds(3600));
        create(student, midnight.plusSeconds(7200), midnight.plusSeconds(10800));
        login(operator);
        service.approve(id);
        var availability = rooms.availability("sala-114", date);
        assertEquals(1, availability.occupied().size());
        assertEquals(midnight, availability.occupied().getFirst().startTime());
        assertEquals(midnight.plusSeconds(3600), availability.available().getFirst().startTime());
    }

    @Test
    void emptyDayIsEntirelyAvailableAndUnknownRoomIs404() {
        assertEquals(
                1, rooms.availability("sala-114", LocalDate.of(2030, 1, 1)).available().size());
        assertTrue(rooms.availability("sala-114", LocalDate.of(2030, 1, 1)).occupied().isEmpty());
        expectStatus(404, () -> rooms.find("missing"));
    }

    @Test
    void concurrentConflictingApprovalsHaveExactlyOneWinner() throws Exception {
        UUID first = create(student, start, end), second = create(student, start, end);
        assertEquals(
                List.of(200, 409),
                race(() -> service.approve(first), () -> service.approve(second)));
        assertEquals(
                1,
                reservations
                        .findOverlapping("sala-114", ReservationStatus.APPROVED, start, end)
                        .size());
    }

    @Test
    void concurrentApprovalOfSameReservationHasOneWinner() throws Exception {
        UUID id = create(student, start, end);
        assertEquals(List.of(200, 409), race(() -> service.approve(id), () -> service.approve(id)));
    }

    @Test
    void checkoutReturnAndHistoryPreserveAudit() {
        UUID id = approved();
        var movement = keys.checkout(new Checkout(id));
        assertEquals(student.getId(), movement.responsibleUserId());
        assertEquals(operator.getId(), movement.processedByUserId());
        assertTrue(
                keys.status().stream()
                        .anyMatch(
                                s ->
                                        s.roomId().equals("sala-114")
                                                && s.status().equals("CHECKED_OUT")));
        expectStatus(409, () -> keys.checkout(new Checkout(id)));
        expectStatus(409, () -> service.cancel(id));
        var returned = keys.checkin(new Checkin(movement.id()));
        assertEquals("RETURNED", returned.status());
        assertEquals(operator.getId(), returned.returnedByUserId());
        expectStatus(409, () -> keys.checkin(new Checkin(movement.id())));
        assertEquals(
                1, keys.history("sala-114", student.getId(), null, null, 0, 20).getTotalElements());
        assertTrue(keys.status().stream().allMatch(s -> s.status().equals("AVAILABLE")));
    }

    @Test
    void checkoutRequiresApprovedReservationAndCurrentPeriod() {
        UUID pending = create(student, start, end);
        login(operator);
        expectStatus(409, () -> keys.checkout(new Checkout(pending)));
        UUID future = create(student, end, end.plusSeconds(3600));
        login(operator);
        service.approve(future);
        expectStatus(409, () -> keys.checkout(new Checkout(future)));
        UUID past = create(student, start.minusSeconds(7200), start.minusSeconds(3600));
        login(operator);
        service.approve(past);
        expectStatus(409, () -> keys.checkout(new Checkout(past)));
        expectStatus(404, () -> keys.checkout(new Checkout(UUID.randomUUID())));
    }

    @Test
    void concurrentCheckoutsHaveExactlyOneWinner() throws Exception {
        UUID id = approved();
        assertEquals(
                List.of(200, 409),
                race(() -> keys.checkout(new Checkout(id)), () -> keys.checkout(new Checkout(id))));
        assertEquals(1, movements.findByCheckinAtIsNull().size());
    }

    @Test
    void concurrentReturnsHaveExactlyOneWinner() throws Exception {
        var movement = keys.checkout(new Checkout(approved()));
        assertEquals(
                List.of(200, 409),
                race(
                        () -> keys.checkin(new Checkin(movement.id())),
                        () -> keys.checkin(new Checkin(movement.id()))));
    }

    @Test
    void nonOperatorsCannotAccessKeyOperations() {
        UUID id = approved();
        login(student);
        expectStatus(403, () -> keys.checkout(new Checkout(id)));
        expectStatus(403, keys::status);
        expectStatus(403, () -> keys.history(null, null, null, null, 0, 20));
        expectStatus(403, () -> keys.checkin(new Checkin(UUID.randomUUID())));
    }

    @Test
    void reservationVisibilityAndCancellationRespectOwnership() {
        UUID id = create(student, start, end);
        login(teacher);
        expectStatus(403, () -> service.find(id));
        expectStatus(403, () -> service.cancel(id));
        assertEquals(0, service.search(null, null, null, null, null, 0, 20).getTotalElements());
        login(student);
        assertEquals(ReservationStatus.CANCELLED, service.cancel(id).status());
        expectStatus(409, () -> service.cancel(id));
    }

    @Test
    void historyFiltersByRoomResponsibleAndOverlappingPeriod() {
        var movement = keys.checkout(new Checkout(approved()));
        keys.checkin(new Checkin(movement.id()));
        assertEquals(0, keys.history("other", null, null, null, 0, 20).getTotalElements());
        assertEquals(0, keys.history(null, teacher.getId(), null, null, 0, 20).getTotalElements());
        assertEquals(1, keys.history(null, student.getId(), start, end, 0, 20).getTotalElements());
        expectStatus(400, () -> keys.history(null, null, end, start, 0, 20));
    }

    @Test
    void restEndpointsAcceptExistingJwtAndDoNotExposeUserSecrets() throws Exception {
        String token = jwt.gerar(student);
        mvc.perform(get("/api/rooms").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
        mvc.perform(get("/api/rooms/missing").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(
                        get("/api/rooms/sala-114/availability")
                                .param("date", "2026-11-13")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zone").value("America/Sao_Paulo"));
        mvc.perform(
                        post("/api/reservations")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(
                                        "{\"roomId\":\"sala-114\",\"startTime\":\"2026-11-13T10:00:00Z\",\"endTime\":\"2026-11-13T11:00:00Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requesterId").value(student.getId().toString()))
                .andExpect(jsonPath("$.requester").doesNotExist());
        mvc.perform(get("/api/keys/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousAndInvalidTokensAreRejected() throws Exception {
        mvc.perform(get("/api/rooms")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reservations").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedRequestsReturn400() throws Exception {
        var token = jwt.gerar(operator);
        mvc.perform(
                        get("/api/rooms/sala-114/availability")
                                .param("date", "invalid")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/reservations")
                                .contentType("application/json")
                                .content("{}")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/keys/checkout")
                                .contentType("application/json")
                                .content("{}")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentCancellationAndCheckoutCannotLeaveCancelledReservationWithActiveKey()
            throws Exception {
        UUID id = approved();
        assertEquals(
                List.of(200, 409),
                race(() -> service.cancel(id), () -> keys.checkout(new Checkout(id))));
        login(operator);
        if (service.find(id).status() == ReservationStatus.CANCELLED) {
            assertTrue(movements.findByCheckinAtIsNull().isEmpty());
        } else {
            assertEquals(1, movements.findByCheckinAtIsNull().size());
        }
    }

    @Test
    void operatorRestWorkflowSupportsReviewCheckoutReturnAndFilters() throws Exception {
        UUID id = create(student, start, end);
        String authorization = "Bearer " + jwt.gerar(operator);
        SecurityContextHolder.clearContext();
        mvc.perform(
                        get("/api/reservations")
                                .param("status", "PENDING")
                                .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(
                        post("/api/reservations/" + id + "/approve")
                                .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(get("/api/reservations/" + id).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewedBy").value(operator.getId().toString()));
        var result =
                mvc.perform(
                                post("/api/keys/checkout")
                                        .header("Authorization", authorization)
                                        .contentType("application/json")
                                        .content("{\"reservationId\":\"" + id + "\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("CHECKED_OUT"))
                        .andReturn();
        String movementId =
                com.jayway.jsonpath.JsonPath.read(
                        result.getResponse().getContentAsString(), "$.id");
        mvc.perform(
                        post("/api/keys/checkin")
                                .header("Authorization", authorization)
                                .contentType("application/json")
                                .content("{\"movementId\":\"" + movementId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
        mvc.perform(
                        get("/api/keys/history")
                                .param("roomId", "sala-114")
                                .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(
                        post("/api/reservations/" + id + "/cancel")
                                .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        UUID rejected = create(student, start, end);
        SecurityContextHolder.clearContext();
        mvc.perform(
                        post("/api/reservations/" + rejected + "/reject")
                                .header("Authorization", authorization)
                                .contentType("application/json")
                                .content("{\"reason\":\"Maintenance\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void registrationAndLoginReturnValidationDetails() throws Exception {
        mvc.perform(
                        post("/servidor/alunos")
                                .contentType("application/json")
                                .content(
                                        """
{"nome":"Teste", "cpf":"90000000004", "email":"teste@example.com",
 "matricula":"validation-test", "senha":"Password123"}
"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Email institucional invalido"));
        mvc.perform(
                        post("/servidor/alunos")
                                .contentType("application/json")
                                .content(
                                        """
{"nome":"Teste", "cpf":"90000000004", "email":"teste@ifsuldeminas.edu.br",
 "matricula":"validation-test", "senha":"123"}
"""))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.detail").value("A senha deve possuir ao menos 8 caracteres"));
        mvc.perform(post("/servidor/alunos").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Dados obrigatorios ausentes"));
        mvc.perform(
                        post("/auth/login")
                                .contentType("application/json")
                                .content("{\"cpf\":\"90000000004\",\"senha\":\"Password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciais invalidas"));
    }

    private List<Integer> race(Runnable first, Runnable second) throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Runnable action : List.of(first, second))
                futures.add(
                        pool.submit(
                                () -> {
                                    login(operator);
                                    try {
                                        barrier.await(10, TimeUnit.SECONDS);
                                        action.run();
                                        return 200;
                                    } catch (ResponseStatusException ex) {
                                        return ex.getStatusCode().value();
                                    } finally {
                                        SecurityContextHolder.clearContext();
                                    }
                                }));
            List<Integer> results = new ArrayList<>();
            for (var future : futures) results.add(future.get(20, TimeUnit.SECONDS));
            return results.stream().sorted().toList();
        }
    }
}
