package ifsuldeminas.edu.br.ifsystem.aproveitamento.service;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.DisciplinaCursada;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.ParecerIa;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Solicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusParecerIa;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.CoordenadorRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.DisciplinaCursadaRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.ParecerIaRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.SolicitacaoRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.AnalisadorEquivalencia.EntradaAnalise;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.AnalisadorEquivalencia.ResultadoAnalise;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Gera o parecer da IA fora da transacao da submissao; falhas ficam registradas no proprio parecer. */
@Service
public class ParecerIaService {
    private static final Logger log = LoggerFactory.getLogger(ParecerIaService.class);
    private static final int LIMITE_ERRO = 255;

    private final ParecerIaRepository pareceres;
    private final SolicitacaoRepository solicitacoes;
    private final DisciplinaCursadaRepository disciplinas;
    private final AnalisadorEquivalencia analisador;
    private final CoordenadorRepository coordenadores;

    public ParecerIaService(ParecerIaRepository pareceres, SolicitacaoRepository solicitacoes,
                            DisciplinaCursadaRepository disciplinas, AnalisadorEquivalencia analisador,
                            CoordenadorRepository coordenadores) {
        this.pareceres = pareceres;
        this.solicitacoes = solicitacoes;
        this.disciplinas = disciplinas;
        this.analisador = analisador;
        this.coordenadores = coordenadores;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoEnviar(SolicitacaoEnviadaEvent evento) {
        gerar(evento.solicitacaoId());
    }

    /** Gera um novo parecer em segundo plano (usado pela regeneracao manual). */
    @Async
    public void regerar(UUID solicitacaoId) {
        gerar(solicitacaoId);
    }

    /** Regeracao liberada para SERVIDOR e para o professor que coordena o curso da solicitacao. */
    @Transactional(readOnly = true)
    public boolean podeRegerar(UUID usuarioId, Perfil perfil, String curso) {
        return perfil == Perfil.SERVIDOR
                || (perfil == Perfil.PROFESSOR && coordenadores.existsByProfessorIdAndCurso(usuarioId, curso));
    }

    @Transactional(readOnly = true)
    public Optional<ParecerIa> maisRecente(UUID solicitacaoId) {
        return pareceres.findFirstBySolicitacaoIdOrderByGeradoEmDesc(solicitacaoId);
    }

    /** Nunca propaga excecao: quem chama (listener assincrono) nao tem a quem repassar. */
    void gerar(UUID solicitacaoId) {
        ParecerIa parecer = null;
        try {
            Solicitacao solicitacao = solicitacoes.findById(solicitacaoId).orElse(null);
            if (solicitacao == null) {
                log.warn("Parecer da IA ignorado: solicitacao {} nao encontrada", solicitacaoId);
                return;
            }
            parecer = pareceres.save(new ParecerIa(solicitacao, StatusParecerIa.PROCESSANDO,
                    null, null, null, null, null));
            ResultadoAnalise resultado = analisador.analisar(montarEntrada(solicitacao));
            parecer.concluir(resultado.recomendacao(), resultado.similaridade(),
                    resultado.justificativa(), resultado.modelo());
            pareceres.save(parecer);
        } catch (Exception e) {
            log.error("Falha ao gerar parecer da IA da solicitacao {}", solicitacaoId, e);
            registrarFalha(parecer, e);
        }
    }

    private void registrarFalha(ParecerIa parecer, Exception causa) {
        if (parecer == null)
            return;
        try {
            parecer.falhar(mensagem(causa));
            pareceres.save(parecer);
        } catch (Exception e) {
            log.error("Nao foi possivel registrar a falha do parecer {}", parecer.getId(), e);
        }
    }

    private static String mensagem(Exception e) {
        String texto = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return texto.length() > LIMITE_ERRO ? texto.substring(0, LIMITE_ERRO) : texto;
    }

    /** Ementas seguem nulas nesta fase: ainda nao ha campo de ementa na solicitacao. */
    private EntradaAnalise montarEntrada(Solicitacao solicitacao) {
        List<DisciplinaCursada> cursadas = disciplinas.findBySolicitacaoId(solicitacao.getId());
        DisciplinaCursada origem = cursadas.isEmpty() ? null : cursadas.get(0);
        return new EntradaAnalise(
                origem == null ? null : origem.getNome(), null, origem == null ? null : origem.getCargaHoraria(),
                solicitacao.getDisciplinaDestino(), null, solicitacao.getCargaHorariaDestino());
    }
}
