package ifsuldeminas.edu.br.ifsystem.atendimento.service;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Único ponto do módulo que conhece a autenticação.
 * ADAPTAR: se o principal do seu JWT/filtro NÃO for o próprio Usuario (ex.: UserDetails com e-mail),
 * busque o Usuario pelo UsuarioRepository aqui.
 */
@Component
public class UsuarioLogadoProvider {

    public Usuario obter() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario usuario) {
            return usuario;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
    }
}
