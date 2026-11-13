package ifsuldeminas.edu.br.ifsystem.atendimento.service;

import ifsuldeminas.edu.br.ifsystem.atendimento.api.AtendimentoDtos.Janela;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.HorarioAtendimento;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.StatusDisponibilidade;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Regra de negócio: calcula se o profissional está disponível agora ou quando será o próximo atendimento. */
@Service
public class DisponibilidadeService {

    public record Disponibilidade(StatusDisponibilidade status, Janela janela) {}

    private final Clock clock;

    public DisponibilidadeService(Clock clock) {
        this.clock = clock;
    }

    public Disponibilidade calcular(List<HorarioAtendimento> horarios) {
        LocalDateTime agora = LocalDateTime.now(clock);
        DayOfWeek dia = agora.getDayOfWeek();
        LocalTime hora = agora.toLocalTime();

        for (HorarioAtendimento h : horarios) {
            if (h.cobre(dia, hora)) {
                return new Disponibilidade(StatusDisponibilidade.DISPONIVEL_AGORA, janela(h, agora.toLocalDate()));
            }
        }

        LocalDateTime melhorInicio = null;
        Janela melhorJanela = null;
        for (HorarioAtendimento h : horarios) {
            if (!h.isAtivo()) continue;
            for (int offset = 0; offset <= 7; offset++) {
                LocalDate data = agora.toLocalDate().plusDays(offset);
                if (data.getDayOfWeek() != h.getDiaSemana()) continue;
                LocalDateTime inicio = data.atTime(h.getHorarioInicio());
                if (inicio.isAfter(agora)) {
                    if (melhorInicio == null || inicio.isBefore(melhorInicio)) {
                        melhorInicio = inicio;
                        melhorJanela = janela(h, data);
                    }
                    break;
                }
            }
        }

        return melhorJanela != null
                ? new Disponibilidade(StatusDisponibilidade.PROXIMO_ATENDIMENTO, melhorJanela)
                : new Disponibilidade(StatusDisponibilidade.FORA_DO_HORARIO, null);
    }

    private Janela janela(HorarioAtendimento h, LocalDate data) {
        return new Janela(data, h.getDiaSemana(), h.getHorarioInicio(), h.getHorarioFim());
    }
}
