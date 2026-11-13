package ifsuldeminas.edu.br.ifsystem.atendimento.domain;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mensagem_atendimento")
public class MensagemAtendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atendimento_id", nullable = false)
    private Atendimento atendimento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;

    @Column(nullable = false, length = 2000)
    private String conteudo;

    @Column(name = "data_envio", nullable = false, updatable = false)
    private OffsetDateTime dataEnvio;

    @Column(nullable = false)
    private boolean lida = false;

    protected MensagemAtendimento() {}

    public MensagemAtendimento(Atendimento atendimento, Usuario remetente, String conteudo) {
        this.atendimento = atendimento;
        this.remetente = remetente;
        this.conteudo = conteudo;
    }

    @PrePersist
    void aoCriar() { this.dataEnvio = OffsetDateTime.now(); }

    public void marcarComoLida() { this.lida = true; }

    public UUID getId() { return id; }
    public Atendimento getAtendimento() { return atendimento; }
    public Usuario getRemetente() { return remetente; }
    public String getConteudo() { return conteudo; }
    public OffsetDateTime getDataEnvio() { return dataEnvio; }
    public boolean isLida() { return lida; }
}
