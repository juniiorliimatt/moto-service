package br.com.moto.controllers;

import br.com.moto.models.dto.MotorcycleDTO;
import br.com.moto.models.dto.MotorcycleRequestDTO;
import br.com.moto.models.dto.MotorcycleRevisionDTO;
import br.com.moto.services.AuditService;
import br.com.moto.services.MotorcycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Motos")
@RestController
@RequestMapping("/api/v1/motorcycles")
public class MotorcycleController {

    private final MotorcycleService service;
    private final AuditService auditService;

    public MotorcycleController(final MotorcycleService service, final AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @Operation(summary = "Lista as motos do usuário autenticado, por apelido")
    @GetMapping
    public List<MotorcycleDTO> listar(final Authentication authentication) {
        return service.listar(authentication.getName());
    }

    @Operation(summary = "Busca uma moto do usuário (404 se não existir ou for de outro dono)")
    @GetMapping("/{id}")
    public MotorcycleDTO buscar(@PathVariable final UUID id, final Authentication authentication) {
        return service.buscar(id, authentication.getName());
    }

    @Operation(summary = "Cadastra uma moto; o dono vem sempre do token")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MotorcycleDTO criar(@Valid @RequestBody final MotorcycleRequestDTO request, final Authentication authentication) {
        return service.criar(request, authentication.getName());
    }

    @Operation(summary = "Atualiza uma moto do usuário")
    @PutMapping("/{id}")
    public MotorcycleDTO atualizar(@PathVariable final UUID id, @Valid @RequestBody final MotorcycleRequestDTO request,
                                   final Authentication authentication) {
        return service.atualizar(id, request, authentication.getName());
    }

    @Operation(summary = "Exclui a moto e, em cascata, seus abastecimentos, trocas de óleo e leituras")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable final UUID id, final Authentication authentication) {
        service.excluir(id, authentication.getName());
    }

    @Operation(summary = "Histórico de revisões da moto (Hibernate Envers)")
    @GetMapping("/{id}/history")
    public List<MotorcycleRevisionDTO> historico(@PathVariable final UUID id, final Authentication authentication) {
        return auditService.historicoMoto(id, authentication.getName());
    }
}
