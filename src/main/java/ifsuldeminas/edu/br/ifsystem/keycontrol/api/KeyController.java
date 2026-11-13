package ifsuldeminas.edu.br.ifsystem.keycontrol.api;

import ifsuldeminas.edu.br.ifsystem.keycontrol.api.KeyDtos.*;
import ifsuldeminas.edu.br.ifsystem.keycontrol.service.KeyService;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/keys")
public class KeyController {
    private final KeyService service;

    public KeyController(KeyService service) {
        this.service = service;
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse checkout(@RequestBody Checkout request) {
        return service.checkout(request);
    }

    @PostMapping("/checkin")
    public MovementResponse checkin(@RequestBody Checkin request) {
        return service.checkin(request);
    }

    @GetMapping("/status")
    public List<KeyStatus> status() {
        return service.status();
    }

    @GetMapping("/history")
    public Page<MovementResponse> history(
            @RequestParam(required = false) String roomId,
            @RequestParam(required = false) UUID responsibleUserId,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.history(roomId, responsibleUserId, start, end, page, size);
    }
}
