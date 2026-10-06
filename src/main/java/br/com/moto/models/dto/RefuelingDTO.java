package br.com.moto.models.dto;

import br.com.moto.models.enums.FuelType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** {@code pricePerLiter} = valor total / litros (3 casas). */
public record RefuelingDTO(
        UUID id,
        LocalDate date,
        int odometerKm,
        BigDecimal liters,
        BigDecimal totalValue,
        BigDecimal pricePerLiter,
        String station,
        FuelType fuelType,
        boolean fullTank) {
}
