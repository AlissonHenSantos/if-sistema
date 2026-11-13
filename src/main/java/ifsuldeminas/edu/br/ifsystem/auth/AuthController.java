package ifsuldeminas.edu.br.ifsystem.auth;

import ifsuldeminas.edu.br.ifsystem.security.JwtService;
import ifsuldeminas.edu.br.ifsystem.usuario.api.UsuarioDtos.Login;
import ifsuldeminas.edu.br.ifsystem.usuario.api.UsuarioDtos.Token;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.StatusUsuario;
import ifsuldeminas.edu.br.ifsystem.usuario.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UsuarioService usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UsuarioService usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public Token login(@RequestBody Login login) {
        if (login == null || login.cpf() == null || login.senha() == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "CPF e senha sao obrigatorios");
        var usuario = usuarios.buscarPorCpf(login.cpf());
        if (usuario.getStatus() != StatusUsuario.ATIVO || !encoder.matches(login.senha(), usuario.getSenha()))
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Credenciais invalidas");
        return new Token(jwt.gerar(usuario), usuario.getId(), usuario.getNome(), usuario.getPerfil());
    }
}
