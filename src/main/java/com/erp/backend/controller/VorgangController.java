package com.erp.backend.controller;

import com.erp.backend.domain.Vorgang;
import com.erp.backend.domain.VorgangStatus;
import com.erp.backend.domain.VorgangTyp;
import com.erp.backend.dto.VorgangDTO;
import com.erp.backend.mapper.VorgangMapper;
import com.erp.backend.service.VorgangService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vorgaenge")
public class VorgangController {

    private final VorgangService vorgangService;
    private final VorgangMapper vorgangMapper;

    public VorgangController(VorgangService vorgangService, VorgangMapper vorgangMapper) {
        this.vorgangService = vorgangService;
        this.vorgangMapper = vorgangMapper;
    }

    /**
     * Alle Vorgänge paginiert
     */
    @GetMapping
    public ResponseEntity<Page<VorgangDTO>> getAllVorgaenge(Pageable pageable) {
        Page<Vorgang> vorgaenge = vorgangService.findAllePaginated(pageable);
        Page<VorgangDTO> vorgaengeDTO = vorgaenge.map(vorgangMapper::toDTO);
        return ResponseEntity.ok(vorgaengeDTO);
    }

    @GetMapping("/all")
    public ResponseEntity<List<VorgangDTO>> getAllVorgaengeOhnePaging() {
        List<Vorgang> vorgaenge = vorgangService.findAlleWithoutPaginated();
        List<VorgangDTO> vorgaengeDTO = vorgaenge.stream()
                .map(vorgangMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(vorgaengeDTO);
    }

    /**
     * Einzelnen Vorgang abrufen
     */
    @GetMapping("/{id}")
    public ResponseEntity<VorgangDTO> getVorgang(@PathVariable UUID id) {
        return vorgangService.findById(id)
                .map(vorgangMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Vorgänge nach Typ
     */
    @GetMapping("/typ/{typ}")
    public ResponseEntity<List<VorgangDTO>> getVorgaengeByTyp(@PathVariable VorgangTyp typ) {
        List<Vorgang> vorgaenge = vorgangService.findByTyp(typ);
        List<VorgangDTO> vorgaengeDTO = vorgaenge.stream()
                .map(vorgangMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(vorgaengeDTO);
    }

    /**
     * Vorgänge nach Status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<VorgangDTO>> getVorgaengeByStatus(@PathVariable VorgangStatus status) {
        List<Vorgang> vorgaenge = vorgangService.findByStatus(status);
        List<VorgangDTO> vorgaengeDTO = vorgaenge.stream()
                .map(vorgangMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(vorgaengeDTO);
    }

    /**
     * Aktuell laufende Vorgänge
     */
    @GetMapping("/laufend")
    public ResponseEntity<List<VorgangDTO>> getLaufendeVorgaenge() {
        List<Vorgang> laufende = vorgangService.findLaufendeVorgaenge();
        List<VorgangDTO> laufendeDTO = laufende.stream()
                .map(vorgangMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(laufendeDTO);
    }

    /**
     * Rechnungsläufe der letzten X Tage
     */
    @GetMapping("/rechnungslaeufe")
    public ResponseEntity<List<VorgangDTO>> getRecentRechnungslaeufe(@RequestParam(defaultValue = "30") int tage) {
        List<Vorgang> rechnungslaeufe = vorgangService.findRecentRechnungslaeufe(tage);
        List<VorgangDTO> rechnungslaeufeDTO = rechnungslaeufe.stream()
                .map(vorgangMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rechnungslaeufeDTO);
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
    public ResponseEntity<List<VorgangDTO>> getLanglaufendeVorgaenge(@RequestParam(defaultValue = "60") int minuten) {
        List<Vorgang> langlaufende = vorgangService.findLanglaufendeVorgaenge(minuten);
        List<VorgangDTO> langlaufendeDTO = langlaufende.stream()
                .map(vorgangMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(langlaufendeDTO);
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
     * Hängengebliebene Vorgänge korrigieren
     */
    @PostMapping("/korrigieren")
    public ResponseEntity<String> korrigiereHaengengebliebene(@RequestParam(defaultValue = "24") int stundenSchwellwert) {
        int korrigiert = vorgangService.korrigiereHaengengebliebeneVorgaenge(stundenSchwellwert);
        return ResponseEntity.ok(String.format("✓ %d hängengebliebene Vorgänge korrigiert", korrigiert));
    }
}