package ifsuldeminas.edu.br.ifsystem.aproveitamento.service;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.RecomendacaoIa;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.AnalisadorEquivalencia.EntradaAnalise;
import ifsuldeminas.edu.br.ifsystem.aproveitamento.service.AnalisadorEquivalencia.ResultadoAnalise;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalisadorFakeTest {

    private final AnalisadorFake analisador = new AnalisadorFake();

    @Test
    void devolveResultadoDeterministico() {
        EntradaAnalise entrada = new EntradaAnalise("Algoritmos", "Lacos e vetores", 60,
                "Programacao I", "Estruturas de controle", 80);

        ResultadoAnalise resultado = analisador.analisar(entrada);

        assertThat(resultado.recomendacao()).isEqualTo(RecomendacaoIa.INCONCLUSIVO);
        assertThat(resultado.similaridade()).isEqualTo(50);
        assertThat(resultado.modelo()).isEqualTo("fake");
        assertThat(resultado.justificativa()).isNotBlank();
        assertThat(analisador.analisar(entrada)).isEqualTo(resultado);
    }

    @Test
    void ignoraEntradaVazia() {
        ResultadoAnalise resultado = analisador.analisar(new EntradaAnalise(null, null, null, null, null, null));

        assertThat(resultado.recomendacao()).isEqualTo(RecomendacaoIa.INCONCLUSIVO);
        assertThat(resultado.similaridade()).isEqualTo(50);
    }
}
