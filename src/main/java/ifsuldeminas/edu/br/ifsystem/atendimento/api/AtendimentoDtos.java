package ifsuldeminas.edu.br.ifsystem.atendimento.api;

import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.*;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class AtendimentoDtos {

    private AtendimentoDtos() {}

    // ---------- entrada ----------
    public record CriarAtendimento(UUID profissionalId, CategoriaAtendimento categoria,
                                   String assunto, String mensagemInicial) {}

    public record AlterarStatus(StatusAtendimento status) {}

    public record EnviarMensagem(String conteudo) {}

    public record CriarHorario(DayOfWeek diaSemana, LocalTime horarioInicio, LocalTime horarioFim) {}

    // ---------- saída ----------
    public record Janela(LocalDate data, DayOfWeek diaSemana, LocalTime inicio, LocalTime fim) {}

    public record HorarioResposta(UUID id, DayOfWeek diaSemana, LocalTime horarioInicio,
                                  LocalTime horarioFim, boolean ativo) {}

    public record ProfissionalResposta(UUID id, String nome, String iniciais, String area,
                                       Perfil perfil, StatusDisponibilidade status,
                                       Janela janela, List<HorarioResposta> horarios) {}

    public record UsuarioResumo(UUID id, String nome, String iniciais) {}

    public record AtendimentoResposta(UUID id, UsuarioResumo aluno, UsuarioResumo atendente,
                                      CategoriaAtendimento categoria, String assunto,
                                      StatusAtendimento status, OffsetDateTime dataCriacao,
                                      OffsetDateTime dataAtualizacao, OffsetDateTime dataFinalizacao,
                                      String ultimaMensagem) {}

    public record MensagemResposta(UUID id, UUID remetenteId, String remetenteNome, String conteudo,
                                   OffsetDateTime dataEnvio, boolean lida, boolean minha) {}

    public record ResumoPainel(long profissionaisDisponiveis, long emAndamento, long aguardandoResposta,
                               long finalizados, long novasSolicitacoes) {}
}
