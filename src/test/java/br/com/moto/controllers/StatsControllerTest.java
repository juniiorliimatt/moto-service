package br.com.moto.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moto.config.ClockConfig;
import br.com.moto.config.MessageConfig;
import br.com.moto.exceptions.InvalidPeriodException;
import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.MonthlyStatsDTO;
import br.com.moto.models.dto.StatsDTO;
import br.com.moto.models.dto.YearlyStatsDTO;
import br.com.moto.services.StatsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@ActiveProfiles("test")
@Import({MessageConfig.class, ClockConfig.class})
@WebMvcTest(StatsController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class StatsControllerTest {

    private static final UUID MOTO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String URL = "/api/v1/motorcycles/" + MOTO + "/stats";
    private static final StatsDTO STATS = new StatsDTO(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), 300,
            new BigDecimal("9.68"), new BigDecimal("18.00"), new BigDecimal("110.00"), new BigDecimal("6.111"),
            new BigDecimal("37.50"), new BigDecimal("0.17"), 1, true, new BigDecimal("37.50"), new BigDecimal("37.50"), 300);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService service;

    private static RequestPostProcessor jr() {
        return opaqueToken().attributes(attributes -> attributes.put("sub", "jr"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("resumo devolve as métricas e repassa o período e o dono")
    void resumo_retornaMetricas() throws Exception {
        when(service.resumo(eq(MOTO), eq("jr"), any(), any(), any(LocalDate.class))).thenReturn(STATS);

        mockMvc.perform(get(URL).with(jr()).param("from", "2026-01-01").param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.km").value(300))
                .andExpect(jsonPath("$.kmPerLiter").value(37.5))
                .andExpect(jsonPath("$.lowConfidence").value(true))
                .andExpect(jsonPath("$.from").value("2026-01-01"))
                .andExpect(jsonPath("$.longestSegmentKm").value(300));

        verify(service).resumo(eq(MOTO), eq("jr"), eq(LocalDate.parse("2026-01-01")), eq(LocalDate.parse("2026-01-31")), any(LocalDate.class));
    }

    @Test
    @DisplayName("resumo sem período repassa nulos (o serviço decide o padrão)")
    void resumo_semPeriodo() throws Exception {
        when(service.resumo(eq(MOTO), eq("jr"), eq(null), eq(null), any(LocalDate.class))).thenReturn(STATS);

        mockMvc.perform(get(URL).with(jr())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("data malformada responde 400")
    void resumo_dataMalformada_retorna400() throws Exception {
        mockMvc.perform(get(URL).with(jr()).param("from", "ontem")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("período invertido responde 400 com a mensagem")
    void resumo_periodoInvertido_retorna400() throws Exception {
        when(service.resumo(eq(MOTO), eq("jr"), any(), any(), any(LocalDate.class)))
                .thenThrow(new InvalidPeriodException("A data inicial deve ser anterior ou igual à final"));

        mockMvc.perform(get(URL).with(jr()).param("from", "2026-03-01").param("to", "2026-02-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A data inicial deve ser anterior ou igual à final"));
    }

    @Test
    @DisplayName("moto de outro dono responde 404")
    void resumo_motoDeOutroDono_retorna404() throws Exception {
        when(service.resumo(eq(MOTO), eq("jr"), any(), any(), any(LocalDate.class)))
                .thenThrow(new ResourceNotFoundException("Moto não encontrada"));

        mockMvc.perform(get(URL).with(jr())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("série mensal devolve o mês, as métricas e a variação do gasto")
    void mensal_retornaSerie() throws Exception {
        when(service.mensal(eq(MOTO), eq("jr"), eq(2026), any(LocalDate.class)))
                .thenReturn(List.of(new MonthlyStatsDTO(2026, 1, STATS, null), new MonthlyStatsDTO(2026, 2, STATS, new BigDecimal("-13.64"))));

        mockMvc.perform(get(URL + "/monthly").with(jr()).param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].month").value(1))
                .andExpect(jsonPath("$[0].spentChangePct").doesNotExist())
                .andExpect(jsonPath("$[1].spentChangePct").value(-13.64))
                .andExpect(jsonPath("$[1].stats.km").value(300));
    }

    @Test
    @DisplayName("série mensal sem year responde 400, com year inválido também")
    void mensal_yearAusenteOuInvalido_retorna400() throws Exception {
        mockMvc.perform(get(URL + "/monthly").with(jr())).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "/monthly").with(jr()).param("year", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("série anual devolve um item por ano")
    void anual_retornaSerie() throws Exception {
        when(service.anual(eq(MOTO), eq("jr"), any(LocalDate.class))).thenReturn(List.of(new YearlyStatsDTO(2026, STATS)));

        mockMvc.perform(get(URL + "/yearly").with(jr()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[0].stats.totalSpent").value(110.0));
    }
}
