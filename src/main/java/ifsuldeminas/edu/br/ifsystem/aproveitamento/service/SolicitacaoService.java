package ifsuldeminas.edu.br.ifsystem.aproveitamento.service;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.CriarSolicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.DisciplinaCursadaEntrada;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.HistoricoItem;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.SolicitacaoDetalhe;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.SolicitacaoResumo;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.DisciplinaCursada;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.HistoricoStatus;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Solicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.AnexoRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.DisciplinaCursadaRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.HistoricoStatusRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.PreDiagnosticoRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.SolicitacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Criacao e consultas de solicitacoes. Regras de perfil ficam no controller. */
@Service
public class SolicitacaoService {
    private final SolicitacaoRepository solicitacoes;
    private final DisciplinaCursadaRepository disciplinas;
    private final AnexoRepository anexos;
    private final PreDiagnosticoRepository preDiagnosticos;
    private final HistoricoStatusRepository historicos;

    public SolicitacaoService(SolicitacaoRepository solicitacoes, DisciplinaCursadaRepository disciplinas,
                              AnexoRepository anexos, PreDiagnosticoRepository preDiagnosticos,
                              HistoricoStatusRepository historicos) {
        this.solicitacoes = solicitacoes;
        this.disciplinas = disciplinas;
        this.anexos = anexos;
        this.preDiagnosticos = preDiagnosticos;
        this.historicos = historicos;
    }

    @Transactional
    public Solicitacao criar(CriarSolicitacao d) {
        Solicitacao solicitacao = solicitacoes.save(new Solicitacao(
                d.alunoId(), d.curso(), d.disciplinaDestino(), d.cargaHorariaDestino()));
        for (DisciplinaCursadaEntrada e : d.disciplinas()) {
            disciplinas.save(new DisciplinaCursada(solicitacao, e.nome(), e.instituicao(), e.cargaHoraria()));
        }
        historicos.save(new HistoricoStatus(solicitacao, null, StatusSolicitacao.RASCUNHO, d.alunoId(), null));
        return solicitacao;
    }

    @Transactional(readOnly = true)
    public SolicitacaoDetalhe detalhar(UUID id) {
        Solicitacao s = buscar(id);
        return SolicitacaoDetalhe.de(s,
                disciplinas.findBySolicitacaoId(id),
                anexos.findBySolicitacaoId(id),
                preDiagnosticos.findBySolicitacaoId(id).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<HistoricoItem> historico(UUID id) {
        buscar(id);
        return historicos.findBySolicitacaoIdOrderByCriadoEmAsc(id).stream().map(HistoricoItem::de).toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoResumo> listar(StatusSolicitacao status, UUID alunoId) {
        List<Solicitacao> encontradas;
        if (alunoId != null) {
            encontradas = solicitacoes.findByAlunoId(alunoId);
            if (status != null)
                encontradas = encontradas.stream().filter(s -> s.getStatus() == status).toList();
        } else if (status != null) {
            encontradas = solicitacoes.findByStatus(status);
        } else {
            encontradas = solicitacoes.findAll();
        }
        return encontradas.stream().map(SolicitacaoResumo::de).toList();
    }

    /** Consulta leve da solicitacao (sem filhos); 404 se nao existir. */
    @Transactional(readOnly = true)
    public Solicitacao buscar(UUID id) {
        return solicitacoes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitacao nao encontrada"));
    }
}
