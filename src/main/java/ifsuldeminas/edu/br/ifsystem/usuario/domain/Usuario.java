package ifsuldeminas.edu.br.ifsystem.usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_cpf", columnNames = "cpf"),
        @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_usuario_matricula", columnNames = "matricula")
})
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String nome;
    @Column(nullable = false, length = 11)
    private String cpf;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String matricula;
    @Column(nullable = false)
    private String senha;
    private String telefone;
    private String curso;
    private Integer periodo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusUsuario status;
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;
    @Column(nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Usuario() {
    }

    public Usuario(String nome, String cpf, String email, String matricula, String senha,
                   String telefone, String curso, Integer periodo, Perfil perfil) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.matricula = matricula;
        this.senha = senha;
        this.telefone = telefone;
        this.curso = curso;
        this.periodo = periodo;
        this.perfil = perfil;
        this.status = StatusUsuario.ATIVO;
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

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getSenha() {
        return senha;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getCurso() {
        return curso;
    }

    public Integer getPeriodo() {
        return periodo;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public StatusUsuario getStatus() {
        return status;
    }

    public void atualizar(String nome, String email, String telefone, String curso, Integer periodo) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.curso = curso;
        this.periodo = periodo;
    }

    public void alterarSenha(String senha) {
        this.senha = senha;
    }

    public void alterarStatus(StatusUsuario status) {
        this.status = status;
    }
}
