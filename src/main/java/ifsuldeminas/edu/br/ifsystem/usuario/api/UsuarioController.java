package ifsuldeminas.edu.br.ifsystem.usuario.api;

import ifsuldeminas.edu.br.ifsystem.usuario.api.UsuarioDtos.AlterarStatus;
import ifsuldeminas.edu.br.ifsystem.usuario.api.UsuarioDtos.AtualizarUsuario;
import ifsuldeminas.edu.br.ifsystem.usuario.api.UsuarioDtos.CriarUsuario;
import ifsuldeminas.edu.br.ifsystem.usuario.api.UsuarioDtos.Resposta;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import ifsuldeminas.edu.br.ifsystem.usuario.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class UsuarioController {
    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/administrador/usuarios")
    public ResponseEntity<Resposta> criarAdministrador(@RequestBody CriarUsuario d) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Resposta.de(service.criar(d, Perfil.ADMINISTRADOR)));
    }

    @PostMapping("/administrador/servidores")
    public ResponseEntity<Resposta> criarServidor(@RequestBody CriarUsuario d) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Resposta.de(service.criar(d, Perfil.SERVIDOR)));
    }

    @PostMapping("/servidor/alunos")
    public ResponseEntity<Resposta> criarAluno(@RequestBody CriarUsuario d) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Resposta.de(service.criar(d, Perfil.ALUNO)));
    }

    @PostMapping("/servidor/professores")
    public ResponseEntity<Resposta> criarProfessor(@RequestBody CriarUsuario d) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Resposta.de(service.criar(d, Perfil.PROFESSOR)));
    }

    @GetMapping("/administrador/usuarios")
    public List<Resposta> listar() {
        return service.listar();
    }

    @GetMapping("/administrador/usuarios/{id}")
    public Resposta buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PutMapping("/administrador/usuarios/{id}")
    public Resposta atualizar(@PathVariable UUID id, @RequestBody AtualizarUsuario d) {
        return service.atualizar(id, d);
    }

    @PatchMapping("/administrador/usuarios/{id}/status")
    public ResponseEntity<Void> status(@PathVariable UUID id, @RequestBody AlterarStatus d) {
        if (d == null || d.status() == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Status obrigatorio");
        service.status(id, d.status());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/administrador/usuarios/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
