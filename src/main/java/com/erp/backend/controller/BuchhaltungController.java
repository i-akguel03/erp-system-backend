package com.erp.backend.controller;

import com.erp.backend.dto.BuchungssatzDTO;
import com.erp.backend.dto.KontoDTO;
import com.erp.backend.service.BuchhaltungService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/buchhaltung")
@CrossOrigin
@Tag(name = "Buchhaltung")
public class BuchhaltungController {

    private final BuchhaltungService buchhaltungService;

    public BuchhaltungController(BuchhaltungService buchhaltungService) {
        this.buchhaltungService = buchhaltungService;
    }

    @Operation(summary = "Alle Buchungssätze abrufen — optional paginiert")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BUCHHALTUNG_READ')")
    @GetMapping("/buchungen")
    public ResponseEntity<List<BuchungssatzDTO>> getAllBuchungen(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "buchungsDatum") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        List<BuchungssatzDTO> all = buchhaltungService.findAll();
        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), all.size());
            List<BuchungssatzDTO> pageContent = start >= all.size() ? List.of() : all.subList(start, end);
            Page<BuchungssatzDTO> buchungenPage = new PageImpl<>(pageContent, pageable, all.size());
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(buchungenPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(buchungenPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .header("Access-Control-Expose-Headers", "X-Total-Count,X-Total-Pages,X-Current-Page")
                    .body(buchungenPage.getContent());
        }
        return ResponseEntity.ok(all);
    }

    @Operation(summary = "Buchungssatz nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BUCHHALTUNG_READ')")
    @GetMapping("/buchungen/{id}")
    public ResponseEntity<BuchungssatzDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(buchhaltungService.findById(id));
    }

    @Operation(summary = "Buchungssätze nach Belegs-Referenz (z.B. Rechnungs-ID)")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BUCHHALTUNG_READ')")
    @GetMapping("/buchungen/referenz/{referenzId}")
    public ResponseEntity<List<BuchungssatzDTO>> getByReferenz(@PathVariable String referenzId) {
        return ResponseEntity.ok(buchhaltungService.findByReferenz(referenzId));
    }

    @Operation(summary = "Kontoauszug — alle Buchungen zu einem Konto")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BUCHHALTUNG_READ')")
    @GetMapping("/konten/{kontonummer}/buchungen")
    public ResponseEntity<List<BuchungssatzDTO>> getByKonto(@PathVariable Long kontonummer) {
        return ResponseEntity.ok(buchhaltungService.findByKonto(kontonummer));
    }

    @Operation(summary = "Kontosaldo für ein Geschäftsjahr berechnen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BUCHHALTUNG_READ')")
    @GetMapping("/konten/{kontonummer}/saldo")
    public ResponseEntity<KontoDTO> getSaldo(
            @PathVariable Long kontonummer,
            @RequestParam(defaultValue = "0") int jahr) {
        int j = jahr == 0 ? LocalDate.now().getYear() : jahr;
        return ResponseEntity.ok(buchhaltungService.kontoMitSaldo(kontonummer, j));
    }

    @Operation(summary = "GuV-Übersicht für ein Geschäftsjahr")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BUCHHALTUNG_READ')")
    @GetMapping("/berichte/guv")
    public ResponseEntity<Map<String, BigDecimal>> getGuv(
            @RequestParam(defaultValue = "0") int jahr) {
        int j = jahr == 0 ? LocalDate.now().getYear() : jahr;
        return ResponseEntity.ok(buchhaltungService.guvUebersicht(j));
    }

    @Operation(summary = "Buchungssatz stornieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/buchungen/{id}/stornieren")
    public ResponseEntity<BuchungssatzDTO> stornieren(@PathVariable UUID id) {
        return ResponseEntity.ok(buchhaltungService.stornieren(id));
    }
}