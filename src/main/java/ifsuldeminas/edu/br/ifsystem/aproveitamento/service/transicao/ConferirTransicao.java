package ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ConferirTransicao implements TransicaoStatus {
    @Override
    public String acao() {
        return "conferir";
    }

    @Override
    public Set<StatusSolicitacao> origens() {
        return Set.of(StatusSolicitacao.ENVIADA);
    }

    @Override
    public StatusSolicitacao destino() {
        return StatusSolicitacao.CONFERENCIA;
    }

    @Override
    public Perfil perfilExigido() {
        return Perfil.SERVIDOR;
    }
}
