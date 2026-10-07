package br.com.moto.controllers;

import br.com.moto.models.dto.MonthlyStatsDTO;
import br.com.moto.models.dto.StatsDTO;
import br.com.moto.models.dto.YearlyStatsDTO;
import br.com.moto.services.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Métricas")
@RestController
@RequestMapping("/api/v1/motorcycles/{motoId}/stats")
public class StatsController {

    private final StatsService service;
    private final Clock clock;

    public StatsController(final StatsService service, final Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Operation(summary = "Métricas de rodagem e consumo de um período. Sem `from`: data do primeiro registro; sem `to`: hoje. km/l e custo por km são ponderados (Σkm / Σlitros), com selo de baixa confiança quando há menos de 3 trechos")
    @GetMapping
    public StatsDTO resumo(@PathVariable final UUID motoId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
                           final Authentication authentication) {
        return service.resumo(motoId, authentication.getName(), from, to, LocalDate.now(clock));
    }

    @Operation(summary = "Série mensal de um ano: sempre 12 meses (sem movimento ou futuros vêm zerados), com a variação do gasto contra o mês anterior")
    @GetMapping("/monthly")
    public List<MonthlyStatsDTO> mensal(@PathVariable final UUID motoId, @RequestParam final int year,
                                        final Authentication authentication) {
        return service.mensal(motoId, authentication.getName(), year, LocalDate.now(clock));
    }

    @Operation(summary = "Série anual: um item por ano com algum registro, em ordem crescente")
    @GetMapping("/yearly")
    public List<YearlyStatsDTO> anual(@PathVariable final UUID motoId, final Authentication authentication) {
        return service.anual(motoId, authentication.getName(), LocalDate.now(clock));
    }
}
