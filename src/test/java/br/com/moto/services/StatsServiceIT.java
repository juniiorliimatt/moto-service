package br.com.moto.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moto.PostgresIT;
import br.com.moto.exceptions.InvalidPeriodException;
import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.MotorcycleRequestDTO;
import br.com.moto.models.dto.OdometerReadingRequestDTO;
import br.com.moto.models.dto.RefuelingRequestDTO;
import br.com.moto.models.enums.FuelType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Massa: moto com 1.000 km iniciais; jan (cheio, cheio), fev (parcial, cheio) — a mesma do
 * {@code ConsumptionCalculatorTest}. "Hoje" fixo em 2026-03-15.
 */
class StatsServiceIT extends PostgresIT {

    private static final LocalDate HOJE = LocalDate.parse("2026-03-15");

    @Autowired
    private MotorcycleService motorcycleService;

    @Autowired
    private RefuelingService refuelingService;

    @Autowired
    private OdometerReadingService readingService;

    @Autowired
    private StatsService service;

    private static String dono() {
        return "dono-" + UUID.randomUUID();
    }

    private UUID moto(final String dono) {
        return motorcycleService.criar(
                new MotorcycleRequestDTO("Fazer", null, "Fazer 250", null, null, 1000, null, null), dono).id();
    }

    private void abastecer(final UUID moto, final String dono, final String data, final int km, final String litros,
                           final String valor, final boolean cheio) {
        refuelingService.criar(moto, new RefuelingRequestDTO(LocalDate.parse(data), km, new BigDecimal(litros),
                new BigDecimal(valor), null, FuelType.GASOLINA_COMUM, cheio), dono);
    }

    private UUID motoComMassa(final String dono) {
        final var moto = moto(dono);
        abastecer(moto, dono, "2026-01-05", 1000, "10.00", "60.00", true);
        abastecer(moto, dono, "2026-01-20", 1300, "8.00", "50.00", true);
        abastecer(moto, dono, "2026-02-10", 1500, "5.00", "32.00", false);
        abastecer(moto, dono, "2026-02-25", 1700, "10.00", "63.00", true);
        return moto;
    }

    @Test
    void mensal_devolve12MesesComOsNumerosDeCadaUm() {
        final var dono = dono();
        final var moto = motoComMassa(dono);

        final var meses = service.mensal(moto, dono, 2026, HOJE);

        assertThat(meses).hasSize(12);
        assertThat(meses).extracting("month").containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

        final var jan = meses.get(0).stats();
        assertThat(jan.from()).isEqualTo(LocalDate.parse("2026-01-01"));
        assertThat(jan.to()).isEqualTo(LocalDate.parse("2026-01-31"));
        assertThat(jan.km()).isEqualTo(300);
        assertThat(jan.kmPerLiter()).isEqualByComparingTo("37.50");
        assertThat(jan.totalSpent()).isEqualByComparingTo("110.00");
        assertThat(jan.litersRefueled()).isEqualByComparingTo("18.00");
        assertThat(jan.lowConfidence()).isTrue();
        assertThat(meses.get(0).spentChangePct()).as("janeiro não tem mês anterior").isNull();

        final var fev = meses.get(1).stats();
        assertThat(fev.km()).isEqualTo(400);
        assertThat(fev.kmPerLiter()).isEqualByComparingTo("26.67");
        assertThat(fev.totalSpent()).isEqualByComparingTo("95.00");
        assertThat(fev.bestKmPerLiter()).isEqualByComparingTo("40.00");
        assertThat(fev.worstKmPerLiter()).isEqualByComparingTo("20.00");
        assertThat(meses.get(1).spentChangePct()).isEqualByComparingTo("-13.64");
    }

    @Test
    void mensal_mesSemMovimentoEMesesFuturosVemZerados() {
        final var dono = dono();
        final var moto = motoComMassa(dono);

        final var meses = service.mensal(moto, dono, 2026, HOJE);

        final var mar = meses.get(2);
        assertThat(mar.stats().km()).isZero();
        assertThat(mar.stats().totalSpent()).isEqualByComparingTo("0");
        assertThat(mar.stats().kmPerLiter()).isNull();
        assertThat(mar.stats().kmPerDay()).isEqualByComparingTo("0");
        assertThat(mar.spentChangePct()).as("gastou 0 contra 95 do mês anterior").isEqualByComparingTo("-100.00");

        final var abr = meses.get(3);
        assertThat(abr.stats().km()).isZero();
        assertThat(abr.stats().totalSpent()).isEqualByComparingTo("0");
        assertThat(abr.spentChangePct()).as("mês anterior sem gasto: sem base de comparação").isNull();
        assertThat(meses.get(11).stats().km()).isZero();
    }

    @Test
    void mensal_leituraAvulsaAumentaOKmDoMesSemMexerNoConsumo() {
        final var dono = dono();
        final var moto = motoComMassa(dono);
        readingService.criar(moto, new OdometerReadingRequestDTO(LocalDate.parse("2026-03-10"), 1850), dono);

        final var mar = service.mensal(moto, dono, 2026, HOJE).get(2).stats();

        assertThat(mar.km()).isEqualTo(150);
        assertThat(mar.kmPerLiter()).isNull();
        assertThat(mar.kmPerDay()).as("150 km em 15 dias decorridos de março").isEqualByComparingTo("10.00");
    }

    @Test
    void mensal_anoSemDadosVemTodoZerado() {
        final var dono = dono();
        final var moto = motoComMassa(dono);

        final var meses = service.mensal(moto, dono, 2025, HOJE);

        assertThat(meses).hasSize(12);
        assertThat(meses).allSatisfy(m -> {
            assertThat(m.stats().km()).isZero();
            assertThat(m.stats().totalSpent()).isEqualByComparingTo("0");
        });
    }

    @Test
    void resumo_semPeriodoCobreDoPrimeiroRegistroAteHoje() {
        final var dono = dono();
        final var moto = motoComMassa(dono);

        final var resumo = service.resumo(moto, dono, null, null, HOJE);

        assertThat(resumo.from()).isEqualTo(LocalDate.parse("2026-01-05"));
        assertThat(resumo.to()).isEqualTo(HOJE);
        assertThat(resumo.km()).isEqualTo(700);
        assertThat(resumo.kmPerLiter()).isEqualByComparingTo("30.43");
        assertThat(resumo.segmentCount()).isEqualTo(3);
        assertThat(resumo.lowConfidence()).isFalse();
        assertThat(resumo.kmPerDay()).as("700 km em 70 dias").isEqualByComparingTo("10.00");
        assertThat(resumo.litersRefueled()).isEqualByComparingTo("33.00");
        assertThat(resumo.totalSpent()).isEqualByComparingTo("205.00");
        assertThat(resumo.longestSegmentKm()).isEqualTo(300);
        assertThat(resumo.costPerKm()).as("(50 + 32 + 63) / 700 km").isEqualByComparingTo("0.21");
    }

    @Test
    void resumo_comPeriodoExplicito() {
        final var dono = dono();
        final var moto = motoComMassa(dono);

        final var fev = service.resumo(moto, dono, LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28"), HOJE);

        assertThat(fev.km()).isEqualTo(400);
        assertThat(fev.kmPerDay()).as("400 km em 28 dias").isEqualByComparingTo("14.29");
    }

    @Test
    void resumo_motoSemRegistrosNaoQuebra() {
        final var dono = dono();
        final var moto = moto(dono);

        final var resumo = service.resumo(moto, dono, null, null, HOJE);

        assertThat(resumo.km()).isZero();
        assertThat(resumo.kmPerLiter()).isNull();
        assertThat(resumo.totalSpent()).isEqualByComparingTo("0");
        assertThat(resumo.from()).isEqualTo(HOJE);
        assertThat(resumo.lowConfidence()).isTrue();
    }

    @Test
    void resumo_periodoInvertidoELancaExcecao() {
        final var dono = dono();
        final var moto = moto(dono);

        assertThatThrownBy(() -> service.resumo(moto, dono, LocalDate.parse("2026-03-01"), LocalDate.parse("2026-02-01"), HOJE))
                .isInstanceOf(InvalidPeriodException.class)
                .hasMessage("A data inicial deve ser anterior ou igual à final");
    }

    @Test
    void anual_umItemPorAnoComRegistro() {
        final var dono = dono();
        final var moto = motoComMassa(dono);
        readingService.criar(moto, new OdometerReadingRequestDTO(LocalDate.parse("2027-01-10"), 2000), dono);

        final var anos = service.anual(moto, dono, HOJE);

        assertThat(anos).extracting("year").containsExactly(2026, 2027);
        assertThat(anos.get(0).stats().km()).isEqualTo(700);
        assertThat(anos.get(0).stats().kmPerLiter()).isEqualByComparingTo("30.43");
        assertThat(anos.get(0).stats().totalSpent()).isEqualByComparingTo("205.00");
        assertThat(anos.get(1).stats().km()).isEqualTo(300);
        assertThat(anos.get(1).stats().totalSpent()).isEqualByComparingTo("0");
    }

    @Test
    void anual_motoSemRegistrosDevolveListaVazia() {
        final var dono = dono();

        assertThat(service.anual(moto(dono), dono, HOJE)).isEmpty();
    }

    @Test
    void outroDonoNaoVeAsMetricas() {
        final var dono = dono();
        final var moto = motoComMassa(dono);
        final var intruso = dono();

        assertThatThrownBy(() -> service.resumo(moto, intruso, null, null, HOJE)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.mensal(moto, intruso, 2026, HOJE)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.anual(moto, intruso, HOJE)).isInstanceOf(ResourceNotFoundException.class);
    }
}
