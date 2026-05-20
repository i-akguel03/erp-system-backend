package com.erp.backend.controller;

import com.erp.backend.domain.AngebotStatus;
import com.erp.backend.dto.AngebotDTO;
import com.erp.backend.dto.AngebotPositionDTO;
import com.erp.backend.dto.OrderDTO;
import com.erp.backend.service.AngebotService;
import com.erp.backend.service.OrderService;
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
@RequestMapping("/api/angebote")
@CrossOrigin
@Tag(name = "Angebote")
public class AngebotController {

    private final AngebotService angebotService;
    private final OrderService orderService;

    public AngebotController(AngebotService angebotService, OrderService orderService) {
        this.angebotService = angebotService;
        this.orderService = orderService;
    }

    @Operation(summary = "Alle Angebote abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'ANGEBOTE_READ')")
    @GetMapping
    public ResponseEntity<List<AngebotDTO>> getAll() {
        return ResponseEntity.ok(angebotService.findAll());
    }

    @Operation(summary = "Angebot nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'ANGEBOTE_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<AngebotDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(angebotService.findById(id));
    }

    @Operation(summary = "Angebote nach Kunde abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'ANGEBOTE_READ')")
    @GetMapping("/kunde/{customerId}")
    public ResponseEntity<List<AngebotDTO>> getByKunde(@PathVariable UUID customerId) {
        return ResponseEntity.ok(angebotService.findByCustomer(customerId));
    }

    @Operation(summary = "Angebote nach Status filtern")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'ANGEBOTE_READ')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<AngebotDTO>> getByStatus(@PathVariable AngebotStatus status) {
        return ResponseEntity.ok(angebotService.findByStatus(status));
    }

    @Operation(summary = "Neues Angebot erstellen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AngebotDTO> erstellen(@RequestBody CreateAngebotRequest request) {
        AngebotDTO dto = angebotService.erstellen(
                request.getCustomerId(),
                request.getPositionen(),
                request.getGueltigBis(),
                request.getNotizen()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(summary = "Angebotsstatus ändern")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<AngebotDTO> statusAendern(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        AngebotStatus neuerStatus = AngebotStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(angebotService.statusAendern(id, neuerStatus));
    }

    @Operation(summary = "Angebot zu Auftrag konvertieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/konvertieren")
    public ResponseEntity<OrderDTO> zuAuftragKonvertieren(@PathVariable UUID id) {
        OrderDTO order = orderService.auftragAusAngebotErstellen(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Operation(summary = "Angebot löschen")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> loeschen(@PathVariable UUID id) {
        angebotService.loeschen(id);
        return ResponseEntity.noContent().build();
    }

    public static class CreateAngebotRequest {
        private UUID customerId;
        private List<AngebotPositionDTO> positionen;
        private LocalDate gueltigBis;
        private String notizen;

        public UUID getCustomerId() { return customerId; }
        public void setCustomerId(UUID customerId) { this.customerId = customerId; }
        public List<AngebotPositionDTO> getPositionen() { return positionen; }
        public void setPositionen(List<AngebotPositionDTO> positionen) { this.positionen = positionen; }
        public LocalDate getGueltigBis() { return gueltigBis; }
        public void setGueltigBis(LocalDate gueltigBis) { this.gueltigBis = gueltigBis; }
        public String getNotizen() { return notizen; }
        public void setNotizen(String notizen) { this.notizen = notizen; }
    }
}