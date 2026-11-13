package ifsuldeminas.edu.br.ifsystem.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(
        basePackages = {
            "ifsuldeminas.edu.br.ifsystem.room",
            "ifsuldeminas.edu.br.ifsystem.reservation",
            "ifsuldeminas.edu.br.ifsystem.keycontrol",
            "ifsuldeminas.edu.br.ifsystem.usuario.api",
            "ifsuldeminas.edu.br.ifsystem.auth"
        })
public class ModuleExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handle(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(
                        ProblemDetail.forStatusAndDetail(
                                ex.getStatusCode(),
                                ex.getReason() == null ? "Request failed" : ex.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflict() {
        return ResponseEntity.status(409)
                .body(
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.CONFLICT, "Conflicting operation"));
    }
}
