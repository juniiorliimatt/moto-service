package br.com.moto.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Trecho entre dois abastecimentos consecutivos. {@code date} é a do abastecimento <b>final</b>
 * (é a ela que o trecho pertence em qualquer janela); {@code liters} e {@code cost} são os desse
 * abastecimento final (repõem o que foi gasto desde o anterior). {@code exact} só quando os dois
 * abastecimentos completaram o tanque — caso contrário o km/l é uma estimativa.
 */
public record Segment(LocalDate date, int km, BigDecimal liters, BigDecimal cost, boolean exact) {

    /** km/l do trecho (2 casas, HALF_UP); {@code null} quando não houve km rodado. */
    public BigDecimal kmPerLiter() {
        if (km <= 0 || liters.signum() <= 0) {
            return null;
        }
        return BigDecimal.valueOf(km).divide(liters, 2, RoundingMode.HALF_UP);
    }
}
