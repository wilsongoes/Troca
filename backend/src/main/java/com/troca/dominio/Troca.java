package com.troca.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * O fluxo fundamental: uma entidade (objeto) sai de alguém (de) e vai
 * para alguém (para). Uma venda são duas pernas com o mesmo grupoId:
 *   Fábrica -> Cliente : TV (1)
 *   Cliente -> Fábrica : Real BRL (2500)
 * Dinheiro também é Entidade — não existe caso especial.
 */
@Entity
@Table(name = "troca", indexes = {
        @Index(name = "idx_troca_grupo", columnList = "grupoId"),
        @Index(name = "idx_troca_de", columnList = "de_id"),
        @Index(name = "idx_troca_para", columnList = "para_id")
})
public class Troca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Liga as pernas da mesma transação (a venda inteira). */
    @Column(nullable = false)
    private UUID grupoId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "de_id", nullable = false)
    private Entidade de;

    @ManyToOne(optional = false)
    @JoinColumn(name = "para_id", nullable = false)
    private Entidade para;

    @ManyToOne(optional = false)
    @JoinColumn(name = "objeto_id", nullable = false)
    private Entidade objeto;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantidade = BigDecimal.ONE;

    @Column(nullable = false)
    private String tipo;

    private String descricao;

    @Column(nullable = false)
    private Instant data = Instant.now();

    public Long getId() { return id; }
    public UUID getGrupoId() { return grupoId; }
    public void setGrupoId(UUID grupoId) { this.grupoId = grupoId; }
    public Entidade getDe() { return de; }
    public void setDe(Entidade de) { this.de = de; }
    public Entidade getPara() { return para; }
    public void setPara(Entidade para) { this.para = para; }
    public Entidade getObjeto() { return objeto; }
    public void setObjeto(Entidade objeto) { this.objeto = objeto; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Instant getData() { return data; }
    public void setData(Instant data) { this.data = data; }
}
