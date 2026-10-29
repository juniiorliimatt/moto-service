package br.com.moto.domain;

import java.time.LocalDate;

/**
 * Trecho entre dois abastecimentos consecutivos: os {@code km} rodados (diferença de hodômetro). {@code date} é a do
 * abastecimento <b>final</b> — é a ela que o trecho pertence em qualquer janela. Não há km/l por trecho: com abastecimento
 * parcial os litros de um único abastecimento não representam o que foi gasto no trecho (dava valores como 70 km/l); o consumo
 * só é calculado por janela, em {@link ConsumptionCalculator#janela}.
 */
public record Segment(LocalDate date, int km) {
}
