package com.troca.servico;

import com.troca.dominio.Entidade;
import com.troca.dominio.Estrutura;
import com.troca.repositorio.EntidadeRepository;
import com.troca.repositorio.EstruturaRepository;
import com.troca.repositorio.TrocaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Planejamento de compras (MRP): não cria conceito novo — é pura leitura.
 * Explode a Estrutura recursivamente, abate do estoque disponível
 * (derivado das trocas) e o que sobra é o que falta comprar ou contratar.
 */
@Service
public class PlanejamentoService {

    public record ItemPlano(Long objetoId, String nome, String tipo,
                            BigDecimal necessario, BigDecimal doEstoque,
                            BigDecimal aProduzir, BigDecimal comprar) {}

    private static final class Acumulador {
        Entidade objeto;
        BigDecimal necessario = BigDecimal.ZERO;
        BigDecimal doEstoque = BigDecimal.ZERO;
        BigDecimal aProduzir = BigDecimal.ZERO;
        BigDecimal comprar = BigDecimal.ZERO;
    }

    private final EstruturaRepository estruturas;
    private final TrocaRepository trocas;
    private final EntidadeRepository entidades;
    private final PosicaoService posicoes;

    public PlanejamentoService(EstruturaRepository estruturas, TrocaRepository trocas,
                               EntidadeRepository entidades, PosicaoService posicoes) {
        this.estruturas = estruturas;
        this.trocas = trocas;
        this.entidades = entidades;
        this.posicoes = posicoes;
    }

    @Transactional(readOnly = true)
    public List<ItemPlano> planejar(Long produtoId, BigDecimal quantidade, Long produtorId) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva.");
        }
        Entidade produto = entidades.findById(produtoId).orElseThrow(() ->
                new IllegalArgumentException("Produto não encontrado: " + produtoId));
        if (estruturas.findByPaiId(produtoId).isEmpty()) {
            throw new IllegalArgumentException(
                    "'" + produto.getNome() + "' não tem Estrutura — nada para planejar.");
        }

        // Estoque disponível do produtor (saldo - comprometido), mutável:
        // conforme o plano aloca, o disponível vai sendo consumido.
        Map<Long, BigDecimal> estoque = posicoes.posicao(produtorId).stream()
                .collect(Collectors.toMap(PosicaoService.Posicao::objetoId,
                        PosicaoService.Posicao::disponivel));

        Map<Long, Acumulador> plano = new LinkedHashMap<>();
        explodir(produtoId, quantidade, estoque, plano, new LinkedHashSet<>());

        return plano.values().stream()
                .map(a -> new ItemPlano(a.objeto.getId(), a.objeto.getNome(), a.objeto.getTipo(),
                        a.necessario, a.doEstoque, a.aProduzir, a.comprar))
                .toList();
    }

    private void explodir(Long produtoId, BigDecimal quantidade,
                          Map<Long, BigDecimal> estoque, Map<Long, Acumulador> plano,
                          Set<Long> caminho) {
        if (!caminho.add(produtoId)) {
            throw new IllegalArgumentException(
                    "Ciclo na Estrutura: o produto " + produtoId + " depende de si mesmo.");
        }
        for (Estrutura componente : estruturas.findByPaiId(produtoId)) {
            Entidade filho = componente.getFilho();
            BigDecimal necessario = componente.getQuantidade().multiply(quantidade);

            Acumulador acc = plano.computeIfAbsent(filho.getId(), k -> new Acumulador());
            acc.objeto = filho;
            acc.necessario = acc.necessario.add(necessario);

            BigDecimal disponivel = estoque.getOrDefault(filho.getId(), BigDecimal.ZERO)
                    .max(BigDecimal.ZERO);
            BigDecimal usado = disponivel.min(necessario);
            estoque.put(filho.getId(), disponivel.subtract(usado));
            acc.doEstoque = acc.doEstoque.add(usado);

            BigDecimal falta = necessario.subtract(usado);
            if (falta.signum() > 0) {
                if (estruturas.findByPaiId(filho.getId()).isEmpty()) {
                    acc.comprar = acc.comprar.add(falta);
                } else {
                    acc.aProduzir = acc.aProduzir.add(falta);
                    explodir(filho.getId(), falta, estoque, plano, caminho);
                }
            }
        }
        caminho.remove(produtoId);
    }
}
