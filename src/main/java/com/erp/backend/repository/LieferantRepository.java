package com.erp.backend.repository;

import com.erp.backend.domain.Lieferant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LieferantRepository extends JpaRepository<Lieferant, UUID> {
    Optional<Lieferant> findByLieferantennummer(String lieferantennummer);
    Optional<Lieferant> findByEmail(String email);
    List<Lieferant> findByAktivTrue();
    boolean existsByLieferantennummer(String lieferantennummer);
}