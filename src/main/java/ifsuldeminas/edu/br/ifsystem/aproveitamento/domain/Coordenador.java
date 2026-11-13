package ifsuldeminas.edu.br.ifsystem.aproveitamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "coordenadores_aproveitamento", uniqueConstraints = {
        @UniqueConstraint(name = "uk_coordenador_professor_curso", columnNames = {"professor_id", "curso"})
})
public class Coordenador {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "professor_id", nullable = false)
    private UUID professorId;
    @Column(nullable = false)
    private String curso;
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected Coordenador() {
    }

    public Coordenador(UUID professorId, String curso) {
        this.professorId = professorId;
        this.curso = curso;
        this.criadoEm = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfessorId() {
        return professorId;
    }

    public String getCurso() {
        return curso;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
