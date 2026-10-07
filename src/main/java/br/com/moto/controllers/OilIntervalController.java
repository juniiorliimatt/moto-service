package br.com.moto.controllers;

import br.com.moto.models.dto.OilIntervalDTO;
import br.com.moto.models.enums.OilType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Troca de óleo")
@RestController
@RequestMapping("/api/v1/oil-intervals")
public class OilIntervalController {

    @Operation(summary = "Intervalo de troca sugerido por tipo de óleo (padrão e faixa em km, e meses) — só pré-preenche o formulário")
    @GetMapping
    public List<OilIntervalDTO> listar() {
        return Arrays.stream(OilType.values()).map(OilIntervalDTO::de).toList();
    }
}
