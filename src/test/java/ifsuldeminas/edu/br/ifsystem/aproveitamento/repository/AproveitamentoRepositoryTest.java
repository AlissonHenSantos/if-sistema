package ifsuldeminas.edu.br.ifsystem.aproveitamento.repository;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Anexo;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.DisciplinaCursada;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.HistoricoStatus;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.PreDiagnostico;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Solicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusPreDiagnostico;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AproveitamentoRepositoryTest {

    private static final OffsetDateTime BASE = OffsetDateTime.parse("2026-01-01T10:00:00Z");

    @Autowired
    private TestEntityManager tem;
    @Autowired
    private SolicitacaoRepository solicitacoes;
    @Autowired
    private DisciplinaCursadaRepository disciplinas;
    @Autowired
    private AnexoRepository anexos;
    @Autowired
    private PreDiagnosticoRepository preDiagnosticos;
    @Autowired
    private HistoricoStatusRepository historicos;

    private Solicitacao novaSolicitacao(UUID alunoId) {
        return tem.persist(new Solicitacao(alunoId, "ADS", "Banco de Dados", 60));
    }

    /** Fixa criadoEm antes de persistir para que a ordenacao nao dependa do relogio. */
    private <T> T comCriadoEm(T entidade, int minutos) {
        ReflectionTestUtils.setField(entidade, "criadoEm", BASE.plusMinutes(minutos));
        return entidade;
    }

    private void sincronizar() {
        tem.flush();
        tem.clear();
    }

    @Test
    void solicitacaoFindByAlunoId() {
        UUID aluno = UUID.randomUUID();
        Solicitacao a = novaSolicitacao(aluno);
        Solicitacao b = novaSolicitacao(aluno);
        novaSolicitacao(UUID.randomUUID());
        sincronizar();

        assertThat(solicitacoes.findByAlunoId(aluno))
                .extracting(Solicitacao::getId)
                .containsExactlyInAnyOrder(a.getId(), b.getId());
        assertThat(solicitacoes.findByAlunoId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void solicitacaoFindByStatus() {
        Solicitacao rascunho = novaSolicitacao(UUID.randomUUID());
        Solicitacao enviada = novaSolicitacao(UUID.randomUUID());
        enviada.alterarStatus(StatusSolicitacao.ENVIADA);
        sincronizar();

        assertThat(solicitacoes.findByStatus(StatusSolicitacao.ENVIADA))
                .extracting(Solicitacao::getId)
                .containsExactly(enviada.getId());
        assertThat(solicitacoes.findByStatus(StatusSolicitacao.RASCUNHO))
                .extracting(Solicitacao::getId)
                .containsExactly(rascunho.getId());
        assertThat(solicitacoes.findByStatus(StatusSolicitacao.DEFERIDA)).isEmpty();
    }

    @Test
    void disciplinaCursadaFindBySolicitacaoId() {
        Solicitacao s = novaSolicitacao(UUID.randomUUID());
        Solicitacao outra = novaSolicitacao(UUID.randomUUID());
        DisciplinaCursada d1 = tem.persist(new DisciplinaCursada(s, "BD I", "UFLA", 40));
        DisciplinaCursada d2 = tem.persist(new DisciplinaCursada(s, "BD II", "UFLA", 30));
        tem.persist(new DisciplinaCursada(outra, "Calculo", "UFMG", 60));
        sincronizar();

        assertThat(disciplinas.findBySolicitacaoId(s.getId()))
                .extracting(DisciplinaCursada::getId)
                .containsExactlyInAnyOrder(d1.getId(), d2.getId());
        assertThat(disciplinas.findBySolicitacaoId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void anexoFindBySolicitacaoId() {
        Solicitacao s = novaSolicitacao(UUID.randomUUID());
        Solicitacao outra = novaSolicitacao(UUID.randomUUID());
        Anexo a1 = tem.persist(new Anexo(s, "historico.pdf", "application/pdf", 100L));
        Anexo a2 = tem.persist(new Anexo(s, "ementa.pdf", "application/pdf", 200L));
        tem.persist(new Anexo(outra, "outro.pdf", "application/pdf", 300L));
        sincronizar();

        assertThat(anexos.findBySolicitacaoId(s.getId()))
                .extracting(Anexo::getId)
                .containsExactlyInAnyOrder(a1.getId(), a2.getId());
        assertThat(anexos.findBySolicitacaoId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void preDiagnosticoFindBySolicitacaoIdRetornaOMaisRecente() {
        Solicitacao s = novaSolicitacao(UUID.randomUUID());
        Solicitacao outra = novaSolicitacao(UUID.randomUUID());
        tem.persist(comCriadoEm(new PreDiagnostico(s, StatusPreDiagnostico.FALHA,
                new BigDecimal("40.00"), null, "antigo", "p", "m"), 0));
        PreDiagnostico recente = tem.persist(comCriadoEm(new PreDiagnostico(s, StatusPreDiagnostico.SUCESSO,
                new BigDecimal("90.00"), new BigDecimal("80.00"), "recente", "p", "m"), 10));
        tem.persist(comCriadoEm(new PreDiagnostico(s, StatusPreDiagnostico.SUCESSO,
                new BigDecimal("50.00"), new BigDecimal("50.00"), "intermediario", "p", "m"), 5));
        tem.persist(comCriadoEm(new PreDiagnostico(outra, StatusPreDiagnostico.SUCESSO,
                new BigDecimal("99.00"), new BigDecimal("99.00"), "de outra", "p", "m"), 60));
        sincronizar();

        assertThat(preDiagnosticos.findBySolicitacaoId(s.getId()))
                .get()
                .extracting(PreDiagnostico::getId)
                .isEqualTo(recente.getId());
        assertThat(preDiagnosticos.findBySolicitacaoId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void historicoStatusFindBySolicitacaoIdOrderByCriadoEmAsc() {
        Solicitacao s = novaSolicitacao(UUID.randomUUID());
        Solicitacao outra = novaSolicitacao(UUID.randomUUID());
        HistoricoStatus segundo = tem.persist(comCriadoEm(new HistoricoStatus(s,
                StatusSolicitacao.RASCUNHO, StatusSolicitacao.ENVIADA, null, null), 5));
        HistoricoStatus primeiro = tem.persist(comCriadoEm(new HistoricoStatus(s,
                null, StatusSolicitacao.RASCUNHO, null, null), 0));
        HistoricoStatus terceiro = tem.persist(comCriadoEm(new HistoricoStatus(s,
                StatusSolicitacao.ENVIADA, StatusSolicitacao.CONFERENCIA, null, null), 10));
        tem.persist(comCriadoEm(new HistoricoStatus(outra,
                null, StatusSolicitacao.RASCUNHO, null, null), 1));
        sincronizar();

        assertThat(historicos.findBySolicitacaoIdOrderByCriadoEmAsc(s.getId()))
                .extracting(HistoricoStatus::getId)
                .containsExactly(primeiro.getId(), segundo.getId(), terceiro.getId());
        assertThat(historicos.findBySolicitacaoIdOrderByCriadoEmAsc(UUID.randomUUID())).isEmpty();
    }
}
