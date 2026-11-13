package ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DevolverPendenciaTransicao implements TransicaoStatus {
    @Override
    public String acao() {
        return "devolver-pendencia";
    }

    @Override
    public Set<StatusSolicitacao> origens() {
        return Set.of(StatusSolicitacao.CONFERENCIA, StatusSolicitacao.ANALISE);
    }

    @Override
    public StatusSolicitacao destino() {
        return StatusSolicitacao.PENDENTE;
    }

    @Override
    public Perfil perfilExigido() {
        return Perfil.SERVIDOR;
    }
}
