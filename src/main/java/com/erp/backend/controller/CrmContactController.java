package com.erp.backend.controller;

import com.erp.backend.domain.CrmContact;
import com.erp.backend.dto.CrmContactDto;
import com.erp.backend.service.CrmContactService;
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
@RequestMapping("/api/crm/contacts")
@CrossOrigin
@Tag(name = "CRM – Ansprechpartner")
public class CrmContactController {

    private static final Logger logger = LoggerFactory.getLogger(CrmContactController.class);

    private final CrmContactService service;

    public CrmContactController(CrmContactService service) {
        this.service = service;
    }

    @Operation(summary = "Ansprechpartner erstellen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CrmContactDto> createContact(
            @Valid @RequestBody CrmContact contact,
            @RequestParam UUID customerId) {

        CrmContact created = service.createContact(contact, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(CrmContactDto.fromEntity(created));
    }

    @Operation(summary = "Ansprechpartner nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<CrmContactDto> getContact(@PathVariable UUID id) {
        return ResponseEntity.ok(CrmContactDto.fromEntity(service.findById(id)));
    }

    @Operation(summary = "Ansprechpartner eines Kunden abrufen – optional paginiert")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<List<CrmContactDto>> getContactsByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        if (paginated) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
            Page<CrmContactDto> contactPage = service.getContactsByCustomer(customerId, pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(contactPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(contactPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(contactPage.getContent());
        }
        return ResponseEntity.ok(service.getContactsByCustomer(customerId));
    }

    @Operation(summary = "Ansprechpartner suchen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/search")
    public ResponseEntity<List<CrmContactDto>> searchContacts(@RequestParam String q) {
        return ResponseEntity.ok(service.searchContacts(q));
    }

    @Operation(summary = "Ansprechpartner aktualisieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CrmContactDto> updateContact(
            @PathVariable UUID id,
            @Valid @RequestBody CrmContact updated) {

        CrmContact saved = service.updateContact(id, updated);
        return ResponseEntity.ok(CrmContactDto.fromEntity(saved));
    }

    @Operation(summary = "Als Hauptansprechpartner setzen")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/set-primary")
    public ResponseEntity<CrmContactDto> setPrimary(@PathVariable UUID id) {
        CrmContact saved = service.setPrimaryContact(id);
        return ResponseEntity.ok(CrmContactDto.fromEntity(saved));
    }

    @Operation(summary = "Ansprechpartner löschen (Soft-Delete)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable UUID id) {
        service.deleteContact(id);
        return ResponseEntity.noContent().build();
    }
}
