package com.erp.backend.controller;

import com.erp.backend.domain.CrmActivity;
import com.erp.backend.dto.CrmActivityDto;
import com.erp.backend.service.CrmActivityService;
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
@RequestMapping("/api/crm/activities")
@CrossOrigin
@Tag(name = "CRM – Aktivitäten")
public class CrmActivityController {

    private static final Logger logger = LoggerFactory.getLogger(CrmActivityController.class);

    private final CrmActivityService service;

    public CrmActivityController(CrmActivityService service) {
        this.service = service;
    }

    @Operation(summary = "Aktivität erstellen (für Kunde oder Vertrag)")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping
    public ResponseEntity<CrmActivityDto> createActivity(
            @Valid @RequestBody CrmActivity activity,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID contractId) {

        CrmActivity created = service.createActivity(activity, customerId, contractId);
        return ResponseEntity.status(HttpStatus.CREATED).body(CrmActivityDto.fromEntity(created));
    }

    @Operation(summary = "Aktivität nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<CrmActivityDto> getActivity(@PathVariable UUID id) {
        return ResponseEntity.ok(CrmActivityDto.fromEntity(service.findById(id)));
    }

    @Operation(summary = "Aktivitäten eines Kunden abrufen – optional paginiert")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<List<CrmActivityDto>> getByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "activityDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        if (paginated) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
            Page<CrmActivityDto> actPage = service.getActivitiesByCustomer(customerId, pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(actPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(actPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(actPage.getContent());
        }
        return ResponseEntity.ok(service.getActivitiesByCustomer(customerId));
    }

    @Operation(summary = "Aktivitäten eines Vertrags abrufen – optional paginiert")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/by-contract/{contractId}")
    public ResponseEntity<List<CrmActivityDto>> getByContract(
            @PathVariable UUID contractId,
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "activityDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        if (paginated) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
            Page<CrmActivityDto> actPage = service.getActivitiesByContract(contractId, pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(actPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(actPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(actPage.getContent());
        }
        return ResponseEntity.ok(service.getActivitiesByContract(contractId));
    }

    @Operation(summary = "Überfällige Aktivitäten abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/overdue")
    public ResponseEntity<List<CrmActivityDto>> getOverdueActivities() {
        return ResponseEntity.ok(service.getOverdueActivities());
    }

    @Operation(summary = "Aktivität aktualisieren")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping("/{id}")
    public ResponseEntity<CrmActivityDto> updateActivity(
            @PathVariable UUID id,
            @Valid @RequestBody CrmActivity updated) {

        CrmActivity saved = service.updateActivity(id, updated);
        return ResponseEntity.ok(CrmActivityDto.fromEntity(saved));
    }

    @Operation(summary = "Aktivität als abgeschlossen markieren")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<CrmActivityDto> completeActivity(
            @PathVariable UUID id,
            @RequestParam(required = false) String result) {

        CrmActivity saved = service.completeActivity(id, result);
        return ResponseEntity.ok(CrmActivityDto.fromEntity(saved));
    }

    @Operation(summary = "Aktivität löschen (Soft-Delete)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable UUID id) {
        service.deleteActivity(id);
        return ResponseEntity.noContent().build();
    }
}
