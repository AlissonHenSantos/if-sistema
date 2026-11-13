package ifsuldeminas.edu.br.ifsystem.aproveitamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "solicitacoes_aproveitamento")
public class Solicitacao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private UUID alunoId;
    @Column(nullable = false)
    private String curso;
    @Column(nullable = false)
    private String disciplinaDestino;
    @Column(nullable = false)
    private Integer cargaHorariaDestino;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitacao status;
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;
    @Column(nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Solicitacao() {
    }

    public Solicitacao(UUID alunoId, String curso, String disciplinaDestino, Integer cargaHorariaDestino) {
        this.alunoId = alunoId;
        this.curso = curso;
        this.disciplinaDestino = disciplinaDestino;
        this.cargaHorariaDestino = cargaHorariaDestino;
        this.status = StatusSolicitacao.RASCUNHO;
        this.criadoEm = OffsetDateTime.now();
        this.atualizadoEm = this.criadoEm;
    }

    @PreUpdate
    void atualizarData() {
        atualizadoEm = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlunoId() {
        return alunoId;
    }

    public String getCurso() {
        return curso;
    }

    public String getDisciplinaDestino() {
        return disciplinaDestino;
    }

    public Integer getCargaHorariaDestino() {
        return cargaHorariaDestino;
    }

    public StatusSolicitacao getStatus() {
        return status;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void alterarStatus(StatusSolicitacao status) {
        this.status = status;
    }
}
