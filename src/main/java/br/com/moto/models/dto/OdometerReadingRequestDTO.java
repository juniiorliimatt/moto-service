package br.com.moto.models.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record OdometerReadingRequestDTO(
        @NotNull(message = "{leitura.dataObrigatoria}") LocalDate date,
        @NotNull(message = "{leitura.hodometroObrigatorio}") @Min(value = 0, message = "{leitura.hodometroNegativo}") Integer odometerKm) {
}
