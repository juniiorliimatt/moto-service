package br.com.moto.models.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Uma revisão do histórico de {@code Motorcycle} (Hibernate Envers). */
public record MotorcycleRevisionDTO(
        int revision,
        LocalDateTime changedAt,
        String changedBy,
        String revisionType,
        UUID id,
        String nickname,
        String model,
        int initialOdometerKm,
        boolean active) {
}
