package br.com.moto.services;

import br.com.moto.domain.ConsumptionCalculator;
import br.com.moto.domain.FuelEntry;
import br.com.moto.domain.OdometerPoint;
import br.com.moto.domain.OdometerTimeline;
import br.com.moto.domain.Segment;
import br.com.moto.exceptions.InvalidPeriodException;
import br.com.moto.models.dto.MonthlyStatsDTO;
import br.com.moto.models.dto.StatsDTO;
import br.com.moto.models.dto.YearlyStatsDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Métricas de rodagem e consumo. Carrega os registros do dono (centenas por ano, no máximo) e
 * delega o cálculo ao domínio puro ({@link ConsumptionCalculator}, {@link OdometerTimeline}).
 */
@Service
public class StatsService {

    private final MotorcycleService motorcycleService;
    private final RefuelingService refuelingService;
    private final OdometerService odometerService;
    private final MessageSourceAccessor messages;

    public StatsService(final MotorcycleService motorcycleService, final RefuelingService refuelingService,
                        final OdometerService odometerService, final MessageSourceAccessor messages) {
        this.motorcycleService = motorcycleService;
        this.refuelingService = refuelingService;
        this.odometerService = odometerService;
        this.messages = messages;
    }

    /**
     * Resumo de um período. {@code de} nulo = data do primeiro registro da moto (ou {@code ate}, se não
     * houver nenhum); {@code ate} nulo = {@code hoje}.
     */
    @Transactional(readOnly = true)
    public StatsDTO resumo(final UUID motoId, final String owner, final LocalDate de, final LocalDate ate, final LocalDate hoje) {
        final var dados = carregar(motoId, owner);
        final var fim = ate != null ? ate : hoje;
        final var inicio = de != null ? de : primeiraData(dados).orElse(fim);
        if (inicio.isAfter(fim)) {
            throw new InvalidPeriodException(messages.getMessage("estatisticas.periodoInvalido"));
        }
        return montar(dados, inicio, fim, hoje);
    }

    /** Os 12 meses do {@code ano}, em ordem; meses sem movimento ou futuros vêm zerados. */
    @Transactional(readOnly = true)
    public List<MonthlyStatsDTO> mensal(final UUID motoId, final String owner, final int ano, final LocalDate hoje) {
        final var dados = carregar(motoId, owner);
        final var meses = new ArrayList<MonthlyStatsDTO>(12);
        StatsDTO anterior = null;
        for (int mes = 1; mes <= 12; mes++) {
            final var inicio = LocalDate.of(ano, mes, 1);
            final var atual = montar(dados, inicio, inicio.plusMonths(1).minusDays(1), hoje);
            meses.add(new MonthlyStatsDTO(ano, mes, atual, variacaoDoGasto(anterior, atual)));
            anterior = atual;
        }
        return List.copyOf(meses);
    }

    /** Um item por ano com algum registro, em ordem crescente. */
    @Transactional(readOnly = true)
    public List<YearlyStatsDTO> anual(final UUID motoId, final String owner, final LocalDate hoje) {
        final var dados = carregar(motoId, owner);
        return Stream.concat(dados.entradas().stream().map(e -> e.date().getYear()),
                        dados.pontos().stream().map(p -> p.date().getYear()))
                .distinct().sorted()
                .map(ano -> new YearlyStatsDTO(ano, montar(dados, LocalDate.of(ano, 1, 1), LocalDate.of(ano, 12, 31), hoje)))
                .toList();
    }

    private Dados carregar(final UUID motoId, final String owner) {
        final var moto = motorcycleService.exigirDoDono(motoId, owner);
        final var entradas = refuelingService.entradasOrdenadas(motoId, owner);
        return new Dados(moto.getInitialOdometerKm(), entradas, ConsumptionCalculator.segmentos(entradas),
                odometerService.pontos(motoId, owner));
    }

    private static StatsDTO montar(final Dados dados, final LocalDate de, final LocalDate ate, final LocalDate hoje) {
        final var janela = ConsumptionCalculator.janela(dados.entradas(), dados.segmentos(), de, ate);
        final int km = OdometerTimeline.kmNaJanela(dados.pontos(), dados.hodometroInicial(), de, ate);
        final var ultimoDia = ate.isAfter(hoje) ? hoje : ate;
        final long dias = Math.max(1, ChronoUnit.DAYS.between(de, ultimoDia) + 1);
        final var kmPorDia = BigDecimal.valueOf(km).divide(BigDecimal.valueOf(dias), 2, RoundingMode.HALF_UP);
        return new StatsDTO(de, ate, km, kmPorDia, janela.litersRefueled(), janela.totalSpent(), janela.pricePerLiter(),
                janela.kmPerLiter(), janela.costPerKm(), janela.segmentCount(), janela.lowConfidence(),
                janela.bestKmPerLiter(), janela.worstKmPerLiter(), janela.longestSegmentKm());
    }

    private static BigDecimal variacaoDoGasto(final StatsDTO anterior, final StatsDTO atual) {
        if (anterior == null || anterior.totalSpent().signum() <= 0) {
            return null;
        }
        return atual.totalSpent().subtract(anterior.totalSpent())
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior.totalSpent(), 2, RoundingMode.HALF_UP);
    }

    private static Optional<LocalDate> primeiraData(final Dados dados) {
        return Stream.concat(dados.entradas().stream().map(FuelEntry::date), dados.pontos().stream().map(OdometerPoint::date))
                .min(Comparator.naturalOrder());
    }

    /** Tudo o que o cálculo precisa de uma moto, carregado uma vez por requisição. */
    private record Dados(int hodometroInicial, List<FuelEntry> entradas, List<Segment> segmentos, List<OdometerPoint> pontos) {
    }
}
