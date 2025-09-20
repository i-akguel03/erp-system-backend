
// ===============================================================================================
// VORGANG REPOSITORY
// ===============================================================================================

package com.erp.backend.repository;

import com.erp.backend.domain.Vorgang;
import com.erp.backend.domain.VorgangStatus;
import com.erp.backend.domain.VorgangTyp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VorgangRepository extends JpaRepository<Vorgang, UUID> {

    // ================= BASIS-ABFRAGEN =================

    Optional<Vorgang> findByVorgangsnummer(String vorgangsnummer);

    List<Vorgang> findByTyp(VorgangTyp typ);

    List<Vorgang> findByStatus(VorgangStatus status);

    List<Vorgang> findByTypAndStatus(VorgangTyp typ, VorgangStatus status);

    List<Vorgang> findByAutomatisch(boolean automatisch);

    // ================= ZEITBASIERTE ABFRAGEN =================

    List<Vorgang> findByStartZeitpunktBetween(LocalDateTime start, LocalDateTime ende);

    List<Vorgang> findByStartZeitpunktAfter(LocalDateTime zeitpunkt);

    List<Vorgang> findByEndeZeitpunktBetween(LocalDateTime start, LocalDateTime ende);

    // ================= ERWEITERTE ABFRAGEN =================

    @Query("SELECT v FROM Vorgang v WHERE v.typ = :typ AND v.startZeitpunkt >= :seit ORDER BY v.startZeitpunkt DESC")
    List<Vorgang> findRecentByTyp(@Param("typ") VorgangTyp typ, @Param("seit") LocalDateTime seit);

    @Query("SELECT v FROM Vorgang v WHERE v.status IN :statusList ORDER BY v.startZeitpunkt DESC")
    List<Vorgang> findByStatusIn(@Param("statusList") List<VorgangStatus> statusList);

    @Query("SELECT v FROM Vorgang v WHERE v.ausgeloestVon = :benutzer ORDER BY v.startZeitpunkt DESC")
    List<Vorgang> findByBenutzer(@Param("benutzer") String benutzer);

    // ================= STATISTIKEN =================

    long countByStatus(VorgangStatus status);

    long countByTyp(VorgangTyp typ);

    @Query("SELECT COUNT(v) FROM Vorgang v WHERE v.status = :status AND v.startZeitpunkt >= :seit")
    long countByStatusSince(@Param("status") VorgangStatus status, @Param("seit") LocalDateTime seit);

    @Query("SELECT v.typ, COUNT(v) FROM Vorgang v GROUP BY v.typ ORDER BY COUNT(v) DESC")
    List<Object[]> getTypStatistiken();

    @Query("SELECT v.status, COUNT(v) FROM Vorgang v GROUP BY v.status")
    List<Object[]> getStatusStatistiken();

    // ================= RECHNUNGSLAUF-SPEZIFISCHE ABFRAGEN =================

    @Query("SELECT v FROM Vorgang v WHERE v.typ = 'RECHNUNGSLAUF' ORDER BY v.startZeitpunkt DESC")
    List<Vorgang> findAllRechnungslaeufe();

    @Query("SELECT v FROM Vorgang v WHERE v.typ = 'RECHNUNGSLAUF' AND v.status = 'ERFOLGREICH' ORDER BY v.startZeitpunkt DESC")
    List<Vorgang> findErfolgreicheRechnungslaeufe();

    @Query("SELECT v FROM Vorgang v WHERE v.typ = 'RECHNUNGSLAUF' AND v.startZeitpunkt >= :seit ORDER BY v.startZeitpunkt DESC")
    List<Vorgang> findRechnungslaeufeSeite(@Param("seit") LocalDateTime seit);

    // ================= PERFORMANCE UND MONITORING =================

    @Query("SELECT v FROM Vorgang v WHERE v.status = 'LAUFEND' AND v.startZeitpunkt < :zeitpunkt")
    List<Vorgang> findLanglaufendeVorgaenge(@Param("zeitpunkt") LocalDateTime zeitpunkt);

//    @Query("SELECT AVG(EXTRACT(EPOCH FROM (v.endeZeitpunkt - v.startZeitpunkt))) FROM Vorgang v WHERE v.typ = :typ AND v.status = 'ERFOLGREICH'")
//    Double getAverageDurationByTyp(@Param("typ") VorgangTyp typ);

    // ================= PAGEABLE ABFRAGEN =================

    Page<Vorgang> findByTypOrderByStartZeitpunktDesc(VorgangTyp typ, Pageable pageable);

    Page<Vorgang> findByStatusOrderByStartZeitpunktDesc(VorgangStatus status, Pageable pageable);

    Page<Vorgang> findAllByOrderByStartZeitpunktDesc(Pageable pageable);
}