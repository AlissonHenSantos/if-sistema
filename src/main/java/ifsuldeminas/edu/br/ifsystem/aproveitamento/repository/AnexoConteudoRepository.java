package ifsuldeminas.edu.br.ifsystem.aproveitamento.repository;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.AnexoConteudo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnexoConteudoRepository extends JpaRepository<AnexoConteudo, UUID> {
}
