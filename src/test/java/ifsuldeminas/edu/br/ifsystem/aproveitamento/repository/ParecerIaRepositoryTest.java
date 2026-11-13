package ifsuldeminas.edu.br.ifsystem.aproveitamento.repository;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.ParecerIa;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.RecomendacaoIa;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Solicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusParecerIa;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class ParecerIaRepositoryTest {

    @Autowired
    private TestEntityManager tem;
    @Autowired
    private ParecerIaRepository pareceres;

    private Solicitacao novaSolicitacao() {
        return tem.persist(new Solicitacao(UUID.randomUUID(), "ADS", "Banco de Dados", 60));
    }

    @Test
    void gravaELeParecerConcluido() {
        Solicitacao s = novaSolicitacao();
        ParecerIa salvo = pareceres.save(new ParecerIa(s, StatusParecerIa.CONCLUIDO,
                RecomendacaoIa.SUGERE_DEFERIR, 82, "Conteudos equivalentes", "fake", null));
        tem.flush();
        tem.clear();

        ParecerIa lido = pareceres.findById(salvo.getId()).orElseThrow();

        assertThat(lido.getSolicitacao().getId()).isEqualTo(s.getId());
        assertThat(lido.getStatus()).isEqualTo(StatusParecerIa.CONCLUIDO);
        assertThat(lido.getRecomendacao()).isEqualTo(RecomendacaoIa.SUGERE_DEFERIR);
        assertThat(lido.getSimilaridade()).isEqualTo(82);
        assertThat(lido.getJustificativa()).isEqualTo("Conteudos equivalentes");
        assertThat(lido.getModelo()).isEqualTo("fake");
        assertThat(lido.getGeradoEm()).isNotNull();
        assertThat(lido.getErro()).isNull();
    }

    @Test
    void gravaParecerFalhouComCamposOpcionaisNulos() {
        Solicitacao s = novaSolicitacao();
        ParecerIa salvo = pareceres.save(new ParecerIa(s, StatusParecerIa.FALHOU,
                null, null, null, "fake", "timeout"));
        tem.flush();
        tem.clear();

        ParecerIa lido = pareceres.findById(salvo.getId()).orElseThrow();

        assertThat(lido.getStatus()).isEqualTo(StatusParecerIa.FALHOU);
        assertThat(lido.getRecomendacao()).isNull();
        assertThat(lido.getSimilaridade()).isNull();
        assertThat(lido.getErro()).isEqualTo("timeout");
    }

    @Test
    void buscaOParecerMaisRecenteDaSolicitacao() {
        Solicitacao s = novaSolicitacao();
        ParecerIa antigo = new ParecerIa(s, StatusParecerIa.CONCLUIDO, RecomendacaoIa.INCONCLUSIVO, 50, "a", "fake", null);
        ParecerIa novo = new ParecerIa(s, StatusParecerIa.CONCLUIDO, RecomendacaoIa.SUGERE_INDEFERIR, 10, "b", "fake", null);
        OffsetDateTime base = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        ReflectionTestUtils.setField(antigo, "geradoEm", base);
        ReflectionTestUtils.setField(novo, "geradoEm", base.plusMinutes(5));
        tem.persist(novo);
        tem.persist(antigo);
        tem.flush();
        tem.clear();

        ParecerIa lido = pareceres.findFirstBySolicitacaoIdOrderByGeradoEmDesc(s.getId()).orElseThrow();

        assertThat(lido.getId()).isEqualTo(novo.getId());
        assertThat(lido.getRecomendacao()).isEqualTo(RecomendacaoIa.SUGERE_INDEFERIR);
    }

    @Test
    void rejeitaSimilaridadeForaDoIntervalo() {
        Solicitacao s = novaSolicitacao();

        assertThatThrownBy(() -> new ParecerIa(s, StatusParecerIa.CONCLUIDO, RecomendacaoIa.INCONCLUSIVO,
                101, "x", "fake", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParecerIa(s, StatusParecerIa.CONCLUIDO, RecomendacaoIa.INCONCLUSIVO,
                -1, "x", "fake", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
