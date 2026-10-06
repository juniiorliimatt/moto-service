package br.com.moto.domain;

import java.math.BigDecimal;

/**
 * Consumo agregado de um período. Litros e gasto contam <b>todos</b> os abastecimentos da data
 * na janela; km/l e custo por km vêm só dos trechos que terminam na janela (ponderados:
 * Σkm / Σlitros). {@code lowConfidence}: menos de
 * {@link ConsumptionCalculator#MIN_SEGMENTS_CONFIDENT} trechos — a média pode oscilar.
 */
public record ConsumptionWindow(
        BigDecimal litersRefueled,
        BigDecimal totalSpent,
        BigDecimal pricePerLiter,
        int segmentKm,
        BigDecimal kmPerLiter,
        BigDecimal costPerKm,
        int segmentCount,
        boolean lowConfidence,
        BigDecimal bestKmPerLiter,
        BigDecimal worstKmPerLiter,
        Integer longestSegmentKm) {
}
