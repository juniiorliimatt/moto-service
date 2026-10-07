package br.com.moto.models.dto;

import br.com.moto.models.enums.OilType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OilChangeDTO(
        UUID id,
        LocalDate date,
        int odometerKm,
        OilType oilType,
        String brand,
        String viscosity,
        BigDecimal cost,
        int intervalKm,
        int intervalMonths) {
}
