package com.erp.backend.controller;

import com.erp.backend.domain.CrmDocument;
import com.erp.backend.dto.CrmDocumentDto;
import com.erp.backend.service.CrmDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/crm/documents")
@CrossOrigin
@Tag(name = "CRM – Dokumente")
public class CrmDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(CrmDocumentController.class);

    private final CrmDocumentService service;

    public CrmDocumentController(CrmDocumentService service) {
        this.service = service;
    }

    @Operation(summary = "Dokument hochladen (für Kunde oder Vertrag)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CrmDocumentDto> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID contractId,
            @RequestParam(required = false) CrmDocument.DocumentType documentType,
            @RequestParam(required = false) String description) {

        CrmDocument doc = service.uploadDocument(file, customerId, contractId, documentType, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(CrmDocumentDto.fromEntity(doc));
    }

    @Operation(summary = "Dokument herunterladen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID id) {
        CrmDocument doc = service.findById(id);
        byte[] content = service.downloadDocument(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveMediaType(doc.getMimeType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(doc.getOriginalFileName())
                .build());
        headers.setContentLength(content.length);

        return ResponseEntity.ok().headers(headers).body(content);
    }

    @Operation(summary = "Dokument-Metadaten abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<CrmDocumentDto> getDocument(@PathVariable UUID id) {
        return ResponseEntity.ok(CrmDocumentDto.fromEntity(service.findById(id)));
    }

    @Operation(summary = "Dokumente eines Kunden abrufen – optional paginiert")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<List<CrmDocumentDto>> getByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        if (paginated) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
            Page<CrmDocumentDto> docPage = service.getDocumentsByCustomer(customerId, pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(docPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(docPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(docPage.getContent());
        }
        return ResponseEntity.ok(service.getDocumentsByCustomer(customerId));
    }

    @Operation(summary = "Dokumente eines Vertrags abrufen – optional paginiert")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'CRM_READ')")
    @GetMapping("/by-contract/{contractId}")
    public ResponseEntity<List<CrmDocumentDto>> getByContract(
            @PathVariable UUID contractId,
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        if (paginated) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
            Page<CrmDocumentDto> docPage = service.getDocumentsByContract(contractId, pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(docPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(docPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(docPage.getContent());
        }
        return ResponseEntity.ok(service.getDocumentsByContract(contractId));
    }

    @Operation(summary = "Dokument-Metadaten aktualisieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<CrmDocumentDto> updateDocument(
            @PathVariable UUID id,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) CrmDocument.DocumentType documentType) {

        CrmDocument updated = service.updateDescription(id, description, documentType);
        return ResponseEntity.ok(CrmDocumentDto.fromEntity(updated));
    }

    @Operation(summary = "Dokument löschen (Soft-Delete)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        service.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    private MediaType resolveMediaType(String mimeType) {
        if (mimeType == null) return MediaType.APPLICATION_OCTET_STREAM;
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
