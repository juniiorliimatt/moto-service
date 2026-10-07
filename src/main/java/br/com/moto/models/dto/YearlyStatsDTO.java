package br.com.moto.models.dto;

/** Um ano com algum registro (abastecimento, troca de óleo ou leitura de hodômetro). */
public record YearlyStatsDTO(int year, StatsDTO stats) {
}
