package br.com.moto.controllers;

import br.com.moto.models.dto.OdometerReadingDTO;
import br.com.moto.models.dto.OdometerReadingRequestDTO;
import br.com.moto.services.OdometerReadingService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Leituras de hodômetro")
@RestController
@RequestMapping("/api/v1/motorcycles/{motoId}/odometer-readings")
public class OdometerReadingController {

    private final OdometerReadingService service;

    public OdometerReadingController(final OdometerReadingService service) {
        this.service = service;
    }

    @Operation(summary = "Lista as leituras avulsas de hodômetro da moto, em ordem cronológica")
    @GetMapping
    public List<OdometerReadingDTO> listar(@PathVariable final UUID motoId, final Authentication authentication) {
        return service.listar(motoId, authentication.getName());
    }

    @Operation(summary = "Registra um km avulso (sem abastecer). 400 se o hodômetro quebrar a ordem cronológica da moto")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OdometerReadingDTO criar(@PathVariable final UUID motoId, @Valid @RequestBody final OdometerReadingRequestDTO request,
                                    final Authentication authentication) {
        return service.criar(motoId, request, authentication.getName());
    }

    @Operation(summary = "Exclui uma leitura avulsa")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable final UUID motoId, @PathVariable final UUID id, final Authentication authentication) {
        service.excluir(motoId, id, authentication.getName());
    }
}
