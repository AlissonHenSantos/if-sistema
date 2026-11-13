package ifsuldeminas.edu.br.ifsystem.room.service;

import ifsuldeminas.edu.br.ifsystem.reservation.domain.ReservationStatus;
import ifsuldeminas.edu.br.ifsystem.reservation.repository.ReservationRepository;
import ifsuldeminas.edu.br.ifsystem.room.domain.Room;
import ifsuldeminas.edu.br.ifsystem.room.provider.RoomDataProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
public class RoomService {
    private final RoomDataProvider rooms;
    private final ReservationRepository reservations;
    private final ZoneId zone;

    public RoomService(
            RoomDataProvider rooms,
            ReservationRepository reservations,
            @Value("${app.rooms.zone:America/Sao_Paulo}") String zone) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.zone = ZoneId.of(zone);
    }

    public List<Room> findAll() {
        return rooms.findAll();
    }

    public Room find(String id) {
        return rooms.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    @Transactional(readOnly = true)
    public Availability availability(String id, LocalDate date) {
        find(id);
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<TimeSlot> occupied =
                reservations.findOverlapping(id, ReservationStatus.APPROVED, start, end).stream()
                        .map(
                                r ->
                                        new TimeSlot(
                                                r.getStartTime().isBefore(start)
                                                        ? start
                                                        : r.getStartTime(),
                                                r.getEndTime().isAfter(end) ? end : r.getEndTime()))
                        .sorted(Comparator.comparing(TimeSlot::startTime))
                        .toList();
        List<TimeSlot> free = new ArrayList<>();
        Instant cursor = start;
        for (TimeSlot slot : occupied) {
            if (cursor.isBefore(slot.startTime())) free.add(new TimeSlot(cursor, slot.startTime()));
            if (cursor.isBefore(slot.endTime())) cursor = slot.endTime();
        }
        if (cursor.isBefore(end)) free.add(new TimeSlot(cursor, end));
        return new Availability(id, date, zone.getId(), occupied, List.copyOf(free));
    }

    public record TimeSlot(Instant startTime, Instant endTime) {}

    public record Availability(
            String roomId,
            LocalDate date,
            String zone,
            List<TimeSlot> occupied,
            List<TimeSlot> available) {}
}
