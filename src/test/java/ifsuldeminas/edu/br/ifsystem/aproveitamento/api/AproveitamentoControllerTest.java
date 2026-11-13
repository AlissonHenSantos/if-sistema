package ifsuldeminas.edu.br.ifsystem.aproveitamento.api;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.HistoricoItem;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.api.AproveitamentoDtos.SolicitacaoDetalhe;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Anexo;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.AnexoConteudo;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Solicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.AnexoService;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.FluxoStatusService;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.SolicitacaoService;
import ifsuldeminas.edu.br.ifsystem.security.JwtService;
import ifsuldeminas.edu.br.ifsystem.security.SecurityConfig;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import ifsuldeminas.edu.br.ifsystem.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    private AnexoService anexoService;
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

    private static <T extends AbstractMockHttpServletRequestBuilder<T>> T como(T req, UUID id, Perfil perfil) {
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

    // ---------- anexos ----------

    private final UUID anexoId = UUID.randomUUID();
    private final byte[] pdfBytes = "%PDF-1.7\nconteudo".getBytes(StandardCharsets.US_ASCII);

    private Anexo anexo(Solicitacao s) {
        Anexo a = new Anexo(s, "historico.pdf", "application/pdf", (long) pdfBytes.length);
        ReflectionTestUtils.setField(a, "id", anexoId);
        return a;
    }

    private void solicitacaoDoAluno(UUID dono) {
        when(solicitacoes.buscar(solicitacaoId)).thenReturn(solicitacao(dono));
    }

    private MockMultipartHttpServletRequestBuilder postAnexo() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "historico.pdf", "application/pdf", pdfBytes);
        return multipart("/aproveitamentos/{id}/anexos", solicitacaoId).file(arquivo);
    }

    private MockHttpServletRequestBuilder getAnexo() {
        return get("/aproveitamentos/{id}/anexos/{anexoId}", solicitacaoId, anexoId);
    }

    private MockHttpServletRequestBuilder deleteAnexo() {
        return delete("/aproveitamentos/{id}/anexos/{anexoId}", solicitacaoId, anexoId);
    }

    // POST anexos

    @Test
    void anexarRetorna201ComLocation() throws Exception {
        Solicitacao s = solicitacao(aluno);
        solicitacaoDoAluno(aluno);
        when(anexoService.anexar(eq(solicitacaoId), any(MultipartFile.class))).thenReturn(anexo(s));

        mvc.perform(como(postAnexo(), aluno, Perfil.ALUNO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/aproveitamentos/" + solicitacaoId + "/anexos/" + anexoId))
                .andExpect(jsonPath("$.id").value(anexoId.toString()))
                .andExpect(jsonPath("$.nomeArquivo").value("historico.pdf"))
                .andExpect(jsonPath("$.tamanho").value(pdfBytes.length));

        verify(anexoService).anexar(eq(solicitacaoId), any(MultipartFile.class));
    }

    @Test
    void anexarSemHeadersRetorna401() throws Exception {
        mvc.perform(postAnexo())
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(anexoService);
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"SERVIDOR", "PROFESSOR", "ADMINISTRADOR"})
    void anexarComPerfilDiferenteDeAlunoRetorna403(Perfil perfil) throws Exception {
        mvc.perform(como(postAnexo(), servidor, perfil))
                .andExpect(status().isForbidden());
        verify(anexoService, never()).anexar(any(), any());
    }

    @Test
    void anexarEmSolicitacaoDeOutroAlunoRetorna403() throws Exception {
        solicitacaoDoAluno(outroAluno);

        mvc.perform(como(postAnexo(), aluno, Perfil.ALUNO))
                .andExpect(status().isForbidden());
        verify(anexoService, never()).anexar(any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 409, 415})
    void anexarPropagaErrosDoServico(int codigo) throws Exception {
        solicitacaoDoAluno(aluno);
        doThrow(new ResponseStatusException(HttpStatus.valueOf(codigo), "erro do servico"))
                .when(anexoService).anexar(eq(solicitacaoId), any(MultipartFile.class));

        mvc.perform(como(postAnexo(), aluno, Perfil.ALUNO))
                .andExpect(status().is(codigo));
    }

    @Test
    void anexarSolicitacaoInexistenteRetorna404() throws Exception {
        when(solicitacoes.buscar(solicitacaoId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitacao nao encontrada"));

        mvc.perform(como(postAnexo(), aluno, Perfil.ALUNO))
                .andExpect(status().isNotFound());
        verify(anexoService, never()).anexar(any(), any());
    }

    @Test
    void anexarAcimaDoLimiteRetorna413() throws Exception {
        solicitacaoDoAluno(aluno);
        doThrow(new MaxUploadSizeExceededException(5L * 1024 * 1024))
                .when(anexoService).anexar(eq(solicitacaoId), any(MultipartFile.class));

        mvc.perform(como(postAnexo(), aluno, Perfil.ALUNO))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.erro").exists());
    }

    // GET download

    @Test
    void baixarRetorna200ComHeadersEBytes() throws Exception {
        Solicitacao s = solicitacao(aluno);
        solicitacaoDoAluno(aluno);
        when(anexoService.baixar(solicitacaoId, anexoId)).thenReturn(new AnexoConteudo(anexo(s), pdfBytes));

        mvc.perform(como(getAnexo(), aluno, Perfil.ALUNO))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"historico.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"SERVIDOR", "PROFESSOR"})
    void baixarPermiteServidorEProfessor(Perfil perfil) throws Exception {
        Solicitacao s = solicitacao(outroAluno);
        when(anexoService.baixar(solicitacaoId, anexoId)).thenReturn(new AnexoConteudo(anexo(s), pdfBytes));

        mvc.perform(como(getAnexo(), servidor, perfil))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void baixarComAdministradorRetorna403() throws Exception {
        mvc.perform(como(getAnexo(), servidor, Perfil.ADMINISTRADOR))
                .andExpect(status().isForbidden());
        verify(anexoService, never()).baixar(any(), any());
    }

    @Test
    void baixarAnexoDeOutroAlunoRetorna403() throws Exception {
        solicitacaoDoAluno(outroAluno);

        mvc.perform(como(getAnexo(), aluno, Perfil.ALUNO))
                .andExpect(status().isForbidden());
        verify(anexoService, never()).baixar(any(), any());
    }

    @Test
    void baixarSemHeadersRetorna401() throws Exception {
        mvc.perform(getAnexo())
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(anexoService);
    }

    @Test
    void baixarPropagaNaoEncontradoDoServico() throws Exception {
        when(anexoService.baixar(solicitacaoId, anexoId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Anexo nao encontrado"));

        mvc.perform(como(getAnexo(), servidor, Perfil.SERVIDOR))
                .andExpect(status().isNotFound());
    }

    // DELETE anexos

    @Test
    void removerRetorna204ParaAlunoDono() throws Exception {
        solicitacaoDoAluno(aluno);

        mvc.perform(como(deleteAnexo(), aluno, Perfil.ALUNO))
                .andExpect(status().isNoContent());
        verify(anexoService).remover(solicitacaoId, anexoId);
    }

    @Test
    void removerComServidorRetorna403() throws Exception {
        mvc.perform(como(deleteAnexo(), servidor, Perfil.SERVIDOR))
                .andExpect(status().isForbidden());
        verify(anexoService, never()).remover(any(), any());
    }

    @Test
    void removerAnexoDeOutroAlunoRetorna403() throws Exception {
        solicitacaoDoAluno(outroAluno);

        mvc.perform(como(deleteAnexo(), aluno, Perfil.ALUNO))
                .andExpect(status().isForbidden());
        verify(anexoService, never()).remover(any(), any());
    }

    @Test
    void removerPropagaConflitoDoServico() throws Exception {
        solicitacaoDoAluno(aluno);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Status nao permite remover"))
                .when(anexoService).remover(solicitacaoId, anexoId);

        mvc.perform(como(deleteAnexo(), aluno, Perfil.ALUNO))
                .andExpect(status().isConflict());
    }
}
