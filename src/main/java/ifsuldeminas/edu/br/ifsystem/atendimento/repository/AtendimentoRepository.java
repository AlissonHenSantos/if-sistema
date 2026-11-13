package ifsuldeminas.edu.br.ifsystem.atendimento.repository;

import ifsuldeminas.edu.br.ifsystem.atendimento.domain.Atendimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AtendimentoRepository extends JpaRepository<Atendimento, UUID> {
    List<Atendimento> findByAlunoIdOrAtendenteIdOrderByDataAtualizacaoDesc(UUID alunoId, UUID atendenteId);
}
