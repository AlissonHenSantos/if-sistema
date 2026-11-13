package ifsuldeminas.edu.br.ifsystem.atendimento.domain;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import jakarta.persistence.*;

import java.util.UUID;

/** Habilita um Usuario existente (perfil PROFESSOR ou SERVIDOR) como profissional de atendimento e guarda sua área. */
@Entity
@Table(name = "profissional_atendimento")
public class ProfissionalAtendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String area;

    @Column(nullable = false)
    private boolean ativo = true;

    protected ProfissionalAtendimento() {}

    public ProfissionalAtendimento(Usuario usuario, String area) {
        this.usuario = usuario;
        this.area = area;
    }

    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getArea() { return area; }
    public boolean isAtivo() { return ativo; }

    public void atualizarArea(String area) { this.area = area; }
    public void desativar() { this.ativo = false; }
}
