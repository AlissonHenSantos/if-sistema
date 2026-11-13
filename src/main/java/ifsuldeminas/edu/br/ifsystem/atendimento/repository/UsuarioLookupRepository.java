package ifsuldeminas.edu.br.ifsystem.atendimento.repository;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Consulta auxiliar do módulo (seed): o UsuarioRepository existente não possui busca por e-mail. */
public interface UsuarioLookupRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
}
