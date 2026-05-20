package com.erp.backend.repository;

import com.erp.backend.domain.BelegTyp;
import com.erp.backend.domain.BuchungStatus;
import com.erp.backend.domain.Buchungssatz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuchungssatzRepository extends JpaRepository<Buchungssatz, UUID> {

    Optional<Buchungssatz> findByBuchungsnummer(String buchungsnummer);

    List<Buchungssatz> findByGeschaeftsjahr(int geschaeftsjahr);

    List<Buchungssatz> findByGeschaeftsjahrAndMonat(int geschaeftsjahr, int monat);

    List<Buchungssatz> findByBelegTyp(BelegTyp belegTyp);

    List<Buchungssatz> findByBelegReferenzId(String belegReferenzId);

    List<Buchungssatz> findByStatus(BuchungStatus status);

    List<Buchungssatz> findByBuchungsDatumBetween(LocalDate von, LocalDate bis);

    boolean existsByBuchungsnummer(String buchungsnummer);

    @Query("SELECT COALESCE(SUM(p.betrag), 0) FROM Buchungsposition p " +
           "WHERE p.konto.kontonummer = :kontonummer AND p.buchungsTyp = 'SOLL' " +
           "AND p.buchungssatz.geschaeftsjahr = :jahr AND p.buchungssatz.status = 'GEBUCHT'")
    BigDecimal sumSollByKontoAndJahr(@Param("kontonummer") Long kontonummer, @Param("jahr") int jahr);

    @Query("SELECT COALESCE(SUM(p.betrag), 0) FROM Buchungsposition p " +
           "WHERE p.konto.kontonummer = :kontonummer AND p.buchungsTyp = 'HABEN' " +
           "AND p.buchungssatz.geschaeftsjahr = :jahr AND p.buchungssatz.status = 'GEBUCHT'")
    BigDecimal sumHabenByKontoAndJahr(@Param("kontonummer") Long kontonummer, @Param("jahr") int jahr);

    @Query("SELECT b FROM Buchungssatz b JOIN b.positionen p " +
           "WHERE p.konto.kontonummer = :kontonummer AND b.status = 'GEBUCHT' " +
           "ORDER BY b.buchungsDatum DESC")
    List<Buchungssatz> findByKonto(@Param("kontonummer") Long kontonummer);
}