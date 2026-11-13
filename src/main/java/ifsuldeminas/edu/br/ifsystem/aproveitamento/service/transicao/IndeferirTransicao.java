package ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class IndeferirTransicao implements TransicaoStatus {
    @Override
    public String acao() {
        return "indeferir";
    }

    @Override
    public Set<StatusSolicitacao> origens() {
        return Set.of(StatusSolicitacao.DECISAO);
    }

    @Override
    public StatusSolicitacao destino() {
        return StatusSolicitacao.INDEFERIDA;
    }

    @Override
    public Perfil perfilExigido() {
        return Perfil.PROFESSOR;
    }
}
