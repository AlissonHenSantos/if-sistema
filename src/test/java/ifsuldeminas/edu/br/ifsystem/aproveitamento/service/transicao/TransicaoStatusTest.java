package ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransicaoStatusTest {

    private final List<TransicaoStatus> transicoes = List.of(
            new EnviarTransicao(),
            new ConferirTransicao(),
            new DevolverPendenciaTransicao(),
            new ReenviarTransicao(),
            new ConcluirPreDiagnosticoTransicao(),
            new AnalisarTransicao(),
            new DeferirTransicao(),
            new IndeferirTransicao(),
            new RegistrarTransicao());

    @Test
    void noveAcoesComNomesUnicos() {
        assertThat(transicoes).hasSize(9);
        assertThat(transicoes).extracting(TransicaoStatus::acao).doesNotHaveDuplicates();
    }

    @Test
    void nomeDaClasseCorrespondeAAcaoEmPascalCase() {
        for (TransicaoStatus t : transicoes) {
            String acao = t.acao();
            String esperado = Character.toUpperCase(acao.charAt(0)) + acao.substring(1) + "Transicao";
            assertThat(t.getClass().getSimpleName()).isEqualTo(esperado);
        }
    }

    @Test
    void todasTemOrigemDestinoEPerfil() {
        for (TransicaoStatus t : transicoes) {
            assertThat(t.origens()).isNotEmpty();
            assertThat(t.destino()).isNotNull().isNotIn(t.origens());
            assertThat(t.perfilExigido()).isNotNull();
        }
    }

    @Test
    void registrarPartePorDeferidaOuIndeferida_comPerfilServidor() {
        TransicaoStatus registrar = new RegistrarTransicao();
        assertThat(registrar.origens()).containsExactlyInAnyOrder(
                StatusSolicitacao.DEFERIDA, StatusSolicitacao.INDEFERIDA);
        assertThat(registrar.perfilExigido()).isEqualTo(Perfil.SERVIDOR);
    }
}
