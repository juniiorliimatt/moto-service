package br.com.moto.controllers;

import br.com.moto.models.dto.OilChangeDTO;
import br.com.moto.models.dto.OilChangeRequestDTO;
import br.com.moto.models.dto.OilStatusDTO;
import br.com.moto.services.OilChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
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

@Tag(name = "Troca de óleo")
@RestController
@RequestMapping("/api/v1/motorcycles/{motoId}")
public class OilChangeController {

    private final OilChangeService service;
    private final Clock clock;

    public OilChangeController(final OilChangeService service, final Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Operation(summary = "Lista as trocas de óleo da moto, mais recente primeiro")
    @GetMapping("/oil-changes")
    public List<OilChangeDTO> listar(@PathVariable final UUID motoId, final Authentication authentication) {
        return service.listar(motoId, authentication.getName());
    }

    @Operation(summary = "Registra uma troca de óleo. 400 se o hodômetro quebrar a ordem cronológica da moto")
    @PostMapping("/oil-changes")
    @ResponseStatus(HttpStatus.CREATED)
    public OilChangeDTO criar(@PathVariable final UUID motoId, @Valid @RequestBody final OilChangeRequestDTO request,
                              final Authentication authentication) {
        return service.criar(motoId, request, authentication.getName());
    }

    @Operation(summary = "Atualiza uma troca de óleo")
    @PutMapping("/oil-changes/{id}")
    public OilChangeDTO atualizar(@PathVariable final UUID motoId, @PathVariable final UUID id,
                                  @Valid @RequestBody final OilChangeRequestDTO request, final Authentication authentication) {
        return service.atualizar(motoId, id, request, authentication.getName());
    }

    @Operation(summary = "Exclui uma troca de óleo")
    @DeleteMapping("/oil-changes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable final UUID motoId, @PathVariable final UUID id, final Authentication authentication) {
        service.excluir(motoId, id, authentication.getName());
    }

    @Operation(summary = "Próxima troca de óleo: vence primeiro o que ocorrer antes entre km e tempo. Sem troca registrada, só o hodômetro atual vem preenchido")
    @GetMapping("/oil-status")
    public OilStatusDTO status(@PathVariable final UUID motoId, final Authentication authentication) {
        return service.status(motoId, authentication.getName(), LocalDate.now(clock));
    }
}
