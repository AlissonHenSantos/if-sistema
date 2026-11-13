package ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AnalisarTransicao implements TransicaoStatus {
    @Override
    public String acao() {
        return "analisar";
    }

    @Override
    public Set<StatusSolicitacao> origens() {
        return Set.of(StatusSolicitacao.ANALISE);
    }

    @Override
    public StatusSolicitacao destino() {
        return StatusSolicitacao.DECISAO;
    }

    @Override
    public Perfil perfilExigido() {
        return Perfil.PROFESSOR;
    }
}
