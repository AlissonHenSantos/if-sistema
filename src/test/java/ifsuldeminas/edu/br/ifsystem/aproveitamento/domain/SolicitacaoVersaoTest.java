package ifsuldeminas.edu.br.ifsystem.aproveitamento.domain;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.SolicitacaoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SolicitacaoVersaoTest {

    @Autowired
    private SolicitacaoRepository solicitacoes;

    @Autowired
    private EntityManager em;

    @Test
    void segundaGravacaoComVersaoDesatualizadaLancaConflitoOtimista() {
        UUID id = solicitacoes.saveAndFlush(new Solicitacao(UUID.randomUUID(), "ADS", "Banco de Dados", 60)).getId();
        em.clear();

        Solicitacao primeira = solicitacoes.findById(id).orElseThrow();
        em.detach(primeira);
        Solicitacao segunda = solicitacoes.findById(id).orElseThrow();
        em.detach(segunda);
        assertThat(primeira).isNotSameAs(segunda);

        primeira.alterarStatus(StatusSolicitacao.ENVIADA);
        solicitacoes.saveAndFlush(primeira);

        segunda.alterarStatus(StatusSolicitacao.CONFERENCIA);
        assertThatThrownBy(() -> solicitacoes.saveAndFlush(segunda))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
