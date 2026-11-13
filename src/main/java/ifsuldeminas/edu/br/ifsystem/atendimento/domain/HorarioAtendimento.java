package ifsuldeminas.edu.br.ifsystem.atendimento.domain;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "horario_atendimento")
public class HorarioAtendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false)
    private ProfissionalAtendimento profissional;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 10)
    private DayOfWeek diaSemana;

    @Column(name = "horario_inicio", nullable = false)
    private LocalTime horarioInicio;

    @Column(name = "horario_fim", nullable = false)
    private LocalTime horarioFim;

    @Column(nullable = false)
    private boolean ativo = true;

    protected HorarioAtendimento() {}

    public HorarioAtendimento(ProfissionalAtendimento profissional, DayOfWeek diaSemana,
                              LocalTime horarioInicio, LocalTime horarioFim) {
        if (!horarioInicio.isBefore(horarioFim)) {
            throw new IllegalArgumentException("O horário de início deve ser anterior ao horário de fim.");
        }
        this.profissional = profissional;
        this.diaSemana = diaSemana;
        this.horarioInicio = horarioInicio;
        this.horarioFim = horarioFim;
    }

    /** O horário cobre o instante informado? (início inclusivo, fim exclusivo) */
    public boolean cobre(DayOfWeek dia, LocalTime hora) {
        return ativo && diaSemana == dia && !hora.isBefore(horarioInicio) && hora.isBefore(horarioFim);
    }

    public boolean conflitaCom(DayOfWeek dia, LocalTime inicio, LocalTime fim) {
        return ativo && diaSemana == dia && inicio.isBefore(horarioFim) && fim.isAfter(horarioInicio);
    }

    public UUID getId() { return id; }
    public ProfissionalAtendimento getProfissional() { return profissional; }
    public DayOfWeek getDiaSemana() { return diaSemana; }
    public LocalTime getHorarioInicio() { return horarioInicio; }
    public LocalTime getHorarioFim() { return horarioFim; }
    public boolean isAtivo() { return ativo; }
    public void desativar() { this.ativo = false; }
}
