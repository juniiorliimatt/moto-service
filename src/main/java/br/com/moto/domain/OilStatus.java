package br.com.moto.domain;

import java.time.LocalDate;

/** Restantes negativos significam troca vencida há tantos km/dias. */
public record OilStatus(
        LocalDate dueDate,
        int dueKm,
        int kmRemaining,
        long daysRemaining,
        OilStatusLevel level,
        OilLimit limitedBy) {
}
