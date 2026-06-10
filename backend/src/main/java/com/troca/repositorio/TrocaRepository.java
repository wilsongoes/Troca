package com.troca.repositorio;

import com.troca.dominio.Troca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TrocaRepository extends JpaRepository<Troca, Long> {

    List<Troca> findByGrupoId(UUID grupoId);

    List<Troca> findTop100ByOrderByDataDescIdDesc();

    @Query("""
            select t from Troca t
            where t.de.id = :entidadeId or t.para.id = :entidadeId
            order by t.data desc, t.id desc
            """)
    List<Troca> historicoDaEntidade(@Param("entidadeId") Long entidadeId);

    /**
     * A posição (estoque/saldo) de uma entidade não é cadastrada:
     * ela emerge das trocas. entrou - saiu, por objeto.
     */
    @Query("""
            select t.objeto.id, t.objeto.nome, t.objeto.tipo,
                   sum(case when t.para.id = :entidadeId then t.quantidade else -t.quantidade end)
            from Troca t
            where t.de.id = :entidadeId or t.para.id = :entidadeId
            group by t.objeto.id, t.objeto.nome, t.objeto.tipo
            order by t.objeto.nome
            """)
    List<Object[]> posicaoDaEntidade(@Param("entidadeId") Long entidadeId);
}
