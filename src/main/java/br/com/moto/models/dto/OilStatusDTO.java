package br.com.moto.models.dto;

import br.com.moto.domain.OilLimit;
import br.com.moto.domain.OilStatusLevel;
import java.time.LocalDate;

/**
 * Próxima troca de óleo. Moto que nunca trocou óleo: {@code lastChange} e todos os campos de
 * vencimento nulos (só {@code currentOdometerKm} vem preenchido). Restantes negativos = vencida
 * há tantos km/dias.
 */
public record OilStatusDTO(
        OilChangeDTO lastChange,
        int currentOdometerKm,
        LocalDate dueDate,
        Integer dueKm,
        Integer kmRemaining,
        Long daysRemaining,
        OilStatusLevel level,
        OilLimit limitedBy) {
}
