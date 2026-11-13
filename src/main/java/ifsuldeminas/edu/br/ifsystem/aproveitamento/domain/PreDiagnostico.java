package ifsuldeminas.edu.br.ifsystem.aproveitamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pre_diagnosticos")
public class PreDiagnostico {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private Solicitacao solicitacao;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPreDiagnostico status;
    @Column(precision = 5, scale = 2)
    private BigDecimal percentualCarga;
    /** Nulo quando a carga horaria fica abaixo do limite e o conteudo nao e avaliado. */
    @Column(precision = 5, scale = 2)
    private BigDecimal percentualConteudo;
    @Column(columnDefinition = "text")
    private String justificativa;
    @Column(columnDefinition = "text")
    private String prompt;
    private String modelo;
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected PreDiagnostico() {
    }

    public PreDiagnostico(Solicitacao solicitacao, StatusPreDiagnostico status, BigDecimal percentualCarga,
                          BigDecimal percentualConteudo, String justificativa, String prompt, String modelo) {
        this.solicitacao = solicitacao;
        this.status = status;
        this.percentualCarga = percentualCarga;
        this.percentualConteudo = percentualConteudo;
        this.justificativa = justificativa;
        this.prompt = prompt;
        this.modelo = modelo;
        this.criadoEm = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Solicitacao getSolicitacao() {
        return solicitacao;
    }

    public StatusPreDiagnostico getStatus() {
        return status;
    }

    public BigDecimal getPercentualCarga() {
        return percentualCarga;
    }

    public BigDecimal getPercentualConteudo() {
        return percentualConteudo;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getModelo() {
        return modelo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
