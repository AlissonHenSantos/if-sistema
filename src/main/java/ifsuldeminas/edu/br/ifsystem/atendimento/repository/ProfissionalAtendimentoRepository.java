package ifsuldeminas.edu.br.ifsystem.atendimento.repository;

import ifsuldeminas.edu.br.ifsystem.atendimento.domain.ProfissionalAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfissionalAtendimentoRepository extends JpaRepository<ProfissionalAtendimento, UUID> {
    List<ProfissionalAtendimento> findByAtivoTrue();
    Optional<ProfissionalAtendimento> findByUsuarioId(UUID usuarioId);
}
