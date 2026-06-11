package com.troca.servico;

import com.troca.dominio.Entidade;
import com.troca.dominio.Estrutura;
import com.troca.dominio.Troca;
import com.troca.repositorio.EntidadeRepository;
import com.troca.repositorio.EstruturaRepository;
import com.troca.repositorio.TrocaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Produção não é um conceito novo: é uma Troca com uma entidade
 * transformadora. O produtor entrega os componentes (segundo a Estrutura)
 * para o transformador, e o transformador devolve o produto pronto.
 *
 * Em cascata, um componente em falta que tenha Estrutura própria é
 * produzido primeiro (recursivamente) — cada nível vira um grupo de
 * trocas próprio, tudo na mesma transação.
 */
@Service
public class ProducaoService {

    private final EstruturaRepository estruturas;
    private final TrocaRepository trocas;
    private final EntidadeRepository entidades;
    private final TrocaService trocaService;
    private final PosicaoService posicoes;

    public ProducaoService(EstruturaRepository estruturas, TrocaRepository trocas,
                           EntidadeRepository entidades, TrocaService trocaService,
                           PosicaoService posicoes) {
        this.estruturas = estruturas;
        this.trocas = trocas;
        this.entidades = entidades;
        this.trocaService = trocaService;
        this.posicoes = posicoes;
    }

    @Transactional
    public List<Troca> produzir(Long produtoId, BigDecimal quantidade,
                                Long produtorId, Long transformadorId, boolean cascata) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva.");
        }
        // Serializa produções concorrentes do mesmo produtor: a validação de
        // estoque e o consumo acontecem sem corrida (lock até o fim da transação).
        trocas.travarEntidade(produtorId);
        List<Troca> acumulado = new ArrayList<>();
        produzirRecursivo(produtoId, quantidade, produtorId, transformadorId,
                cascata, new LinkedHashSet<>(), acumulado);
        return acumulado;
    }

    private void produzirRecursivo(Long produtoId, BigDecimal quantidade,
                                   Long produtorId, Long transformadorId,
                                   boolean cascata, Set<Long> caminho, List<Troca> acumulado) {
        if (!caminho.add(produtoId)) {
            throw new IllegalArgumentException(
                    "Ciclo na Estrutura: o produto " + produtoId + " depende de si mesmo.");
        }
        Entidade produto = entidades.findById(produtoId).orElseThrow(() ->
                new IllegalArgumentException("Produto não encontrado: " + produtoId));

        List<Estrutura> bom = estruturas.findByPaiId(produtoId);
        if (bom.isEmpty()) {
            throw new IllegalArgumentException(
                    "'" + produto.getNome() + "' não tem Estrutura cadastrada — nada para produzir.");
        }

        for (Estrutura componente : bom) {
            BigDecimal necessario = componente.getQuantidade().multiply(quantidade);
            BigDecimal disponivel = disponivelDe(produtorId, componente.getFilho().getId());

            if (disponivel.compareTo(necessario) < 0) {
                BigDecimal deficit = necessario.subtract(disponivel);
                boolean componentTemEstrutura = !estruturas.findByPaiId(componente.getFilho().getId()).isEmpty();
                if (cascata && componentTemEstrutura) {
                    produzirRecursivo(componente.getFilho().getId(), deficit,
                            produtorId, transformadorId, true, caminho, acumulado);
                } else {
                    throw new IllegalArgumentException("Estoque insuficiente de '"
                            + componente.getFilho().getNome() + "': precisa de " + necessario
                            + ", disponível " + disponivel
                            + (componentTemEstrutura ? " (use cascata para produzir o que falta)" : ""));
                }
            }
        }

        List<TrocaService.Perna> pernas = new ArrayList<>();
        for (Estrutura componente : bom) {
            pernas.add(new TrocaService.Perna(produtorId, transformadorId,
                    componente.getFilho().getId(), componente.getQuantidade().multiply(quantidade)));
        }
        pernas.add(new TrocaService.Perna(transformadorId, produtorId, produtoId, quantidade));

        acumulado.addAll(trocaService.registrar("PRODUCAO",
                "Produção de " + quantidade.stripTrailingZeros().toPlainString()
                        + "x " + produto.getNome(), pernas));
        caminho.remove(produtoId);
    }

    /** Disponível = saldo efetivado − comprometido em reservas. */
    private BigDecimal disponivelDe(Long entidadeId, Long objetoId) {
        return posicoes.disponivel(entidadeId, objetoId);
    }
}
