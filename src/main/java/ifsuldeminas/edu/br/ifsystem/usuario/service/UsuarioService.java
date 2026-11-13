package ifsuldeminas.edu.br.ifsystem.usuario.service;

import ifsuldeminas.edu.br.ifsystem.usuario.api.UsuarioDtos.AtualizarUsuario;
import ifsuldeminas.edu.br.ifsystem.usuario.api.UsuarioDtos.CriarUsuario;
import ifsuldeminas.edu.br.ifsystem.usuario.api.UsuarioDtos.Resposta;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.StatusUsuario;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import ifsuldeminas.edu.br.ifsystem.usuario.factory.UsuarioFactory;
import ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final Map<Perfil, UsuarioFactory> factories;


    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder,
                          Map<String, UsuarioFactory> factories) {
        this.repository = repository;
        this.encoder = encoder;
        this.factories = Map.of(Perfil.ALUNO, factories.get("alunoFactory"),
                Perfil.PROFESSOR, factories.get("professorFactory"),
                Perfil.SERVIDOR, factories.get("servidorFactory"),
                Perfil.ADMINISTRADOR, factories.get("administradorFactory"));
    }

    public Usuario buscarPorCpf(String cpf) {
        return repository.findByCpf(normalizarCpf(cpf))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"));
    }

    public Usuario criar(CriarUsuario d, Perfil perfil) {
        if (d == null || isBlank(d.nome()) || isBlank(d.cpf()) || isBlank(d.email())
                || isBlank(d.matricula()) || isBlank(d.senha()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados obrigatorios ausentes");

        if (d.senha().length() < 8)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve possuir ao menos 8 caracteres");
        validarEmail(d.email());
        String cpf = normalizarCpf(d.cpf());
        if (repository.existsByCpf(cpf) || repository.existsByEmail(d.email().toLowerCase())
                || repository.existsByMatricula(d.matricula()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF, email ou matricula ja cadastrados");
        CriarUsuario normalizado = new CriarUsuario(d.nome(), cpf, d.email().toLowerCase(),
                d.matricula(), d.senha(), d.telefone(), d.curso(), d.periodo());
        return repository.save(factories.get(perfil).criar(normalizado, encoder.encode(d.senha())));
    }

    public List<Resposta> listar() {
        return repository.findAll().stream().map(Resposta::de).toList();
    }

    public Resposta buscar(UUID id) {
        return Resposta.de(repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado")));
    }

    public Resposta atualizar(UUID id, AtualizarUsuario d) {
        if (d == null || isBlank(d.nome()) || isBlank(d.email()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados obrigatorios ausentes");
        Usuario u = get(id);
        validarEmail(d.email());
        u.atualizar(d.nome(), d.email().toLowerCase(),
                d.telefone(), d.curso(), d.periodo());
        return Resposta.de(repository.save(u));
    }

    public void status(UUID id, StatusUsuario status) {
        get(id).alterarStatus(status);
        repository.flush();
    }

    public void excluir(UUID id) {
        status(id, StatusUsuario.EXCLUIDO);
    }

    private Usuario get(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
    }

    private void validarEmail(String email) {
        if (!email.toLowerCase().endsWith("@ifsuldeminas.edu.br"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email institucional invalido");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizarCpf(String cpf) {
        return cpf.replaceAll("\\D", "");
    }
}
