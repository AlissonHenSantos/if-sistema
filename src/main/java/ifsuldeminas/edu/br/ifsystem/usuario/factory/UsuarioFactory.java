package ifsuldeminas.edu.br.ifsystem.usuario.factory;

import ifsuldeminas.edu.br.ifsystem.usuario.api.UsuarioDtos.CriarUsuario;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;

public interface UsuarioFactory {
    Usuario criar(CriarUsuario dados, String senhaHash);
}
