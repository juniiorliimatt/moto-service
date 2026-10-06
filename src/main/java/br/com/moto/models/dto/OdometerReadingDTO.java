package br.com.moto.models.dto;

import java.time.LocalDate;
import java.util.UUID;

public record OdometerReadingDTO(UUID id, LocalDate date, int odometerKm) {
}
