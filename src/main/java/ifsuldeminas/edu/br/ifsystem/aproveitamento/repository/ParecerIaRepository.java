package ifsuldeminas.edu.br.ifsystem.aproveitamento.repository;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.ParecerIa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ParecerIaRepository extends JpaRepository<ParecerIa, UUID> {

    /** Parecer mais recente da solicitacao (por geradoEm). */
    Optional<ParecerIa> findFirstBySolicitacaoIdOrderByGeradoEmDesc(UUID solicitacaoId);
}
