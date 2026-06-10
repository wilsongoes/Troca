package com.troca.repositorio;

import com.troca.dominio.Estrutura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstruturaRepository extends JpaRepository<Estrutura, Long> {

    List<Estrutura> findByPaiId(Long paiId);

    List<Estrutura> findByFilhoId(Long filhoId);
}
