package com.erp.backend.controller;

import com.erp.backend.domain.CrmNote;
import com.erp.backend.dto.CrmNoteDto;
import com.erp.backend.service.CrmNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/crm/notes")
@CrossOrigin
@Tag(name = "CRM – Notizen")
public class CrmNoteController {

    private static final Logger logger = LoggerFactory.getLogger(CrmNoteController.class);

    private final CrmNoteService service;

    public CrmNoteController(CrmNoteService service) {
        this.service = service;
    }

    @Operation(summary = "Notiz erstellen (für Kunde oder Vertrag)")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping
    public ResponseEntity<CrmNoteDto> createNote(
            @Valid @RequestBody CrmNote note,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID contractId) {

        CrmNote created = service.createNote(note, customerId, contractId);
        return ResponseEntity.status(HttpStatus.CREATED).body(CrmNoteDto.fromEntity(created));
    }

    @Operation(summary = "Notiz nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<CrmNoteDto> getNote(@PathVariable UUID id) {
        return ResponseEntity.ok(CrmNoteDto.fromEntity(service.findById(id)));
    }

    @Operation(summary = "Notizen eines Kunden abrufen – optional paginiert")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<List<CrmNoteDto>> getNotesByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        if (paginated) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
            Page<CrmNoteDto> notePage = service.getNotesByCustomer(customerId, pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(notePage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(notePage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(notePage.getContent());
        }
        return ResponseEntity.ok(service.getNotesByCustomer(customerId));
    }

    @Operation(summary = "Notizen eines Vertrags abrufen – optional paginiert")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/by-contract/{contractId}")
    public ResponseEntity<List<CrmNoteDto>> getNotesByContract(
            @PathVariable UUID contractId,
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        if (paginated) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
            Page<CrmNoteDto> notePage = service.getNotesByContract(contractId, pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(notePage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(notePage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(notePage.getContent());
        }
        return ResponseEntity.ok(service.getNotesByContract(contractId));
    }

    @Operation(summary = "Notiz aktualisieren")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping("/{id}")
    public ResponseEntity<CrmNoteDto> updateNote(
            @PathVariable UUID id,
            @Valid @RequestBody CrmNote updated) {

        CrmNote saved = service.updateNote(id, updated);
        return ResponseEntity.ok(CrmNoteDto.fromEntity(saved));
    }

    @Operation(summary = "Notiz löschen (Soft-Delete)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable UUID id) {
        service.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
}
