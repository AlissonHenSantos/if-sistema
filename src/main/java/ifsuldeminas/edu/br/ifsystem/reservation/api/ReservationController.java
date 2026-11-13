package ifsuldeminas.edu.br.ifsystem.reservation.api;

import ifsuldeminas.edu.br.ifsystem.reservation.api.ReservationDtos.*;
import ifsuldeminas.edu.br.ifsystem.reservation.domain.ReservationStatus;
import ifsuldeminas.edu.br.ifsystem.reservation.service.ReservationService;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@RequestBody CreateReservation request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public ReservationResponse find(@PathVariable UUID id) {
        return service.find(id);
    }

    @GetMapping
    public Page<ReservationResponse> search(
            @RequestParam(required = false) String roomId,
            @RequestParam(required = false) UUID requesterId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(roomId, requesterId, status, start, end, page, size);
    }

    @PostMapping("/{id}/approve")
    public ReservationResponse approve(@PathVariable UUID id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    public ReservationResponse reject(
            @PathVariable UUID id, @RequestBody RejectReservation request) {
        return service.reject(id, request);
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }
}
