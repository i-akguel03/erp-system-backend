package com.erp.backend.repository;

import com.erp.backend.domain.Eingangsrechnung;
import com.erp.backend.domain.EingangsrechnungStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EingangsrechnungRepository extends JpaRepository<Eingangsrechnung, UUID> {
    List<Eingangsrechnung> findByLieferant_Id(UUID lieferantId);
    List<Eingangsrechnung> findByStatus(EingangsrechnungStatus status);
    List<Eingangsrechnung> findByFaelligDatumBefore(LocalDate datum);
    boolean existsByEingangsrechnungsnummer(String eingangsrechnungsnummer);

    @Query("SELECT e FROM Eingangsrechnung e WHERE e.status IN ('ERFASST','GEPRUEFT','FREIGEGEBEN') AND e.faelligDatum < :heute")
    List<Eingangsrechnung> findUeberfaellig(@Param("heute") LocalDate heute);
}