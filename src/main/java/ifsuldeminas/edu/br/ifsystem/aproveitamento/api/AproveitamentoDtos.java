package ifsuldeminas.edu.br.ifsystem.aproveitamento.api;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Anexo;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.DisciplinaCursada;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.HistoricoStatus;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.PreDiagnostico;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.Solicitacao;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusPreDiagnostico;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.StatusSolicitacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AproveitamentoDtos {
    private AproveitamentoDtos() {
    }

    // ---------- Entrada ----------

    public record CriarSolicitacao(
            @NotNull UUID alunoId,
            @NotBlank String curso,
            @NotBlank String disciplinaDestino,
            @NotNull @Positive Integer cargaHorariaDestino,
            @NotEmpty List<@Valid DisciplinaCursadaEntrada> disciplinas) {
    }

    public record DisciplinaCursadaEntrada(
            @NotBlank String nome,
            @NotBlank String instituicao,
            @NotNull @Positive Integer cargaHoraria) {
    }

    public record AcaoRequest(@Size(max = 2000) String observacao) {
    }

    // ---------- Saida ----------

    public record SolicitacaoResumo(UUID id, UUID alunoId, String curso, String disciplinaDestino,
                                    StatusSolicitacao status, OffsetDateTime atualizadoEm) {
        public static SolicitacaoResumo de(Solicitacao s) {
            return new SolicitacaoResumo(s.getId(), s.getAlunoId(), s.getCurso(), s.getDisciplinaDestino(),
                    s.getStatus(), s.getAtualizadoEm());
        }
    }

    public record DisciplinaCursadaResposta(UUID id, String nome, String instituicao, Integer cargaHoraria) {
        public static DisciplinaCursadaResposta de(DisciplinaCursada d) {
            return new DisciplinaCursadaResposta(d.getId(), d.getNome(), d.getInstituicao(), d.getCargaHoraria());
        }
    }

    public record AnexoResposta(UUID id, String nomeArquivo, Long tamanho, OffsetDateTime criadoEm) {
        public static AnexoResposta de(Anexo a) {
            return new AnexoResposta(a.getId(), a.getNomeArquivo(), a.getTamanho(), a.getCriadoEm());
        }
    }

    /** Nao expoe prompt nem modelo. */
    public record PreDiagnosticoResposta(StatusPreDiagnostico status, BigDecimal percentualCarga,
                                         BigDecimal percentualConteudo, String justificativa,
                                         OffsetDateTime criadoEm) {
        public static PreDiagnosticoResposta de(PreDiagnostico p) {
            return new PreDiagnosticoResposta(p.getStatus(), p.getPercentualCarga(), p.getPercentualConteudo(),
                    p.getJustificativa(), p.getCriadoEm());
        }
    }

    /** {@code preDiagnostico} e nulo quando a solicitacao ainda nao tem pre-diagnostico. */
    public record SolicitacaoDetalhe(UUID id, UUID alunoId, String curso, String disciplinaDestino,
                                     Integer cargaHorariaDestino, StatusSolicitacao status,
                                     OffsetDateTime criadoEm, OffsetDateTime atualizadoEm,
                                     List<DisciplinaCursadaResposta> disciplinas,
                                     List<AnexoResposta> anexos,
                                     PreDiagnosticoResposta preDiagnostico) {
        public static SolicitacaoDetalhe de(Solicitacao s, List<DisciplinaCursada> disciplinas,
                                            List<Anexo> anexos, PreDiagnostico preDiagnostico) {
            return new SolicitacaoDetalhe(s.getId(), s.getAlunoId(), s.getCurso(), s.getDisciplinaDestino(),
                    s.getCargaHorariaDestino(), s.getStatus(), s.getCriadoEm(), s.getAtualizadoEm(),
                    disciplinas.stream().map(DisciplinaCursadaResposta::de).toList(),
                    anexos.stream().map(AnexoResposta::de).toList(),
                    preDiagnostico == null ? null : PreDiagnosticoResposta.de(preDiagnostico));
        }
    }

    public record HistoricoItem(StatusSolicitacao statusAnterior, StatusSolicitacao statusNovo,
                                UUID responsavelId, String observacao, OffsetDateTime criadoEm) {
        public static HistoricoItem de(HistoricoStatus h) {
            return new HistoricoItem(h.getStatusAnterior(), h.getStatusNovo(), h.getResponsavelId(),
                    h.getObservacao(), h.getCriadoEm());
        }
    }
}
