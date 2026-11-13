package ifsuldeminas.edu.br.ifsystem.aproveitamento.repository;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.DisciplinaCursada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisciplinaCursadaRepository extends JpaRepository<DisciplinaCursada, UUID> {
    List<DisciplinaCursada> findBySolicitacaoId(UUID solicitacaoId);
}
