package br.com.moto.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Um abastecimento como o cálculo de consumo o enxerga. {@code fullTank}: tanque completado. */
public record FuelEntry(LocalDate date, int odometerKm, BigDecimal liters, BigDecimal totalValue, boolean fullTank) {
}
