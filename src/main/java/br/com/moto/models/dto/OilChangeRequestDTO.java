package br.com.moto.models.dto;

import br.com.moto.models.enums.OilType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code intervalKm}/{@code intervalMonths}: o intervalo escolhido — o padrão de {@code GET /oil-intervals} só sugere. */
public record OilChangeRequestDTO(
        @NotNull(message = "{oleo.dataObrigatoria}") LocalDate date,
        @NotNull(message = "{oleo.hodometroObrigatorio}") @Min(value = 0, message = "{oleo.hodometroNegativo}") Integer odometerKm,
        @NotNull(message = "{oleo.tipoObrigatorio}") OilType oilType,
        @Size(max = 60) String brand,
        @Size(max = 20) String viscosity,
        @DecimalMin(value = "0.0", message = "{oleo.custoInvalido}") @Digits(integer = 8, fraction = 2, message = "{oleo.custoInvalido}") BigDecimal cost,
        @NotNull(message = "{oleo.intervaloKmObrigatorio}") @Min(value = 1, message = "{oleo.intervaloKmInvalido}")
        @Max(value = 100000, message = "{oleo.intervaloKmInvalido}") Integer intervalKm,
        @NotNull(message = "{oleo.intervaloMesesObrigatorio}") @Min(value = 1, message = "{oleo.intervaloMesesInvalido}")
        @Max(value = 60, message = "{oleo.intervaloMesesInvalido}") Integer intervalMonths) {
}
