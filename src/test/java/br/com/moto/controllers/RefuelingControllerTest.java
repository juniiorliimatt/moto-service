package br.com.moto.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moto.config.MessageConfig;
import br.com.moto.exceptions.InvalidOdometerException;
import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.RefuelingDTO;
import br.com.moto.models.enums.FuelType;
import br.com.moto.services.RefuelingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@ActiveProfiles("test")
@Import(MessageConfig.class)
@WebMvcTest(RefuelingController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class RefuelingControllerTest {

    private static final UUID MOTO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String URL = "/api/v1/motorcycles/" + MOTO + "/refuelings";
    private static final String VALIDO = """
            {"date":"2026-02-10","odometerKm":1500,"liters":5.00,"totalValue":32.00,"fuelType":"GASOLINA_COMUM"}""";
    private static final RefuelingDTO DTO = new RefuelingDTO(ID, LocalDate.parse("2026-02-10"), 1500,
            new BigDecimal("5.00"), new BigDecimal("32.00"), new BigDecimal("6.400"), null, FuelType.GASOLINA_COMUM, false);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RefuelingService service;

    private static RequestPostProcessor jr() {
        return opaqueToken().attributes(attributes -> attributes.put("sub", "jr"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("criar abastecimento válido responde 201")
    void criar_valido_retorna201() throws Exception {
        when(service.criar(eq(MOTO), any(), eq("jr"))).thenReturn(DTO);

        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.odometerKm").value(1500))
                .andExpect(jsonPath("$.pricePerLiter").value(6.4))
                .andExpect(jsonPath("$.fullTank").value(false));
    }

    @Test
    @DisplayName("litros zero responde 400 com a mensagem do campo")
    void criar_litrosZero_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-02-10","odometerKm":1500,"liters":0,"totalValue":32.00,"fuelType":"GASOLINA_COMUM"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("liters"))
                .andExpect(jsonPath("$.errors[0].message").value("Litros deve ser maior que zero"));
    }

    @Test
    @DisplayName("valor ausente responde 400")
    void criar_semValor_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-02-10","odometerKm":1500,"liters":5.00,"fuelType":"GASOLINA_COMUM"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("totalValue"));
    }

    @Test
    @DisplayName("hodômetro negativo responde 400")
    void criar_hodometroNegativo_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-02-10","odometerKm":-1,"liters":5.00,"totalValue":32.00,"fuelType":"ETANOL"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("odometerKm"));
    }

    @Test
    @DisplayName("data ausente responde 400")
    void criar_semData_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"odometerKm":1500,"liters":5.00,"totalValue":32.00,"fuelType":"ETANOL"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("date"));
    }

    @Test
    @DisplayName("combustível ausente ou inválido responde 400")
    void criar_combustivelInvalido_retorna400() throws Exception {
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-02-10","odometerKm":1500,"liters":5.00,"totalValue":32.00,"fuelType":"DIESEL"}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-02-10","odometerKm":1500,"liters":5.00,"totalValue":32.00}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("fuelType"));
    }

    @Test
    @DisplayName("hodômetro que quebra a ordem cronológica responde 400 com o motivo")
    void criar_hodometroRegressivo_retorna400ComMotivo() throws Exception {
        when(service.criar(eq(MOTO), any(), eq("jr")))
                .thenThrow(new InvalidOdometerException("Hodômetro (1500 km) menor que o registro de 2026-01-10 (1600 km)"));

        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Hodômetro (1500 km) menor que o registro de 2026-01-10 (1600 km)"));
    }

    @Test
    @DisplayName("moto de outro dono responde 404")
    void criar_motoDeOutroDono_retorna404() throws Exception {
        when(service.criar(eq(MOTO), any(), eq("jr"))).thenThrow(new ResourceNotFoundException("Moto não encontrada"));

        mockMvc.perform(post(URL).with(jr()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("listar devolve página com os metadados e repassa o filtro de período")
    void listar_retornaPaginaEPassaFiltros() throws Exception {
        when(service.listar(eq(MOTO), eq("jr"), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(DTO), PageRequest.of(0, 20), 41));

        mockMvc.perform(get(URL).with(jr()).param("from", "2026-02-01").param("to", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ID.toString()))
                .andExpect(jsonPath("$.page.totalElements").value(41))
                .andExpect(jsonPath("$.page.size").value(20));

        final var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listar(eq(MOTO), eq("jr"), eq(LocalDate.parse("2026-02-01")), eq(LocalDate.parse("2026-02-28")), pageable.capture());
        org.assertj.core.api.Assertions.assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        org.assertj.core.api.Assertions.assertThat(pageable.getValue().getSort().toString()).isEqualTo("date: DESC,odometerKm: DESC");
    }

    @Test
    @DisplayName("período malformado responde 400")
    void listar_dataMalformada_retorna400() throws Exception {
        mockMvc.perform(get(URL).with(jr()).param("from", "ontem")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("tamanho de página é limitado")
    void listar_tamanhoMaximo() throws Exception {
        when(service.listar(eq(MOTO), eq("jr"), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        mockMvc.perform(get(URL).with(jr()).param("size", "5000")).andExpect(status().isOk());

        final var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listar(eq(MOTO), eq("jr"), any(), any(), pageable.capture());
        org.assertj.core.api.Assertions.assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("atualizar responde 200")
    void atualizar_retorna200() throws Exception {
        when(service.atualizar(eq(MOTO), eq(ID), any(), eq("jr"))).thenReturn(DTO);

        mockMvc.perform(put(URL + "/" + ID).with(jr()).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    @DisplayName("excluir responde 204 e abastecimento inexistente 404")
    void excluir() throws Exception {
        mockMvc.perform(delete(URL + "/" + ID).with(jr())).andExpect(status().isNoContent());

        doThrow(new ResourceNotFoundException("Abastecimento não encontrado")).when(service).excluir(MOTO, ID, "jr");
        mockMvc.perform(delete(URL + "/" + ID).with(jr())).andExpect(status().isNotFound());
    }
}
