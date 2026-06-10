package com.troca.api;

import com.troca.dominio.Troca;
import com.troca.servico.ProducaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/producoes")
public class ProducaoController {

    public record ProducaoRequest(@NotNull Long produtoId, @NotNull BigDecimal quantidade,
                                  @NotNull Long produtorId, @NotNull Long transformadorId) {}

    private final ProducaoService servico;

    public ProducaoController(ProducaoService servico) {
        this.servico = servico;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<Troca> produzir(@Valid @RequestBody ProducaoRequest req) {
        return servico.produzir(req.produtoId(), req.quantidade(),
                req.produtorId(), req.transformadorId());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> erroDeNegocio(IllegalArgumentException ex) {
        return Map.of("erro", ex.getMessage());
    }
}
