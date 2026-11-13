package ifsuldeminas.edu.br.ifsystem.room.api;

import ifsuldeminas.edu.br.ifsystem.room.domain.Room;
import ifsuldeminas.edu.br.ifsystem.room.service.RoomService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @GetMapping
    public List<Room> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Room find(@PathVariable String id) {
        return service.find(id);
    }

    @GetMapping("/{id}/availability")
    public RoomService.Availability availability(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.availability(id, date);
    }
}
