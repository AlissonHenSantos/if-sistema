package ifsuldeminas.edu.br.ifsystem.atendimento.service;

import ifsuldeminas.edu.br.ifsystem.atendimento.domain.HorarioAtendimento;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.StatusDisponibilidade;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DisponibilidadeServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private DisponibilidadeService em(String dataHora) {
        Clock clock = Clock.fixed(LocalDateTime.parse(dataHora).atZone(ZONA).toInstant(), ZONA);
        return new DisponibilidadeService(clock);
    }

    private final List<HorarioAtendimento> segundaTarde =
            List.of(new HorarioAtendimento(null, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0)));

    @Test
    void disponivelDentroDoHorario() { // 16/11/2026 é segunda-feira
        var r = em("2026-11-16T15:00:00").calcular(segundaTarde);
        assertEquals(StatusDisponibilidade.DISPONIVEL_AGORA, r.status());
        assertEquals(LocalTime.of(16, 0), r.janela().fim());
    }

    @Test
    void fimDoHorarioEhExclusivo() {
        var r = em("2026-11-16T16:00:00").calcular(segundaTarde);
        assertEquals(StatusDisponibilidade.PROXIMO_ATENDIMENTO, r.status());
        assertEquals(LocalDate.of(2026, 11, 23), r.janela().data());
    }

    @Test
    void proximoAtendimentoNoMesmoDiaAntesDoHorario() {
        var r = em("2026-11-16T10:00:00").calcular(segundaTarde);
        assertEquals(StatusDisponibilidade.PROXIMO_ATENDIMENTO, r.status());
        assertEquals(LocalDate.of(2026, 11, 16), r.janela().data());
    }

    @Test
    void foraDoHorarioSemNenhumHorarioCadastrado() {
        var r = em("2026-11-16T10:00:00").calcular(List.of());
        assertEquals(StatusDisponibilidade.FORA_DO_HORARIO, r.status());
        assertNull(r.janela());
    }
}
