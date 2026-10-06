package br.com.moto.domain;

import java.time.LocalDate;
import java.util.UUID;

/** Um registro de hodômetro da moto (abastecimento, troca de óleo ou leitura avulsa) numa data. */
public record OdometerPoint(UUID id, LocalDate date, int km) {
}
