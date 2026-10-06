package br.com.moto.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moto.config.MessageConfig;
import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.MotorcycleDTO;
import br.com.moto.models.dto.MotorcycleRevisionDTO;
import br.com.moto.services.AuditService;
import br.com.moto.services.MotorcycleService;
import java.time.LocalDateTime;
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
@WebMvcTest(MotorcycleController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class MotorcycleControllerTest {

    private static final String URL = "/api/v1/motorcycles";
    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final MotorcycleDTO DTO =
            new MotorcycleDTO(ID, "Fazer", null, "Fazer 250", null, null, 1200, null, true);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MotorcycleService service;

    @MockitoBean
    private AuditService auditService;

    private static RequestPostProcessor jr() {
        return opaqueToken().attributes(attributes -> attributes.put("sub", "jr"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("criar com payload válido responde 201 e usa o dono do token")
    void criar_comPayloadValido_retorna201() throws Exception {
        when(service.criar(any(), org.mockito.ArgumentMatchers.eq("jr"))).thenReturn(DTO);

        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"Fazer","model":"Fazer 250","initialOdometerKm":1200}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("Fazer"))
                .andExpect(jsonPath("$.initialOdometerKm").value(1200));
    }

    @Test
    @DisplayName("criar sem apelido responde 400 apontando o campo")
    void criar_semApelido_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"model":"Fazer 250","initialOdometerKm":1200}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.errors[0].message").value("Apelido é obrigatório"));
    }

    @Test
    @DisplayName("criar com hodômetro inicial negativo responde 400")
    void criar_hodometroNegativo_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"Fazer","model":"Fazer 250","initialOdometerKm":-1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("initialOdometerKm"));
    }

    @Test
    @DisplayName("corpo JSON malformado responde 400, não 500")
    void criar_jsonMalformado_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON).content("{nickname"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("buscar moto de outro dono (ou inexistente) responde 404 com ProblemDetail")
    void buscar_deOutroDono_retorna404() throws Exception {
        when(service.buscar(ID, "jr")).thenThrow(new ResourceNotFoundException("Moto não encontrada"));

        mockMvc.perform(get(URL + "/" + ID).with(jr()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Moto não encontrada"));
    }

    @Test
    @DisplayName("id que não é UUID responde 400")
    void buscar_idInvalido_retorna400() throws Exception {
        mockMvc.perform(get(URL + "/abc").with(jr())).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("atualizar responde 200 com a moto atualizada")
    void atualizar_retorna200() throws Exception {
        when(service.atualizar(any(), any(), org.mockito.ArgumentMatchers.eq("jr"))).thenReturn(DTO);

        mockMvc.perform(put(URL + "/" + ID).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"Fazer","model":"Fazer 250","initialOdometerKm":1200}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    @DisplayName("excluir responde 204")
    void excluir_retorna204() throws Exception {
        mockMvc.perform(delete(URL + "/" + ID).with(jr())).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("excluir moto inexistente responde 404")
    void excluir_inexistente_retorna404() throws Exception {
        doThrow(new ResourceNotFoundException("Moto não encontrada")).when(service).excluir(ID, "jr");

        mockMvc.perform(delete(URL + "/" + ID).with(jr())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("histórico devolve as revisões do Envers")
    void historico_retornaRevisoes() throws Exception {
        when(auditService.historicoMoto(ID, "jr")).thenReturn(List.of(
                new MotorcycleRevisionDTO(1, LocalDateTime.of(2026, 10, 6, 12, 0), "jr", "ADD", ID, "Fazer", "Fazer 250", 1200, true)));

        mockMvc.perform(get(URL + "/" + ID + "/history").with(jr()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revisionType").value("ADD"))
                .andExpect(jsonPath("$[0].changedBy").value("jr"));
    }
}
