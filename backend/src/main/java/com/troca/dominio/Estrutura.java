package com.troca.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A composição: uma entidade é formada por outras entidades.
 * Carro -> Motor (1), Carro -> Roda (4). Projeto -> Tarefa.
 * O papel diz a natureza do vínculo (COMPONENTE, MEMBRO, RESPONSAVEL...).
 */
@Entity
@Table(name = "estrutura", uniqueConstraints = @UniqueConstraint(
        columnNames = {"pai_id", "filho_id", "papel"}))
public class Estrutura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pai_id", nullable = false)
    private Entidade pai;

    @ManyToOne(optional = false)
    @JoinColumn(name = "filho_id", nullable = false)
    private Entidade filho;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantidade = BigDecimal.ONE;

    @Column(nullable = false)
    private String papel = "COMPONENTE";

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    public Long getId() { return id; }
    public Entidade getPai() { return pai; }
    public void setPai(Entidade pai) { this.pai = pai; }
    public Entidade getFilho() { return filho; }
    public void setFilho(Entidade filho) { this.filho = filho; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
    public Instant getCriadoEm() { return criadoEm; }
}
