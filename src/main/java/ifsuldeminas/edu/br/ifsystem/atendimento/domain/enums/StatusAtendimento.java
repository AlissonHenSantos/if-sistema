package ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums;

public enum StatusAtendimento {
    ABERTO, EM_ATENDIMENTO, AGUARDANDO_ALUNO, FINALIZADO, CANCELADO;

    public boolean encerrado() {
        return this == FINALIZADO || this == CANCELADO;
    }
}
