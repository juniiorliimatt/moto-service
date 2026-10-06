package br.com.moto.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moto.PostgresIT;
import br.com.moto.exceptions.ResourceNotFoundException;
import br.com.moto.models.dto.MotorcycleRequestDTO;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MotorcycleServiceIT extends PostgresIT {

    @Autowired
    private MotorcycleService service;

    @Autowired
    private AuditService auditService;

    private static MotorcycleRequestDTO request(final String apelido) {
        return new MotorcycleRequestDTO(apelido, "Yamaha", "Fazer 250", 2022, "ABC1D23", 1200, new BigDecimal("14.00"), null);
    }

    /** Dono único por teste: o container é compartilhado entre os ITs. */
    private static String dono() {
        return "dono-" + UUID.randomUUID();
    }

    @Test
    void criar_persisteComDonoEAtivaPorPadrao() {
        final var dono = dono();

        final var criada = service.criar(request("Fazer"), dono);

        assertThat(criada.id()).isNotNull();
        assertThat(criada.active()).isTrue();
        assertThat(criada.initialOdometerKm()).isEqualTo(1200);
        assertThat(criada.tankCapacityLiters()).isEqualByComparingTo("14.00");
        assertThat(service.buscar(criada.id(), dono).nickname()).isEqualTo("Fazer");
    }

    @Test
    void listar_devolveSomenteMotosDoDonoOrdenadasPorApelido() {
        final var dono = dono();
        service.criar(request("Zeta"), dono);
        service.criar(request("Alfa"), dono);
        service.criar(request("De outro"), dono());

        assertThat(service.listar(dono)).extracting("nickname").containsExactly("Alfa", "Zeta");
    }

    @Test
    void buscar_deOutroDono_lancaNaoEncontrado() {
        final var criada = service.criar(request("Fazer"), dono());

        assertThatThrownBy(() -> service.buscar(criada.id(), dono()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void atualizar_deOutroDono_lancaNaoEncontradoESemAlterarOriginal() {
        final var dono = dono();
        final var criada = service.criar(request("Fazer"), dono);

        assertThatThrownBy(() -> service.atualizar(criada.id(), request("Invadida"), dono()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.buscar(criada.id(), dono).nickname()).isEqualTo("Fazer");
    }

    @Test
    void excluir_removeAMoto() {
        final var dono = dono();
        final var criada = service.criar(request("Fazer"), dono);

        service.excluir(criada.id(), dono);

        assertThatThrownBy(() -> service.buscar(criada.id(), dono)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void historico_registraCriacaoEAlteracao() {
        final var dono = dono();
        final var criada = service.criar(request("Fazer"), dono);
        service.atualizar(criada.id(), request("Fazer Renomeada"), dono);

        final var historico = auditService.historicoMoto(criada.id(), dono);

        assertThat(historico).extracting("revisionType").containsExactly("ADD", "MOD");
        assertThat(historico.get(1).nickname()).isEqualTo("Fazer Renomeada");
    }

    @Test
    void historico_deOutroDono_lancaNaoEncontrado() {
        final var criada = service.criar(request("Fazer"), dono());

        assertThatThrownBy(() -> auditService.historicoMoto(criada.id(), dono()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
