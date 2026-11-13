package ifsuldeminas.edu.br.ifsystem.aproveitamento.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ApoioMapeamentoTest {

    @Autowired
    private TestEntityManager tem;

    @Autowired
    private EntityManager em;

    private Solicitacao novaSolicitacao() {
        return tem.persist(new Solicitacao(UUID.randomUUID(), "ADS", "Banco de Dados", 60));
    }

    @Test
    void persisteAnexoComConteudoBinario() {
        Solicitacao s = novaSolicitacao();
        byte[] bytes = new byte[100_000];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        Anexo anexo = tem.persist(new Anexo(s, "historico.pdf", "application/pdf", (long) bytes.length));
        tem.persist(new AnexoConteudo(anexo, bytes));
        tem.flush();
        tem.clear();

        Anexo carregado = tem.find(Anexo.class, anexo.getId());
        assertThat(carregado.getNomeArquivo()).isEqualTo("historico.pdf");
        assertThat(carregado.getTipoConteudo()).isEqualTo("application/pdf");
        assertThat(carregado.getTamanho()).isEqualTo(100_000L);
        assertThat(carregado.getSolicitacao().getId()).isEqualTo(s.getId());
        assertThat(carregado.getCriadoEm()).isNotNull();

        AnexoConteudo conteudo = tem.find(AnexoConteudo.class, anexo.getId());
        assertThat(conteudo.getId()).isEqualTo(anexo.getId());
        assertThat(conteudo.getConteudo()).containsExactly(bytes);
    }

    @Test
    void persistePreDiagnostico() {
        Solicitacao s = novaSolicitacao();
        String textoLongo = "x".repeat(10_000);
        PreDiagnostico ok = tem.persist(new PreDiagnostico(s, StatusPreDiagnostico.SUCESSO,
                new BigDecimal("85.50"), new BigDecimal("72.25"), textoLongo, "prompt completo", "modelo-x"));
        PreDiagnostico falha = tem.persist(new PreDiagnostico(s, StatusPreDiagnostico.FALHA,
                new BigDecimal("40.00"), null, "Carga abaixo do limite", "prompt", "modelo-x"));
        tem.flush();
        tem.clear();

        PreDiagnostico a = tem.find(PreDiagnostico.class, ok.getId());
        PreDiagnostico b = tem.find(PreDiagnostico.class, falha.getId());
        assertThat(a.getStatus()).isEqualTo(StatusPreDiagnostico.SUCESSO);
        assertThat(a.getPercentualCarga()).isEqualByComparingTo("85.50");
        assertThat(a.getPercentualConteudo()).isEqualByComparingTo("72.25");
        assertThat(a.getJustificativa()).isEqualTo(textoLongo);
        assertThat(a.getPrompt()).isEqualTo("prompt completo");
        assertThat(a.getModelo()).isEqualTo("modelo-x");
        assertThat(a.getSolicitacao().getId()).isEqualTo(s.getId());
        assertThat(a.getCriadoEm()).isNotNull();
        assertThat(b.getStatus()).isEqualTo(StatusPreDiagnostico.FALHA);
        assertThat(b.getPercentualCarga()).isEqualByComparingTo("40.00");
        assertThat(b.getPercentualConteudo()).isNull();
    }

    @Test
    void persisteHistoricoStatus() {
        Solicitacao s = novaSolicitacao();
        UUID responsavel = UUID.randomUUID();
        HistoricoStatus primeiro = tem.persist(
                new HistoricoStatus(s, null, StatusSolicitacao.RASCUNHO, responsavel, null));
        HistoricoStatus segundo = tem.persist(
                new HistoricoStatus(s, StatusSolicitacao.RASCUNHO, StatusSolicitacao.ENVIADA, responsavel, "Enviada"));
        tem.flush();
        tem.clear();

        HistoricoStatus h1 = tem.find(HistoricoStatus.class, primeiro.getId());
        HistoricoStatus h2 = tem.find(HistoricoStatus.class, segundo.getId());
        assertThat(h1.getStatusAnterior()).isNull();
        assertThat(h1.getStatusNovo()).isEqualTo(StatusSolicitacao.RASCUNHO);
        assertThat(h2.getStatusAnterior()).isEqualTo(StatusSolicitacao.RASCUNHO);
        assertThat(h2.getStatusNovo()).isEqualTo(StatusSolicitacao.ENVIADA);
        assertThat(h2.getResponsavelId()).isEqualTo(responsavel);
        assertThat(h2.getObservacao()).isEqualTo("Enviada");
        assertThat(h2.getSolicitacao().getId()).isEqualTo(s.getId());
        assertThat(h2.getCriadoEm()).isNotNull();
    }

    @Test
    void persisteCoordenadorEImpedeDuplicidadeProfessorCurso() {
        UUID professor = UUID.randomUUID();
        Coordenador c = tem.persist(new Coordenador(professor, "ADS"));
        tem.persist(new Coordenador(professor, "Agronomia"));
        tem.persist(new Coordenador(UUID.randomUUID(), "ADS"));
        tem.flush();
        tem.clear();

        Coordenador carregado = tem.find(Coordenador.class, c.getId());
        assertThat(carregado.getProfessorId()).isEqualTo(professor);
        assertThat(carregado.getCurso()).isEqualTo("ADS");

        assertThatThrownBy(() -> {
            em.persist(new Coordenador(professor, "ADS"));
            em.flush();
        }).isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("UK_COORDENADOR_PROFESSOR_CURSO");
    }
}
