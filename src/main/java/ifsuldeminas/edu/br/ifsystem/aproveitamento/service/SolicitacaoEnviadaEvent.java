package ifsuldeminas.edu.br.ifsystem.aproveitamento.service;

import java.util.UUID;

/** Publicado quando a solicitacao sai de RASCUNHO (submissao do aluno). */
public record SolicitacaoEnviadaEvent(UUID solicitacaoId) {
}
