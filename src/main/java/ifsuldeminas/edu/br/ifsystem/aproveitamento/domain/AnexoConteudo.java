package ifsuldeminas.edu.br.ifsystem.aproveitamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "anexos_conteudo")
public class AnexoConteudo {
    @Id
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id")
    private Anexo anexo;
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, length = 5_242_880)
    private byte[] conteudo;

    protected AnexoConteudo() {
    }

    public AnexoConteudo(Anexo anexo, byte[] conteudo) {
        this.anexo = anexo;
        this.conteudo = conteudo;
    }

    public UUID getId() {
        return id;
    }

    public Anexo getAnexo() {
        return anexo;
    }

    public byte[] getConteudo() {
        return conteudo;
    }
}
