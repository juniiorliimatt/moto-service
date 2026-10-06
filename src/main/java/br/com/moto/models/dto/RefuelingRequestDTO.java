package br.com.moto.models.dto;

import br.com.moto.models.enums.FuelType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Corpo de criação/atualização de abastecimento. {@code odometerKm} é o hodômetro total da moto;
 * {@code fullTank} nulo = tanque não completado.
 */
public record RefuelingRequestDTO(
        @NotNull(message = "{abastecimento.dataObrigatoria}") LocalDate date,
        @NotNull(message = "{abastecimento.hodometroObrigatorio}") @Min(value = 0, message = "{abastecimento.hodometroNegativo}") Integer odometerKm,
        @NotNull(message = "{abastecimento.litrosObrigatorio}")
        @DecimalMin(value = "0.0", inclusive = false, message = "{abastecimento.litrosInvalidos}")
        @Digits(integer = 4, fraction = 2, message = "{abastecimento.litrosInvalidos}") BigDecimal liters,
        @NotNull(message = "{abastecimento.valorObrigatorio}")
        @DecimalMin(value = "0.0", inclusive = false, message = "{abastecimento.valorInvalido}")
        @Digits(integer = 8, fraction = 2, message = "{abastecimento.valorInvalido}") BigDecimal totalValue,
        @Size(max = 120) String station,
        @NotNull(message = "{abastecimento.combustivelObrigatorio}") FuelType fuelType,
        Boolean fullTank) {
}
