package ifsuldeminas.edu.br.ifsystem.aproveitamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "disciplinas_cursadas")
public class DisciplinaCursada {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private Solicitacao solicitacao;
    @Column(nullable = false)
    private String nome;
    @Column(nullable = false)
    private String instituicao;
    @Column(nullable = false)
    private Integer cargaHoraria;

    protected DisciplinaCursada() {
    }

    public DisciplinaCursada(Solicitacao solicitacao, String nome, String instituicao, Integer cargaHoraria) {
        this.solicitacao = solicitacao;
        this.nome = nome;
        this.instituicao = instituicao;
        this.cargaHoraria = cargaHoraria;
    }

    public UUID getId() {
        return id;
    }

    public Solicitacao getSolicitacao() {
        return solicitacao;
    }

    public String getNome() {
        return nome;
    }

    public String getInstituicao() {
        return instituicao;
    }

    public Integer getCargaHoraria() {
        return cargaHoraria;
    }
}
