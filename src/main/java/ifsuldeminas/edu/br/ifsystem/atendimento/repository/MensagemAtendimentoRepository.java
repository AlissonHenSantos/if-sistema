package ifsuldeminas.edu.br.ifsystem.atendimento.repository;

import ifsuldeminas.edu.br.ifsystem.atendimento.domain.MensagemAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MensagemAtendimentoRepository extends JpaRepository<MensagemAtendimento, UUID> {
    List<MensagemAtendimento> findByAtendimentoIdOrderByDataEnvioAsc(UUID atendimentoId);
    Optional<MensagemAtendimento> findFirstByAtendimentoIdOrderByDataEnvioDesc(UUID atendimentoId);
}
