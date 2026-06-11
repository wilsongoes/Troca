package com.troca.repositorio;

import com.troca.dominio.PosicaoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PosicaoSnapshotRepository extends JpaRepository<PosicaoSnapshot, Long> {

    List<PosicaoSnapshot> findByEntidadeId(Long entidadeId);

    void deleteByEntidadeId(Long entidadeId);

    void deleteByEntidadeIdIn(Collection<Long> entidadeIds);
}
