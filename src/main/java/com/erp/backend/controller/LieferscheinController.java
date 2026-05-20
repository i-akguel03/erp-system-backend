package com.erp.backend.controller;

import com.erp.backend.domain.LieferscheinStatus;
import com.erp.backend.dto.LieferscheinDTO;
import com.erp.backend.dto.LieferscheinPositionDTO;
import com.erp.backend.service.LieferscheinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/lieferscheine")
@CrossOrigin
@Tag(name = "Lieferscheine")
public class LieferscheinController {

    private final LieferscheinService lieferscheinService;

    public LieferscheinController(LieferscheinService lieferscheinService) {
        this.lieferscheinService = lieferscheinService;
    }

    @Operation(summary = "Alle Lieferscheine abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'LIEFERSCHEINE_READ')")
    @GetMapping
    public ResponseEntity<List<LieferscheinDTO>> getAll() {
        return ResponseEntity.ok(lieferscheinService.findAll());
    }

    @Operation(summary = "Lieferschein nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'LIEFERSCHEINE_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<LieferscheinDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(lieferscheinService.findById(id));
    }

    @Operation(summary = "Lieferscheine nach Auftrag abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'LIEFERSCHEINE_READ')")
    @GetMapping("/auftrag/{auftragId}")
    public ResponseEntity<List<LieferscheinDTO>> getByAuftrag(@PathVariable Long auftragId) {
        return ResponseEntity.ok(lieferscheinService.findByAuftrag(auftragId));
    }

    @Operation(summary = "Neuen Lieferschein erstellen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<LieferscheinDTO> erstellen(@RequestBody CreateLieferscheinRequest request) {
        LieferscheinDTO dto = lieferscheinService.erstellen(
                request.getAuftragId(),
                request.getPositionen(),
                request.getLieferDatum(),
                request.getLieferAdresseId(),
                request.getNotizen()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(summary = "Lieferscheinstatus ändern")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<LieferscheinDTO> statusAendern(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        LieferscheinStatus neuerStatus = LieferscheinStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(lieferscheinService.statusAendern(id, neuerStatus));
    }

    public static class CreateLieferscheinRequest {
        private Long auftragId;
        private List<LieferscheinPositionDTO> positionen;
        private LocalDate lieferDatum;
        private Long lieferAdresseId;
        private String notizen;

        public Long getAuftragId() { return auftragId; }
        public void setAuftragId(Long auftragId) { this.auftragId = auftragId; }
        public List<LieferscheinPositionDTO> getPositionen() { return positionen; }
        public void setPositionen(List<LieferscheinPositionDTO> positionen) { this.positionen = positionen; }
        public LocalDate getLieferDatum() { return lieferDatum; }
        public void setLieferDatum(LocalDate lieferDatum) { this.lieferDatum = lieferDatum; }
        public Long getLieferAdresseId() { return lieferAdresseId; }
        public void setLieferAdresseId(Long lieferAdresseId) { this.lieferAdresseId = lieferAdresseId; }
        public String getNotizen() { return notizen; }
        public void setNotizen(String notizen) { this.notizen = notizen; }
    }
}