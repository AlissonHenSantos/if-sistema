package ifsuldeminas.edu.br.ifsystem.aproveitamento.service;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Anexo;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.AnexoConteudo;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Solicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(AnexoService.class)
class AnexoServiceTest {

    private static final int CINCO_MB = 5 * 1024 * 1024;

    @Autowired
    private AnexoService service;
    @Autowired
    private TestEntityManager tem;

    // ---------- helpers ----------

    private Solicitacao solicitacao(StatusSolicitacao status) {
        Solicitacao s = new Solicitacao(UUID.randomUUID(), "ADS", "Banco de Dados", 60);
        s.alterarStatus(status);
        return tem.persist(s);
    }

    private static byte[] pdf(int tamanho) {
        byte[] bytes = new byte[tamanho];
        Arrays.fill(bytes, (byte) 'x');
        byte[] cabecalho = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(cabecalho, 0, bytes, 0, Math.min(cabecalho.length, tamanho));
        return bytes;
    }

    private static MockMultipartFile arquivo(String tipo, byte[] bytes) {
        return new MockMultipartFile("arquivo", "historico.pdf", tipo, bytes);
    }

    private static MockMultipartFile pdfValido() {
        return arquivo("application/pdf", pdf(2_000));
    }

    private Anexo anexoPersistido(Solicitacao s) {
        Anexo a = tem.persist(new Anexo(s, "historico.pdf", "application/pdf", 10L));
        tem.persist(new AnexoConteudo(a, pdf(10)));
        tem.flush();
        tem.clear();
        return a;
    }

    private static void assertStatus(Runnable acao, HttpStatus esperado) {
        assertThatThrownBy(acao::run)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(esperado));
    }

    private long totalAnexos() {
        return tem.getEntityManager().createQuery("select count(a) from Anexo a", Long.class).getSingleResult();
    }

    // ---------- anexar ----------

    @Test
    void anexarSalvaAnexoEConteudo() {
        Solicitacao s = solicitacao(StatusSolicitacao.RASCUNHO);
        byte[] bytes = pdf(3_000);

        Anexo anexo = service.anexar(s.getId(), arquivo("application/pdf", bytes));
        tem.flush();
        tem.clear();

        Anexo salvo = tem.find(Anexo.class, anexo.getId());
        assertThat(salvo.getNomeArquivo()).isEqualTo("historico.pdf");
        assertThat(salvo.getTipoConteudo()).isEqualTo("application/pdf");
        assertThat(salvo.getTamanho()).isEqualTo(3_000L);
        assertThat(salvo.getSolicitacao().getId()).isEqualTo(s.getId());
        assertThat(tem.find(AnexoConteudo.class, anexo.getId()).getConteudo()).containsExactly(bytes);
    }

    @ParameterizedTest
    @EnumSource(value = StatusSolicitacao.class, names = {"RASCUNHO", "PENDENTE"})
    void anexarAceitaStatusEditaveis(StatusSolicitacao status) {
        Solicitacao s = solicitacao(status);
        assertThat(service.anexar(s.getId(), pdfValido()).getId()).isNotNull();
    }

    @Test
    void anexarAceitaExatamenteCincoMb() {
        Solicitacao s = solicitacao(StatusSolicitacao.RASCUNHO);
        Anexo anexo = service.anexar(s.getId(), arquivo("application/pdf", pdf(CINCO_MB)));
        tem.flush();
        assertThat(anexo.getTamanho()).isEqualTo((long) CINCO_MB);
    }

    @Test
    void anexarAceitaContentTypeComParametros() {
        Solicitacao s = solicitacao(StatusSolicitacao.RASCUNHO);
        assertThat(service.anexar(s.getId(), arquivo("Application/PDF; charset=binary", pdf(100))).getId())
                .isNotNull();
    }

    @Test
    void anexarRemoveCaminhoDoNomeDoArquivo() {
        Solicitacao s = solicitacao(StatusSolicitacao.RASCUNHO);
        MockMultipartFile f = new MockMultipartFile("arquivo", "C:\\temp\\..\\historico.pdf",
                "application/pdf", pdf(100));
        assertThat(service.anexar(s.getId(), f).getNomeArquivo()).isEqualTo("historico.pdf");
    }

    @Test
    void anexarSolicitacaoInexistenteDa404() {
        assertStatus(() -> service.anexar(UUID.randomUUID(), pdfValido()), HttpStatus.NOT_FOUND);
    }

    @Test
    void anexarArquivoVazioDa400() {
        Solicitacao s = solicitacao(StatusSolicitacao.RASCUNHO);
        assertStatus(() -> service.anexar(s.getId(), arquivo("application/pdf", new byte[0])),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void anexarArquivoMaiorQueCincoMbDa413() {
        Solicitacao s = solicitacao(StatusSolicitacao.RASCUNHO);
        assertStatus(() -> service.anexar(s.getId(), arquivo("application/pdf", pdf(CINCO_MB + 1))),
                HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(totalAnexos()).isZero();
    }

    @Test
    void anexarContentTypeDiferenteDePdfDa415() {
        Solicitacao s = solicitacao(StatusSolicitacao.RASCUNHO);
        assertStatus(() -> service.anexar(s.getId(), arquivo("image/png", pdf(100))),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertStatus(() -> service.anexar(s.getId(), arquivo(null, pdf(100))),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(totalAnexos()).isZero();
    }

    @Test
    void anexarPdfFalsoPelosBytesDa415() {
        Solicitacao s = solicitacao(StatusSolicitacao.RASCUNHO);
        byte[] texto = "isto nao e um pdf".getBytes(StandardCharsets.UTF_8);
        assertStatus(() -> service.anexar(s.getId(), arquivo("application/pdf", texto)),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertStatus(() -> service.anexar(s.getId(), arquivo("application/pdf", new byte[]{'%', 'P'})),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(totalAnexos()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = StatusSolicitacao.class, mode = EnumSource.Mode.EXCLUDE, names = {"RASCUNHO", "PENDENTE"})
    void anexarEmStatusNaoEditavelDa409(StatusSolicitacao status) {
        Solicitacao s = solicitacao(status);
        assertStatus(() -> service.anexar(s.getId(), pdfValido()), HttpStatus.CONFLICT);
        assertThat(totalAnexos()).isZero();
    }

    // ---------- baixar ----------

    @Test
    void baixarRetornaConteudo() {
        Solicitacao s = solicitacao(StatusSolicitacao.ENVIADA);
        Anexo a = anexoPersistido(s);

        AnexoConteudo conteudo = service.baixar(s.getId(), a.getId());

        assertThat(conteudo.getConteudo()).containsExactly(pdf(10));
        assertThat(conteudo.getAnexo().getNomeArquivo()).isEqualTo("historico.pdf");
    }

    @Test
    void baixarAnexoInexistenteDa404() {
        Solicitacao s = solicitacao(StatusSolicitacao.RASCUNHO);
        assertStatus(() -> service.baixar(s.getId(), UUID.randomUUID()), HttpStatus.NOT_FOUND);
    }

    @Test
    void baixarAnexoDeOutraSolicitacaoDa404() {
        Solicitacao dona = solicitacao(StatusSolicitacao.RASCUNHO);
        Solicitacao outra = solicitacao(StatusSolicitacao.RASCUNHO);
        Anexo a = anexoPersistido(dona);

        assertStatus(() -> service.baixar(outra.getId(), a.getId()), HttpStatus.NOT_FOUND);
    }

    // ---------- remover ----------

    @ParameterizedTest
    @EnumSource(value = StatusSolicitacao.class, names = {"RASCUNHO", "PENDENTE"})
    void removerApagaAnexoEConteudo(StatusSolicitacao status) {
        Solicitacao s = solicitacao(status);
        Anexo a = anexoPersistido(s);

        service.remover(s.getId(), a.getId());
        tem.flush();
        tem.clear();

        assertThat(tem.find(Anexo.class, a.getId())).isNull();
        assertThat(tem.find(AnexoConteudo.class, a.getId())).isNull();
    }

    @Test
    void removerAnexoInexistenteDa404() {
        Solicitacao s = solicitacao(StatusSolicitacao.RASCUNHO);
        assertStatus(() -> service.remover(s.getId(), UUID.randomUUID()), HttpStatus.NOT_FOUND);
    }

    @Test
    void removerAnexoDeOutraSolicitacaoDa404() {
        Solicitacao dona = solicitacao(StatusSolicitacao.RASCUNHO);
        Solicitacao outra = solicitacao(StatusSolicitacao.RASCUNHO);
        Anexo a = anexoPersistido(dona);

        assertStatus(() -> service.remover(outra.getId(), a.getId()), HttpStatus.NOT_FOUND);
        assertThat(tem.find(Anexo.class, a.getId())).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = StatusSolicitacao.class, mode = EnumSource.Mode.EXCLUDE, names = {"RASCUNHO", "PENDENTE"})
    void removerEmStatusNaoEditavelDa409(StatusSolicitacao status) {
        Solicitacao s = solicitacao(status);
        Anexo a = anexoPersistido(s);

        assertStatus(() -> service.remover(s.getId(), a.getId()), HttpStatus.CONFLICT);
        assertThat(tem.find(Anexo.class, a.getId())).isNotNull();
    }
}
