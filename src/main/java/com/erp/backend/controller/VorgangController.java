// ===============================================================================================
// CONTROLLER FÜR VORGÄNGE
// ===============================================================================================

package com.erp.backend.controller;

import com.erp.backend.domain.Vorgang;
import com.erp.backend.domain.VorgangStatus;
import com.erp.backend.domain.VorgangTyp;
import com.erp.backend.service.VorgangService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vorgaenge")
public class VorgangController {

    private final VorgangService vorgangService;

    public VorgangController(VorgangService vorgangService) {
        this.vorgangService = vorgangService;
    }

    /**
     * Alle Vorgänge paginiert
     */
    @GetMapping
    public ResponseEntity<Page<Vorgang>> getAllVorgaenge(Pageable pageable) {
        Page<Vorgang> vorgaenge = vorgangService.findAllePaginated(pageable);
        return ResponseEntity.ok(vorgaenge);
    }

    /**
     * Einzelnen Vorgang abrufen
     */
    @GetMapping("/{id}")
    public ResponseEntity<Vorgang> getVorgang(@PathVariable UUID id) {
        return vorgangService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Vorgänge nach Typ
     */
    @GetMapping("/typ/{typ}")
    public ResponseEntity<List<Vorgang>> getVorgaengeByTyp(@PathVariable VorgangTyp typ) {
        List<Vorgang> vorgaenge = vorgangService.findByTyp(typ);
        return ResponseEntity.ok(vorgaenge);
    }

    /**
     * Vorgänge nach Status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Vorgang>> getVorgaengeByStatus(@PathVariable VorgangStatus status) {
        List<Vorgang> vorgaenge = vorgangService.findByStatus(status);
        return ResponseEntity.ok(vorgaenge);
    }

    /**
     * Aktuell laufende Vorgänge
     */
    @GetMapping("/laufend")
    public ResponseEntity<List<Vorgang>> getLaufendeVorgaenge() {
        List<Vorgang> laufende = vorgangService.findLaufendeVorgaenge();
        return ResponseEntity.ok(laufende);
    }

    /**
     * Rechnungsläufe der letzten X Tage
     */
    @GetMapping("/rechnungslaeufe")
    public ResponseEntity<List<Vorgang>> getRecentRechnungslaeufe(@RequestParam(defaultValue = "30") int tage) {
        List<Vorgang> rechnungslaeufe = vorgangService.findRecentRechnungslaeufe(tage);
        return ResponseEntity.ok(rechnungslaeufe);
    }

    /**
     * Vorgang-Statistiken
     */
    @GetMapping("/statistiken")
    public ResponseEntity<VorgangService.VorgangStatistik> getVorgangStatistiken() {
        VorgangService.VorgangStatistik stats = vorgangService.getVorgangStatistik();
        return ResponseEntity.ok(stats);
    }

    /**
     * Langlaufende Vorgänge (mehr als X Minuten)
     */
    @GetMapping("/langlaufend")
    public ResponseEntity<List<Vorgang>> getLanglaufendeVorgaenge(@RequestParam(defaultValue = "60") int minuten) {
        List<Vorgang> langlaufende = vorgangService.findLanglaufendeVorgaenge(minuten);
        return ResponseEntity.ok(langlaufende);
    }

    /**
     * Vorgang manuell abbrechen
     */
    @PostMapping("/{id}/abbrechen")
    public ResponseEntity<String> vorgangAbbrechen(@PathVariable UUID id, @RequestParam String grund) {
        try {
            vorgangService.vorgangAbbrechen(id, grund);
            return ResponseEntity.ok("Vorgang erfolgreich abgebrochen");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Fehler beim Abbrechen: " + e.getMessage());
        }
    }

    /**
     * Alte Vorgänge bereinigen
     */
    @DeleteMapping("/bereinigen")
    public ResponseEntity<String> bereinigeAlteVorgaenge(
            @RequestParam(defaultValue = "90") int tageAlt,
            @RequestParam(defaultValue = "true") boolean nurErfolgreiche) {

        int geloescht = 0; //vorgangService.bereinigeAlteVorgaenge(tageAlt, nurErfolgreiche);
        return ResponseEntity.ok(String.format("✓ %d alte Vorgänge gelöscht (älter als %d Tage)", geloescht, tageAlt));
    }

    /**
     * Hängengebliebene Vorgänge korrigieren
     */
    @PostMapping("/korrigieren")
    public ResponseEntity<String> korrigiereHaengengebliebene(@RequestParam(defaultValue = "24") int stundenSchwellwert) {
        int korrigiert = vorgangService.korrigiereHaengengebliebeneVorgaenge(stundenSchwellwert);
        return ResponseEntity.ok(String.format("✓ %d hängengebliebene Vorgänge korrigiert", korrigiert));
    }
}