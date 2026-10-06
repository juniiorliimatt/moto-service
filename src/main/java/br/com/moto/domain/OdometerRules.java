package br.com.moto.domain;

import br.com.moto.exceptions.InvalidOdometerException;
import java.time.LocalDate;
import java.util.List;

/** Regras do hodômetro de uma moto — puras, sem Spring nem banco. */
public final class OdometerRules {

    private OdometerRules() {
    }

    /**
     * O hodômetro é monotônico no tempo: nenhum registro anterior pode ter km maior, nenhum
     * posterior pode ter km menor, e nenhum pode ficar abaixo do hodômetro inicial da moto.
     * Registros na <b>mesma data</b> não são comparados (a ordem dentro do dia é desconhecida).
     *
     * @param outros todos os demais registros da moto (sem o que está sendo criado/editado)
     * @throws InvalidOdometerException se a regra for violada
     */
    public static void validar(final List<OdometerPoint> outros, final int hodometroInicial, final LocalDate data, final int km) {
        if (km < hodometroInicial) {
            throw new InvalidOdometerException(
                    "Hodômetro (%d km) abaixo do hodômetro inicial da moto (%d km)".formatted(km, hodometroInicial));
        }
        for (final var ponto : outros) {
            if (ponto.date().isBefore(data) && ponto.km() > km) {
                throw new InvalidOdometerException(
                        "Hodômetro (%d km) menor que o registro de %s (%d km)".formatted(km, ponto.date(), ponto.km()));
            }
            if (ponto.date().isAfter(data) && ponto.km() < km) {
                throw new InvalidOdometerException(
                        "Hodômetro (%d km) maior que o registro de %s (%d km)".formatted(km, ponto.date(), ponto.km()));
            }
        }
    }

    /** Hodômetro atual: o maior entre os registros e o inicial da moto. */
    public static int atual(final List<OdometerPoint> pontos, final int hodometroInicial) {
        return Math.max(hodometroInicial, pontos.stream().mapToInt(OdometerPoint::km).max().orElse(hodometroInicial));
    }
}
