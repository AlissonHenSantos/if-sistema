package ifsuldeminas.edu.br.ifsystem.aproveitamento.repository;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.HistoricoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, UUID> {
    List<HistoricoStatus> findBySolicitacaoIdOrderByCriadoEmAsc(UUID solicitacaoId);
}
