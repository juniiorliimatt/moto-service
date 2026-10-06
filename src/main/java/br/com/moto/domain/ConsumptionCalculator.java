package br.com.moto.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Cálculo de consumo por trecho e por janela — puro, sem Spring nem banco.
 *
 * <p>Como o tanque nem sempre é completado, o litro abastecido em N só aproxima o que foi gasto
 * desde N-1. Por isso a média de uma janela é <b>ponderada</b> (Σkm / Σlitros, nunca média de
 * médias): o erro de nível de tanque fica limitado a uma capacidade de tanque e se dilui quanto
 * maior a janela.
 */
public final class ConsumptionCalculator {

    /** Abaixo disto a média de uma janela é marcada como de baixa confiança. */
    public static final int MIN_SEGMENTS_CONFIDENT = 3;

    private ConsumptionCalculator() {
    }

    /**
     * Trechos entre abastecimentos consecutivos.
     *
     * @param ordenados abastecimentos por data e depois hodômetro, ascendente
     */
    public static List<Segment> segmentos(final List<FuelEntry> ordenados) {
        final var segmentos = new ArrayList<Segment>();
        for (int i = 1; i < ordenados.size(); i++) {
            final var anterior = ordenados.get(i - 1);
            final var atual = ordenados.get(i);
            final int km = Math.max(0, atual.odometerKm() - anterior.odometerKm());
            segmentos.add(new Segment(atual.date(), km, atual.liters(), atual.totalValue(),
                    anterior.fullTank() && atual.fullTank()));
        }
        return List.copyOf(segmentos);
    }

    /**
     * Consumo de uma janela. Um trecho pertence à janela do abastecimento que o <b>termina</b>.
     *
     * @param de  início inclusivo, ou {@code null} para sem limite
     * @param ate fim inclusivo, ou {@code null} para sem limite
     */
    public static ConsumptionWindow janela(final List<FuelEntry> ordenados, final List<Segment> segmentos,
                                           final LocalDate de, final LocalDate ate) {
        final var entradas = ordenados.stream().filter(e -> dentro(e.date(), de, ate)).toList();
        final var trechos = segmentos.stream().filter(s -> dentro(s.date(), de, ate)).toList();

        final var litros = entradas.stream().map(FuelEntry::liters).reduce(BigDecimal.ZERO, BigDecimal::add);
        final var gasto = entradas.stream().map(FuelEntry::totalValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        final var preco = litros.signum() > 0 ? gasto.divide(litros, 3, RoundingMode.HALF_UP) : null;

        final int km = trechos.stream().mapToInt(Segment::km).sum();
        final var litrosDosTrechos = trechos.stream().map(Segment::liters).reduce(BigDecimal.ZERO, BigDecimal::add);
        final var custoDosTrechos = trechos.stream().map(Segment::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
        final var kmPorLitro = km > 0 && litrosDosTrechos.signum() > 0
                ? BigDecimal.valueOf(km).divide(litrosDosTrechos, 2, RoundingMode.HALF_UP) : null;
        final var custoPorKm = km > 0
                ? custoDosTrechos.divide(BigDecimal.valueOf(km), 2, RoundingMode.HALF_UP) : null;

        final var eficiencias = trechos.stream().map(Segment::kmPerLiter).filter(Objects::nonNull).toList();
        return new ConsumptionWindow(litros, gasto, preco, km, kmPorLitro, custoPorKm, trechos.size(),
                trechos.size() < MIN_SEGMENTS_CONFIDENT,
                eficiencias.stream().max(Comparator.naturalOrder()).orElse(null),
                eficiencias.stream().min(Comparator.naturalOrder()).orElse(null),
                trechos.stream().map(Segment::km).max(Comparator.naturalOrder()).orElse(null));
    }

    private static boolean dentro(final LocalDate data, final LocalDate de, final LocalDate ate) {
        return (de == null || !data.isBefore(de)) && (ate == null || !data.isAfter(ate));
    }
}
