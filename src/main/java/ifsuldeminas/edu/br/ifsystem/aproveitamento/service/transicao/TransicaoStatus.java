package ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;

import java.util.Set;

public interface TransicaoStatus {
    String acao();

    Set<StatusSolicitacao> origens();

    StatusSolicitacao destino();

    Perfil perfilExigido();

    default boolean exigeCoordenador() {
        return false;
    }
}
