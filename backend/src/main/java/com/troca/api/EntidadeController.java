package com.troca.api;

import com.troca.dominio.Entidade;
import com.troca.repositorio.EntidadeRepository;
import com.troca.servico.PosicaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/entidades")
public class EntidadeController {

    public record EntidadeRequest(@NotBlank String nome, @NotBlank String tipo,
                                  String descricao, Map<String, Object> atributos) {}

    private final EntidadeRepository entidades;
    private final PosicaoService posicoes;

    public EntidadeController(EntidadeRepository entidades, PosicaoService posicoes) {
        this.entidades = entidades;
        this.posicoes = posicoes;
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
    public List<PosicaoService.Posicao> posicao(@PathVariable Long id) {
        buscar(id);
        return posicoes.posicao(id);
    }

    /** Consolida a posição como projeção (snapshot + delta nas próximas leituras). */
    @PostMapping("/{id}/consolidar")
    public List<PosicaoService.Posicao> consolidar(@PathVariable Long id) {
        buscar(id);
        return posicoes.consolidar(id);
    }

    private void aplicar(Entidade e, EntidadeRequest req) {
        validarPeloTipo(req);
        e.setNome(req.nome());
        e.setTipo(req.tipo().toUpperCase());
        e.setDescricao(req.descricao());
        if (req.atributos() != null) {
            e.setAtributos(req.atributos());
        }
    }

    /**
     * Meta-modelo: o Tipo é uma Entidade (tipo=TIPO) cujos atributos
     * declaram o que é obrigatório. A regra vive em DADOS, não em código.
     */
    private void validarPeloTipo(EntidadeRequest req) {
        entidades.findFirstByNomeIgnoreCaseAndTipoIgnoreCaseAndAtivoTrue(req.tipo(), "TIPO")
                .ifPresent(tipoDef -> {
                    Object obrigatorios = tipoDef.getAtributos() == null
                            ? null : tipoDef.getAtributos().get("obrigatorios");
                    if (obrigatorios instanceof List<?> campos) {
                        for (Object campo : campos) {
                            String chave = String.valueOf(campo);
                            Object valor = req.atributos() == null ? null : req.atributos().get(chave);
                            if (valor == null || String.valueOf(valor).isBlank()) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "O tipo " + tipoDef.getNome().toUpperCase()
                                                + " exige o atributo '" + chave + "'");
                            }
                        }
                    }
                });
    }
}
