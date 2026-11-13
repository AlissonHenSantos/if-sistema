package ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EnviarTransicao implements TransicaoStatus {
    @Override
    public String acao() {
        return "enviar";
    }

    @Override
    public Set<StatusSolicitacao> origens() {
        return Set.of(StatusSolicitacao.RASCUNHO);
    }

    @Override
    public StatusSolicitacao destino() {
        return StatusSolicitacao.ENVIADA;
    }

    @Override
    public Perfil perfilExigido() {
        return Perfil.ALUNO;
    }
}
