package com.troca.api;

import com.troca.dominio.Entidade;
import com.troca.repositorio.EntidadeRepository;
import com.troca.repositorio.EstruturaRepository;
import com.troca.repositorio.TrocaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/entidades")
public class EntidadeController {

    public record EntidadeRequest(@NotBlank String nome, @NotBlank String tipo,
                                  String descricao, Map<String, Object> atributos) {}

    public record PosicaoItem(Long objetoId, String objetoNome, String objetoTipo, BigDecimal saldo) {}

    private final EntidadeRepository entidades;
    private final EstruturaRepository estruturas;
    private final TrocaRepository trocas;

    public EntidadeController(EntidadeRepository entidades, EstruturaRepository estruturas,
                              TrocaRepository trocas) {
        this.entidades = entidades;
        this.estruturas = estruturas;
        this.trocas = trocas;
    }

    @GetMapping
    public List<Entidade> listar(@RequestParam(required = false) String tipo) {
        return tipo == null
                ? entidades.findByAtivoTrueOrderByNome()
                : entidades.findByTipoIgnoreCaseAndAtivoTrueOrderByNome(tipo);
    }

    @GetMapping("/{id}")
    public Entidade buscar(@PathVariable Long id) {
        return entidades.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidade não encontrada"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Entidade criar(@Valid @RequestBody EntidadeRequest req) {
        Entidade e = new Entidade();
        aplicar(e, req);
        return entidades.save(e);
    }

    @PutMapping("/{id}")
    public Entidade atualizar(@PathVariable Long id, @Valid @RequestBody EntidadeRequest req) {
        Entidade e = buscar(id);
        aplicar(e, req);
        return entidades.save(e);
    }

    /** Exclusão lógica: a história das trocas é imutável. */
    @DeleteMapping("/{id}")
    public void desativar(@PathVariable Long id) {
        Entidade e = buscar(id);
        e.setAtivo(false);
        entidades.save(e);
    }

    /** Posição derivada das trocas: o que essa entidade tem agora. */
    @GetMapping("/{id}/posicao")
    public List<PosicaoItem> posicao(@PathVariable Long id) {
        buscar(id);
        return trocas.posicaoDaEntidade(id).stream()
                .map(r -> new PosicaoItem((Long) r[0], (String) r[1], (String) r[2], (BigDecimal) r[3]))
                .toList();
    }

    private void aplicar(Entidade e, EntidadeRequest req) {
        e.setNome(req.nome());
        e.setTipo(req.tipo().toUpperCase());
        e.setDescricao(req.descricao());
        if (req.atributos() != null) {
            e.setAtributos(req.atributos());
        }
    }
}
