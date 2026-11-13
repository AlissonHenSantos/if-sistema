package ifsuldeminas.edu.br.ifsystem.aproveitamento.service;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.RecomendacaoIa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Analisador deterministico, usado enquanto nao ha integracao real com a IA. */
@Component
@ConditionalOnProperty(name = "ifconnect.analisador", havingValue = "fake", matchIfMissing = true)
public class AnalisadorFake implements AnalisadorEquivalencia {

    static final String MODELO = "fake";

    @Override
    public ResultadoAnalise analisar(EntradaAnalise entrada) {
        return new ResultadoAnalise(RecomendacaoIa.INCONCLUSIVO, 50,
                "Analise simulada: nenhum modelo de IA foi consultado.", MODELO);
    }
}
