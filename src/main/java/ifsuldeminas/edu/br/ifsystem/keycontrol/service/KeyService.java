package ifsuldeminas.edu.br.ifsystem.keycontrol.service;

import ifsuldeminas.edu.br.ifsystem.common.*;
import ifsuldeminas.edu.br.ifsystem.keycontrol.api.KeyDtos.*;
import ifsuldeminas.edu.br.ifsystem.keycontrol.domain.KeyMovement;
import ifsuldeminas.edu.br.ifsystem.keycontrol.repository.KeyMovementRepository;
import ifsuldeminas.edu.br.ifsystem.reservation.domain.ReservationStatus;
import ifsuldeminas.edu.br.ifsystem.reservation.repository.ReservationRepository;
import ifsuldeminas.edu.br.ifsystem.room.provider.RoomDataProvider;

import jakarta.persistence.*;

import org.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
public class KeyService {
    private static final Logger log = LoggerFactory.getLogger(KeyService.class);
    private final KeyMovementRepository movements;
    private final ReservationRepository reservations;
    private final RoomDataProvider rooms;
    private final CurrentUser currentUser;
    private final RoomOperationLock lock;
    private final Clock clock;
    @PersistenceContext private EntityManager entityManager;

    public KeyService(
            KeyMovementRepository movements,
            ReservationRepository reservations,
            RoomDataProvider rooms,
            CurrentUser currentUser,
            RoomOperationLock lock,
            Clock clock) {
        this.movements = movements;
        this.reservations = reservations;
        this.rooms = rooms;
        this.currentUser = currentUser;
        this.lock = lock;
        this.clock = clock;
    }

    @Transactional
    public MovementResponse checkout(Checkout request) {
        var operator = currentUser.requireOperator();
        if (request == null || request.reservationId() == null)
            throw error(HttpStatus.BAD_REQUEST, "Reservation is required");
        var reservation =
                reservations
                        .findById(request.reservationId())
                        .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Reservation not found"));
        lock.acquire(reservation.getRoomId());
        entityManager.refresh(reservation);
        if (rooms.findById(reservation.getRoomId()).isEmpty())
            throw error(HttpStatus.NOT_FOUND, "Room not found");
        if (reservation.getStatus() != ReservationStatus.APPROVED)
            throw error(HttpStatus.CONFLICT, "Approved reservation required");
        Instant now = clock.instant();
        if (now.isBefore(reservation.getStartTime()) || !now.isBefore(reservation.getEndTime()))
            throw error(HttpStatus.CONFLICT, "Checkout is only allowed during the reservation");
        if (reservation.getRequester().getStatus()
                != ifsuldeminas.edu.br.ifsystem.usuario.domain.StatusUsuario.ATIVO)
            throw error(HttpStatus.CONFLICT, "Responsible user is inactive");
        if (movements.existsByRoomIdAndCheckinAtIsNull(reservation.getRoomId()))
            throw error(HttpStatus.CONFLICT, "Key is already checked out");
        var movement = movements.save(new KeyMovement(reservation, operator, now));
        log.info("Key checked out: {}", movement.getId());
        return MovementResponse.from(movement);
    }

    @Transactional
    public MovementResponse checkin(Checkin request) {
        var operator = currentUser.requireOperator();
        if (request == null || request.movementId() == null)
            throw error(HttpStatus.BAD_REQUEST, "Movement is required");
        var movement =
                movements
                        .findById(request.movementId())
                        .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Movement not found"));
        lock.acquire(movement.getRoomId());
        entityManager.refresh(movement);
        if (movement.getCheckinAt() != null)
            throw error(HttpStatus.CONFLICT, "Key already returned");
        movement.checkin(operator, clock.instant());
        log.info("Key returned: {}", movement.getId());
        return MovementResponse.from(movement);
    }

    @Transactional(readOnly = true)
    public List<KeyStatus> status() {
        currentUser.requireOperator();
        Map<String, KeyStatus> result = new TreeMap<>();
        for (var room : rooms.findAll())
            result.put(room.id(), new KeyStatus(room.id(), "AVAILABLE", null, null));
        // Include active movements whose room was subsequently removed from the external source.
        for (var movement : movements.findByCheckinAtIsNull())
            result.put(
                    movement.getRoomId(),
                    new KeyStatus(
                            movement.getRoomId(),
                            "CHECKED_OUT",
                            movement.getResponsibleUser().getId(),
                            movement.getCheckoutAt()));
        return List.copyOf(result.values());
    }

    @Transactional(readOnly = true)
    public Page<MovementResponse> history(
            String roomId, UUID responsibleUserId, Instant start, Instant end, int page, int size) {
        currentUser.requireOperator();
        start = start == null ? Instant.parse("1900-01-01T00:00:00Z") : start;
        end = end == null ? Instant.parse("9999-01-01T00:00:00Z") : end;
        if (!start.isBefore(end) || page < 0 || size < 1 || size > 100)
            throw error(HttpStatus.BAD_REQUEST, "Invalid period or pagination");
        return movements
                .search(
                        roomId,
                        responsibleUserId,
                        start,
                        end,
                        PageRequest.of(
                                page, size, Sort.by("checkoutAt").descending().and(Sort.by("id"))))
                .map(MovementResponse::from);
    }

    private static ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
