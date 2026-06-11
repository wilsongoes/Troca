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
     * ela emerge das trocas. Por objeto:
     *   saldo        = efetivadas que entraram - efetivadas que saíram
     *   comprometido = reservadas que vão sair
     *   aReceber     = reservadas que vão entrar
     */
    @Query("""
            select t.objeto.id, t.objeto.nome, t.objeto.tipo,
                   sum(case when t.status = 'EFETIVADA'
                            then (case when t.para.id = :entidadeId then t.quantidade else -t.quantidade end)
                            else 0 end),
                   sum(case when t.status = 'RESERVADA' and t.de.id = :entidadeId then t.quantidade else 0 end),
                   sum(case when t.status = 'RESERVADA' and t.para.id = :entidadeId then t.quantidade else 0 end)
            from Troca t
            where t.de.id = :entidadeId or t.para.id = :entidadeId
            group by t.objeto.id, t.objeto.nome, t.objeto.tipo
            order by t.objeto.nome
            """)
    List<Object[]> posicaoDaEntidade(@Param("entidadeId") Long entidadeId);

    /** Delta da posição: só as trocas após o corte do snapshot. */
    @Query("""
            select t.objeto.id, t.objeto.nome, t.objeto.tipo,
                   sum(case when t.status = 'EFETIVADA'
                            then (case when t.para.id = :entidadeId then t.quantidade else -t.quantidade end)
                            else 0 end),
                   sum(case when t.status = 'RESERVADA' and t.de.id = :entidadeId then t.quantidade else 0 end),
                   sum(case when t.status = 'RESERVADA' and t.para.id = :entidadeId then t.quantidade else 0 end)
            from Troca t
            where (t.de.id = :entidadeId or t.para.id = :entidadeId) and t.id > :corte
            group by t.objeto.id, t.objeto.nome, t.objeto.tipo
            """)
    List<Object[]> posicaoDaEntidadeDesde(@Param("entidadeId") Long entidadeId,
                                          @Param("corte") Long corte);

    @Query("select max(t.id) from Troca t")
    Long maxTrocaId();

    boolean existsByParaIdAndObjetoIdAndStatus(Long paraId, Long objetoId, String status);

    /**
     * Lock de aconselhamento por entidade, liberado no fim da transação.
     * Serializa quem valida/consome o estoque da mesma entidade.
     */
    @Query(value = "select pg_advisory_xact_lock(:chave) is null", nativeQuery = true)
    Boolean travarEntidade(@Param("chave") long chave);
}
