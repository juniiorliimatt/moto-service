package br.com.moto.controllers;

import br.com.moto.models.dto.RefuelingDTO;
import br.com.moto.models.dto.RefuelingRequestDTO;
import br.com.moto.services.RefuelingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Abastecimentos")
@RestController
@RequestMapping("/api/v1/motorcycles/{motoId}/refuelings")
public class RefuelingController {

    private final RefuelingService service;

    public RefuelingController(final RefuelingService service) {
        this.service = service;
    }

    @Operation(summary = "Lista abastecimentos da moto, paginado (padrão: 20 por página, mais recentes primeiro). `from`/`to` filtram pela data (inclusivos).")
    @GetMapping
    public Page<RefuelingDTO> listar(@PathVariable final UUID motoId,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
                                     @PageableDefault(size = 20, sort = {"date", "odometerKm"}, direction = Sort.Direction.DESC) final Pageable pageable,
                                     final Authentication authentication) {
        return service.listar(motoId, authentication.getName(), from, to, pageable);
    }

    @Operation(summary = "Registra um abastecimento. 400 se o hodômetro quebrar a ordem cronológica da moto")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RefuelingDTO criar(@PathVariable final UUID motoId, @Valid @RequestBody final RefuelingRequestDTO request,
                              final Authentication authentication) {
        return service.criar(motoId, request, authentication.getName());
    }

    @Operation(summary = "Atualiza um abastecimento")
    @PutMapping("/{id}")
    public RefuelingDTO atualizar(@PathVariable final UUID motoId, @PathVariable final UUID id,
                                  @Valid @RequestBody final RefuelingRequestDTO request, final Authentication authentication) {
        return service.atualizar(motoId, id, request, authentication.getName());
    }

    @Operation(summary = "Exclui um abastecimento")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable final UUID motoId, @PathVariable final UUID id, final Authentication authentication) {
        service.excluir(motoId, id, authentication.getName());
    }
}
