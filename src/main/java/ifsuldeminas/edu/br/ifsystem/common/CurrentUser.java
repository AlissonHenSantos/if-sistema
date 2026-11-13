package ifsuldeminas.edu.br.ifsystem.common;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUser {
    public Usuario require() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof Usuario user)
                || user.getStatus() != StatusUsuario.ATIVO)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        return user;
    }

    public boolean isOperator(Usuario user) {
        return user.getPerfil() == Perfil.ADMINISTRADOR || user.getPerfil() == Perfil.SERVIDOR;
    }

    public Usuario requireOperator() {
        Usuario user = require();
        if (!isOperator(user))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operator permission required");
        return user;
    }
}
