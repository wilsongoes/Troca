package com.troca.servico;

import com.troca.dominio.PosicaoSnapshot;
import com.troca.repositorio.PosicaoSnapshotRepository;
import com.troca.repositorio.TrocaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Leitura de posição com snapshot + delta (a resposta dos bancos):
 * posição = projeção consolidada até o corte + agregação só das trocas
 * posteriores. Sem snapshot, cai na agregação completa — mesmo resultado,
 * só mais lento. A projeção é descartável e reconstruível por definição.
 */
@Service
public class PosicaoService {

    public record Posicao(Long objetoId, String objetoNome, String objetoTipo,
                          BigDecimal saldo, BigDecimal comprometido, BigDecimal aReceber,
                          BigDecimal disponivel) {}

    private static final class Acumulador {
        String nome, tipo;
        BigDecimal saldo = BigDecimal.ZERO;
        BigDecimal comprometido = BigDecimal.ZERO;
        BigDecimal aReceber = BigDecimal.ZERO;
    }

    private final TrocaRepository trocas;
    private final PosicaoSnapshotRepository snapshots;

    public PosicaoService(TrocaRepository trocas, PosicaoSnapshotRepository snapshots) {
        this.trocas = trocas;
        this.snapshots = snapshots;
    }

    @Transactional(readOnly = true)
    public List<Posicao> posicao(Long entidadeId) {
        Map<Long, Acumulador> acc = new HashMap<>();
        long corte = 0;

        for (PosicaoSnapshot s : snapshots.findByEntidadeId(entidadeId)) {
            corte = s.getCorte();
            Acumulador a = acc.computeIfAbsent(s.getObjetoId(), k -> new Acumulador());
            a.nome = s.getObjetoNome();
            a.tipo = s.getObjetoTipo();
            a.saldo = s.getSaldo();
            a.comprometido = s.getComprometido();
            a.aReceber = s.getAReceber();
        }

        for (Object[] r : trocas.posicaoDaEntidadeDesde(entidadeId, corte)) {
            Acumulador a = acc.computeIfAbsent((Long) r[0], k -> new Acumulador());
            a.nome = (String) r[1];
            a.tipo = (String) r[2];
            a.saldo = a.saldo.add((BigDecimal) r[3]);
            a.comprometido = a.comprometido.add((BigDecimal) r[4]);
            a.aReceber = a.aReceber.add((BigDecimal) r[5]);
        }

        return acc.entrySet().stream()
                .map(e -> new Posicao(e.getKey(), e.getValue().nome, e.getValue().tipo,
                        e.getValue().saldo, e.getValue().comprometido, e.getValue().aReceber,
                        e.getValue().saldo.subtract(e.getValue().comprometido)))
                .sorted(Comparator.comparing(Posicao::objetoNome,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    /** Disponível (saldo − comprometido) de um objeto específico. */
    @Transactional(readOnly = true)
    public BigDecimal disponivel(Long entidadeId, Long objetoId) {
        return posicao(entidadeId).stream()
                .filter(p -> p.objetoId().equals(objetoId))
                .map(Posicao::disponivel)
                .findFirst().orElse(BigDecimal.ZERO);
    }

    /** Consolida a posição atual como projeção, com corte no max(id) das trocas. */
    @Transactional
    public List<Posicao> consolidar(Long entidadeId) {
        Long max = trocas.maxTrocaId();
        long corte = max == null ? 0 : max;

        snapshots.deleteByEntidadeId(entidadeId);
        for (Object[] r : trocas.posicaoDaEntidade(entidadeId)) {
            PosicaoSnapshot s = new PosicaoSnapshot();
            s.setEntidadeId(entidadeId);
            s.setObjetoId((Long) r[0]);
            s.setObjetoNome((String) r[1]);
            s.setObjetoTipo((String) r[2]);
            s.setSaldo((BigDecimal) r[3]);
            s.setComprometido((BigDecimal) r[4]);
            s.setAReceber((BigDecimal) r[5]);
            s.setCorte(corte);
            s.setAtualizadoEm(Instant.now());
            snapshots.save(s);
        }
        return posicao(entidadeId);
    }

    /**
     * Trocas novas são pegas pelo delta; só a MUTAÇÃO de trocas antigas
     * (efetivar/cancelar reserva) invalida a projeção.
     */
    @Transactional
    public void invalidar(Collection<Long> entidadeIds) {
        if (!entidadeIds.isEmpty()) {
            snapshots.deleteByEntidadeIdIn(entidadeIds);
        }
    }
}
