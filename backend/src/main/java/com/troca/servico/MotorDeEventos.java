package com.troca.servico;

import com.troca.dominio.Entidade;
import com.troca.dominio.Troca;
import com.troca.repositorio.EntidadeRepository;
import com.troca.repositorio.TrocaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A reverberação: eventos que disparam eventos. As regras são DADOS —
 * Entidades de tipo REGRA cujos atributos descrevem o gatilho e a ação:
 *
 *   { "tipoTroca": "VENDA", "objetoId": 3, "minimo": 5,
 *     "reporQuantidade": 10, "fornecedorId": 7, "tipoAcao": "COMPRA" }
 *
 * Quando uma troca EFETIVADA casa com o gatilho e o disponível de quem
 * entregou cai abaixo do mínimo, nasce uma Reserva de reposição com o
 * fornecedor. A reserva criada NÃO dispara regras (só efetivadas disparam)
 * — o guarda natural contra reação em cadeia infinita.
 */
@Service
public class MotorDeEventos {

    private final EntidadeRepository entidades;
    private final TrocaRepository trocas;
    private final PosicaoService posicoes;

    public MotorDeEventos(EntidadeRepository entidades, TrocaRepository trocas,
                          PosicaoService posicoes) {
        this.entidades = entidades;
        this.trocas = trocas;
        this.posicoes = posicoes;
    }

    public List<Troca> reagir(List<Troca> efetivadas) {
        List<Entidade> regras = entidades.findByTipoIgnoreCaseAndAtivoTrueOrderByNome("REGRA");
        if (regras.isEmpty()) {
            return List.of();
        }
        return efetivadas.stream()
                .flatMap(t -> regras.stream()
                        .map(regra -> aplicar(regra, t))
                        .filter(java.util.Objects::nonNull))
                .toList();
    }

    private Troca aplicar(Entidade regra, Troca troca) {
        Map<String, Object> a = regra.getAtributos();
        if (a == null || a.isEmpty()) {
            return null;
        }
        if (!troca.getTipo().equalsIgnoreCase(String.valueOf(a.get("tipoTroca")))) {
            return null;
        }
        long objetoId = numero(a.get("objetoId")).longValue();
        if (!troca.getObjeto().getId().equals(objetoId)) {
            return null;
        }

        Long entregadorId = troca.getDe().getId();
        BigDecimal disponivel = posicoes.disponivel(entregadorId, objetoId);
        BigDecimal minimo = numero(a.get("minimo"));
        if (disponivel.compareTo(minimo) >= 0) {
            return null;
        }
        // Já existe reposição pendente? Não duplica o pedido.
        if (trocas.existsByParaIdAndObjetoIdAndStatus(entregadorId, objetoId, "RESERVADA")) {
            return null;
        }
        Entidade fornecedor = entidades.findById(numero(a.get("fornecedorId")).longValue())
                .orElse(null);
        if (fornecedor == null) {
            return null;
        }

        Troca pedido = new Troca();
        pedido.setGrupoId(UUID.randomUUID());
        pedido.setDe(fornecedor);
        pedido.setPara(troca.getDe());
        pedido.setObjeto(troca.getObjeto());
        pedido.setQuantidade(numero(a.get("reporQuantidade")));
        pedido.setTipo(String.valueOf(a.getOrDefault("tipoAcao", "COMPRA")).toUpperCase());
        pedido.setStatus("RESERVADA");
        pedido.setDescricao("Disparado pela regra '" + regra.getNome()
                + "': disponível " + disponivel.stripTrailingZeros().toPlainString()
                + " < mínimo " + minimo.stripTrailingZeros().toPlainString());
        pedido.setData(Instant.now());
        return trocas.save(pedido);
    }

    private BigDecimal numero(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(o));
    }
}
