package br.com.moto.models.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Métricas de rodagem e consumo de um período ({@code from}..{@code to}, inclusivos).
 *
 * <ul>
 *   <li>{@code km}: rodados no período, pela linha do tempo de hodômetro (abastecimentos, trocas e leituras avulsas).</li>
 *   <li>{@code kmPerDay}: {@code km} / dias do período já decorridos (um período em curso não conta dias futuros).</li>
 *   <li>{@code litersRefueled}, {@code totalSpent}, {@code pricePerLiter}: todos os abastecimentos datados no período.</li>
 *   <li>{@code kmPerLiter}, {@code costPerKm}: km dos trechos que <b>terminam</b> no período ÷ litros (e gasto) de todos os
 *       abastecimentos do período — nunca média de médias; nulos quando não há km rodado nos trechos.</li>
 *   <li>{@code lowConfidence}: menos de 3 trechos no período — a média pode oscilar.</li>
 * </ul>
 */
public record StatsDTO(
        LocalDate from,
        LocalDate to,
        int km,
        BigDecimal kmPerDay,
        BigDecimal litersRefueled,
        BigDecimal totalSpent,
        BigDecimal pricePerLiter,
        BigDecimal kmPerLiter,
        BigDecimal costPerKm,
        int segmentCount,
        boolean lowConfidence) {
}
