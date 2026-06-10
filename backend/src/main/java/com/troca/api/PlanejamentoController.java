package com.troca.api;

import com.troca.servico.PlanejamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planejamentos")
public class PlanejamentoController {

    private final PlanejamentoService servico;

    public PlanejamentoController(PlanejamentoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<PlanejamentoService.ItemPlano> planejar(@RequestParam Long produtoId,
                                                        @RequestParam BigDecimal quantidade,
                                                        @RequestParam Long produtorId) {
        return servico.planejar(produtoId, quantidade, produtorId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> erroDeNegocio(IllegalArgumentException ex) {
        return Map.of("erro", ex.getMessage());
    }
}
