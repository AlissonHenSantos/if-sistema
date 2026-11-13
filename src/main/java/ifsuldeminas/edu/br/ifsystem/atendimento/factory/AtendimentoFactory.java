package ifsuldeminas.edu.br.ifsystem.atendimento.factory;

import ifsuldeminas.edu.br.ifsystem.atendimento.domain.*;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.CategoriaAtendimento;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Padrão Factory do projeto: centraliza a criação dos objetos de domínio do módulo. */
@Component
public class AtendimentoFactory {

    public Atendimento criarAtendimento(Usuario aluno, Usuario atendente, CategoriaAtendimento categoria, String assunto) {
        return new Atendimento(aluno, atendente, categoria, assunto);
    }

    public MensagemAtendimento criarMensagem(Atendimento atendimento, Usuario remetente, String conteudo) {
        return new MensagemAtendimento(atendimento, remetente, conteudo);
    }

    public HorarioAtendimento criarHorario(ProfissionalAtendimento profissional, DayOfWeek dia, LocalTime inicio, LocalTime fim) {
        return new HorarioAtendimento(profissional, dia, inicio, fim);
    }
}
