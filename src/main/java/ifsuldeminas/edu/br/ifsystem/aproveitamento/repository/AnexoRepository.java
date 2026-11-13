package ifsuldeminas.edu.br.ifsystem.aproveitamento.repository;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Anexo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnexoRepository extends JpaRepository<Anexo, UUID> {
    List<Anexo> findBySolicitacaoId(UUID solicitacaoId);

    Optional<Anexo> findByIdAndSolicitacaoId(UUID id, UUID solicitacaoId);
}
