package br.com.moto.controllers;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moto.config.ClockConfig;
import br.com.moto.config.MessageConfig;
import br.com.moto.domain.OilLimit;
import br.com.moto.domain.OilStatusLevel;
import br.com.moto.exceptions.InvalidOdometerException;
import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.OilChangeDTO;
import br.com.moto.models.dto.OilStatusDTO;
import br.com.moto.models.enums.OilType;
import br.com.moto.services.OilChangeService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@ActiveProfiles("test")
@Import({MessageConfig.class, ClockConfig.class})
@WebMvcTest({OilChangeController.class, OilIntervalController.class})
@MockitoBean(types = JpaMetamodelMappingContext.class)
class OilChangeControllerTest {

    private static final UUID MOTO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String URL = "/api/v1/motorcycles/" + MOTO + "/oil-changes";
    private static final String VALIDO = """
            {"date":"2026-06-01","odometerKm":20000,"oilType":"SEMI_SYNTHETIC","intervalKm":4000,"intervalMonths":6}""";
    private static final OilChangeDTO DTO = new OilChangeDTO(ID, LocalDate.parse("2026-06-01"), 20000, OilType.SEMI_SYNTHETIC,
            null, null, null, 4000, 6);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OilChangeService service;

    private static RequestPostProcessor jr() {
        return opaqueToken().attributes(attributes -> attributes.put("sub", "jr"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("criar troca válida responde 201")
    void criar_valida_retorna201() throws Exception {
        when(service.criar(eq(MOTO), any(), eq("jr"))).thenReturn(DTO);

        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.oilType").value("SEMI_SYNTHETIC"))
                .andExpect(jsonPath("$.intervalKm").value(4000));
    }

    @Test
    @DisplayName("intervalo em km igual a zero responde 400")
    void criar_intervaloKmZero_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-06-01","odometerKm":20000,"oilType":"MINERAL","intervalKm":0,"intervalMonths":6}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("intervalKm"))
                .andExpect(jsonPath("$.errors[0].message").value("Intervalo em km deve ser maior que zero"));
    }

    @Test
    @DisplayName("intervalo em meses ausente responde 400")
    void criar_semIntervaloMeses_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-06-01","odometerKm":20000,"oilType":"MINERAL","intervalKm":1500}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("intervalMonths"));
    }

    @Test
    @DisplayName("tipo de óleo ausente responde 400")
    void criar_semTipo_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-06-01","odometerKm":20000,"intervalKm":1500,"intervalMonths":6}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("oilType"));
    }

    @Test
    @DisplayName("custo negativo responde 400")
    void criar_custoNegativo_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-06-01","odometerKm":20000,"oilType":"MINERAL","intervalKm":1500,"intervalMonths":6,"cost":-1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("cost"));
    }

    @Test
    @DisplayName("hodômetro regressivo responde 400 com o motivo")
    void criar_hodometroRegressivo_retorna400() throws Exception {
        when(service.criar(eq(MOTO), any(), eq("jr")))
                .thenThrow(new InvalidOdometerException("Hodômetro (100 km) abaixo do hodômetro inicial da moto (1000 km)"));

        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Hodômetro (100 km) abaixo do hodômetro inicial da moto (1000 km)"));
    }

    @Test
    @DisplayName("listar, atualizar e excluir")
    void listarAtualizarExcluir() throws Exception {
        when(service.listar(MOTO, "jr")).thenReturn(List.of(DTO));
        when(service.atualizar(eq(MOTO), eq(ID), any(), eq("jr"))).thenReturn(DTO);

        mockMvc.perform(get(URL).with(jr())).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(ID.toString()));
        mockMvc.perform(put(URL + "/" + ID).with(jr()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk());
        mockMvc.perform(delete(URL + "/" + ID).with(jr())).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("excluir troca inexistente ou de outro dono responde 404")
    void excluir_inexistente_retorna404() throws Exception {
        doThrow(new ResourceNotFoundException("Troca de óleo não encontrada")).when(service).excluir(MOTO, ID, "jr");

        mockMvc.perform(delete(URL + "/" + ID).with(jr())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("oil-status devolve o vencimento calculado, com a data de hoje do Clock")
    void oilStatus_retornaVencimento() throws Exception {
        when(service.status(eq(MOTO), eq("jr"), any(LocalDate.class))).thenReturn(new OilStatusDTO(DTO, 23600,
                LocalDate.parse("2026-12-01"), 24000, 400, 122L, OilStatusLevel.PERTO, OilLimit.KM));

        mockMvc.perform(get("/api/v1/motorcycles/" + MOTO + "/oil-status").with(jr()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("PERTO"))
                .andExpect(jsonPath("$.limitedBy").value("KM"))
                .andExpect(jsonPath("$.kmRemaining").value(400))
                .andExpect(jsonPath("$.dueDate").value("2026-12-01"))
                .andExpect(jsonPath("$.lastChange.id").value(ID.toString()));
    }

    @Test
    @DisplayName("oil-status de moto que nunca trocou óleo responde 200 com lastChange nulo")
    void oilStatus_semTroca_retornaLastChangeNulo() throws Exception {
        when(service.status(eq(MOTO), eq("jr"), any(LocalDate.class)))
                .thenReturn(new OilStatusDTO(null, 1200, null, null, null, null, null, null));

        mockMvc.perform(get("/api/v1/motorcycles/" + MOTO + "/oil-status").with(jr()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastChange").value(nullValue()))
                .andExpect(jsonPath("$.level").value(nullValue()))
                .andExpect(jsonPath("$.currentOdometerKm").value(1200));
    }

    @Test
    @DisplayName("oil-intervals lista os 3 tipos com padrão e faixa")
    void oilIntervals_listaPadroes() throws Exception {
        mockMvc.perform(get("/api/v1/oil-intervals").with(jr()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("MINERAL"))
                .andExpect(jsonPath("$[0].defaultKm").value(1500))
                .andExpect(jsonPath("$[0].minKm").value(1000))
                .andExpect(jsonPath("$[0].maxKm").value(1500))
                .andExpect(jsonPath("$[0].defaultMonths").value(6))
                .andExpect(jsonPath("$[2].type").value("SYNTHETIC"))
                .andExpect(jsonPath("$[2].defaultMonths").value(12));
    }
}
