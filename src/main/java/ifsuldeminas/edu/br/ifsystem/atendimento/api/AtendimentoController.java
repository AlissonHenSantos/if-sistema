package ifsuldeminas.edu.br.ifsystem.atendimento.api;

import ifsuldeminas.edu.br.ifsystem.atendimento.api.AtendimentoDtos.*;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.FiltroProfissional;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.StatusAtendimento;
import ifsuldeminas.edu.br.ifsystem.atendimento.service.AtendimentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/atendimentos")
public class AtendimentoController {

    private final AtendimentoService service;

    public AtendimentoController(AtendimentoService service) {
        this.service = service;
    }

    @GetMapping("/profissionais")
    public List<ProfissionalResposta> profissionais(@RequestParam(required = false) String busca,
                                                    @RequestParam(required = false) FiltroProfissional filtro) {
        return service.listarProfissionais(busca, filtro);
    }

    @GetMapping("/professores")
    public List<ProfissionalResposta> professores(@RequestParam(required = false) String busca) {
        return service.listarProfissionais(busca, FiltroProfissional.PROFESSORES);
    }

    @GetMapping("/painel")
    public ResumoPainel painel() {
        return service.painel();
    }

    @GetMapping("/meus")
    public List<AtendimentoResposta> meus(@RequestParam(required = false) StatusAtendimento status) {
        return service.meus(status);
    }

    @GetMapping("/{id}")
    public AtendimentoResposta buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<AtendimentoResposta> criar(@RequestBody CriarAtendimento dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @PatchMapping("/{id}/status")
    public AtendimentoResposta alterarStatus(@PathVariable UUID id, @RequestBody AlterarStatus dto) {
        return service.alterarStatus(id, dto);
    }

    @PatchMapping("/{id}/finalizar")
    public AtendimentoResposta finalizar(@PathVariable UUID id) {
        return service.finalizar(id);
    }

    @GetMapping("/{id}/mensagens")
    public List<MensagemResposta> mensagens(@PathVariable UUID id) {
        return service.listarMensagens(id);
    }

    @PostMapping("/{id}/mensagens")
    public ResponseEntity<MensagemResposta> enviar(@PathVariable UUID id, @RequestBody EnviarMensagem dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.enviarMensagem(id, dto));
    }
}
