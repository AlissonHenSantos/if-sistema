package ifsuldeminas.edu.br.ifsystem.usuario.factory;

import ifsuldeminas.edu.br.ifsystem.usuario.api.UsuarioDtos.CriarUsuario;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import org.springframework.stereotype.Component;

abstract class FactoryBase implements UsuarioFactory {
    private final Perfil perfil;

    FactoryBase(Perfil perfil) {
        this.perfil = perfil;
    }

    public Usuario criar(CriarUsuario d, String hash) {
        boolean academico = perfil == Perfil.ALUNO;
        if (academico && (d.curso() == null || d.curso().isBlank() || d.periodo() == null))
            throw new IllegalArgumentException("Curso e periodo sao obrigatorios para aluno");
        if (!academico && (d.curso() != null || d.periodo() != null))
            throw new IllegalArgumentException("Curso e periodo nao se aplicam a este perfil");
        return new Usuario(d.nome(), d.cpf(), d.email(), d.matricula(), hash, d.telefone(),
                d.curso(), d.periodo(), perfil);
    }
}

@Component("alunoFactory")
class AlunoFactory extends FactoryBase {
    AlunoFactory() {
        super(Perfil.ALUNO);
    }
}

@Component("professorFactory")
class ProfessorFactory extends FactoryBase {
    ProfessorFactory() {
        super(Perfil.PROFESSOR);
    }
}

@Component("servidorFactory")
class ServidorFactory extends FactoryBase {
    ServidorFactory() {
        super(Perfil.SERVIDOR);
    }
}

@Component("administradorFactory")
class AdministradorFactory extends FactoryBase {
    AdministradorFactory() {
        super(Perfil.ADMINISTRADOR);
    }
}
