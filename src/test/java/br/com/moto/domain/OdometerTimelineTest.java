package br.com.moto.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OdometerTimelineTest {

    private static OdometerPoint p(final String data, final int km) {
        return new OdometerPoint(UUID.randomUUID(), LocalDate.parse(data), km);
    }

    private static final List<OdometerPoint> PONTOS =
            List.of(p("2026-01-20", 1300), p("2026-02-10", 1500), p("2026-02-25", 1700));

    @Test
    void kmDoMesUsaFimDoMesAnteriorComoBase() {
        assertThat(OdometerTimeline.kmNaJanela(PONTOS, 1000, LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28")))
                .isEqualTo(400);
    }

    @Test
    void primeiroMesUsaHodometroInicial() {
        assertThat(OdometerTimeline.kmNaJanela(PONTOS, 1000, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31")))
                .isEqualTo(300);
    }

    @Test
    void mesSemRegistrosDaZero() {
        assertThat(OdometerTimeline.kmNaJanela(PONTOS, 1000, LocalDate.parse("2026-03-01"), LocalDate.parse("2026-03-31")))
                .isZero();
    }

    @Test
    void mesAntesDeQualquerRegistroDaZero() {
        assertThat(OdometerTimeline.kmNaJanela(PONTOS, 1000, LocalDate.parse("2025-12-01"), LocalDate.parse("2025-12-31")))
                .isZero();
    }

    @Test
    void semLimitesSomaTudoDesdeOInicial() {
        assertThat(OdometerTimeline.kmNaJanela(PONTOS, 1000, null, null)).isEqualTo(700);
    }

    @Test
    void semPontosDaZero() {
        assertThat(OdometerTimeline.kmNaJanela(List.of(), 1000, null, null)).isZero();
    }

    @Test
    void janelaAbertaNoInicioComFimDefinido() {
        assertThat(OdometerTimeline.kmNaJanela(PONTOS, 1000, null, LocalDate.parse("2026-02-10"))).isEqualTo(500);
    }

    @Test
    void leituraAvulsaContaComoKmMesmoSemAbastecer() {
        final var comLeitura = List.of(p("2026-02-10", 1500), p("2026-02-28", 1650));

        assertThat(OdometerTimeline.kmNaJanela(comLeitura, 1000, LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28")))
                .isEqualTo(650);
    }
}
