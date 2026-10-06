package br.com.moto.models.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MotorcycleDTO(
        UUID id,
        String nickname,
        String brand,
        String model,
        Integer modelYear,
        String plate,
        int initialOdometerKm,
        BigDecimal tankCapacityLiters,
        boolean active) {
}
