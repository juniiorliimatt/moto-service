package br.com.moto.domain;

import java.time.LocalDate;
import java.util.List;

/** Km rodados num período a partir da linha do tempo de hodômetro (todas as fontes) — puro. */
public final class OdometerTimeline {

    private OdometerTimeline() {
    }

    /**
     * Km entre o hodômetro ao fim do período anterior (ou o inicial da moto, se não houver) e o
     * último registro até {@code ate}. Como o hodômetro é monotônico, o "último" é o maior.
     *
     * @param de  início inclusivo, ou {@code null} para desde o hodômetro inicial
     * @param ate fim inclusivo, ou {@code null} para sem limite
     */
    public static int kmNaJanela(final List<OdometerPoint> pontos, final int hodometroInicial,
                                 final LocalDate de, final LocalDate ate) {
        final int superior = pontos.stream()
                .filter(p -> ate == null || !p.date().isAfter(ate))
                .mapToInt(OdometerPoint::km).max().orElse(hodometroInicial);
        final int inferior = de == null ? hodometroInicial : pontos.stream()
                .filter(p -> p.date().isBefore(de))
                .mapToInt(OdometerPoint::km).max().orElse(hodometroInicial);
        return Math.max(0, Math.max(superior, hodometroInicial) - Math.max(inferior, hodometroInicial));
    }
}
