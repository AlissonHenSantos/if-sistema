package ifsuldeminas.edu.br.ifsystem.aproveitamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "anexos_aproveitamento")
public class Anexo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private Solicitacao solicitacao;
    @Column(nullable = false)
    private String nomeArquivo;
    @Column(nullable = false)
    private String tipoConteudo;
    @Column(nullable = false)
    private Long tamanho;
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected Anexo() {
    }

    public Anexo(Solicitacao solicitacao, String nomeArquivo, String tipoConteudo, Long tamanho) {
        this.solicitacao = solicitacao;
        this.nomeArquivo = nomeArquivo;
        this.tipoConteudo = tipoConteudo;
        this.tamanho = tamanho;
        this.criadoEm = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Solicitacao getSolicitacao() {
        return solicitacao;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public String getTipoConteudo() {
        return tipoConteudo;
    }

    public Long getTamanho() {
        return tamanho;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
