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

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "historico_status")
public class HistoricoStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private Solicitacao solicitacao;
    @Enumerated(EnumType.STRING)
    private StatusSolicitacao statusAnterior;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitacao statusNovo;
    private UUID responsavelId;
    @Column(length = 2000)
    private String observacao;
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected HistoricoStatus() {
    }

    public HistoricoStatus(Solicitacao solicitacao, StatusSolicitacao statusAnterior,
                           StatusSolicitacao statusNovo, UUID responsavelId, String observacao) {
        this.solicitacao = solicitacao;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.responsavelId = responsavelId;
        this.observacao = observacao;
        this.criadoEm = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Solicitacao getSolicitacao() {
        return solicitacao;
    }

    public StatusSolicitacao getStatusAnterior() {
        return statusAnterior;
    }

    public StatusSolicitacao getStatusNovo() {
        return statusNovo;
    }

    public UUID getResponsavelId() {
        return responsavelId;
    }

    public String getObservacao() {
        return observacao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
