package br.com.moto.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moto.PostgresIT;
import br.com.moto.exceptions.InvalidOdometerException;
import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.MotorcycleRequestDTO;
import br.com.moto.models.dto.OdometerReadingRequestDTO;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OdometerServiceIT extends PostgresIT {

    @Autowired
    private MotorcycleService motorcycleService;

    @Autowired
    private OdometerReadingService readingService;

    @Autowired
    private OdometerService odometerService;

    private static String dono() {
        return "dono-" + UUID.randomUUID();
    }

    private UUID motoComInicial(final String dono, final int inicial) {
        return motorcycleService.criar(
                new MotorcycleRequestDTO("Fazer", null, "Fazer 250", null, null, inicial, null, null), dono).id();
    }

    private static OdometerReadingRequestDTO leitura(final String data, final int km) {
        return new OdometerReadingRequestDTO(LocalDate.parse(data), km);
    }

    @Test
    void leituraValidaEPersistidaEViraPonto() {
        final var dono = dono();
        final var moto = motoComInicial(dono, 1000);

        final var criada = readingService.criar(moto, leitura("2026-01-10", 1500), dono);

        assertThat(criada.odometerKm()).isEqualTo(1500);
        assertThat(odometerService.pontos(moto, dono)).extracting("km").containsExactly(1500);
        assertThat(odometerService.hodometroAtual(moto, dono)).isEqualTo(1500);
    }

    @Test
    void hodometroAtualSemRegistrosEOInicial() {
        final var dono = dono();
        final var moto = motoComInicial(dono, 1000);

        assertThat(odometerService.hodometroAtual(moto, dono)).isEqualTo(1000);
    }

    @Test
    void leituraMenorQueAAnteriorERejeitada() {
        final var dono = dono();
        final var moto = motoComInicial(dono, 1000);
        readingService.criar(moto, leitura("2026-01-10", 1500), dono);

        assertThatThrownBy(() -> readingService.criar(moto, leitura("2026-01-20", 1400), dono))
                .isInstanceOf(InvalidOdometerException.class);
        assertThat(readingService.listar(moto, dono)).hasSize(1);
    }

    @Test
    void leituraAbaixoDoInicialERejeitada() {
        final var dono = dono();
        final var moto = motoComInicial(dono, 1000);

        assertThatThrownBy(() -> readingService.criar(moto, leitura("2026-01-10", 999), dono))
                .isInstanceOf(InvalidOdometerException.class);
    }

    @Test
    void leituraNoPassadoEntreDoisVizinhosEAceita() {
        final var dono = dono();
        final var moto = motoComInicial(dono, 1000);
        readingService.criar(moto, leitura("2026-01-10", 1200), dono);
        readingService.criar(moto, leitura("2026-03-10", 2000), dono);

        assertThat(readingService.criar(moto, leitura("2026-02-10", 1600), dono).odometerKm()).isEqualTo(1600);
        assertThatThrownBy(() -> readingService.criar(moto, leitura("2026-02-11", 2100), dono))
                .isInstanceOf(InvalidOdometerException.class);
    }

    @Test
    void listarDevolveEmOrdemCronologica() {
        final var dono = dono();
        final var moto = motoComInicial(dono, 0);
        readingService.criar(moto, leitura("2026-03-10", 300), dono);
        readingService.criar(moto, leitura("2026-01-10", 100), dono);

        assertThat(readingService.listar(moto, dono)).extracting("odometerKm").containsExactly(100, 300);
    }

    @Test
    void leituraDeMotoDeOutroDonoELancaNaoEncontrado() {
        final var moto = motoComInicial(dono(), 1000);

        assertThatThrownBy(() -> readingService.criar(moto, leitura("2026-01-10", 1500), dono()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> odometerService.hodometroAtual(moto, dono()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void excluirRemoveALeituraEOutroDonoNaoConsegue() {
        final var dono = dono();
        final var moto = motoComInicial(dono, 1000);
        final var leitura = readingService.criar(moto, leitura("2026-01-10", 1500), dono);

        assertThatThrownBy(() -> readingService.excluir(moto, leitura.id(), dono()))
                .isInstanceOf(ResourceNotFoundException.class);
        readingService.excluir(moto, leitura.id(), dono);

        assertThat(readingService.listar(moto, dono)).isEmpty();
    }
}
