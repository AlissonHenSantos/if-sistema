package ifsuldeminas.edu.br.ifsystem.aproveitamento.service;

import ifsuldeminas.edu.br.ifsystem.aproveitamento.domain.RecomendacaoIa;

public interface AnalisadorEquivalencia {

    ResultadoAnalise analisar(EntradaAnalise entrada);

    record EntradaAnalise(String disciplinaOrigem, String ementaOrigem, Integer cargaHorariaOrigem,
                          String disciplinaDestino, String ementaDestino, Integer cargaHorariaDestino) {
    }

    record ResultadoAnalise(RecomendacaoIa recomendacao, Integer similaridade, String justificativa,
                            String modelo) {
    }
}
