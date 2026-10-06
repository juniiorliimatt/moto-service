package br.com.moto.models.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Corpo de criação/atualização de moto. {@code active} nulo = ativa. */
public record MotorcycleRequestDTO(
        @NotBlank(message = "{moto.apelidoObrigatorio}") @Size(max = 60) String nickname,
        @Size(max = 60) String brand,
        @NotBlank(message = "{moto.modeloObrigatorio}") @Size(max = 60) String model,
        @Min(1900) @Max(2100) Integer modelYear,
        @Size(max = 10) String plate,
        @NotNull(message = "{moto.hodometroObrigatorio}") @Min(value = 0, message = "{moto.hodometroNegativo}") Integer initialOdometerKm,
        @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 3, fraction = 2) BigDecimal tankCapacityLiters,
        Boolean active) {
}
