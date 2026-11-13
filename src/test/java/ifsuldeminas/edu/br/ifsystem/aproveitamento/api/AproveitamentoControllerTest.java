package ifsuldeminas.edu.br.ifsystem.aproveitamento.api;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.HistoricoItem;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.SolicitacaoDetalhe;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.ParecerIa;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.RecomendacaoIa;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Solicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusParecerIa;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.FluxoStatusService;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.ParecerIaService;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.SolicitacaoService;
import ifsuldeminas.edu.br.ifsystem.security.JwtService;
import ifsuldeminas.edu.br.ifsystem.security.SecurityConfig;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import ifsuldeminas.edu.br.ifsystem.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AproveitamentoController.class)
@Import(SecurityConfig.class)
class AproveitamentoControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private SolicitacaoService solicitacoes;
    @MockitoBean
    private FluxoStatusService fluxo;
    @MockitoBean
    private ParecerIaService parecerIa;
    // Dependencias do JwtFilter, que o @WebMvcTest registra como filtro.
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioService usuarioService;

    private final UUID aluno = UUID.randomUUID();
    private final UUID outroAluno = UUID.randomUUID();
    private final UUID servidor = UUID.randomUUID();
    private final UUID solicitacaoId = UUID.randomUUID();

    // ---------- helpers ----------

    private Solicitacao solicitacao(UUID alunoId) {
        Solicitacao s = new Solicitacao(alunoId, "ADS", "Banco de Dados", 60);
        ReflectionTestUtils.setField(s, "id", solicitacaoId);
        return s;
    }

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder req, UUID id, Perfil perfil) {
        return req.header("X-Usuario-Id", id.toString()).header("X-Perfil", perfil.name());
    }

    private String corpoCriar(UUID alunoId) {
        return """
                {"alunoId":"%s","curso":"ADS","disciplinaDestino":"Banco de Dados","cargaHorariaDestino":60,
                 "disciplinas":[{"nome":"BD I","instituicao":"UFLA","cargaHoraria":40}]}
                """.formatted(alunoId);
    }

    private MockHttpServletRequestBuilder postCriar(String corpo) {
        return post("/aproveitamentos").contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    // ---------- POST criar ----------

    @Test
    void criarRetorna201ComLocation() throws Exception {
        when(solicitacoes.criar(any())).thenReturn(solicitacao(aluno));

        mvc.perform(como(postCriar(corpoCriar(aluno)), aluno, Perfil.ALUNO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/aproveitamentos/" + solicitacaoId))
                .andExpect(jsonPath("$.id").value(solicitacaoId.toString()))
                .andExpect(jsonPath("$.alunoId").value(aluno.toString()))
                .andExpect(jsonPath("$.status").value("RASCUNHO"));
    }

    @Test
    void criarComBodyInvalidoRetorna400() throws Exception {
        String invalido = """
                {"alunoId":"%s","curso":"","disciplinaDestino":"BD","cargaHorariaDestino":0,"disciplinas":[]}
                """.formatted(aluno);

        mvc.perform(como(postCriar(invalido), aluno, Perfil.ALUNO))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(solicitacoes);
    }

    @Test
    void criarSemHeadersRetorna401() throws Exception {
        mvc.perform(postCriar(corpoCriar(aluno)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(solicitacoes);
    }

    @Test
    void criarComPerfilDiferenteDeAlunoRetorna403() throws Exception {
        mvc.perform(como(postCriar(corpoCriar(servidor)), servidor, Perfil.SERVIDOR))
                .andExpect(status().isForbidden());
        verify(solicitacoes, never()).criar(any());
    }

    @Test
    void criarComAlunoIdDiferenteDoHeaderRetorna403() throws Exception {
        mvc.perform(como(postCriar(corpoCriar(outroAluno)), aluno, Perfil.ALUNO))
                .andExpect(status().isForbidden());
        verify(solicitacoes, never()).criar(any());
    }

    // ---------- GET por id ----------

    @Test
    void detalharRetorna200() throws Exception {
        Solicitacao s = solicitacao(aluno);
        when(solicitacoes.buscar(solicitacaoId)).thenReturn(s);
        when(solicitacoes.detalhar(solicitacaoId))
                .thenReturn(SolicitacaoDetalhe.de(s, List.of(), List.of(), null));

        mvc.perform(como(get("/aproveitamentos/{id}", solicitacaoId), aluno, Perfil.ALUNO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(solicitacaoId.toString()))
                .andExpect(jsonPath("$.cargaHorariaDestino").value(60))
                .andExpect(jsonPath("$.preDiagnostico").doesNotExist());
    }

    @Test
    void detalharSolicitacaoDeOutroAlunoRetorna403() throws Exception {
        when(solicitacoes.buscar(solicitacaoId)).thenReturn(solicitacao(outroAluno));

        mvc.perform(como(get("/aproveitamentos/{id}", solicitacaoId), aluno, Perfil.ALUNO))
                .andExpect(status().isForbidden());
        verify(solicitacoes, never()).detalhar(any());
    }

    @Test
    void detalharPermiteServidorVerSolicitacaoDeQualquerAluno() throws Exception {
        Solicitacao s = solicitacao(outroAluno);
        when(solicitacoes.buscar(solicitacaoId)).thenReturn(s);
        when(solicitacoes.detalhar(solicitacaoId))
                .thenReturn(SolicitacaoDetalhe.de(s, List.of(), List.of(), null));

        mvc.perform(como(get("/aproveitamentos/{id}", solicitacaoId), servidor, Perfil.SERVIDOR))
                .andExpect(status().isOk());
    }

    @Test
    void detalharInexistenteRetorna404() throws Exception {
        when(solicitacoes.buscar(solicitacaoId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitacao nao encontrada"));

        mvc.perform(como(get("/aproveitamentos/{id}", solicitacaoId), aluno, Perfil.ALUNO))
                .andExpect(status().isNotFound());
    }

    // ---------- GET lista ----------

    @Test
    void listarForcaAlunoIdParaOProprioAluno() throws Exception {
        when(solicitacoes.listar(any(), any())).thenReturn(List.of());

        mvc.perform(como(get("/aproveitamentos")
                        .param("alunoId", outroAluno.toString())
                        .param("status", "ENVIADA"), aluno, Perfil.ALUNO))
                .andExpect(status().isOk());

        verify(solicitacoes).listar(StatusSolicitacao.ENVIADA, aluno);
        verify(solicitacoes, never()).listar(any(), eq(outroAluno));
    }

    @Test
    void listarMantemFiltroEnviadoParaServidor() throws Exception {
        Solicitacao s = solicitacao(outroAluno);
        when(solicitacoes.listar(StatusSolicitacao.CONFERENCIA, outroAluno)).thenReturn(List.of(
                AproveitamentoDtos.SolicitacaoResumo.de(s)));

        mvc.perform(como(get("/aproveitamentos")
                        .param("alunoId", outroAluno.toString())
                        .param("status", "CONFERENCIA"), servidor, Perfil.SERVIDOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(solicitacaoId.toString()));

        verify(solicitacoes).listar(StatusSolicitacao.CONFERENCIA, outroAluno);
    }

    @Test
    void listarSemFiltrosParaServidorNaoRestringeAluno() throws Exception {
        when(solicitacoes.listar(null, null)).thenReturn(List.of());

        mvc.perform(como(get("/aproveitamentos"), servidor, Perfil.SERVIDOR))
                .andExpect(status().isOk());

        verify(solicitacoes).listar(null, null);
    }

    // ---------- GET historico ----------

    @Test
    void historicoRetorna200() throws Exception {
        when(solicitacoes.buscar(solicitacaoId)).thenReturn(solicitacao(aluno));
        when(solicitacoes.historico(solicitacaoId)).thenReturn(List.of(
                new HistoricoItem(null, StatusSolicitacao.RASCUNHO, aluno, null, OffsetDateTime.now())));

        mvc.perform(como(get("/aproveitamentos/{id}/historico", solicitacaoId), aluno, Perfil.ALUNO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statusNovo").value("RASCUNHO"))
                .andExpect(jsonPath("$[0].responsavelId").value(aluno.toString()));
    }

    @Test
    void historicoDeOutroAlunoRetorna403() throws Exception {
        when(solicitacoes.buscar(solicitacaoId)).thenReturn(solicitacao(outroAluno));

        mvc.perform(como(get("/aproveitamentos/{id}/historico", solicitacaoId), aluno, Perfil.ALUNO))
                .andExpect(status().isForbidden());
        verify(solicitacoes, never()).historico(any());
    }

    // ---------- POST acao ----------

    private MockHttpServletRequestBuilder postAcao(String acao, String corpo) {
        MockHttpServletRequestBuilder req = post("/aproveitamentos/{id}/{acao}", solicitacaoId, acao);
        return corpo == null ? req : req.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    @Test
    void acaoDelegaOsArgumentosAoFluxo() throws Exception {
        Solicitacao s = solicitacao(aluno);
        s.alterarStatus(StatusSolicitacao.ENVIADA);
        when(fluxo.executar(solicitacaoId, "enviar", aluno, Perfil.ALUNO, "pronto")).thenReturn(s);

        mvc.perform(como(postAcao("enviar", "{\"observacao\":\"pronto\"}"), aluno, Perfil.ALUNO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(solicitacaoId.toString()))
                .andExpect(jsonPath("$.status").value("ENVIADA"));

        verify(fluxo).executar(solicitacaoId, "enviar", aluno, Perfil.ALUNO, "pronto");
    }

    @Test
    void acaoSemBodyEnviaObservacaoNula() throws Exception {
        when(fluxo.executar(solicitacaoId, "conferir", servidor, Perfil.SERVIDOR, null))
                .thenReturn(solicitacao(aluno));

        mvc.perform(como(postAcao("conferir", null), servidor, Perfil.SERVIDOR))
                .andExpect(status().isOk());

        verify(fluxo).executar(solicitacaoId, "conferir", servidor, Perfil.SERVIDOR, null);
    }

    @Test
    void acaoPropagaErrosDoFluxo() throws Exception {
        for (HttpStatus erro : List.of(HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN, HttpStatus.CONFLICT)) {
            doThrow(new ResponseStatusException(erro, "erro do fluxo"))
                    .when(fluxo).executar(any(), any(), any(), any(), any());

            mvc.perform(como(postAcao("enviar", null), aluno, Perfil.ALUNO))
                    .andExpect(status().is(erro.value()));
        }
    }

    @Test
    void acaoComObservacaoMuitoLongaRetorna400() throws Exception {
        mvc.perform(como(postAcao("enviar", "{\"observacao\":\"" + "x".repeat(2001) + "\"}"), aluno, Perfil.ALUNO))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(fluxo);
    }

    @Test
    void acaoSemHeadersRetorna401() throws Exception {
        mvc.perform(postAcao("enviar", null))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(fluxo);
    }

    // ---------- parecer da IA ----------

    private ParecerIa parecer() {
        return new ParecerIa(solicitacao(aluno), StatusParecerIa.CONCLUIDO, RecomendacaoIa.INCONCLUSIVO,
                50, "Analise simulada", "fake", null);
    }

    @Test
    void parecerIaRetorna200ParaServidor() throws Exception {
        when(solicitacoes.buscar(solicitacaoId)).thenReturn(solicitacao(aluno));
        when(parecerIa.maisRecente(solicitacaoId)).thenReturn(Optional.of(parecer()));

        mvc.perform(como(get("/aproveitamentos/{id}/parecer-ia", solicitacaoId), servidor, Perfil.SERVIDOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.recomendacao").value("INCONCLUSIVO"))
                .andExpect(jsonPath("$.similaridade").value(50))
                .andExpect(jsonPath("$.modelo").value("fake"));
    }

    @Test
    void parecerIaRetorna403ParaAlunoMesmoSendoDono() throws Exception {
        mvc.perform(como(get("/aproveitamentos/{id}/parecer-ia", solicitacaoId), aluno, Perfil.ALUNO))
                .andExpect(status().isForbidden());
        verifyNoInteractions(parecerIa);
    }

    @Test
    void parecerIaRetorna404SemParecer() throws Exception {
        when(solicitacoes.buscar(solicitacaoId)).thenReturn(solicitacao(aluno));
        when(parecerIa.maisRecente(solicitacaoId)).thenReturn(Optional.empty());

        mvc.perform(como(get("/aproveitamentos/{id}/parecer-ia", solicitacaoId), servidor, Perfil.SERVIDOR))
                .andExpect(status().isNotFound());
    }

    @Test
    void regerarParecerIaRetorna202QuandoPermitido() throws Exception {
        when(solicitacoes.buscar(solicitacaoId)).thenReturn(solicitacao(aluno));
        when(parecerIa.podeRegerar(servidor, Perfil.SERVIDOR, "ADS")).thenReturn(true);

        mvc.perform(como(post("/aproveitamentos/{id}/parecer-ia/regerar", solicitacaoId), servidor, Perfil.SERVIDOR))
                .andExpect(status().isAccepted());
        verify(parecerIa).regerar(solicitacaoId);
    }

    @Test
    void regerarParecerIaRetorna403SemPermissao() throws Exception {
        when(solicitacoes.buscar(solicitacaoId)).thenReturn(solicitacao(aluno));
        when(parecerIa.podeRegerar(aluno, Perfil.ALUNO, "ADS")).thenReturn(false);

        mvc.perform(como(post("/aproveitamentos/{id}/parecer-ia/regerar", solicitacaoId), aluno, Perfil.ALUNO))
                .andExpect(status().isForbidden());
        verify(parecerIa, never()).regerar(any());
    }
}
