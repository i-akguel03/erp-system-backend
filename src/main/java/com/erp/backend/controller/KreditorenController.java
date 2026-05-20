package com.erp.backend.controller;

import com.erp.backend.domain.Eingangsrechnung;
import com.erp.backend.domain.Lieferant;
import com.erp.backend.service.KreditorenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/kreditoren")
@CrossOrigin
@Tag(name = "Kreditorenbuchhaltung")
public class KreditorenController {

    private final KreditorenService kreditorenService;

    public KreditorenController(KreditorenService kreditorenService) {
        this.kreditorenService = kreditorenService;
    }

    // -------------------------------------------------------------------------
    // Lieferanten
    // -------------------------------------------------------------------------

    @Operation(summary = "Alle Lieferanten abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'KREDITOREN_READ')")
    @GetMapping("/lieferanten")
    public ResponseEntity<List<Lieferant>> getAllLieferanten() {
        return ResponseEntity.ok(kreditorenService.findAllLieferanten());
    }

    @Operation(summary = "Lieferant nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'KREDITOREN_READ')")
    @GetMapping("/lieferanten/{id}")
    public ResponseEntity<Lieferant> getLieferantById(@PathVariable UUID id) {
        return ResponseEntity.ok(kreditorenService.findLieferantById(id));
    }

    @Operation(summary = "Neuen Lieferant anlegen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/lieferanten")
    public ResponseEntity<Lieferant> lieferantAnlegen(@RequestBody Lieferant lieferant) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kreditorenService.lieferantAnlegen(lieferant));
    }

    @Operation(summary = "Lieferant aktualisieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/lieferanten/{id}")
    public ResponseEntity<Lieferant> lieferantAktualisieren(@PathVariable UUID id, @RequestBody Lieferant lieferant) {
        return ResponseEntity.ok(kreditorenService.lieferantAktualisieren(id, lieferant));
    }

    // -------------------------------------------------------------------------
    // Eingangsrechnungen
    // -------------------------------------------------------------------------

    @Operation(summary = "Alle Eingangsrechnungen abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'KREDITOREN_READ')")
    @GetMapping("/eingangsrechnungen")
    public ResponseEntity<List<Eingangsrechnung>> getAllEingangsrechnungen() {
        return ResponseEntity.ok(kreditorenService.findAllEingangsrechnungen());
    }

    @Operation(summary = "Eingangsrechnung nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'KREDITOREN_READ')")
    @GetMapping("/eingangsrechnungen/{id}")
    public ResponseEntity<Eingangsrechnung> getEingangsrechnungById(@PathVariable UUID id) {
        return ResponseEntity.ok(kreditorenService.findEingangsrechnungById(id));
    }

    @Operation(summary = "Überfällige Eingangsrechnungen abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'KREDITOREN_READ')")
    @GetMapping("/eingangsrechnungen/ueberfaellig")
    public ResponseEntity<List<Eingangsrechnung>> getUeberfaellig() {
        return ResponseEntity.ok(kreditorenService.findUeberfaellig());
    }

    @Operation(summary = "Neue Eingangsrechnung erfassen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/eingangsrechnungen")
    public ResponseEntity<Eingangsrechnung> erfassen(@RequestBody ErfassenRequest req) {
        Eingangsrechnung er = kreditorenService.erfassen(
                req.getLieferantId(), req.getLieferantenRechnungsNr(),
                req.getRechnungsDatum(), req.getFaelligDatum(),
                req.getNettobetrag(), req.getSteuersatz(),
                req.getAufwandskontoNr(), req.getNotizen()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(er);
    }

    @Operation(summary = "Eingangsrechnung freigeben (löst GL-Buchung aus)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/eingangsrechnungen/{id}/freigeben")
    public ResponseEntity<Eingangsrechnung> freigeben(@PathVariable UUID id) {
        return ResponseEntity.ok(kreditorenService.freigeben(id));
    }

    @Operation(summary = "Eingangsrechnung als bezahlt markieren (löst GL-Buchung aus)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/eingangsrechnungen/{id}/bezahlen")
    public ResponseEntity<Eingangsrechnung> bezahlen(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(kreditorenService.bezahlen(id, body.get("zahlungsreferenz")));
    }

    public static class ErfassenRequest {
        private UUID lieferantId;
        private String lieferantenRechnungsNr;
        private LocalDate rechnungsDatum;
        private LocalDate faelligDatum;
        private BigDecimal nettobetrag;
        private BigDecimal steuersatz;
        private Long aufwandskontoNr;
        private String notizen;

        public UUID getLieferantId() { return lieferantId; }
        public void setLieferantId(UUID lieferantId) { this.lieferantId = lieferantId; }
        public String getLieferantenRechnungsNr() { return lieferantenRechnungsNr; }
        public void setLieferantenRechnungsNr(String lieferantenRechnungsNr) { this.lieferantenRechnungsNr = lieferantenRechnungsNr; }
        public LocalDate getRechnungsDatum() { return rechnungsDatum; }
        public void setRechnungsDatum(LocalDate rechnungsDatum) { this.rechnungsDatum = rechnungsDatum; }
        public LocalDate getFaelligDatum() { return faelligDatum; }
        public void setFaelligDatum(LocalDate faelligDatum) { this.faelligDatum = faelligDatum; }
        public BigDecimal getNettobetrag() { return nettobetrag; }
        public void setNettobetrag(BigDecimal nettobetrag) { this.nettobetrag = nettobetrag; }
        public BigDecimal getSteuersatz() { return steuersatz; }
        public void setSteuersatz(BigDecimal steuersatz) { this.steuersatz = steuersatz; }
        public Long getAufwandskontoNr() { return aufwandskontoNr; }
        public void setAufwandskontoNr(Long aufwandskontoNr) { this.aufwandskontoNr = aufwandskontoNr; }
        public String getNotizen() { return notizen; }
        public void setNotizen(String notizen) { this.notizen = notizen; }
    }
}