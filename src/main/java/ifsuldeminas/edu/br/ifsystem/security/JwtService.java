package ifsuldeminas.edu.br.ifsystem.security;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiration;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expiration) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalArgumentException("app.jwt.secret deve possuir pelo menos 32 bytes");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String gerar(Usuario usuario) {
        Date agora = new Date();
        return Jwts.builder().subject(usuario.getCpf())
                .claim("role", usuario.getPerfil().name())
                .issuedAt(agora).expiration(new Date(agora.getTime() + expiration))
                .signWith(key).compact();
    }

    public String cpf(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }
}
