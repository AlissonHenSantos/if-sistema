package ifsuldeminas.edu.br.ifsystem.atendimento.api;

import ifsuldeminas.edu.br.ifsystem.atendimento.api.AtendimentoDtos.*;
import ifsuldeminas.edu.br.ifsystem.atendimento.service.AtendimentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/horarios-atendimento")
public class HorarioAtendimentoController {

    private final AtendimentoService service;

    public HorarioAtendimentoController(AtendimentoService service) {
        this.service = service;
    }

    /** Sem parâmetro: horários do próprio profissional logado. Com profissionalId: horários daquele profissional. */
    @GetMapping
    public List<HorarioResposta> listar(@RequestParam(required = false) UUID profissionalId) {
        return service.listarHorarios(profissionalId);
    }

    @PostMapping
    public ResponseEntity<HorarioResposta> criar(@RequestBody CriarHorario dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarHorario(dto));
    }
}
