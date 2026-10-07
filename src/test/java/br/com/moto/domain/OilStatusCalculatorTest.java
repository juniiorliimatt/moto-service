package br.com.moto.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.moto.models.enums.OilType;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Troca em 2026-06-01, a 20.000 km, intervalo de 4.000 km / 6 meses → vence em 24.000 km ou 2026-12-01. */
class OilStatusCalculatorTest {

    private static OilStatus calcular(final int hodometroAtual, final String hoje) {
        return OilStatusCalculator.calcular(LocalDate.parse("2026-06-01"), 20000, 4000, 6, hodometroAtual, LocalDate.parse(hoje));
    }

    @Test
    @DisplayName("em dia quando falta bastante em km e em tempo")
    void emDia() {
        final var status = calcular(23000, "2026-08-01");

        assertThat(status.dueKm()).isEqualTo(24000);
        assertThat(status.dueDate()).isEqualTo(LocalDate.parse("2026-12-01"));
        assertThat(status.kmRemaining()).isEqualTo(1000);
        assertThat(status.daysRemaining()).isEqualTo(122);
        assertThat(status.level()).isEqualTo(OilStatusLevel.OK);
        assertThat(status.limitedBy()).isEqualTo(OilLimit.KM);
    }

    @Test
    @DisplayName("perto quando faltam até 500 km")
    void pertoPorKm() {
        final var status = calcular(23600, "2026-08-01");

        assertThat(status.kmRemaining()).isEqualTo(400);
        assertThat(status.level()).isEqualTo(OilStatusLevel.PERTO);
        assertThat(status.limitedBy()).isEqualTo(OilLimit.KM);
    }

    @Test
    @DisplayName("exatamente no limiar de 500 km já é 'perto'; 501 km ainda é 'ok'")
    void limiarDeKm() {
        assertThat(calcular(23500, "2026-08-01").level()).isEqualTo(OilStatusLevel.PERTO);
        assertThat(calcular(23499, "2026-08-01").level()).isEqualTo(OilStatusLevel.OK);
    }

    @Test
    @DisplayName("perto quando faltam até 30 dias, e o limitante é o tempo")
    void pertoPorTempo() {
        final var status = calcular(20500, "2026-11-10");

        assertThat(status.daysRemaining()).isEqualTo(21);
        assertThat(status.level()).isEqualTo(OilStatusLevel.PERTO);
        assertThat(status.limitedBy()).isEqualTo(OilLimit.TIME);
    }

    @Test
    @DisplayName("vencida por km: restantes negativos")
    void vencidaPorKm() {
        final var status = calcular(24100, "2026-08-01");

        assertThat(status.level()).isEqualTo(OilStatusLevel.VENCIDA);
        assertThat(status.kmRemaining()).isEqualTo(-100);
        assertThat(status.limitedBy()).isEqualTo(OilLimit.KM);
    }

    @Test
    @DisplayName("exatamente no km de vencimento já está vencida")
    void vencidaNoLimiteExatoDeKm() {
        assertThat(calcular(24000, "2026-08-01").level()).isEqualTo(OilStatusLevel.VENCIDA);
    }

    @Test
    @DisplayName("vencida por tempo: dias restantes negativos, mesmo com km sobrando")
    void vencidaPorTempo() {
        final var status = calcular(21000, "2026-12-02");

        assertThat(status.level()).isEqualTo(OilStatusLevel.VENCIDA);
        assertThat(status.daysRemaining()).isEqualTo(-1);
        assertThat(status.limitedBy()).isEqualTo(OilLimit.TIME);
    }

    @Test
    @DisplayName("limiar de 'perto' é 10% do intervalo quando maior que o mínimo (500 km)")
    void limiarProporcionalParaIntervaloLongo() {
        final var status = OilStatusCalculator.calcular(LocalDate.parse("2026-06-01"), 0, 10000, 12, 9200, LocalDate.parse("2026-07-01"));

        assertThat(status.kmRemaining()).isEqualTo(800);
        assertThat(status.level()).isEqualTo(OilStatusLevel.PERTO);   // 800 <= max(500, 10% de 10.000)
    }

    @Test
    @DisplayName("vencimento por data respeita fim de mês (31/08 + 6 meses = 28/02)")
    void vencimentoNoFimDoMes() {
        final var status = OilStatusCalculator.calcular(LocalDate.parse("2026-08-31"), 1000, 4000, 6, 1000, LocalDate.parse("2026-09-01"));

        assertThat(status.dueDate()).isEqualTo(LocalDate.parse("2027-02-28"));
    }

    @Test
    @DisplayName("padrões por tipo de óleo ficam dentro das faixas e o padrão cabe na faixa")
    void padroesPorTipo() {
        for (final var tipo : OilType.values()) {
            assertThat(tipo.getDefaultKm()).isBetween(tipo.getMinKm(), tipo.getMaxKm());
            assertThat(tipo.getDefaultMonths()).isPositive();
        }
        assertThat(OilType.MINERAL.getMaxKm()).isLessThan(OilType.SEMI_SYNTHETIC.getMaxKm());
        assertThat(OilType.SEMI_SYNTHETIC.getMaxKm()).isLessThan(OilType.SYNTHETIC.getMaxKm());
        assertThat(OilType.SYNTHETIC.getDefaultKm()).isEqualTo(6000);
        assertThat(OilType.SYNTHETIC.getDefaultMonths()).isEqualTo(12);
    }
}
