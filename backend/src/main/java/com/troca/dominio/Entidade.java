package com.troca.dominio;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A "coisa". Qualquer entidade do mundo: pessoa, produto, carro, projeto,
 * tarefa, dinheiro. O tipo é livre — o significado vem da abstração, não
 * de uma tabela nova por conceito.
 */
@Entity
@Table(name = "entidade")
public class Entidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nome;

    @NotBlank
    @Column(nullable = false)
    private String tipo;

    private String descricao;

    /** Atributos livres por tipo (ex: cor, CPF, moeda) sem criar colunas novas. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> atributos = new HashMap<>();

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Map<String, Object> getAtributos() { return atributos; }
    public void setAtributos(Map<String, Object> atributos) { this.atributos = atributos; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCriadoEm() { return criadoEm; }
}
