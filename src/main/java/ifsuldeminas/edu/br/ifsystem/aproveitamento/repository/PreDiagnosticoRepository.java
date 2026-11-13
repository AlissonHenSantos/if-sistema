package ifsuldeminas.edu.br.ifsystem.aproveitamento.repository;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.PreDiagnostico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PreDiagnosticoRepository extends JpaRepository<PreDiagnostico, UUID> {

    Optional<PreDiagnostico> findFirstBySolicitacaoIdOrderByCriadoEmDesc(UUID solicitacaoId);

    /** Pre-diagnostico mais recente da solicitacao (por criadoEm). */
    default Optional<PreDiagnostico> findBySolicitacaoId(UUID solicitacaoId) {
        return findFirstBySolicitacaoIdOrderByCriadoEmDesc(solicitacaoId);
    }
}
