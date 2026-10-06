package br.com.moto.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moto.config.MessageConfig;
import br.com.moto.exceptions.InvalidOdometerException;
import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.OdometerReadingDTO;
import br.com.moto.services.OdometerReadingService;
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
@Import(MessageConfig.class)
@WebMvcTest(OdometerReadingController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class OdometerReadingControllerTest {

    private static final UUID MOTO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LEITURA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String URL = "/api/v1/motorcycles/" + MOTO + "/odometer-readings";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OdometerReadingService service;

    private static RequestPostProcessor jr() {
        return opaqueToken().attributes(attributes -> attributes.put("sub", "jr"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("criar leitura válida responde 201")
    void criar_valida_retorna201() throws Exception {
        when(service.criar(eq(MOTO), any(), eq("jr")))
                .thenReturn(new OdometerReadingDTO(LEITURA, LocalDate.parse("2026-02-10"), 1850));

        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-02-10","odometerKm":1850}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.odometerKm").value(1850))
                .andExpect(jsonPath("$.date").value("2026-02-10"));
    }

    @Test
    @DisplayName("criar sem hodômetro responde 400 apontando o campo")
    void criar_semHodometro_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-02-10"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("odometerKm"))
                .andExpect(jsonPath("$.errors[0].message").value("Hodômetro é obrigatório"));
    }

    @Test
    @DisplayName("criar com hodômetro negativo responde 400")
    void criar_hodometroNegativo_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-02-10","odometerKm":-5}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("odometerKm"));
    }

    @Test
    @DisplayName("criar sem data responde 400")
    void criar_semData_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"odometerKm":1850}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("date"));
    }

    @Test
    @DisplayName("hodômetro que quebra a regra de monotonicidade responde 400 com o motivo")
    void criar_hodometroRegressivo_retorna400ComMotivo() throws Exception {
        when(service.criar(eq(MOTO), any(), eq("jr")))
                .thenThrow(new InvalidOdometerException("Hodômetro (1400 km) menor que o registro de 2026-01-10 (1500 km)"));

        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-02-10","odometerKm":1400}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Hodômetro (1400 km) menor que o registro de 2026-01-10 (1500 km)"));
    }

    @Test
    @DisplayName("moto de outro dono responde 404")
    void criar_motoDeOutroDono_retorna404() throws Exception {
        when(service.criar(eq(MOTO), any(), eq("jr"))).thenThrow(new ResourceNotFoundException("Moto não encontrada"));

        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-02-10","odometerKm":1850}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("listar devolve as leituras da moto")
    void listar_retorna200() throws Exception {
        when(service.listar(MOTO, "jr"))
                .thenReturn(List.of(new OdometerReadingDTO(LEITURA, LocalDate.parse("2026-02-10"), 1850)));

        mockMvc.perform(get(URL).with(jr()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(LEITURA.toString()));
    }

    @Test
    @DisplayName("excluir responde 204")
    void excluir_retorna204() throws Exception {
        mockMvc.perform(delete(URL + "/" + LEITURA).with(jr())).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("excluir leitura inexistente responde 404")
    void excluir_inexistente_retorna404() throws Exception {
        doThrow(new ResourceNotFoundException("Leitura não encontrada")).when(service).excluir(MOTO, LEITURA, "jr");

        mockMvc.perform(delete(URL + "/" + LEITURA).with(jr())).andExpect(status().isNotFound());
    }
}
