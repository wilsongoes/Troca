package com.troca.repositorio;

import com.troca.dominio.Entidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntidadeRepository extends JpaRepository<Entidade, Long> {

    List<Entidade> findByAtivoTrueOrderByNome();

    List<Entidade> findByTipoIgnoreCaseAndAtivoTrueOrderByNome(String tipo);

    java.util.Optional<Entidade> findFirstByNomeIgnoreCaseAndTipoIgnoreCaseAndAtivoTrue(String nome, String tipo);
}
