package com.troca.api;

import com.troca.dominio.Troca;
import com.troca.repositorio.TrocaRepository;
import com.troca.servico.TrocaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trocas")
public class TrocaController {

    public record PernaRequest(@NotNull Long deId, @NotNull Long paraId,
                               @NotNull Long objetoId, @NotNull BigDecimal quantidade) {}

    public record TrocaRequest(@NotBlank String tipo, String descricao,
                               @NotEmpty List<PernaRequest> pernas, Boolean reservada) {}

    private final TrocaService servico;
    private final TrocaRepository trocas;

    public TrocaController(TrocaService servico, TrocaRepository trocas) {
        this.servico = servico;
        this.trocas = trocas;
    }

    @GetMapping
    public List<Troca> recentes(@RequestParam(required = false) Long entidadeId) {
        return entidadeId == null
                ? trocas.findTop100ByOrderByDataDescIdDesc()
                : trocas.historicoDaEntidade(entidadeId);
    }

    @GetMapping("/grupo/{grupoId}")
    public List<Troca> porGrupo(@PathVariable UUID grupoId) {
        return trocas.findByGrupoId(grupoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<Troca> registrar(@Valid @RequestBody TrocaRequest req) {
        List<TrocaService.Perna> pernas = req.pernas().stream()
                .map(p -> new TrocaService.Perna(p.deId(), p.paraId(), p.objetoId(), p.quantidade()))
                .toList();
        String status = Boolean.TRUE.equals(req.reservada()) ? "RESERVADA" : "EFETIVADA";
        return servico.registrar(req.tipo().toUpperCase(), req.descricao(), pernas, status);
    }

    @PostMapping("/grupo/{grupoId}/efetivar")
    public List<Troca> efetivar(@PathVariable UUID grupoId) {
        return servico.efetivarGrupo(grupoId);
    }

    @DeleteMapping("/grupo/{grupoId}")
    public void cancelar(@PathVariable UUID grupoId) {
        servico.cancelarGrupo(grupoId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public java.util.Map<String, String> erroDeNegocio(IllegalArgumentException ex) {
        return java.util.Map.of("erro", ex.getMessage());
    }
}
