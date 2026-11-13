package ifsuldeminas.edu.br.ifsystem.reservation.service;

import ifsuldeminas.edu.br.ifsystem.common.*;
import ifsuldeminas.edu.br.ifsystem.keycontrol.repository.KeyMovementRepository;
import ifsuldeminas.edu.br.ifsystem.reservation.api.ReservationDtos.*;
import ifsuldeminas.edu.br.ifsystem.reservation.domain.*;
import ifsuldeminas.edu.br.ifsystem.reservation.repository.ReservationRepository;
import ifsuldeminas.edu.br.ifsystem.room.provider.RoomDataProvider;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.*;

import org.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReservationService {
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRepository repository;
    private final RoomDataProvider rooms;
    private final CurrentUser currentUser;
    private final RoomOperationLock lock;
    private final KeyMovementRepository movements;

    public ReservationService(
            ReservationRepository repository,
            RoomDataProvider rooms,
            CurrentUser currentUser,
            RoomOperationLock lock,
            KeyMovementRepository movements) {
        this.repository = repository;
        this.rooms = rooms;
        this.currentUser = currentUser;
        this.lock = lock;
        this.movements = movements;
    }

    @Transactional
    public ReservationResponse create(CreateReservation request) {
        var user = currentUser.require();
        if (request == null || request.roomId() == null || request.roomId().isBlank())
            throw error(HttpStatus.BAD_REQUEST, "Room is required");
        validatePeriod(request.startTime(), request.endTime());
        if (rooms.findById(request.roomId()).isEmpty())
            throw error(HttpStatus.NOT_FOUND, "Room not found");
        lock.acquire(request.roomId());
        return ReservationResponse.from(
                repository.save(
                        new Reservation(
                                request.roomId(), user, request.startTime(), request.endTime())));
    }

    @Transactional
    public ReservationResponse approve(UUID id) {
        var operator = currentUser.requireOperator();
        Reservation reservation = locked(id);
        requirePending(reservation);
        if (rooms.findById(reservation.getRoomId()).isEmpty())
            throw error(HttpStatus.NOT_FOUND, "Room not found");
        if (!repository
                .findOverlapping(
                        reservation.getRoomId(),
                        ReservationStatus.APPROVED,
                        reservation.getStartTime(),
                        reservation.getEndTime())
                .isEmpty()) {
            log.info("Reservation approval conflict: {}", id);
            throw error(HttpStatus.CONFLICT, "An approved reservation overlaps this period");
        }
        if (reservation.getRequester().getPerfil() == Perfil.ALUNO
                && repository
                        .findOverlapping(
                                reservation.getRoomId(),
                                ReservationStatus.PENDING,
                                reservation.getStartTime(),
                                reservation.getEndTime())
                        .stream()
                        .anyMatch(other -> other.getRequester().getPerfil() == Perfil.PROFESSOR)) {
            log.info("Reservation approval deferred for teacher priority: {}", id);
            throw error(HttpStatus.CONFLICT, "A pending teacher request has priority");
        }
        reservation.review(ReservationStatus.APPROVED, operator, null);
        log.info("Reservation approved: {}", id);
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse reject(UUID id, RejectReservation request) {
        var operator = currentUser.requireOperator();
        if (request == null
                || request.reason() == null
                || request.reason().isBlank()
                || request.reason().length() > 1000)
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "A rejection reason of at most 1000 characters is required");
        Reservation reservation = locked(id);
        requirePending(reservation);
        reservation.review(ReservationStatus.REJECTED, operator, request.reason().strip());
        log.info("Reservation rejected: {}", id);
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse cancel(UUID id) {
        var user = currentUser.require();
        Reservation reservation = locked(id);
        requireAccess(reservation, user);
        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.APPROVED)
            throw error(HttpStatus.CONFLICT, "Reservation cannot be cancelled");
        if (movements.existsByReservationIdAndCheckinAtIsNull(id))
            throw error(HttpStatus.CONFLICT, "Return the key before cancelling");
        reservation.cancel();
        log.info("Reservation cancelled: {}", id);
        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationResponse find(UUID id) {
        var user = currentUser.require();
        var reservation = get(id);
        requireAccess(reservation, user);
        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> search(
            String roomId,
            UUID requesterId,
            ReservationStatus status,
            Instant start,
            Instant end,
            int page,
            int size) {
        var user = currentUser.require();
        if (!currentUser.isOperator(user)) {
            if (requesterId != null && !requesterId.equals(user.getId()))
                throw error(HttpStatus.FORBIDDEN, "Access denied");
            requesterId = user.getId();
        }
        start = start == null ? Instant.parse("1900-01-01T00:00:00Z") : start;
        end = end == null ? Instant.parse("9999-01-01T00:00:00Z") : end;
        validatePeriod(start, end);
        if (page < 0 || size < 1 || size > 100)
            throw error(HttpStatus.BAD_REQUEST, "Invalid pagination");
        return repository
                .search(
                        roomId,
                        requesterId,
                        status,
                        start,
                        end,
                        PageRequest.of(
                                page, size, Sort.by("startTime").ascending().and(Sort.by("id"))))
                .map(ReservationResponse::from);
    }

    private Reservation locked(UUID id) {
        Reservation reservation = get(id);
        lock.acquire(reservation.getRoomId());
        // Refresh after waiting: another transaction may already have reviewed the same entity.
        entityManager.refresh(reservation);
        return reservation;
    }

    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    private Reservation get(UUID id) {
        return repository
                .findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Reservation not found"));
    }

    private void requirePending(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING)
            throw error(HttpStatus.CONFLICT, "Reservation is not pending");
    }

    private void requireAccess(Reservation reservation, Usuario user) {
        if (!currentUser.isOperator(user)
                && !reservation.getRequester().getId().equals(user.getId()))
            throw error(HttpStatus.FORBIDDEN, "Access denied");
    }

    public static void validatePeriod(Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end))
            throw error(HttpStatus.BAD_REQUEST, "Invalid time interval");
    }

    private static ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
