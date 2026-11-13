package ifsuldeminas.edu.br.ifsystem.aproveitamento.domain;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SolicitacaoMapeamentoTest {

    @Autowired
    private TestEntityManager tem;

    @Autowired
    private EntityManager em;

    @Test
    void persisteSolicitacaoComDuasDisciplinasCursadas() {
        UUID alunoId = UUID.randomUUID();
        Solicitacao solicitacao = tem.persist(new Solicitacao(alunoId, "ADS", "Banco de Dados", 60));
        DisciplinaCursada d1 = tem.persist(new DisciplinaCursada(solicitacao, "Modelagem de Dados", "UFLA", 40));
        DisciplinaCursada d2 = tem.persist(new DisciplinaCursada(solicitacao, "SQL Avançado", "UNIFAL", 30));
        tem.flush();
        tem.clear();

        Solicitacao carregada = tem.find(Solicitacao.class, solicitacao.getId());
        assertThat(carregada.getAlunoId()).isEqualTo(alunoId);
        assertThat(carregada.getCurso()).isEqualTo("ADS");
        assertThat(carregada.getDisciplinaDestino()).isEqualTo("Banco de Dados");
        assertThat(carregada.getCargaHorariaDestino()).isEqualTo(60);
        assertThat(carregada.getStatus()).isEqualTo(StatusSolicitacao.RASCUNHO);
        assertThat(carregada.getCriadoEm()).isNotNull();
        assertThat(carregada.getAtualizadoEm()).isNotNull();

        List<DisciplinaCursada> cursadas = em
                .createQuery("select d from DisciplinaCursada d where d.solicitacao.id = :id order by d.nome",
                        DisciplinaCursada.class)
                .setParameter("id", solicitacao.getId())
                .getResultList();
        assertThat(cursadas).hasSize(2);
        assertThat(cursadas).extracting(DisciplinaCursada::getId).containsExactlyInAnyOrder(d1.getId(), d2.getId());
        assertThat(cursadas).extracting(DisciplinaCursada::getNome)
                .containsExactly("Modelagem de Dados", "SQL Avançado");
        assertThat(cursadas).extracting(DisciplinaCursada::getInstituicao).containsExactly("UFLA", "UNIFAL");
        assertThat(cursadas).extracting(DisciplinaCursada::getCargaHoraria).containsExactly(40, 30);
        assertThat(cursadas).allSatisfy(d -> assertThat(d.getSolicitacao().getId()).isEqualTo(solicitacao.getId()));
    }
}
