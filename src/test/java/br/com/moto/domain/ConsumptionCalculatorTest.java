package br.com.moto.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConsumptionCalculatorTest {

    private static FuelEntry e(final String data, final int km, final String litros, final String valor, final boolean cheio) {
        return new FuelEntry(LocalDate.parse(data), km, new BigDecimal(litros), new BigDecimal(valor), cheio);
    }

    /** Massa: jan (cheio, cheio) e fev (parcial, cheio). */
    private static final List<FuelEntry> MASSA = List.of(
            e("2026-01-05", 1000, "10.00", "60.00", true),
            e("2026-01-20", 1300, "8.00", "50.00", true),
            e("2026-02-10", 1500, "5.00", "32.00", false),
            e("2026-02-25", 1700, "10.00", "63.00", true));

    @Test
    @DisplayName("o primeiro abastecimento (ou lista vazia) não gera trecho")
    void primeiroAbastecimentoNaoGeraTrecho() {
        assertThat(ConsumptionCalculator.segmentos(List.of(MASSA.get(0)))).isEmpty();
        assertThat(ConsumptionCalculator.segmentos(List.of())).isEmpty();
    }

    @Test
    @DisplayName("trecho entre dois tanques cheios é exato: km / litros do abastecimento final")
    void trechoEntreDoisCheiosEExato() {
        final var segmentos = ConsumptionCalculator.segmentos(MASSA);

        assertThat(segmentos).hasSize(3);
        assertThat(segmentos.get(0).km()).isEqualTo(300);
        assertThat(segmentos.get(0).kmPerLiter()).isEqualByComparingTo("37.50");
        assertThat(segmentos.get(0).exact()).isTrue();
        assertThat(segmentos.get(0).date()).isEqualTo(LocalDate.parse("2026-01-20"));
    }

    @Test
    @DisplayName("trecho com abastecimento parcial (atual ou anterior) é estimado")
    void trechoComParcialEEstimado() {
        final var segmentos = ConsumptionCalculator.segmentos(MASSA);

        assertThat(segmentos.get(1).kmPerLiter()).isEqualByComparingTo("40.00");
        assertThat(segmentos.get(1).exact()).as("atual parcial").isFalse();
        assertThat(segmentos.get(2).kmPerLiter()).isEqualByComparingTo("20.00");
        assertThat(segmentos.get(2).exact()).as("anterior parcial").isFalse();
    }

    @Test
    @DisplayName("janela de janeiro: um trecho, baixa confiança")
    void janelaDeJaneiroTemUmTrechoEBaixaConfianca() {
        final var segmentos = ConsumptionCalculator.segmentos(MASSA);

        final var jan = ConsumptionCalculator.janela(MASSA, segmentos, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"));

        assertThat(jan.litersRefueled()).isEqualByComparingTo("18.00");
        assertThat(jan.totalSpent()).isEqualByComparingTo("110.00");
        assertThat(jan.segmentKm()).isEqualTo(300);
        assertThat(jan.kmPerLiter()).isEqualByComparingTo("37.50");
        assertThat(jan.costPerKm()).isEqualByComparingTo("0.17");
        assertThat(jan.segmentCount()).isEqualTo(1);
        assertThat(jan.lowConfidence()).isTrue();
    }

    @Test
    @DisplayName("janela é ponderada (Σkm / Σlitros), nunca média de médias")
    void janelaDeFevereiroEPonderada() {
        final var segmentos = ConsumptionCalculator.segmentos(MASSA);

        final var fev = ConsumptionCalculator.janela(MASSA, segmentos, LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28"));

        assertThat(fev.kmPerLiter()).as("400 km / 15 L, e não (40 + 20) / 2").isEqualByComparingTo("26.67");
        assertThat(fev.costPerKm()).isEqualByComparingTo("0.24");
        assertThat(fev.pricePerLiter()).isEqualByComparingTo("6.333");
        assertThat(fev.bestKmPerLiter()).isEqualByComparingTo("40.00");
        assertThat(fev.worstKmPerLiter()).isEqualByComparingTo("20.00");
        assertThat(fev.longestSegmentKm()).isEqualTo(200);
        assertThat(fev.segmentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("janela sem limites cobre tudo e, com 3 trechos, ganha confiança")
    void janelaSemLimitesCobreTudoEGanhaConfianca() {
        final var segmentos = ConsumptionCalculator.segmentos(MASSA);

        final var tudo = ConsumptionCalculator.janela(MASSA, segmentos, null, null);

        assertThat(tudo.kmPerLiter()).as("700 km / 23 L").isEqualByComparingTo("30.43");
        assertThat(tudo.segmentCount()).isEqualTo(3);
        assertThat(tudo.lowConfidence()).isFalse();
        assertThat(tudo.litersRefueled()).isEqualByComparingTo("33.00");
    }

    @Test
    @DisplayName("trecho cruzando a virada do mês pertence ao mês do abastecimento final")
    void trechoQueCruzaOMesPertenceAoMesDoAbastecimentoFinal() {
        final var entradas = List.of(
                e("2026-01-28", 1000, "10.00", "60.00", true),
                e("2026-02-03", 1250, "8.00", "50.00", true));
        final var segmentos = ConsumptionCalculator.segmentos(entradas);

        final var jan = ConsumptionCalculator.janela(entradas, segmentos, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"));
        final var fev = ConsumptionCalculator.janela(entradas, segmentos, LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28"));

        assertThat(jan.segmentCount()).isZero();
        assertThat(jan.kmPerLiter()).isNull();
        assertThat(fev.segmentCount()).isEqualTo(1);
        assertThat(fev.segmentKm()).isEqualTo(250);
    }

    @Test
    @DisplayName("janela sem dados devolve zeros e nulos, com baixa confiança")
    void janelaSemDados() {
        final var vazia = ConsumptionCalculator.janela(List.of(), List.of(), null, null);

        assertThat(vazia.litersRefueled()).isEqualByComparingTo("0");
        assertThat(vazia.totalSpent()).isEqualByComparingTo("0");
        assertThat(vazia.pricePerLiter()).isNull();
        assertThat(vazia.kmPerLiter()).isNull();
        assertThat(vazia.costPerKm()).isNull();
        assertThat(vazia.bestKmPerLiter()).isNull();
        assertThat(vazia.longestSegmentKm()).isNull();
        assertThat(vazia.lowConfidence()).isTrue();
    }

    @Test
    @DisplayName("trecho sem km rodado não entra em melhor/pior nem na média")
    void trechoSemKmNaoEntraNoMelhorPior() {
        final var entradas = List.of(
                e("2026-03-01", 2000, "5.00", "30.00", true),
                e("2026-03-02", 2000, "1.00", "6.00", false));
        final var segmentos = ConsumptionCalculator.segmentos(entradas);

        assertThat(segmentos.get(0).kmPerLiter()).isNull();
        final var janela = ConsumptionCalculator.janela(entradas, segmentos, null, null);
        assertThat(janela.bestKmPerLiter()).isNull();
        assertThat(janela.worstKmPerLiter()).isNull();
        assertThat(janela.kmPerLiter()).isNull();
        assertThat(janela.costPerKm()).isNull();
        assertThat(janela.litersRefueled()).isEqualByComparingTo("6.00");
    }

    @Test
    @DisplayName("hodômetro regressivo (dado inconsistente) vira trecho de 0 km, nunca negativo")
    void hodometroRegressivoNaoGeraKmNegativo() {
        final var entradas = List.of(
                e("2026-03-01", 2000, "5.00", "30.00", true),
                e("2026-03-05", 1990, "5.00", "30.00", true));

        assertThat(ConsumptionCalculator.segmentos(entradas).get(0).km()).isZero();
    }
}
