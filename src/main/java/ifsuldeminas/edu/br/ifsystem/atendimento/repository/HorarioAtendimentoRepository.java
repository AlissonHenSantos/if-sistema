package ifsuldeminas.edu.br.ifsystem.atendimento.repository;

import ifsuldeminas.edu.br.ifsystem.atendimento.domain.HorarioAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface HorarioAtendimentoRepository extends JpaRepository<HorarioAtendimento, UUID> {
    List<HorarioAtendimento> findByProfissionalIdAndAtivoTrue(UUID profissionalId);
    List<HorarioAtendimento> findByProfissionalIdInAndAtivoTrue(Collection<UUID> profissionalIds);
}
