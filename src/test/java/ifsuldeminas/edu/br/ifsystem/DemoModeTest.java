package ifsuldeminas.edu.br.ifsystem;

import static org.junit.jupiter.api.Assertions.*;

import ifsuldeminas.edu.br.ifsystem.common.DemoRoomOperationLock;
import ifsuldeminas.edu.br.ifsystem.common.RoomOperationLock;
import ifsuldeminas.edu.br.ifsystem.keycontrol.api.KeyDtos.*;
import ifsuldeminas.edu.br.ifsystem.keycontrol.repository.KeyMovementRepository;
import ifsuldeminas.edu.br.ifsystem.keycontrol.service.KeyService;
import ifsuldeminas.edu.br.ifsystem.reservation.api.ReservationDtos.*;
import ifsuldeminas.edu.br.ifsystem.reservation.repository.ReservationRepository;
import ifsuldeminas.edu.br.ifsystem.reservation.service.ReservationService;
import ifsuldeminas.edu.br.ifsystem.room.provider.RoomDataProvider;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.*;
import ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import javax.sql.DataSource;

@SpringBootTest
@ActiveProfiles("demo")
class DemoModeTest {
    @Autowired DataSource dataSource;
    @Autowired RoomOperationLock lock;
    @Autowired RoomDataProvider rooms;
    @Autowired UsuarioRepository users;
    @Autowired ReservationRepository reservations;
    @Autowired KeyMovementRepository movements;
    @Autowired ReservationService service;
    @Autowired KeyService keys;
    private Usuario operator;

    @BeforeEach
    void setup() {
        movements.deleteAll();
        reservations.deleteAll();
        users.deleteAll();
        operator =
                users.save(
                        new Usuario(
                                "Demo operator",
                                "90000000001",
                                "demo@ifsuldeminas.edu.br",
                                "demo",
                                "unused-password",
                                null,
                                null,
                                null,
                                Perfil.SERVIDOR));
        login();
    }

    private void login() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(operator, null, List.of()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void demoUsesOnlyInMemoryDatabaseAndLabelledFixture() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:ifsystem-demo"));
        }
        assertInstanceOf(DemoRoomOperationLock.class, lock);
        assertEquals(3, rooms.findAll().size());
        SecurityContextHolder.clearContext();

    }

    @Test
    void demoPreservesConflictsAndKeyLifecycle() throws Exception {
        var start = Instant.now().minusSeconds(60);
        var end = Instant.now().plusSeconds(3600);
        var first = service.create(new CreateReservation("sala-114", start, end)).id();
        var second = service.create(new CreateReservation("sala-114", start, end)).id();
        var barrier = new CyclicBarrier(2);
        List<Integer> results = new ArrayList<>();
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (var id : List.of(first, second)) {
                futures.add(
                        pool.submit(
                                () -> {
                                    login();
                                    try {
                                        barrier.await(10, TimeUnit.SECONDS);
                                        service.approve(id);
                                        return 200;
                                    } catch (ResponseStatusException ex) {
                                        return ex.getStatusCode().value();
                                    } finally {
                                        SecurityContextHolder.clearContext();
                                    }
                                }));
            }
            for (var future : futures) results.add(future.get(20, TimeUnit.SECONDS));
        }
        assertEquals(List.of(200, 409), results.stream().sorted().toList());
        var approved = service.find(first).status().name().equals("APPROVED") ? first : second;
        var movement = keys.checkout(new Checkout(approved));
        assertEquals("CHECKED_OUT", movement.status());
        assertEquals("RETURNED", keys.checkin(new Checkin(movement.id())).status());
        assertEquals(1, keys.history("sala-114", null, null, null, 0, 20).getTotalElements());
        assertTrue(keys.status().stream().allMatch(key -> key.status().equals("AVAILABLE")));
    }
}
