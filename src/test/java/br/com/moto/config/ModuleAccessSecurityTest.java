package br.com.moto.config;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.moto.controllers.MotorcycleController;
import br.com.moto.services.AuditService;
import br.com.moto.services.MotorcycleService;
import java.util.List;
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

/**
 * Roda a {@link SecurityConfig} real (os testes de controller usam a segurança padrão do
 * slice): o acesso ao serviço exige o módulo MOTO, vindo da introspecção como authority
 * {@code MODULE_MOTO}. Estar autenticado, ser USER ou ser ADMIN sozinho não basta — o ADMIN passa
 * porque o workbox-api já lista todos os módulos pra ele.
 */
@ActiveProfiles("test")
@Import({SecurityConfig.class, MessageConfig.class})
@WebMvcTest(MotorcycleController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class ModuleAccessSecurityTest {

    private static final String URL = "/api/v1/motorcycles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MotorcycleService service;

    @MockitoBean
    private AuditService auditService;

    @Test
    @DisplayName("sem token responde 401")
    void withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("autenticado só com USER, sem o módulo MOTO, responde 403")
    void userWithoutModule_returnsForbidden() throws Exception {
        mockMvc.perform(get(URL).with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("módulo de outro serviço (FINANCAS) não dá acesso ao MOTO")
    void otherModule_returnsForbidden() throws Exception {
        mockMvc.perform(get(URL).with(opaqueToken().authorities(
                        new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("MODULE_FINANCAS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("com o módulo MOTO responde 200")
    void withModule_returnsOk() throws Exception {
        when(service.listar("user")).thenReturn(List.of());

        mockMvc.perform(get(URL).with(opaqueToken().authorities(
                        new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("MODULE_MOTO"))))
                .andExpect(status().isOk());
    }
}
