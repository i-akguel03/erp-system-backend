package com.erp.backend.repository;

import com.erp.backend.domain.Angebot;
import com.erp.backend.domain.AngebotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AngebotRepository extends JpaRepository<Angebot, UUID> {

    Optional<Angebot> findByAngebotsnummer(String angebotsnummer);

    List<Angebot> findByCustomerId(UUID customerId);

    List<Angebot> findByStatus(AngebotStatus status);

    List<Angebot> findByGueltigBisBefore(LocalDate datum);

    boolean existsByAngebotsnummer(String angebotsnummer);
}