package com.erp.backend.controller;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.Vorgang;
import com.erp.backend.domain.VorgangStatus;
import com.erp.backend.domain.VorgangTyp;
import com.erp.backend.dto.ContractDTO;
import com.erp.backend.dto.InvoiceDTO;
import com.erp.backend.dto.VorgangDTO;
import com.erp.backend.mapper.ContractMapper;
import com.erp.backend.mapper.InvoiceMapper;
import com.erp.backend.mapper.VorgangMapper;
import com.erp.backend.service.VorgangService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VORGANGS_READ')")
    @GetMapping
    public ResponseEntity<Page<VorgangDTO>> getAllVorgaenge(Pageable pageable) {
        Page<Vorgang> vorgaenge = vorgangService.findAllePaginated(pageable);
        Page<VorgangDTO> vorgaengeDTO = vorgaenge.map(vorgangMapper::toDTO);
        return ResponseEntity.ok(vorgaengeDTO);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VORGANGS_READ')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VORGANGS_READ')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VORGANGS_READ')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VORGANGS_READ')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VORGANGS_READ')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VORGANGS_READ')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VORGANGS_READ')")
    @GetMapping("/statistiken")
    public ResponseEntity<VorgangService.VorgangStatistik> getVorgangStatistiken() {
        VorgangService.VorgangStatistik stats = vorgangService.getVorgangStatistik();
        return ResponseEntity.ok(stats);
    }

    /**
     * Langlaufende Vorgänge (mehr als X Minuten)
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VORGANGS_READ')")
    @GetMapping("/langlaufend")
    public ResponseEntity<List<VorgangDTO>> getLanglaufendeVorgaenge(@RequestParam(defaultValue = "60") int minuten) {
        List<Vorgang> langlaufende = vorgangService.findLanglaufendeVorgaenge(minuten);
        List<VorgangDTO> langlaufendeDTO = langlaufende.stream()
                .map(vorgangMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(langlaufendeDTO);
    }

    /**
     * Alle Rechnungen eines Rechnungslauf-Vorgangs
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VORGANGS_READ')")
    @GetMapping("/{id}/rechnungen")
    public ResponseEntity<List<InvoiceDTO>> getRechnungenByVorgang(@PathVariable UUID id) {
        try {
            List<Invoice> rechnungen = vorgangService.findRechnungenByVorgangId(id);
            List<InvoiceDTO> dtos = rechnungen.stream()
                    .map(InvoiceMapper::toDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Alle verlängerten Verträge eines Verlängerungslauf-Vorgangs
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VORGANGS_READ')")
    @GetMapping("/{id}/vertraege")
    public ResponseEntity<List<ContractDTO>> getVertraegeByVorgang(@PathVariable UUID id) {
        try {
            List<Contract> vertraege = vorgangService.findVertraegeByVorgangId(id);
            List<ContractDTO> dtos = vertraege.stream()
                    .map(ContractMapper::toDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Vorgang manuell abbrechen
     */
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/korrigieren")
    public ResponseEntity<String> korrigiereHaengengebliebene(@RequestParam(defaultValue = "24") int stundenSchwellwert) {
        int korrigiert = vorgangService.korrigiereHaengengebliebeneVorgaenge(stundenSchwellwert);
        return ResponseEntity.ok(String.format("✓ %d hängengebliebene Vorgänge korrigiert", korrigiert));
    }
}