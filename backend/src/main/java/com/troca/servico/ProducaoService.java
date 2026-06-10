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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Produção não é um conceito novo: é uma Troca com uma entidade
 * transformadora. O produtor entrega os componentes (segundo a Estrutura)
 * para o transformador, e o transformador devolve o produto pronto.
 * Tudo no mesmo grupo, atômico.
 */
@Service
public class ProducaoService {

    private final EstruturaRepository estruturas;
    private final TrocaRepository trocas;
    private final EntidadeRepository entidades;
    private final TrocaService trocaService;

    public ProducaoService(EstruturaRepository estruturas, TrocaRepository trocas,
                           EntidadeRepository entidades, TrocaService trocaService) {
        this.estruturas = estruturas;
        this.trocas = trocas;
        this.entidades = entidades;
        this.trocaService = trocaService;
    }

    @Transactional
    public List<Troca> produzir(Long produtoId, BigDecimal quantidade,
                                Long produtorId, Long transformadorId) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva.");
        }
        Entidade produto = entidades.findById(produtoId).orElseThrow(() ->
                new IllegalArgumentException("Produto não encontrado: " + produtoId));

        List<Estrutura> bom = estruturas.findByPaiId(produtoId);
        if (bom.isEmpty()) {
            throw new IllegalArgumentException(
                    "'" + produto.getNome() + "' não tem Estrutura cadastrada — nada para produzir.");
        }

        // Posição atual do produtor: o estoque emerge das trocas já feitas.
        Map<Long, BigDecimal> posicao = trocas.posicaoDaEntidade(produtorId).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (BigDecimal) r[3]));

        List<TrocaService.Perna> pernas = new ArrayList<>();
        for (Estrutura componente : bom) {
            BigDecimal necessario = componente.getQuantidade().multiply(quantidade);
            BigDecimal disponivel = posicao.getOrDefault(componente.getFilho().getId(), BigDecimal.ZERO);
            if (disponivel.compareTo(necessario) < 0) {
                throw new IllegalArgumentException("Estoque insuficiente de '"
                        + componente.getFilho().getNome() + "': precisa de " + necessario
                        + ", disponível " + disponivel);
            }
            pernas.add(new TrocaService.Perna(produtorId, transformadorId,
                    componente.getFilho().getId(), necessario));
        }
        pernas.add(new TrocaService.Perna(transformadorId, produtorId, produtoId, quantidade));

        return trocaService.registrar("PRODUCAO",
                "Produção de " + quantidade.stripTrailingZeros().toPlainString()
                        + "x " + produto.getNome(), pernas);
    }
}
