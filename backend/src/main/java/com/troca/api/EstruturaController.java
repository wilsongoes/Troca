package com.troca.api;

import com.troca.dominio.Estrutura;
import com.troca.repositorio.EntidadeRepository;
import com.troca.repositorio.EstruturaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/estruturas")
public class EstruturaController {

    public record EstruturaRequest(@NotNull Long paiId, @NotNull Long filhoId,
                                   BigDecimal quantidade, String papel) {}

    private final EstruturaRepository estruturas;
    private final EntidadeRepository entidades;

    public EstruturaController(EstruturaRepository estruturas, EntidadeRepository entidades) {
        this.estruturas = estruturas;
        this.entidades = entidades;
    }

    /** Do que o pai é formado. */
    @GetMapping
    public List<Estrutura> listar(@RequestParam Long paiId) {
        return estruturas.findByPaiId(paiId);
    }

    /** Onde essa entidade é usada como componente. */
    @GetMapping("/usos")
    public List<Estrutura> usos(@RequestParam Long filhoId) {
        return estruturas.findByFilhoId(filhoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Estrutura criar(@Valid @RequestBody EstruturaRequest req) {
        if (req.paiId().equals(req.filhoId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Uma entidade não pode ser componente de si mesma");
        }
        Estrutura s = new Estrutura();
        s.setPai(entidades.findById(req.paiId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade pai não encontrada")));
        s.setFilho(entidades.findById(req.filhoId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade filha não encontrada")));
        if (req.quantidade() != null) {
            s.setQuantidade(req.quantidade());
        }
        if (req.papel() != null && !req.papel().isBlank()) {
            s.setPapel(req.papel().toUpperCase());
        }
        return estruturas.save(s);
    }

    @DeleteMapping("/{id}")
    public void remover(@PathVariable Long id) {
        estruturas.deleteById(id);
    }
}
