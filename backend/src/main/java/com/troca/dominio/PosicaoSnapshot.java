package com.troca.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * PROJEÇÃO da posição, não um quarto conceito: é derivada das trocas e
 * 100% reconstruível. Guarda a posição consolidada até a troca de id
 * `corte`; a leitura soma só o delta posterior.
 */
@Entity
@Table(name = "posicao_snapshot",
        uniqueConstraints = @UniqueConstraint(columnNames = {"entidade_id", "objeto_id"}),
        indexes = @Index(name = "idx_snapshot_entidade", columnList = "entidade_id"))
public class PosicaoSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entidade_id", nullable = false)
    private Long entidadeId;

    @Column(name = "objeto_id", nullable = false)
    private Long objetoId;

    private String objetoNome;
    private String objetoTipo;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal comprometido = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal aReceber = BigDecimal.ZERO;

    @Column(nullable = false)
    private long corte;

    @Column(nullable = false)
    private Instant atualizadoEm = Instant.now();

    public Long getId() { return id; }
    public Long getEntidadeId() { return entidadeId; }
    public void setEntidadeId(Long entidadeId) { this.entidadeId = entidadeId; }
    public Long getObjetoId() { return objetoId; }
    public void setObjetoId(Long objetoId) { this.objetoId = objetoId; }
    public String getObjetoNome() { return objetoNome; }
    public void setObjetoNome(String objetoNome) { this.objetoNome = objetoNome; }
    public String getObjetoTipo() { return objetoTipo; }
    public void setObjetoTipo(String objetoTipo) { this.objetoTipo = objetoTipo; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public BigDecimal getComprometido() { return comprometido; }
    public void setComprometido(BigDecimal comprometido) { this.comprometido = comprometido; }
    public BigDecimal getAReceber() { return aReceber; }
    public void setAReceber(BigDecimal aReceber) { this.aReceber = aReceber; }
    public long getCorte() { return corte; }
    public void setCorte(long corte) { this.corte = corte; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(Instant atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
