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
    @DisplayName("trecho entre dois abastecimentos: km rodados (diferença de hodômetro), na data do abastecimento final")
    void trechoEOsKmEntreDoisAbastecimentos() {
        final var segmentos = ConsumptionCalculator.segmentos(MASSA);

        assertThat(segmentos).hasSize(3);
        assertThat(segmentos).extracting(Segment::km).containsExactly(300, 200, 200);
        assertThat(segmentos.get(0).date()).isEqualTo(LocalDate.parse("2026-01-20"));
        assertThat(segmentos.get(1).date()).isEqualTo(LocalDate.parse("2026-02-10"));
    }

    @Test
    @DisplayName("janela de janeiro: um trecho, baixa confiança; os litros do 1º abastecimento entram na divisão")
    void janelaDeJaneiroTemUmTrechoEBaixaConfianca() {
        final var segmentos = ConsumptionCalculator.segmentos(MASSA);

        final var jan = ConsumptionCalculator.janela(MASSA, segmentos, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"));

        assertThat(jan.litersRefueled()).isEqualByComparingTo("18.00");
        assertThat(jan.totalSpent()).isEqualByComparingTo("110.00");
        assertThat(jan.segmentKm()).isEqualTo(300);
        assertThat(jan.kmPerLiter()).as("300 km / 18 L (10 + 8)").isEqualByComparingTo("16.67");
        assertThat(jan.costPerKm()).as("R$ 110,00 / 300 km").isEqualByComparingTo("0.37");
        assertThat(jan.segmentCount()).isEqualTo(1);
        assertThat(jan.lowConfidence()).isTrue();
    }

    @Test
    @DisplayName("janela é ponderada (Σkm / Σlitros abastecidos), nunca média de médias")
    void janelaDeFevereiroEPonderada() {
        final var segmentos = ConsumptionCalculator.segmentos(MASSA);

        final var fev = ConsumptionCalculator.janela(MASSA, segmentos, LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28"));

        assertThat(fev.kmPerLiter()).as("400 km / 15 L, e não (40 + 20) / 2").isEqualByComparingTo("26.67");
        assertThat(fev.costPerKm()).isEqualByComparingTo("0.24");
        assertThat(fev.pricePerLiter()).isEqualByComparingTo("6.333");
        assertThat(fev.segmentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("janela sem limites cobre tudo e, com 3 trechos, ganha confiança")
    void janelaSemLimitesCobreTudoEGanhaConfianca() {
        final var segmentos = ConsumptionCalculator.segmentos(MASSA);

        final var tudo = ConsumptionCalculator.janela(MASSA, segmentos, null, null);

        assertThat(tudo.kmPerLiter()).as("700 km / 33 L (todos os abastecimentos)").isEqualByComparingTo("21.21");
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
        assertThat(vazia.lowConfidence()).isTrue();
    }

    @Test
    @DisplayName("trecho sem km rodado não gera média")
    void trechoSemKmNaoGeraMedia() {
        final var entradas = List.of(
                e("2026-03-01", 2000, "5.00", "30.00", true),
                e("2026-03-02", 2000, "1.00", "6.00", false));
        final var segmentos = ConsumptionCalculator.segmentos(entradas);

        assertThat(segmentos.get(0).km()).isZero();
        final var janela = ConsumptionCalculator.janela(entradas, segmentos, null, null);
        assertThat(janela.kmPerLiter()).isNull();
        assertThat(janela.costPerKm()).isNull();
        assertThat(janela.litersRefueled()).isEqualByComparingTo("6.00");
    }

    @Test
    @DisplayName("o consumo é a diferença de hodômetro ÷ todos os litros abastecidos, sem olhar se o tanque foi completado")
    void consumoIgnoraSeOTanqueFoiCompletado() {
        // dados reais da Fazer: 68.629 → 69.497 km (868 km) com 9 + 10 + 5 = 24 L. Dividir só pelos litros dos trechos
        // (15 L) dava 57,9 km/l, valor impossível para a moto; todos os litros abastecidos dão 36,2.
        final var parciais = List.of(
                e("2026-10-05", 68629, "9.00", "64.75", true),
                e("2026-10-19", 69146, "10.00", "71.40", false),
                e("2026-10-28", 69497, "5.00", "36.05", false));
        final var todosCheios = parciais.stream()
                .map(f -> new FuelEntry(f.date(), f.odometerKm(), f.liters(), f.totalValue(), true)).toList();

        final var comParciais = ConsumptionCalculator.janela(parciais, ConsumptionCalculator.segmentos(parciais), null, null);
        final var comCheios = ConsumptionCalculator.janela(todosCheios, ConsumptionCalculator.segmentos(todosCheios), null, null);

        assertThat(comParciais.segmentKm()).isEqualTo(868);
        assertThat(comParciais.litersRefueled()).isEqualByComparingTo("24.00");
        assertThat(comParciais.kmPerLiter()).as("868 km / 24 L").isEqualByComparingTo("36.17");
        assertThat(comParciais.costPerKm()).as("R$ 172,20 / 868 km").isEqualByComparingTo("0.20");
        assertThat(comCheios.kmPerLiter()).as("o resultado não depende do tanque completo").isEqualByComparingTo(comParciais.kmPerLiter());
        assertThat(comCheios.costPerKm()).isEqualByComparingTo(comParciais.costPerKm());
    }

    @Test
    @DisplayName("um único abastecimento (ou nenhum km rodado) não gera consumo, mesmo com litros abastecidos")
    void semKmRodadoNaoHaConsumo() {
        final var um = List.of(e("2026-10-05", 68629, "9.00", "64.75", true));

        final var janela = ConsumptionCalculator.janela(um, ConsumptionCalculator.segmentos(um), null, null);

        assertThat(janela.kmPerLiter()).isNull();
        assertThat(janela.costPerKm()).isNull();
        assertThat(janela.litersRefueled()).isEqualByComparingTo("9.00");
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
