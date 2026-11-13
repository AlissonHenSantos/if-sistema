package ifsuldeminas.edu.br.ifsystem.aproveitamento.service;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Anexo;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.AnexoConteudo;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Solicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.AnexoConteudoRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.AnexoRepository;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.repository.SolicitacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Upload, download e remocao de anexos (somente PDF, ate 5 MB) de uma solicitacao. */
@Service
public class AnexoService {
    static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;
    static final String TIPO_PDF = "application/pdf";
    private static final byte[] ASSINATURA_PDF = {'%', 'P', 'D', 'F'};
    /** Status em que a solicitacao aceita alteracao de anexos (PENDENTE e a pendencia devolvida ao aluno). */
    private static final Set<StatusSolicitacao> STATUS_EDITAVEIS =
            Set.of(StatusSolicitacao.RASCUNHO, StatusSolicitacao.PENDENTE);

    private final SolicitacaoRepository solicitacoes;
    private final AnexoRepository anexos;
    private final AnexoConteudoRepository conteudos;

    public AnexoService(SolicitacaoRepository solicitacoes, AnexoRepository anexos,
                        AnexoConteudoRepository conteudos) {
        this.solicitacoes = solicitacoes;
        this.anexos = anexos;
        this.conteudos = conteudos;
    }

    @Transactional
    public Anexo anexar(UUID solicitacaoId, MultipartFile arquivo) {
        Solicitacao solicitacao = solicitacoes.findById(solicitacaoId)
                .orElseThrow(() -> naoEncontrado("Solicitacao nao encontrada"));

        if (arquivo == null || arquivo.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo vazio");
        if (arquivo.getSize() > TAMANHO_MAXIMO)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo maior que 5 MB");
        if (!pdf(arquivo.getContentType()))
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Apenas arquivos PDF sao aceitos");

        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao foi possivel ler o arquivo");
        }
        if (!comecaComAssinaturaPdf(bytes))
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "O conteudo nao e um PDF valido");

        exigirStatusEditavel(solicitacao);

        Anexo anexo = anexos.save(new Anexo(solicitacao, nomeSeguro(arquivo.getOriginalFilename()),
                TIPO_PDF, (long) bytes.length));
        conteudos.save(new AnexoConteudo(anexo, bytes));
        return anexo;
    }

    @Transactional(readOnly = true)
    public AnexoConteudo baixar(UUID solicitacaoId, UUID anexoId) {
        Anexo anexo = buscar(solicitacaoId, anexoId);
        return conteudos.findById(anexo.getId())
                .orElseThrow(() -> naoEncontrado("Anexo nao encontrado"));
    }

    @Transactional
    public void remover(UUID solicitacaoId, UUID anexoId) {
        Anexo anexo = buscar(solicitacaoId, anexoId);
        exigirStatusEditavel(anexo.getSolicitacao());
        conteudos.findById(anexo.getId()).ifPresent(conteudos::delete);
        anexos.delete(anexo);
    }

    private Anexo buscar(UUID solicitacaoId, UUID anexoId) {
        return anexos.findByIdAndSolicitacaoId(anexoId, solicitacaoId)
                .orElseThrow(() -> naoEncontrado("Anexo nao encontrado"));
    }

    private static void exigirStatusEditavel(Solicitacao solicitacao) {
        if (!STATUS_EDITAVEIS.contains(solicitacao.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Anexos so podem ser alterados em RASCUNHO ou PENDENTE");
    }

    private static ResponseStatusException naoEncontrado(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }

    private static boolean pdf(String contentType) {
        if (contentType == null)
            return false;
        String tipo = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return tipo.equals(TIPO_PDF);
    }

    private static boolean comecaComAssinaturaPdf(byte[] bytes) {
        if (bytes.length < ASSINATURA_PDF.length)
            return false;
        for (int i = 0; i < ASSINATURA_PDF.length; i++) {
            if (bytes[i] != ASSINATURA_PDF[i])
                return false;
        }
        return true;
    }

    /** Remove qualquer caminho do nome enviado pelo cliente; usa um nome padrao se vier vazio. */
    private static String nomeSeguro(String original) {
        if (original == null)
            return "anexo.pdf";
        String nome = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).trim();
        return nome.isEmpty() ? "anexo.pdf" : nome;
    }
}
