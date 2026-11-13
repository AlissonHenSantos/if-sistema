package ifsuldeminas.edu.br.ifsystem.aproveitamento.service.transicao;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RegistrarTransicao implements TransicaoStatus {
    @Override
    public String acao() {
        return "registrar";
    }

    @Override
    public Set<StatusSolicitacao> origens() {
        return Set.of(StatusSolicitacao.DEFERIDA, StatusSolicitacao.INDEFERIDA);
    }

    @Override
    public StatusSolicitacao destino() {
        return StatusSolicitacao.REGISTRADA;
    }

    @Override
    public Perfil perfilExigido() {
        return Perfil.SERVIDOR;
    }
}
