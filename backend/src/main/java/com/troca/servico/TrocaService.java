package com.troca.servico;

import com.troca.dominio.Entidade;
import com.troca.dominio.Troca;
import com.troca.repositorio.EntidadeRepository;
import com.troca.repositorio.TrocaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TrocaService {

    public record Perna(Long deId, Long paraId, Long objetoId, BigDecimal quantidade) {}

    private final TrocaRepository trocas;
    private final EntidadeRepository entidades;

    public TrocaService(TrocaRepository trocas, EntidadeRepository entidades) {
        this.trocas = trocas;
        this.entidades = entidades;
    }

    /**
     * Registra uma transação completa: todas as pernas entram juntas,
     * com o mesmo grupoId, ou nenhuma entra.
     */
    @Transactional
    public List<Troca> registrar(String tipo, String descricao, List<Perna> pernas) {
        return registrar(tipo, descricao, pernas, "EFETIVADA");
    }

    @Transactional
    public List<Troca> registrar(String tipo, String descricao, List<Perna> pernas, String status) {
        if (pernas == null || pernas.isEmpty()) {
            throw new IllegalArgumentException("Uma troca precisa de pelo menos uma perna.");
        }
        UUID grupo = UUID.randomUUID();
        Instant agora = Instant.now();

        List<Troca> salvas = pernas.stream().map(p -> {
            if (p.quantidade() == null || p.quantidade().signum() <= 0) {
                throw new IllegalArgumentException("Quantidade deve ser positiva.");
            }
            if (p.deId().equals(p.paraId())) {
                throw new IllegalArgumentException("Origem e destino devem ser diferentes.");
            }
            Troca t = new Troca();
            t.setGrupoId(grupo);
            t.setDe(buscar(p.deId(), "origem"));
            t.setPara(buscar(p.paraId(), "destino"));
            t.setObjeto(buscar(p.objetoId(), "objeto"));
            t.setQuantidade(p.quantidade());
            t.setTipo(tipo);
            t.setStatus(status);
            t.setDescricao(descricao);
            t.setData(agora);
            return t;
        }).toList();

        return trocas.saveAll(salvas);
    }

    /** A reserva vira realidade: todas as pernas do grupo passam a contar no saldo. */
    @Transactional
    public List<Troca> efetivarGrupo(UUID grupoId) {
        List<Troca> grupo = grupoReservado(grupoId);
        Instant agora = Instant.now();
        grupo.forEach(t -> {
            t.setStatus("EFETIVADA");
            t.setData(agora);
        });
        return trocas.saveAll(grupo);
    }

    /** Reserva pode ser desfeita — só o que foi efetivado é história imutável. */
    @Transactional
    public void cancelarGrupo(UUID grupoId) {
        trocas.deleteAll(grupoReservado(grupoId));
    }

    private List<Troca> grupoReservado(UUID grupoId) {
        List<Troca> grupo = trocas.findByGrupoId(grupoId);
        if (grupo.isEmpty()) {
            throw new IllegalArgumentException("Grupo não encontrado: " + grupoId);
        }
        if (grupo.stream().anyMatch(t -> !"RESERVADA".equals(t.getStatus()))) {
            throw new IllegalArgumentException("Grupo não está reservado — trocas efetivadas são imutáveis.");
        }
        return grupo;
    }

    private Entidade buscar(Long id, String papel) {
        return entidades.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Entidade de " + papel + " não encontrada: " + id));
    }
}
