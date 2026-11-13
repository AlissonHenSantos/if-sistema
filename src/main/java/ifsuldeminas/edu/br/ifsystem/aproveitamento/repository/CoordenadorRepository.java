package ifsuldeminas.edu.br.ifsystem.aproveitamento.repository;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Coordenador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CoordenadorRepository extends JpaRepository<Coordenador, UUID> {
    boolean existsByProfessorIdAndCurso(UUID professorId, String curso);
}
