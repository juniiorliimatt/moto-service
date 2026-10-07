package br.com.moto.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moto.exceptions.InvalidOdometerException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OdometerRulesTest {

    private static OdometerPoint p(final String data, final int km) {
        return new OdometerPoint(UUID.randomUUID(), LocalDate.parse(data), km);
    }

    @Test
    void aceitaHodometroEntreVizinhos() {
        final var outros = List.of(p("2026-01-10", 1000), p("2026-03-10", 2000));

        assertThatCode(() -> OdometerRules.validar(outros, 500, LocalDate.parse("2026-02-10"), 1500))
                .doesNotThrowAnyException();
    }

    @Test
    void aceitaPrimeiroRegistroIgualAoInicial() {
        assertThatCode(() -> OdometerRules.validar(List.of(), 1200, LocalDate.parse("2026-01-01"), 1200))
                .doesNotThrowAnyException();
    }

    @Test
    void rejeitaAbaixoDoInicial() {
        assertThatThrownBy(() -> OdometerRules.validar(List.of(), 1200, LocalDate.parse("2026-01-01"), 1199))
                .isInstanceOf(InvalidOdometerException.class)
                .hasMessageContaining("1200");
    }

    @Test
    void rejeitaMenorQueRegistroAnterior() {
        final var outros = List.of(p("2026-01-10", 1000));

        assertThatThrownBy(() -> OdometerRules.validar(outros, 0, LocalDate.parse("2026-02-01"), 999))
                .isInstanceOf(InvalidOdometerException.class)
                .hasMessageContaining("2026-01-10");
    }

    @Test
    void rejeitaMaiorQueRegistroPosterior() {
        final var outros = List.of(p("2026-03-10", 2000));

        assertThatThrownBy(() -> OdometerRules.validar(outros, 0, LocalDate.parse("2026-02-01"), 2001))
                .isInstanceOf(InvalidOdometerException.class)
                .hasMessageContaining("2026-03-10");
    }

    @Test
    void citaORegistroAnteriorDeMaiorHodometroEnaoOMaisAntigo() {
        final var outros = List.of(p("2026-01-10", 1000), p("2026-01-20", 1300), p("2026-02-25", 1700));

        assertThatThrownBy(() -> OdometerRules.validar(outros, 0, LocalDate.parse("2026-03-01"), 1200))
                .isInstanceOf(InvalidOdometerException.class)
                .hasMessageContaining("2026-02-25")
                .hasMessageContaining("1700")
                .hasMessageNotContaining("2026-01-20");
    }

    @Test
    void citaORegistroPosteriorDeMenorHodometro() {
        final var outros = List.of(p("2026-03-10", 2000), p("2026-04-10", 2500));

        assertThatThrownBy(() -> OdometerRules.validar(outros, 0, LocalDate.parse("2026-02-01"), 2600))
                .isInstanceOf(InvalidOdometerException.class)
                .hasMessageContaining("2026-03-10")
                .hasMessageContaining("2000")
                .hasMessageNotContaining("2026-04-10");
    }

    @Test
    void mesmaDataNaoCompara() {
        final var outros = List.of(p("2026-02-01", 1500));

        assertThatCode(() -> OdometerRules.validar(outros, 0, LocalDate.parse("2026-02-01"), 1400))
                .doesNotThrowAnyException();
    }

    @Test
    void hodometroIgualAoDeUmVizinhoEPermitido() {
        final var outros = List.of(p("2026-01-10", 1000), p("2026-03-10", 1000));

        assertThatCode(() -> OdometerRules.validar(outros, 0, LocalDate.parse("2026-02-10"), 1000))
                .doesNotThrowAnyException();
    }

    @Test
    void atualEOMaiorEntrePontosEInicial() {
        assertThat(OdometerRules.atual(List.of(p("2026-01-10", 1000), p("2026-02-10", 1800)), 500)).isEqualTo(1800);
    }

    @Test
    void atualSemPontosEOInicial() {
        assertThat(OdometerRules.atual(List.of(), 500)).isEqualTo(500);
    }

    @Test
    void atualNuncaFicaAbaixoDoInicial() {
        assertThat(OdometerRules.atual(List.of(p("2026-01-10", 300)), 500)).isEqualTo(500);
    }
}
