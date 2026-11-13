package ifsuldeminas.edu.br.ifsystem.aproveitamento.api;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.AcaoRequest;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.CriarSolicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.HistoricoItem;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.SolicitacaoDetalhe;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.SolicitacaoResumo;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Solicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.FluxoStatusService;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.SolicitacaoService;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/aproveitamentos")
public class AproveitamentoController {
    static final String HEADER_USUARIO_ID = "X-Usuario-Id";
    static final String HEADER_PERFIL = "X-Perfil";

    private final SolicitacaoService solicitacoes;
    private final FluxoStatusService fluxo;

    public AproveitamentoController(SolicitacaoService solicitacoes, FluxoStatusService fluxo) {
        this.solicitacoes = solicitacoes;
        this.fluxo = fluxo;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoResumo> criar(@Valid @RequestBody CriarSolicitacao dados,
                                                   HttpServletRequest request) {
        Usuario usuario = usuarioAtual(request);
        if (usuario.perfil() != Perfil.ALUNO)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas aluno pode criar solicitacao");
        if (!usuario.id().equals(dados.alunoId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solicitacao pertence a outro aluno");

        Solicitacao criada = solicitacoes.criar(dados);
        return ResponseEntity.created(URI.create("/aproveitamentos/" + criada.getId()))
                .body(SolicitacaoResumo.de(criada));
    }

    @GetMapping("/{id}")
    public SolicitacaoDetalhe detalhar(@PathVariable UUID id, HttpServletRequest request) {
        Usuario usuario = usuarioAtual(request);
        exigirDonoSeAluno(usuario, solicitacoes.buscar(id).getAlunoId());
        return solicitacoes.detalhar(id);
    }

    @GetMapping
    public List<SolicitacaoResumo> listar(@RequestParam(required = false) StatusSolicitacao status,
                                          @RequestParam(required = false) UUID alunoId,
                                          HttpServletRequest request) {
        Usuario usuario = usuarioAtual(request);
        UUID filtroAluno = usuario.perfil() == Perfil.ALUNO ? usuario.id() : alunoId;
        return solicitacoes.listar(status, filtroAluno);
    }

    @GetMapping("/{id}/historico")
    public List<HistoricoItem> historico(@PathVariable UUID id, HttpServletRequest request) {
        Usuario usuario = usuarioAtual(request);
        exigirDonoSeAluno(usuario, solicitacoes.buscar(id).getAlunoId());
        return solicitacoes.historico(id);
    }

    @PostMapping("/{id}/{acao}")
    public SolicitacaoResumo executar(@PathVariable UUID id, @PathVariable String acao,
                                      @Valid @RequestBody(required = false) AcaoRequest corpo,
                                      HttpServletRequest request) {
        Usuario usuario = usuarioAtual(request);
        String observacao = corpo == null ? null : corpo.observacao();
        return SolicitacaoResumo.de(fluxo.executar(id, acao, usuario.id(), usuario.perfil(), observacao));
    }

    private static void exigirDonoSeAluno(Usuario usuario, UUID alunoId) {
        if (usuario.perfil() == Perfil.ALUNO && !usuario.id().equals(alunoId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solicitacao pertence a outro aluno");
    }

    /** Identidade provisoria, lida dos headers ate a autenticacao real existir. */
    private Usuario usuarioAtual(HttpServletRequest request) {
        String id = request.getHeader(HEADER_USUARIO_ID);
        String perfil = request.getHeader(HEADER_PERFIL);
        if (id == null || perfil == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identidade ausente");
        try {
            return new Usuario(UUID.fromString(id.trim()), Perfil.valueOf(perfil.trim()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identidade invalida");
        }
    }

    private record Usuario(UUID id, Perfil perfil) {
    }
}
