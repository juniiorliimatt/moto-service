package br.com.moto.models.dto;

import java.math.BigDecimal;

/** Um mês da série anual. {@code spentChangePct}: variação do gasto contra o mês anterior <b>do mesmo ano</b> (nulo em janeiro ou quando o mês anterior não teve gasto). */
public record MonthlyStatsDTO(int year, int month, StatsDTO stats, BigDecimal spentChangePct) {
}
