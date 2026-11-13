package ifsuldeminas.edu.br.ifsystem.atendimento.service;

import ifsuldeminas.edu.br.ifsystem.atendimento.api.AtendimentoDtos.*;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.*;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.*;
import ifsuldeminas.edu.br.ifsystem.atendimento.factory.AtendimentoFactory;
import ifsuldeminas.edu.br.ifsystem.atendimento.repository.*;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.StatusUsuario;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AtendimentoService {

    private static final int MAX_ASSUNTO = 150;
    private static final int MAX_MENSAGEM = 2000;

    private final AtendimentoRepository atendimentos;
    private final MensagemAtendimentoRepository mensagens;
    private final HorarioAtendimentoRepository horarios;
    private final ProfissionalAtendimentoRepository perfis;
    private final DisponibilidadeService disponibilidade;
    private final UsuarioLogadoProvider usuarioLogado;
    private final AtendimentoFactory factory;

    public AtendimentoService(AtendimentoRepository atendimentos, MensagemAtendimentoRepository mensagens,
                              HorarioAtendimentoRepository horarios, ProfissionalAtendimentoRepository perfis,
                              DisponibilidadeService disponibilidade, UsuarioLogadoProvider usuarioLogado,
                              AtendimentoFactory factory) {
        this.atendimentos = atendimentos;
        this.mensagens = mensagens;
        this.horarios = horarios;
        this.perfis = perfis;
        this.disponibilidade = disponibilidade;
        this.usuarioLogado = usuarioLogado;
        this.factory = factory;
    }

    // ===================== PROFISSIONAIS =====================

    @Transactional(readOnly = true)
    public List<ProfissionalResposta> listarProfissionais(String busca, FiltroProfissional filtro) {
        Usuario eu = usuarioLogado.obter();
        FiltroProfissional f = filtro == null ? FiltroProfissional.TODOS : filtro;

        Set<UUID> meus = Set.of();
        if (f == FiltroProfissional.MEUS) {
            meus = atendimentos.findByAlunoIdOrAtendenteIdOrderByDataAtualizacaoDesc(eu.getId(), eu.getId()).stream()
                    .filter(a -> a.getAluno().getId().equals(eu.getId()))
                    .map(a -> a.getAtendente().getId())
                    .collect(Collectors.toSet());
        }
        final Set<UUID> meusIds = meus;
        String termo = normalizar(busca);

        return montarProfissionais().stream()
                .filter(p -> termo.isEmpty() || normalizar(p.nome()).contains(termo) || normalizar(p.area()).contains(termo))
                .filter(p -> switch (f) {
                    case TODOS -> true;
                    case DISPONIVEIS -> p.status() == StatusDisponibilidade.DISPONIVEL_AGORA;
                    case MEUS -> meusIds.contains(p.id());
                    case PROFESSORES -> p.perfil() == Perfil.PROFESSOR;
                    case SERVIDORES -> p.perfil() == Perfil.SERVIDOR;
                })
                .sorted(Comparator.comparing((ProfissionalResposta p) -> p.status() != StatusDisponibilidade.DISPONIVEL_AGORA)
                        .thenComparing(ProfissionalResposta::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumoPainel painel() {
        Usuario eu = usuarioLogado.obter();
        long disponiveis = montarProfissionais().stream()
                .filter(p -> p.status() == StatusDisponibilidade.DISPONIVEL_AGORA).count();
        List<Atendimento> meus = atendimentos.findByAlunoIdOrAtendenteIdOrderByDataAtualizacaoDesc(eu.getId(), eu.getId());
        long emAndamento = contar(meus, StatusAtendimento.EM_ATENDIMENTO);
        long aguardando = contar(meus, StatusAtendimento.ABERTO) + contar(meus, StatusAtendimento.AGUARDANDO_ALUNO);
        long finalizados = contar(meus, StatusAtendimento.FINALIZADO);
        long novas = meus.stream()
                .filter(a -> a.ehAtendente(eu.getId()) && a.getStatus() == StatusAtendimento.ABERTO).count();
        return new ResumoPainel(disponiveis, emAndamento, aguardando, finalizados, novas);
    }

    // ===================== ATENDIMENTOS =====================

    @Transactional
    public AtendimentoResposta criar(CriarAtendimento dto) {
        Usuario aluno = usuarioLogado.obter();
        if (aluno.getPerfil() != Perfil.ALUNO) {
            throw erro(HttpStatus.FORBIDDEN, "Apenas alunos podem abrir solicitações de atendimento.");
        }
        if (dto == null || dto.profissionalId() == null) throw erro(HttpStatus.BAD_REQUEST, "Informe o profissional.");
        if (dto.categoria() == null) throw erro(HttpStatus.BAD_REQUEST, "Informe a categoria.");
        String assunto = textoObrigatorio(dto.assunto(), "assunto", MAX_ASSUNTO);
        String texto = textoObrigatorio(dto.mensagemInicial(), "mensagem inicial", MAX_MENSAGEM);
        if (dto.profissionalId().equals(aluno.getId())) {
            throw erro(HttpStatus.BAD_REQUEST, "Você não pode abrir um atendimento consigo mesmo.");
        }
        ProfissionalAtendimento profissional = perfis.findByUsuarioId(dto.profissionalId())
                .filter(p -> p.isAtivo() && p.getUsuario().getStatus() == StatusUsuario.ATIVO)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Profissional não encontrado."));

        // Fora do horário também é permitido: a solicitação fica registrada para resposta posterior.
        Atendimento a = atendimentos.save(factory.criarAtendimento(aluno, profissional.getUsuario(), dto.categoria(), assunto));
        mensagens.save(factory.criarMensagem(a, aluno, texto));
        return resposta(a);
    }

    @Transactional(readOnly = true)
    public List<AtendimentoResposta> meus(StatusAtendimento status) {
        Usuario eu = usuarioLogado.obter();
        return atendimentos.findByAlunoIdOrAtendenteIdOrderByDataAtualizacaoDesc(eu.getId(), eu.getId()).stream()
                .filter(a -> status == null || a.getStatus() == status)
                .map(this::resposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public AtendimentoResposta buscar(UUID id) {
        return resposta(carregar(id, usuarioLogado.obter()));
    }

    @Transactional
    public AtendimentoResposta alterarStatus(UUID id, AlterarStatus dto) {
        Usuario eu = usuarioLogado.obter();
        Atendimento a = carregar(id, eu);
        if (dto == null || dto.status() == null) throw erro(HttpStatus.BAD_REQUEST, "Informe o novo status.");
        if (a.getStatus().encerrado()) throw erro(HttpStatus.CONFLICT, "O atendimento já está encerrado.");

        boolean atendente = a.ehAtendente(eu.getId());
        if (!atendente && dto.status() != StatusAtendimento.CANCELADO) {
            throw erro(HttpStatus.FORBIDDEN, "O aluno só pode cancelar o próprio atendimento.");
        }
        switch (dto.status()) {
            case FINALIZADO -> a.finalizar();
            case ABERTO -> throw erro(HttpStatus.BAD_REQUEST, "Não é possível reabrir um atendimento por esta rota.");
            default -> a.alterarStatus(dto.status());
        }
        return resposta(atendimentos.save(a));
    }

    @Transactional
    public AtendimentoResposta finalizar(UUID id) {
        Usuario eu = usuarioLogado.obter();
        Atendimento a = carregar(id, eu);
        if (!a.ehAtendente(eu.getId())) throw erro(HttpStatus.FORBIDDEN, "Apenas o profissional pode finalizar o atendimento.");
        if (a.getStatus().encerrado()) throw erro(HttpStatus.CONFLICT, "O atendimento já está encerrado.");
        a.finalizar();
        return resposta(atendimentos.save(a));
    }

    // ===================== MENSAGENS =====================

    @Transactional
    public List<MensagemResposta> listarMensagens(UUID atendimentoId) {
        Usuario eu = usuarioLogado.obter();
        carregar(atendimentoId, eu);
        List<MensagemAtendimento> lista = mensagens.findByAtendimentoIdOrderByDataEnvioAsc(atendimentoId);
        lista.stream()
                .filter(m -> !m.getRemetente().getId().equals(eu.getId()) && !m.isLida())
                .forEach(MensagemAtendimento::marcarComoLida);
        return lista.stream().map(m -> mensagemResposta(m, eu)).toList();
    }

    @Transactional
    public MensagemResposta enviarMensagem(UUID atendimentoId, EnviarMensagem dto) {
        Usuario eu = usuarioLogado.obter();
        Atendimento a = carregar(atendimentoId, eu);
        if (a.getStatus().encerrado()) throw erro(HttpStatus.CONFLICT, "O atendimento está encerrado.");
        String texto = textoObrigatorio(dto == null ? null : dto.conteudo(), "mensagem", MAX_MENSAGEM);

        MensagemAtendimento m = mensagens.save(factory.criarMensagem(a, eu, texto));

        boolean atendente = a.ehAtendente(eu.getId());
        if (atendente && a.getStatus() == StatusAtendimento.ABERTO) {
            a.alterarStatus(StatusAtendimento.EM_ATENDIMENTO);
        } else if (!atendente && a.getStatus() == StatusAtendimento.AGUARDANDO_ALUNO) {
            a.alterarStatus(StatusAtendimento.EM_ATENDIMENTO);
        } else {
            a.registrarAtividade();
        }
        atendimentos.save(a);
        return mensagemResposta(m, eu);
    }

    // ===================== HORÁRIOS =====================

    @Transactional(readOnly = true)
    public List<HorarioResposta> listarHorarios(UUID profissionalUsuarioId) {
        UUID alvo = profissionalUsuarioId != null ? profissionalUsuarioId : usuarioLogado.obter().getId();
        ProfissionalAtendimento p = perfis.findByUsuarioId(alvo)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Profissional não encontrado."));
        return horarios.findByProfissionalIdAndAtivoTrue(p.getId()).stream().map(this::horarioResposta).toList();
    }

    @Transactional
    public HorarioResposta criarHorario(CriarHorario dto) {
        Usuario eu = usuarioLogado.obter();
        ProfissionalAtendimento p = perfis.findByUsuarioId(eu.getId()).filter(ProfissionalAtendimento::isAtivo)
                .orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Apenas profissionais podem cadastrar horários."));
        if (dto == null || dto.diaSemana() == null || dto.horarioInicio() == null || dto.horarioFim() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe dia da semana, início e fim.");
        }
        if (!dto.horarioInicio().isBefore(dto.horarioFim())) {
            throw erro(HttpStatus.BAD_REQUEST, "O horário de início deve ser anterior ao de fim.");
        }
        boolean conflito = horarios.findByProfissionalIdAndAtivoTrue(p.getId()).stream()
                .anyMatch(h -> h.conflitaCom(dto.diaSemana(), dto.horarioInicio(), dto.horarioFim()));
        if (conflito) throw erro(HttpStatus.CONFLICT, "Já existe um horário que conflita com este período.");
        return horarioResposta(horarios.save(factory.criarHorario(p, dto.diaSemana(), dto.horarioInicio(), dto.horarioFim())));
    }

    // ===================== INTERNOS =====================

    private List<ProfissionalResposta> montarProfissionais() {
        List<ProfissionalAtendimento> lista = perfis.findByAtivoTrue().stream()
                .filter(p -> p.getUsuario().getStatus() == StatusUsuario.ATIVO).toList();
        Map<UUID, List<HorarioAtendimento>> porPerfil = lista.isEmpty() ? Map.of()
                : horarios.findByProfissionalIdInAndAtivoTrue(lista.stream().map(ProfissionalAtendimento::getId).toList())
                .stream().collect(Collectors.groupingBy(h -> h.getProfissional().getId()));

        return lista.stream().map(p -> {
            List<HorarioAtendimento> hs = porPerfil.getOrDefault(p.getId(), List.of());
            var disp = disponibilidade.calcular(hs);
            return new ProfissionalResposta(p.getUsuario().getId(), p.getUsuario().getNome(),
                    iniciais(p.getUsuario().getNome()), p.getArea(), p.getUsuario().getPerfil(),
                    disp.status(), disp.janela(), hs.stream().map(this::horarioResposta).toList());
        }).toList();
    }

    private Atendimento carregar(UUID id, Usuario eu) {
        Atendimento a = atendimentos.findById(id)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Atendimento não encontrado."));
        if (!a.ehParticipante(eu.getId())) {
            throw erro(HttpStatus.FORBIDDEN, "Você não tem acesso a este atendimento.");
        }
        return a;
    }

    private AtendimentoResposta resposta(Atendimento a) {
        String previa = mensagens.findFirstByAtendimentoIdOrderByDataEnvioDesc(a.getId())
                .map(MensagemAtendimento::getConteudo).map(this::cortar).orElse(null);
        return new AtendimentoResposta(a.getId(), resumo(a.getAluno()), resumo(a.getAtendente()), a.getCategoria(),
                a.getAssunto(), a.getStatus(), a.getDataCriacao(), a.getDataAtualizacao(), a.getDataFinalizacao(), previa);
    }

    private MensagemResposta mensagemResposta(MensagemAtendimento m, Usuario eu) {
        return new MensagemResposta(m.getId(), m.getRemetente().getId(), m.getRemetente().getNome(),
                m.getConteudo(), m.getDataEnvio(), m.isLida(), m.getRemetente().getId().equals(eu.getId()));
    }

    private HorarioResposta horarioResposta(HorarioAtendimento h) {
        return new HorarioResposta(h.getId(), h.getDiaSemana(), h.getHorarioInicio(), h.getHorarioFim(), h.isAtivo());
    }

    private UsuarioResumo resumo(Usuario u) { return new UsuarioResumo(u.getId(), u.getNome(), iniciais(u.getNome())); }

    private long contar(List<Atendimento> lista, StatusAtendimento s) {
        return lista.stream().filter(a -> a.getStatus() == s).count();
    }

    private String textoObrigatorio(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) throw erro(HttpStatus.BAD_REQUEST, "O campo " + campo + " é obrigatório.");
        String t = valor.trim();
        if (t.length() > max) throw erro(HttpStatus.BAD_REQUEST, "O campo " + campo + " excede " + max + " caracteres.");
        return t;
    }

    private String cortar(String s) { return s.length() <= 80 ? s : s.substring(0, 77) + "..."; }

    static String iniciais(String nome) {
        if (nome == null || nome.isBlank()) return "?";
        String[] p = nome.trim().split("\\s+");
        String ini = p.length == 1 ? p[0].substring(0, 1) : p[0].substring(0, 1) + p[p.length - 1].substring(0, 1);
        return ini.toUpperCase();
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private static ResponseStatusException erro(HttpStatus status, String msg) {
        return new ResponseStatusException(status, msg);
    }
}
