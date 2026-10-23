package ifsuldeminas.edu.br.ifsystem.usuario.api;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.StatusUsuario;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;

import java.util.UUID;

public final class UsuarioDtos {
    private UsuarioDtos() {
    }

    public record CriarUsuario(
            String nome, String cpf, String email, String matricula,
            String senha, String telefone, String curso, Integer periodo) {
    }

    public record AtualizarUsuario(
            String nome, String email, String telefone, String curso, Integer periodo) {
    }

    public record AlterarStatus(StatusUsuario status) {
    }

    public record Login(String cpf, String senha) {
    }

    public record Token(String token, UUID id, String nome, Perfil perfil) {
    }

    public record Resposta(UUID id, String nome, String cpf, String email, String matricula,
                           String telefone, String curso, Integer periodo,
                           Perfil perfil, StatusUsuario status) {
        public static Resposta de(Usuario u) {
            return new Resposta(u.getId(), u.getNome(), u.getCpf(), u.getEmail(), u.getMatricula(),
                    u.getTelefone(), u.getCurso(), u.getPeriodo(), u.getPerfil(), u.getStatus());
        }
    }
}
