package ifsuldeminas.edu.br.ifsystem.aproveitamento.service;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.HistoricoStatus;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Solicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.CoordenadorRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.HistoricoStatusRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.SolicitacaoRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao.TransicaoStatus;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FluxoStatusService {
    private final Map<String, TransicaoStatus> transicoes;
    private final SolicitacaoRepository solicitacoes;
    private final HistoricoStatusRepository historicos;
    private final CoordenadorRepository coordenadores;

    public FluxoStatusService(List<TransicaoStatus> transicoes, SolicitacaoRepository solicitacoes,
                              HistoricoStatusRepository historicos, CoordenadorRepository coordenadores) {
        Map<String, TransicaoStatus> porAcao = new HashMap<>();
        for (TransicaoStatus t : transicoes) {
            if (porAcao.put(t.acao(), t) != null)
                throw new IllegalStateException("Acao de transicao duplicada: " + t.acao());
        }
        this.transicoes = Map.copyOf(porAcao);
        this.solicitacoes = solicitacoes;
        this.historicos = historicos;
        this.coordenadores = coordenadores;
    }

    @Transactional
    public Solicitacao executar(UUID solicitacaoId, String acao, UUID usuarioId, Perfil perfil, String observacao) {
        TransicaoStatus transicao = acao == null ? null : transicoes.get(acao);
        if (transicao == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Acao inexistente");

        Solicitacao solicitacao = solicitacoes.findById(solicitacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitacao nao encontrada"));

        if (transicao.perfilExigido() != perfil)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem permissao para esta acao");

        if (perfil == Perfil.ALUNO && !solicitacao.getAlunoId().equals(usuarioId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solicitacao pertence a outro aluno");

        if (transicao.exigeCoordenador()
                && !coordenadores.existsByProfessorIdAndCurso(usuarioId, solicitacao.getCurso()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Professor nao coordena o curso da solicitacao");

        StatusSolicitacao atual = solicitacao.getStatus();
        if (!transicao.origens().contains(atual))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Acao nao permitida no status atual");

        solicitacao.alterarStatus(transicao.destino());
        solicitacoes.save(solicitacao);
        historicos.save(new HistoricoStatus(solicitacao, atual, transicao.destino(), usuarioId, observacao));
        return solicitacao;
    }
}
