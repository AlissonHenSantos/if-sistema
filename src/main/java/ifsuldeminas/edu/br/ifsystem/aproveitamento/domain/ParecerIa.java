package ifsuldeminas.edu.br.ifsystem.aproveitamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Parecer gerado pela IA. Cada geracao grava um novo registro para a mesma solicitacao. */
@Entity
@Table(name = "pareceres_ia")
public class ParecerIa {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private Solicitacao solicitacao;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusParecerIa status;
    @Enumerated(EnumType.STRING)
    private RecomendacaoIa recomendacao;
    /** De 0 a 100; nulo enquanto nao ha resultado. */
    private Integer similaridade;
    @Column(columnDefinition = "text")
    private String justificativa;
    private String modelo;
    @Column(nullable = false)
    private OffsetDateTime geradoEm;
    private String erro;

    protected ParecerIa() {
    }

    public ParecerIa(Solicitacao solicitacao, StatusParecerIa status, RecomendacaoIa recomendacao,
                     Integer similaridade, String justificativa, String modelo, String erro) {
        validarSimilaridade(similaridade);
        this.solicitacao = solicitacao;
        this.status = status;
        this.recomendacao = recomendacao;
        this.similaridade = similaridade;
        this.justificativa = justificativa;
        this.modelo = modelo;
        this.erro = erro;
        this.geradoEm = OffsetDateTime.now();
    }

    /** Fecha um parecer PROCESSANDO com o resultado da analise. */
    public void concluir(RecomendacaoIa recomendacao, Integer similaridade, String justificativa, String modelo) {
        validarSimilaridade(similaridade);
        this.status = StatusParecerIa.CONCLUIDO;
        this.recomendacao = recomendacao;
        this.similaridade = similaridade;
        this.justificativa = justificativa;
        this.modelo = modelo;
        this.erro = null;
    }

    public void falhar(String erro) {
        this.status = StatusParecerIa.FALHOU;
        this.erro = erro;
    }

    private static void validarSimilaridade(Integer similaridade) {
        if (similaridade != null && (similaridade < 0 || similaridade > 100)) {
            throw new IllegalArgumentException("similaridade deve estar entre 0 e 100");
        }
    }

    public UUID getId() {
        return id;
    }

    public Solicitacao getSolicitacao() {
        return solicitacao;
    }

    public StatusParecerIa getStatus() {
        return status;
    }

    public RecomendacaoIa getRecomendacao() {
        return recomendacao;
    }

    public Integer getSimilaridade() {
        return similaridade;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public String getModelo() {
        return modelo;
    }

    public OffsetDateTime getGeradoEm() {
        return geradoEm;
    }

    public String getErro() {
        return erro;
    }
}
