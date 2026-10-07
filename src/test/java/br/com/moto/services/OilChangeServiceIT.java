package br.com.moto.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moto.PostgresIT;
import br.com.moto.domain.OilLimit;
import br.com.moto.domain.OilStatusLevel;
import br.com.moto.exceptions.InvalidOdometerException;
import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.MotorcycleRequestDTO;
import br.com.moto.models.dto.OdometerReadingRequestDTO;
import br.com.moto.models.dto.OilChangeRequestDTO;
import br.com.moto.models.dto.RefuelingRequestDTO;
import br.com.moto.models.enums.FuelType;
import br.com.moto.models.enums.OilType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OilChangeServiceIT extends PostgresIT {

    @Autowired
    private MotorcycleService motorcycleService;

    @Autowired
    private OilChangeService service;

    @Autowired
    private RefuelingService refuelingService;

    @Autowired
    private OdometerReadingService readingService;

    private static String dono() {
        return "dono-" + UUID.randomUUID();
    }

    private UUID moto(final String dono) {
        return motorcycleService.criar(
                new MotorcycleRequestDTO("Fazer", null, "Fazer 250", null, null, 1000, null, null), dono).id();
    }

    private static OilChangeRequestDTO troca(final String data, final int km, final OilType tipo, final int intervaloKm, final int meses) {
        return new OilChangeRequestDTO(LocalDate.parse(data), km, tipo, "Motul", "10W-40", new BigDecimal("85.00"), intervaloKm, meses);
    }

    @Test
    void criar_persisteTodosOsCampos() {
        final var dono = dono();
        final var moto = moto(dono);

        final var criada = service.criar(moto, troca("2026-06-01", 20000, OilType.SEMI_SYNTHETIC, 4000, 6), dono);

        assertThat(criada.id()).isNotNull();
        assertThat(criada.oilType()).isEqualTo(OilType.SEMI_SYNTHETIC);
        assertThat(criada.brand()).isEqualTo("Motul");
        assertThat(criada.viscosity()).isEqualTo("10W-40");
        assertThat(criada.cost()).isEqualByComparingTo("85.00");
        assertThat(criada.intervalKm()).isEqualTo(4000);
        assertThat(criada.intervalMonths()).isEqualTo(6);
    }

    @Test
    void listar_maisRecentePrimeiro() {
        final var dono = dono();
        final var moto = moto(dono);
        service.criar(moto, troca("2026-01-10", 15000, OilType.MINERAL, 1500, 6), dono);
        service.criar(moto, troca("2026-06-01", 20000, OilType.SEMI_SYNTHETIC, 4000, 6), dono);

        assertThat(service.listar(moto, dono)).extracting("odometerKm").containsExactly(20000, 15000);
    }

    @Test
    void status_usaOHodometroAtualDeQualquerFonteEAUltimaTroca() {
        final var dono = dono();
        final var moto = moto(dono);
        service.criar(moto, troca("2026-01-10", 15000, OilType.MINERAL, 1500, 6), dono);
        service.criar(moto, troca("2026-06-01", 20000, OilType.SEMI_SYNTHETIC, 4000, 6), dono);
        refuelingService.criar(moto, new RefuelingRequestDTO(LocalDate.parse("2026-07-15"), 23600, new BigDecimal("10.00"),
                new BigDecimal("60.00"), null, FuelType.GASOLINA_COMUM, true), dono);

        final var status = service.status(moto, dono, LocalDate.parse("2026-08-01"));

        assertThat(status.lastChange().odometerKm()).as("só a última troca conta").isEqualTo(20000);
        assertThat(status.currentOdometerKm()).isEqualTo(23600);
        assertThat(status.dueKm()).isEqualTo(24000);
        assertThat(status.dueDate()).isEqualTo(LocalDate.parse("2026-12-01"));
        assertThat(status.kmRemaining()).isEqualTo(400);
        assertThat(status.daysRemaining()).isEqualTo(122L);
        assertThat(status.level()).isEqualTo(OilStatusLevel.PERTO);
        assertThat(status.limitedBy()).isEqualTo(OilLimit.KM);
    }

    @Test
    void status_leituraAvulsaTambemAvancaOHodometro() {
        final var dono = dono();
        final var moto = moto(dono);
        service.criar(moto, troca("2026-06-01", 20000, OilType.SEMI_SYNTHETIC, 4000, 6), dono);
        readingService.criar(moto, new OdometerReadingRequestDTO(LocalDate.parse("2026-07-20"), 24100), dono);

        final var status = service.status(moto, dono, LocalDate.parse("2026-08-01"));

        assertThat(status.level()).isEqualTo(OilStatusLevel.VENCIDA);
        assertThat(status.kmRemaining()).isEqualTo(-100);
    }

    @Test
    void status_motoSemTrocaDevolveSoOHodometro() {
        final var dono = dono();
        final var moto = moto(dono);

        final var status = service.status(moto, dono, LocalDate.parse("2026-08-01"));

        assertThat(status.lastChange()).isNull();
        assertThat(status.level()).isNull();
        assertThat(status.dueDate()).isNull();
        assertThat(status.currentOdometerKm()).isEqualTo(1000);
    }

    @Test
    void criar_hodometroMenorQueOAnteriorERejeitado() {
        final var dono = dono();
        final var moto = moto(dono);
        service.criar(moto, troca("2026-06-01", 20000, OilType.SEMI_SYNTHETIC, 4000, 6), dono);

        assertThatThrownBy(() -> service.criar(moto, troca("2026-07-01", 19000, OilType.SEMI_SYNTHETIC, 4000, 6), dono))
                .isInstanceOf(InvalidOdometerException.class);
    }

    @Test
    void abastecimentoPosteriorNaoPodeTerHodometroMenorQueOdaTroca() {
        final var dono = dono();
        final var moto = moto(dono);
        service.criar(moto, troca("2026-06-01", 20000, OilType.SEMI_SYNTHETIC, 4000, 6), dono);

        assertThatThrownBy(() -> refuelingService.criar(moto, new RefuelingRequestDTO(LocalDate.parse("2026-06-10"), 19900,
                new BigDecimal("5.00"), new BigDecimal("30.00"), null, FuelType.ETANOL, false), dono))
                .isInstanceOf(InvalidOdometerException.class);
    }

    @Test
    void atualizar_naoConflitaConsigoMesmo() {
        final var dono = dono();
        final var moto = moto(dono);
        final var criada = service.criar(moto, troca("2026-06-01", 20000, OilType.SEMI_SYNTHETIC, 4000, 6), dono);

        final var atualizada = service.atualizar(moto, criada.id(), troca("2026-06-01", 20000, OilType.SYNTHETIC, 6000, 12), dono);

        assertThat(atualizada.oilType()).isEqualTo(OilType.SYNTHETIC);
        assertThat(atualizada.intervalKm()).isEqualTo(6000);
        assertThat(service.listar(moto, dono)).hasSize(1);
    }

    @Test
    void outroDonoNaoAcessaNada() {
        final var dono = dono();
        final var moto = moto(dono);
        final var criada = service.criar(moto, troca("2026-06-01", 20000, OilType.SEMI_SYNTHETIC, 4000, 6), dono);
        final var intruso = dono();

        assertThatThrownBy(() -> service.criar(moto, troca("2026-07-01", 21000, OilType.MINERAL, 1500, 6), intruso))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.listar(moto, intruso)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.status(moto, intruso, LocalDate.parse("2026-08-01")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.atualizar(moto, criada.id(), troca("2026-06-01", 20000, OilType.MINERAL, 1500, 6), intruso))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.excluir(moto, criada.id(), intruso)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void excluir_removeATrocaEOStatusVoltaAoAnterior() {
        final var dono = dono();
        final var moto = moto(dono);
        service.criar(moto, troca("2026-01-10", 15000, OilType.MINERAL, 1500, 6), dono);
        final var ultima = service.criar(moto, troca("2026-06-01", 20000, OilType.SEMI_SYNTHETIC, 4000, 6), dono);

        service.excluir(moto, ultima.id(), dono);

        assertThat(service.status(moto, dono, LocalDate.parse("2026-02-01")).lastChange().odometerKm()).isEqualTo(15000);
    }
}
