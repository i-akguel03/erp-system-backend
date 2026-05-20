package com.erp.backend.repository;

import com.erp.backend.domain.Konto;
import com.erp.backend.domain.KontoTyp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KontoRepository extends JpaRepository<Konto, Long> {

    List<Konto> findByKontoTyp(KontoTyp kontoTyp);

    List<Konto> findByAktivTrue();

    List<Konto> findByKontoKlasse(String kontoKlasse);

    boolean existsByKontonummer(Long kontonummer);
}