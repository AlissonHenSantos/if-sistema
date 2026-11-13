package ifsuldeminas.edu.br.ifsystem.aproveitamento.service;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.CriarSolicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.DisciplinaCursadaEntrada;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.DisciplinaCursadaResposta;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.HistoricoItem;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.SolicitacaoDetalhe;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.SolicitacaoResumo;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(SolicitacaoService.class)
class SolicitacaoServiceTest {

    @Autowired
    private SolicitacaoService service;
    @Autowired
    private TestEntityManager tem;

    private Solicitacao persistir(UUID aluno, StatusSolicitacao status) {
        Solicitacao s = new Solicitacao(aluno, "ADS", "Banco de Dados", 60);
        s.alterarStatus(status);
        return tem.persist(s);
    }

    private void sincronizar() {
        tem.flush();
        tem.clear();
    }

    private static void assertNotFound(Runnable r) {
        assertThatThrownBy(r::run)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------- criar ----------

    @Test
    void criarSalvaSolicitacaoEDisciplinas() {
        UUID aluno = UUID.randomUUID();
        CriarSolicitacao dto = new CriarSolicitacao(aluno, "ADS", "Banco de Dados", 60, List.of(
                new DisciplinaCursadaEntrada("BD I", "UFLA", 40),
                new DisciplinaCursadaEntrada("BD II", "UFLA", 30)));

        Solicitacao criada = service.criar(dto);
        sincronizar();

        Solicitacao salva = tem.find(Solicitacao.class, criada.getId());
        assertThat(salva.getAlunoId()).isEqualTo(aluno);
        assertThat(salva.getCurso()).isEqualTo("ADS");
        assertThat(salva.getDisciplinaDestino()).isEqualTo("Banco de Dados");
        assertThat(salva.getCargaHorariaDestino()).isEqualTo(60);
        assertThat(salva.getStatus()).isEqualTo(StatusSolicitacao.RASCUNHO);

        List<DisciplinaCursada> ds = tem.getEntityManager()
                .createQuery("select d from DisciplinaCursada d where d.solicitacao.id = :id", DisciplinaCursada.class)
                .setParameter("id", criada.getId()).getResultList();
        assertThat(ds).extracting(DisciplinaCursada::getNome, DisciplinaCursada::getInstituicao,
                        DisciplinaCursada::getCargaHoraria)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("BD I", "UFLA", 40),
                        org.assertj.core.groups.Tuple.tuple("BD II", "UFLA", 30));

        List<HistoricoStatus> hs = tem.getEntityManager()
                .createQuery("select h from HistoricoStatus h where h.solicitacao.id = :id", HistoricoStatus.class)
                .setParameter("id", criada.getId()).getResultList();
        assertThat(hs).singleElement().satisfies(h -> {
            assertThat(h.getStatusAnterior()).isNull();
            assertThat(h.getStatusNovo()).isEqualTo(StatusSolicitacao.RASCUNHO);
            assertThat(h.getResponsavelId()).isEqualTo(aluno);
            assertThat(h.getObservacao()).isNull();
        });
    }

    // ---------- buscar ----------

    @Test
    void buscarRetornaSolicitacaoExistente() {
        UUID aluno = UUID.randomUUID();
        Solicitacao s = persistir(aluno, StatusSolicitacao.ENVIADA);
        sincronizar();

        Solicitacao encontrada = service.buscar(s.getId());

        assertThat(encontrada.getId()).isEqualTo(s.getId());
        assertThat(encontrada.getAlunoId()).isEqualTo(aluno);
        assertThat(encontrada.getStatus()).isEqualTo(StatusSolicitacao.ENVIADA);
    }

    @Test
    void buscarInexistenteDa404() {
        assertNotFound(() -> service.buscar(UUID.randomUUID()));
    }

    // ---------- detalhar ----------

    @Test
    void detalharMontaDetalheCompleto() {
        Solicitacao s = persistir(UUID.randomUUID(), StatusSolicitacao.ANALISE);
        DisciplinaCursada d = tem.persist(new DisciplinaCursada(s, "BD I", "UFLA", 40));
        Anexo a = tem.persist(new Anexo(s, "historico.pdf", "application/pdf", 10L));
        PreDiagnostico antigo = new PreDiagnostico(s, StatusPreDiagnostico.FALHA,
                new BigDecimal("40.00"), null, "antigo", "p", "m");
        ReflectionTestUtils.setField(antigo, "criadoEm", OffsetDateTime.now().minusDays(1));
        tem.persist(antigo);
        tem.persist(new PreDiagnostico(s, StatusPreDiagnostico.SUCESSO,
                new BigDecimal("90.00"), new BigDecimal("80.00"), "recente", "p", "m"));
        sincronizar();

        SolicitacaoDetalhe det = service.detalhar(s.getId());

        assertThat(det.id()).isEqualTo(s.getId());
        assertThat(det.status()).isEqualTo(StatusSolicitacao.ANALISE);
        assertThat(det.disciplinas()).containsExactly(new DisciplinaCursadaResposta(d.getId(), "BD I", "UFLA", 40));
        assertThat(det.anexos()).singleElement().satisfies(r -> assertThat(r.id()).isEqualTo(a.getId()));
        assertThat(det.preDiagnostico().justificativa()).isEqualTo("recente");
    }

    @Test
    void detalharSemPreDiagnosticoRetornaNulo() {
        Solicitacao s = persistir(UUID.randomUUID(), StatusSolicitacao.RASCUNHO);
        sincronizar();

        SolicitacaoDetalhe det = service.detalhar(s.getId());
        assertThat(det.preDiagnostico()).isNull();
        assertThat(det.disciplinas()).isEmpty();
        assertThat(det.anexos()).isEmpty();
    }

    @Test
    void detalharInexistenteDa404() {
        assertNotFound(() -> service.detalhar(UUID.randomUUID()));
    }

    // ---------- historico ----------

    @Test
    void historicoRetornaItensEmOrdemCronologica() {
        Solicitacao s = persistir(UUID.randomUUID(), StatusSolicitacao.ENVIADA);
        UUID responsavel = UUID.randomUUID();
        HistoricoStatus segundo = new HistoricoStatus(s, StatusSolicitacao.RASCUNHO, StatusSolicitacao.ENVIADA,
                responsavel, "Enviada");
        HistoricoStatus primeiro = new HistoricoStatus(s, null, StatusSolicitacao.RASCUNHO, responsavel, null);
        ReflectionTestUtils.setField(primeiro, "criadoEm", OffsetDateTime.now().minusHours(1));
        tem.persist(segundo);
        tem.persist(primeiro);
        sincronizar();

        List<HistoricoItem> itens = service.historico(s.getId());

        assertThat(itens).extracting(HistoricoItem::statusNovo)
                .containsExactly(StatusSolicitacao.RASCUNHO, StatusSolicitacao.ENVIADA);
        assertThat(itens.get(1).observacao()).isEqualTo("Enviada");
        assertThat(itens.get(1).responsavelId()).isEqualTo(responsavel);
    }

    @Test
    void historicoInexistenteDa404() {
        assertNotFound(() -> service.historico(UUID.randomUUID()));
    }

    // ---------- listar ----------

    private record Massa(UUID aluno, Solicitacao alunoRascunho, Solicitacao alunoEnviada,
                         Solicitacao outroEnviada, Solicitacao outroAnalise) {
    }

    private Massa massa() {
        UUID aluno = UUID.randomUUID();
        UUID outro = UUID.randomUUID();
        Massa m = new Massa(aluno,
                persistir(aluno, StatusSolicitacao.RASCUNHO),
                persistir(aluno, StatusSolicitacao.ENVIADA),
                persistir(outro, StatusSolicitacao.ENVIADA),
                persistir(outro, StatusSolicitacao.ANALISE));
        sincronizar();
        return m;
    }

    private static List<UUID> ids(List<SolicitacaoResumo> r) {
        return r.stream().map(SolicitacaoResumo::id).toList();
    }

    @Test
    void listarSemFiltrosRetornaTodas() {
        Massa m = massa();
        assertThat(ids(service.listar(null, null))).containsExactlyInAnyOrder(
                m.alunoRascunho().getId(), m.alunoEnviada().getId(),
                m.outroEnviada().getId(), m.outroAnalise().getId());
    }

    @Test
    void listarSoPorStatus() {
        Massa m = massa();
        assertThat(ids(service.listar(StatusSolicitacao.ENVIADA, null)))
                .containsExactlyInAnyOrder(m.alunoEnviada().getId(), m.outroEnviada().getId());
    }

    @Test
    void listarSoPorAluno() {
        Massa m = massa();
        assertThat(ids(service.listar(null, m.aluno())))
                .containsExactlyInAnyOrder(m.alunoRascunho().getId(), m.alunoEnviada().getId());
    }

    @Test
    void listarPorAlunoEStatus() {
        Massa m = massa();
        assertThat(ids(service.listar(StatusSolicitacao.ENVIADA, m.aluno())))
                .containsExactly(m.alunoEnviada().getId());
        assertThat(service.listar(StatusSolicitacao.ANALISE, m.aluno())).isEmpty();
    }
}
