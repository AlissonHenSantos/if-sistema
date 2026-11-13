package ifsuldeminas.edu.br.ifsystem.aproveitamento.service;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.HistoricoStatus;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Solicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.CoordenadorRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.HistoricoStatusRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.SolicitacaoRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao.AnalisarTransicao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao.ConcluirPreDiagnosticoTransicao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao.ConferirTransicao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao.DeferirTransicao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao.DevolverPendenciaTransicao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao.EnviarTransicao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao.IndeferirTransicao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao.ReenviarTransicao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao.RegistrarTransicao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao.TransicaoStatus;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FluxoStatusServiceTest {

    private static final String CURSO = "ADS";

    private static List<TransicaoStatus> todasTransicoes() {
        return List.of(new EnviarTransicao(), new ConferirTransicao(), new DevolverPendenciaTransicao(),
                new ReenviarTransicao(), new ConcluirPreDiagnosticoTransicao(), new AnalisarTransicao(),
                new DeferirTransicao(), new IndeferirTransicao(), new RegistrarTransicao());
    }

    static Stream<TransicaoStatus> transicoes() {
        return todasTransicoes().stream();
    }

    private SolicitacaoRepository solicitacoes;
    private HistoricoStatusRepository historicos;
    private CoordenadorRepository coordenadores;
    private FluxoStatusService service;
    private UUID alunoId;
    private UUID solicitacaoId;
    private Solicitacao solicitacao;

    @BeforeEach
    void preparar() {
        solicitacoes = mock(SolicitacaoRepository.class);
        historicos = mock(HistoricoStatusRepository.class);
        coordenadores = mock(CoordenadorRepository.class);
        service = new FluxoStatusService(todasTransicoes(), solicitacoes, historicos, coordenadores);
        alunoId = UUID.randomUUID();
        solicitacaoId = UUID.randomUUID();
        solicitacao = new Solicitacao(alunoId, CURSO, "Banco de Dados", 60);
        when(solicitacoes.findById(solicitacaoId)).thenReturn(Optional.of(solicitacao));
        when(solicitacoes.save(any(Solicitacao.class))).thenAnswer(i -> i.getArgument(0));
        when(coordenadores.existsByProfessorIdAndCurso(any(), any())).thenReturn(true);
    }

    @ParameterizedTest
    @MethodSource("transicoes")
    void executaTransicaoValidaParaCadaOrigem(TransicaoStatus t) {
        for (StatusSolicitacao origem : t.origens()) {
            solicitacao.alterarStatus(origem);
            UUID usuario = t.perfilExigido() == Perfil.ALUNO ? alunoId : UUID.randomUUID();

            Solicitacao resultado = service.executar(solicitacaoId, t.acao(), usuario, t.perfilExigido(), "obs");

            assertThat(resultado).isSameAs(solicitacao);
            assertThat(resultado.getStatus()).isEqualTo(t.destino());
            ArgumentCaptor<HistoricoStatus> captor = ArgumentCaptor.forClass(HistoricoStatus.class);
            verify(historicos, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
            HistoricoStatus h = captor.getValue();
            assertThat(h.getStatusAnterior()).isEqualTo(origem);
            assertThat(h.getStatusNovo()).isEqualTo(t.destino());
            assertThat(h.getResponsavelId()).isEqualTo(usuario);
            assertThat(h.getObservacao()).isEqualTo("obs");
            assertThat(h.getSolicitacao()).isSameAs(solicitacao);
        }
    }

    @Test
    void acaoInexistenteRetorna400() {
        assertStatus(() -> service.executar(solicitacaoId, "nao-existe", alunoId, Perfil.ALUNO, null),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.executar(solicitacaoId, null, alunoId, Perfil.ALUNO, null),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void solicitacaoInexistenteRetorna404() {
        assertStatus(() -> service.executar(UUID.randomUUID(), "enviar", alunoId, Perfil.ALUNO, null),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void statusForaDasOrigensRetorna409() {
        solicitacao.alterarStatus(StatusSolicitacao.ENVIADA);
        assertStatus(() -> service.executar(solicitacaoId, "enviar", alunoId, Perfil.ALUNO, null),
                HttpStatus.CONFLICT);
        verify(historicos, never()).save(any());
    }

    @Test
    void perfilErradoComStatusErradoRetorna403EmVezDe409() {
        solicitacao.alterarStatus(StatusSolicitacao.ENVIADA);
        assertStatus(() -> service.executar(solicitacaoId, "enviar", alunoId, Perfil.SERVIDOR, null),
                HttpStatus.FORBIDDEN);
        verify(historicos, never()).save(any());
    }

    @Test
    void alunoNaoDonoComStatusErradoRetorna403() {
        solicitacao.alterarStatus(StatusSolicitacao.ENVIADA);
        assertStatus(() -> service.executar(solicitacaoId, "enviar", UUID.randomUUID(), Perfil.ALUNO, null),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void naoCoordenadorComStatusErradoRetorna403() {
        when(coordenadores.existsByProfessorIdAndCurso(any(), any())).thenReturn(false);
        solicitacao.alterarStatus(StatusSolicitacao.ANALISE);
        assertStatus(() -> service.executar(solicitacaoId, "deferir", UUID.randomUUID(), Perfil.PROFESSOR, null),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void perfilDiferenteRetorna403() {
        assertStatus(() -> service.executar(solicitacaoId, "enviar", alunoId, Perfil.SERVIDOR, null),
                HttpStatus.FORBIDDEN);
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.RASCUNHO);
        verify(historicos, never()).save(any());
    }

    @Test
    void professorQueNaoCoordenaOCursoRetorna403ParaDeferirEIndeferir() {
        when(coordenadores.existsByProfessorIdAndCurso(any(), any())).thenReturn(false);
        for (String acao : List.of("deferir", "indeferir")) {
            solicitacao.alterarStatus(StatusSolicitacao.DECISAO);
            assertStatus(() -> service.executar(solicitacaoId, acao, UUID.randomUUID(), Perfil.PROFESSOR, null),
                    HttpStatus.FORBIDDEN);
            assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.DECISAO);
        }
        verify(historicos, never()).save(any());
    }

    @Test
    void analisarNaoExigeCoordenador() {
        when(coordenadores.existsByProfessorIdAndCurso(any(), any())).thenReturn(false);
        solicitacao.alterarStatus(StatusSolicitacao.ANALISE);
        Solicitacao r = service.executar(solicitacaoId, "analisar", UUID.randomUUID(), Perfil.PROFESSOR, null);
        assertThat(r.getStatus()).isEqualTo(StatusSolicitacao.DECISAO);
    }

    @Test
    void alunoNaoDonoRetorna403EmEnviarEReenviar() {
        UUID outroAluno = UUID.randomUUID();
        solicitacao.alterarStatus(StatusSolicitacao.RASCUNHO);
        assertStatus(() -> service.executar(solicitacaoId, "enviar", outroAluno, Perfil.ALUNO, null),
                HttpStatus.FORBIDDEN);
        solicitacao.alterarStatus(StatusSolicitacao.PENDENTE);
        assertStatus(() -> service.executar(solicitacaoId, "reenviar", outroAluno, Perfil.ALUNO, null),
                HttpStatus.FORBIDDEN);
        verify(historicos, never()).save(any());
    }

    @Test
    void acaoDuplicadaFalhaNaInicializacao() {
        List<TransicaoStatus> duplicadas = List.of(new EnviarTransicao(), new EnviarTransicao());
        assertThatThrownBy(() -> new FluxoStatusService(duplicadas, solicitacoes, historicos, coordenadores))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("enviar");
    }

    private static void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable chamada,
                                     HttpStatus esperado) {
        assertThatThrownBy(chamada)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(esperado));
    }
}
