package ifsuldeminas.edu.br.ifsystem.atendimento.domain;

import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.CategoriaAtendimento;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.StatusAtendimento;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "atendimento")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atendente_id", nullable = false)
    private Usuario atendente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategoriaAtendimento categoria;

    @Column(nullable = false, length = 150)
    private String assunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAtendimento status;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private OffsetDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private OffsetDateTime dataAtualizacao;

    @Column(name = "data_finalizacao")
    private OffsetDateTime dataFinalizacao;

    protected Atendimento() {}

    public Atendimento(Usuario aluno, Usuario atendente, CategoriaAtendimento categoria, String assunto) {
        this.aluno = aluno;
        this.atendente = atendente;
        this.categoria = categoria;
        this.assunto = assunto;
        this.status = StatusAtendimento.ABERTO;
    }

    @PrePersist
    void aoCriar() {
        OffsetDateTime agora = OffsetDateTime.now();
        this.dataCriacao = agora;
        this.dataAtualizacao = agora;
    }

    public void alterarStatus(StatusAtendimento novo) {
        this.status = novo;
        this.dataAtualizacao = OffsetDateTime.now();
    }

    public void finalizar() {
        this.status = StatusAtendimento.FINALIZADO;
        this.dataFinalizacao = OffsetDateTime.now();
        this.dataAtualizacao = this.dataFinalizacao;
    }

    public void registrarAtividade() { this.dataAtualizacao = OffsetDateTime.now(); }

    public boolean ehParticipante(UUID usuarioId) {
        return aluno.getId().equals(usuarioId) || atendente.getId().equals(usuarioId);
    }

    public boolean ehAtendente(UUID usuarioId) { return atendente.getId().equals(usuarioId); }

    public UUID getId() { return id; }
    public Usuario getAluno() { return aluno; }
    public Usuario getAtendente() { return atendente; }
    public CategoriaAtendimento getCategoria() { return categoria; }
    public String getAssunto() { return assunto; }
    public StatusAtendimento getStatus() { return status; }
    public OffsetDateTime getDataCriacao() { return dataCriacao; }
    public OffsetDateTime getDataAtualizacao() { return dataAtualizacao; }
    public OffsetDateTime getDataFinalizacao() { return dataFinalizacao; }
}
