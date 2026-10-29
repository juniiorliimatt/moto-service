package br.com.moto.domain;

import java.math.BigDecimal;

/**
 * Consumo agregado de um período. Litros e gasto contam <b>todos</b> os abastecimentos da data na janela; {@code kmPerLiter} e
 * {@code costPerKm} dividem os km dos trechos que terminam na janela por esses litros e esse gasto (ponderados, nunca média de
 * médias). {@code lowConfidence}: menos de {@link ConsumptionCalculator#MIN_SEGMENTS_CONFIDENT} trechos — a média pode oscilar.
 */
public record ConsumptionWindow(
        BigDecimal litersRefueled,
        BigDecimal totalSpent,
        BigDecimal pricePerLiter,
        int segmentKm,
        BigDecimal kmPerLiter,
        BigDecimal costPerKm,
        int segmentCount,
        boolean lowConfidence) {
}
