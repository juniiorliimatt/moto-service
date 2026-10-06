package br.com.moto.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moto.PostgresIT;
import br.com.moto.exceptions.InvalidOdometerException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class RefuelingServiceIT extends PostgresIT {

    @Autowired
    private MotorcycleService motorcycleService;

    @Autowired
    private RefuelingService service;

    @Autowired
    private OdometerReadingService readingService;

    private static String dono() {
        return "dono-" + UUID.randomUUID();
    }

    private UUID moto(final String dono) {
        return motorcycleService.criar(
                new MotorcycleRequestDTO("Fazer", null, "Fazer 250", null, null, 1000, null, null), dono).id();
    }

    private static RefuelingRequestDTO abastecimento(final String data, final int km, final String litros, final String valor,
                                                     final boolean cheio) {
        return new RefuelingRequestDTO(LocalDate.parse(data), km, new BigDecimal(litros), new BigDecimal(valor), "Posto Shell",
                FuelType.GASOLINA_COMUM, cheio);
    }

    @Test
    void criar_calculaPrecoPorLitro() {
        final var dono = dono();
        final var moto = moto(dono);

        final var criado = service.criar(moto, abastecimento("2026-01-05", 1000, "10.00", "60.00", true), dono);

        assertThat(criado.id()).isNotNull();
        assertThat(criado.pricePerLiter()).isEqualByComparingTo("6.000");
        assertThat(criado.fullTank()).isTrue();
        assertThat(criado.station()).isEqualTo("Posto Shell");
    }

    @Test
    void criar_fullTankNuloViraFalso() {
        final var dono = dono();
        final var moto = moto(dono);

        final var criado = service.criar(moto, new RefuelingRequestDTO(LocalDate.parse("2026-01-05"), 1000,
                new BigDecimal("5.00"), new BigDecimal("30.00"), null, FuelType.ETANOL, null), dono);

        assertThat(criado.fullTank()).isFalse();
    }

    @Test
    void entradasOrdenadas_vemPorDataEHodometroComFullTankPreservado() {
        final var dono = dono();
        final var moto = moto(dono);
        service.criar(moto, abastecimento("2026-02-10", 1500, "5.00", "32.00", false), dono);
        service.criar(moto, abastecimento("2026-01-05", 1000, "10.00", "60.00", true), dono);
        service.criar(moto, abastecimento("2026-01-20", 1300, "8.00", "50.00", true), dono);

        final var entradas = service.entradasOrdenadas(moto, dono);

        assertThat(entradas).extracting("odometerKm").containsExactly(1000, 1300, 1500);
        assertThat(entradas).extracting("fullTank").containsExactly(true, true, false);
        assertThat(entradas.get(0).liters()).isEqualByComparingTo("10.00");
    }

    @Test
    void criar_hodometroMenorQueOAnteriorERejeitado() {
        final var dono = dono();
        final var moto = moto(dono);
        service.criar(moto, abastecimento("2026-01-05", 1500, "10.00", "60.00", true), dono);

        assertThatThrownBy(() -> service.criar(moto, abastecimento("2026-01-20", 1400, "8.00", "50.00", true), dono))
                .isInstanceOf(InvalidOdometerException.class);
    }

    @Test
    void criar_abaixoDoHodometroInicialERejeitado() {
        final var dono = dono();
        final var moto = moto(dono);

        assertThatThrownBy(() -> service.criar(moto, abastecimento("2026-01-05", 999, "10.00", "60.00", true), dono))
                .isInstanceOf(InvalidOdometerException.class);
    }

    @Test
    void atualizar_semMudarOHodometroNaoConflitaConsigoMesmo() {
        final var dono = dono();
        final var moto = moto(dono);
        final var a1 = service.criar(moto, abastecimento("2026-01-05", 1000, "10.00", "60.00", true), dono);
        service.criar(moto, abastecimento("2026-01-20", 1300, "8.00", "50.00", true), dono);

        assertThatCode(() -> service.atualizar(moto, a1.id(), abastecimento("2026-01-05", 1000, "11.00", "66.00", true), dono))
                .doesNotThrowAnyException();
        assertThat(service.entradasOrdenadas(moto, dono).get(0).liters()).isEqualByComparingTo("11.00");
    }

    @Test
    void atualizar_hodometroAcimaDoPosteriorERejeitado() {
        final var dono = dono();
        final var moto = moto(dono);
        final var a1 = service.criar(moto, abastecimento("2026-01-05", 1000, "10.00", "60.00", true), dono);
        service.criar(moto, abastecimento("2026-01-20", 1300, "8.00", "50.00", true), dono);

        assertThatThrownBy(() -> service.atualizar(moto, a1.id(), abastecimento("2026-01-05", 1400, "10.00", "60.00", true), dono))
                .isInstanceOf(InvalidOdometerException.class);
    }

    @Test
    void listar_filtraPorPeriodoEPagina() {
        final var dono = dono();
        final var moto = moto(dono);
        service.criar(moto, abastecimento("2026-01-05", 1000, "10.00", "60.00", true), dono);
        service.criar(moto, abastecimento("2026-01-20", 1300, "8.00", "50.00", true), dono);
        service.criar(moto, abastecimento("2026-02-10", 1500, "5.00", "32.00", false), dono);
        final var pagina = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "date", "odometerKm"));

        final var janeiro = service.listar(moto, dono, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), pagina);
        final var tudo = service.listar(moto, dono, null, null, pagina);

        assertThat(janeiro.getTotalElements()).isEqualTo(2);
        assertThat(janeiro.getContent()).extracting("odometerKm").containsExactly(1300);
        assertThat(tudo.getTotalElements()).isEqualTo(3);
        assertThat(tudo.getContent()).extracting("odometerKm").containsExactly(1500);
    }

    @Test
    void abastecimentoELeituraAvulsaCompartilhamAMesmaLinhaDoTempo() {
        final var dono = dono();
        final var moto = moto(dono);
        service.criar(moto, abastecimento("2026-01-05", 1500, "10.00", "60.00", true), dono);

        assertThatThrownBy(() -> readingService.criar(moto, new OdometerReadingRequestDTO(LocalDate.parse("2026-01-20"), 1400), dono))
                .isInstanceOf(InvalidOdometerException.class);
        readingService.criar(moto, new OdometerReadingRequestDTO(LocalDate.parse("2026-01-20"), 1600), dono);
        assertThatThrownBy(() -> service.criar(moto, abastecimento("2026-02-01", 1550, "5.00", "30.00", false), dono))
                .isInstanceOf(InvalidOdometerException.class);
    }

    @Test
    void outroDonoNaoAcessaNada() {
        final var dono = dono();
        final var moto = moto(dono);
        final var criado = service.criar(moto, abastecimento("2026-01-05", 1000, "10.00", "60.00", true), dono);
        final var intruso = dono();
        final var pagina = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.criar(moto, abastecimento("2026-01-06", 1100, "5.00", "30.00", true), intruso))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.listar(moto, intruso, null, null, pagina)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.atualizar(moto, criado.id(), abastecimento("2026-01-05", 1000, "1.00", "6.00", true), intruso))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.excluir(moto, criado.id(), intruso)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.entradasOrdenadas(moto, intruso)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.entradasOrdenadas(moto, dono)).hasSize(1);
    }

    @Test
    void abastecimentoDeOutraMotoDoMesmoDonoNaoEAlcancadoPelaRotaErrada() {
        final var dono = dono();
        final var motoA = moto(dono);
        final var motoB = moto(dono);
        final var doA = service.criar(motoA, abastecimento("2026-01-05", 1000, "10.00", "60.00", true), dono);

        assertThatThrownBy(() -> service.excluir(motoB, doA.id(), dono)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void excluir_removeEPermiteRegistrarHodometroMenorDepois() {
        final var dono = dono();
        final var moto = moto(dono);
        final var criado = service.criar(moto, abastecimento("2026-01-05", 1500, "10.00", "60.00", true), dono);

        service.excluir(moto, criado.id(), dono);

        assertThat(service.entradasOrdenadas(moto, dono)).isEmpty();
        assertThatCode(() -> service.criar(moto, abastecimento("2026-01-06", 1200, "5.00", "30.00", true), dono))
                .doesNotThrowAnyException();
    }
}
