package ifsuldeminas.edu.br.ifsystem.atendimento.config;

import ifsuldeminas.edu.br.ifsystem.atendimento.domain.HorarioAtendimento;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.ProfissionalAtendimento;
import ifsuldeminas.edu.br.ifsystem.atendimento.repository.HorarioAtendimentoRepository;
import ifsuldeminas.edu.br.ifsystem.atendimento.repository.ProfissionalAtendimentoRepository;
import ifsuldeminas.edu.br.ifsystem.atendimento.repository.UsuarioLookupRepository;
import ifsuldeminas.edu.br.ifsystem.usuario.api.UsuarioDtos.CriarUsuario;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import ifsuldeminas.edu.br.ifsystem.usuario.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Dados de demonstração (APENAS DESENVOLVIMENTO). Ative com: atendimento.seed.enabled=true
 * Cria, via UsuarioService (mesmas validações/factories do sistema), os professores e um aluno fictícios
 * caso o e-mail ainda não exista, e os habilita como profissionais de atendimento com horários fictícios.
 * Defina a senha com atendimento.seed.senha (mínimo 8 caracteres).
 */
@Component
@ConditionalOnProperty(name = "atendimento.seed.enabled", havingValue = "true")
public class AtendimentoDadosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AtendimentoDadosDemo.class);

    private record Demo(String nome, String email, String cpf, String matricula, String area,
                        DayOfWeek dia, LocalTime inicio, LocalTime fim) {}

    private static final List<Demo> DEMO = List.of(
            new Demo("Emerson Carvalho", "emerson.carvalho@ifsuldeminas.edu.br", "90000000001", "DEMO-PROF-001",
                    "Redes de Computadores", DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0)),
            new Demo("Fabio Corsini", "fabio.corsini@ifsuldeminas.edu.br", "90000000002", "DEMO-PROF-002",
                    "Programação", DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)),
            new Demo("Lucia Helena", "lucia.helena@ifsuldeminas.edu.br", "90000000003", "DEMO-PROF-003",
                    "Banco de Dados", DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0)),
            new Demo("André Marques", "andre.marques@ifsuldeminas.edu.br", "90000000004", "DEMO-PROF-004",
                    "Desenvolvimento Web", DayOfWeek.THURSDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)),
            new Demo("Fabio Junior", "fabio.junior@ifsuldeminas.edu.br", "90000000005", "DEMO-PROF-005",
                    "Engenharia de Software", DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)),
            new Demo("Matheus Eloy", "matheus.eloy@ifsuldeminas.edu.br", "90000000006", "DEMO-PROF-006",
                    "Inteligência Artificial", DayOfWeek.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(20, 0)));

    private final UsuarioLookupRepository lookup;
    private final UsuarioService usuarioService;
    private final ProfissionalAtendimentoRepository profissionais;
    private final HorarioAtendimentoRepository horarios;
    private final String senha;

    public AtendimentoDadosDemo(UsuarioLookupRepository lookup, UsuarioService usuarioService,
                                ProfissionalAtendimentoRepository profissionais,
                                HorarioAtendimentoRepository horarios,
                                @Value("${atendimento.seed.senha:}") String senha) {
        this.lookup = lookup;
        this.usuarioService = usuarioService;
        this.profissionais = profissionais;
        this.horarios = horarios;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (senha.length() < 8) {
            log.warn("Atendimento: defina atendimento.seed.senha (mín. 8 caracteres) para executar o seed.");
            return;
        }
        for (Demo d : DEMO) {
            Usuario u = lookup.findByEmail(d.email()).orElseGet(() -> usuarioService.criar(
                    new CriarUsuario(d.nome(), d.cpf(), d.email(), d.matricula(), senha, null, null, null),
                    Perfil.PROFESSOR));
            if (profissionais.findByUsuarioId(u.getId()).isPresent()) continue;
            ProfissionalAtendimento p = profissionais.save(new ProfissionalAtendimento(u, d.area()));
            horarios.save(new HorarioAtendimento(p, d.dia(), d.inicio(), d.fim()));
            log.info("Atendimento: profissional demo '{}' cadastrado.", d.nome());
        }
        String emailAluno = "aluno.demo@ifsuldeminas.edu.br";
        if (lookup.findByEmail(emailAluno).isEmpty()) {
            usuarioService.criar(new CriarUsuario("Aluno Demo", "90000000099", emailAluno, "DEMO-ALU-001",
                    senha, null, "Sistemas de Informação", 5), Perfil.ALUNO);
            log.info("Atendimento: aluno demo criado ({}).", emailAluno);
        }
    }
}
