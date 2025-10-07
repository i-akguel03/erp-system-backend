package com.erp.backend.controller;

import com.erp.backend.domain.Invoice;
import com.erp.backend.dto.InvoiceDTO;
import com.erp.backend.dto.InvoiceItemDTO;
import com.erp.backend.mapper.InvoiceMapper;
import com.erp.backend.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * ===============================================================================
 * INVOICE CONTROLLER - REST-API für Rechnungsverwaltung
 * ===============================================================================
 *
 * REST-Endpunkte für:
 * 1. CRUD-Operationen (Create, Read, Update, Delete)
 * 2. Status-Management (cancel, send, changeStatus)
 * 3. Item-Management (Items hinzufügen/entfernen)
 * 4. Filtering/Queries (nach Kunde, Status, Datum, etc.)
 *
 * WICHTIG:
 * - Alle Operationen nutzen DTOs (Data Transfer Objects) statt Domain-Objekte
 * - InvoiceMapper konvertiert zwischen Domain und DTO
 * - Detailliertes Logging für alle Operationen
 * - Proper HTTP Status Codes (200, 201, 204, 404, etc.)
 *
 * Base-URL: /api/invoices
 * CORS: Aktiviert für Frontend-Zugriff
 */
@RestController
@RequestMapping("/api/invoices")
@CrossOrigin  // Erlaubt Cross-Origin Requests (für Frontend)
public class InvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

    // Service-Layer für Business-Logik
    private final InvoiceService invoiceService;

    /**
     * KONSTRUKTOR - Dependency Injection
     *
     * Spring injiziert automatisch den InvoiceService.
     */
    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // ==============================
    // 1. CRUD-OPERATIONEN
    // ==============================

    /**
     * GET /api/invoices - Alle Rechnungen abrufen
     *
     * Features:
     * - Optional: Paginierung (für große Datenmengen)
     * - Sortierung nach beliebigen Feldern
     * - Response-Headers mit Metadaten (Total Count, Pages, etc.)
     *
     * Query-Parameter:
     * - paginated: true/false (Standard: false)
     * - page: Seitennummer (Standard: 0)
     * - size: Anzahl pro Seite (Standard: 20)
     * - sortBy: Feld für Sortierung (Standard: invoiceDate)
     * - sortDirection: ASC/DESC (Standard: DESC)
     *
     * Beispiele:
     * GET /api/invoices
     * GET /api/invoices?paginated=true&page=0&size=10
     * GET /api/invoices?sortBy=totalAmount&sortDirection=ASC
     *
     * @return Liste von InvoiceDTOs (+ Pagination-Headers falls aktiviert)
     */
    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "invoiceDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logger.info("GET /api/invoices - Fetching invoices (paginated: {})", paginated);

        if (paginated) {
            // PAGINIERTE AUSGABE
            // Sortierung konfigurieren
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            // Alle Rechnungen holen und in Page wrappen
            List<Invoice> allInvoices = invoiceService.getAllInvoices();
            Page<Invoice> invoicePage = new PageImpl<>(allInvoices, pageable, allInvoices.size());

            // In DTOs konvertieren
            List<InvoiceDTO> dtoList = invoicePage.getContent().stream()
                    .map(InvoiceMapper::toDTO)
                    .toList();

            // Response mit Pagination-Metadaten in Headers
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(invoicePage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(invoicePage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(dtoList);
        } else {
            // STANDARD-AUSGABE (alle Rechnungen)
            List<InvoiceDTO> dtoList = invoiceService.getAllInvoices().stream()
                    .map(InvoiceMapper::toDTO)
                    .toList();
            return ResponseEntity.ok(dtoList);
        }
    }

    /**
     * GET /api/invoices/by-subscriptions - Rechnungen nach Subscription-IDs
     *
     * Findet alle Rechnungen die zu bestimmten Subscriptions gehören.
     * Nützlich für Kunden-Portale und Subscription-Übersichten.
     *
     * Query-Parameter:
     * - subscriptionIds: Komma-separierte Liste von UUIDs
     *
     * Beispiel:
     * GET /api/invoices/by-subscriptions?subscriptionIds=uuid1,uuid2,uuid3
     *
     * @param subscriptionIds Liste von Subscription-IDs
     * @return Liste von InvoiceDTOs
     */
    @GetMapping("/by-subscriptions")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesBySubscriptionIds(
            @RequestParam List<UUID> subscriptionIds) {

        logger.info("GET /api/invoices/by-subscriptions - subscriptionIds: {}", subscriptionIds);

        // Service aufrufen und in DTOs konvertieren
        List<InvoiceDTO> dtos = invoiceService.getInvoicesBySubscriptionIds(subscriptionIds)
                .stream()
                .map(InvoiceMapper::toDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/invoices/{id} - Einzelne Rechnung abrufen
     *
     * Sucht eine Rechnung anhand ihrer ID.
     *
     * Path-Parameter:
     * - id: Die Rechnungs-UUID
     *
     * Beispiel:
     * GET /api/invoices/123e4567-e89b-12d3-a456-426614174000
     *
     * Response:
     * - 200 OK: Rechnung gefunden (mit InvoiceDTO)
     * - 404 NOT FOUND: Rechnung existiert nicht
     *
     * @param id Die Rechnungs-ID
     * @return InvoiceDTO oder 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getInvoiceById(@PathVariable UUID id) {
        logger.info("GET /api/invoices/{} - Fetching single invoice", id);

        // Optional<Invoice> → Optional<InvoiceDTO> → ResponseEntity
        return invoiceService.getInvoiceById(id)
                .map(InvoiceMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/invoices - Neue Rechnung erstellen
     *
     * WICHTIG: OpenItem wird automatisch mit erstellt (siehe InvoiceService)!
     *
     * Request-Body: InvoiceDTO (JSON)
     * {
     *   "customerId": "uuid",
     *   "invoiceDate": "2025-01-15",
     *   "dueDate": "2025-02-15"
     * }
     *
     * Response:
     * - 201 CREATED: Rechnung erfolgreich erstellt
     * - Body: Die erstellte Rechnung als InvoiceDTO
     *
     * @param dto Die Rechnungsdaten als DTO
     * @return Die erstellte Rechnung (201 CREATED)
     */
    @PostMapping
    public ResponseEntity<InvoiceDTO> createInvoice(@RequestBody InvoiceDTO dto) {
        logger.info("POST /api/invoices - Creating new invoice for customer {}", dto.getCustomerId());

        // DTO → Domain-Objekt konvertieren
        Invoice invoice = new Invoice();
        invoice.setCustomer(new com.erp.backend.domain.Customer(UUID.fromString(dto.getCustomerId())));
        invoice.setInvoiceDate(dto.getInvoiceDate());
        invoice.setDueDate(dto.getDueDate());

        // Service aufrufen (erstellt auch automatisch OpenItem!)
        Invoice created = invoiceService.createInvoice(invoice);

        // Domain → DTO konvertieren und mit 201 CREATED zurückgeben
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InvoiceMapper.toDTO(created));
    }

    /**
     * PUT /api/invoices/{id} - Rechnung aktualisieren
     *
     * Aktualisiert eine bestehende Rechnung.
     *
     * Path-Parameter:
     * - id: Die Rechnungs-UUID
     *
     * Request-Body: InvoiceDTO (JSON)
     *
     * Response:
     * - 200 OK: Rechnung erfolgreich aktualisiert
     * - 404 NOT FOUND: Rechnung existiert nicht
     *
     * @param id Die Rechnungs-ID
     * @param dto Die aktualisierten Rechnungsdaten
     * @return Die aktualisierte Rechnung (200 OK)
     */
    @PutMapping("/{id}")
    public ResponseEntity<InvoiceDTO> updateInvoice(@PathVariable UUID id, @RequestBody InvoiceDTO dto) {
        logger.info("PUT /api/invoices/{} - Updating invoice", id);

        // DTO → Domain-Objekt konvertieren
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setInvoiceDate(dto.getInvoiceDate());
        invoice.setDueDate(dto.getDueDate());
        invoice.setCustomer(new com.erp.backend.domain.Customer(UUID.fromString(dto.getCustomerId())));

        // Service aufrufen
        Invoice updated = invoiceService.updateInvoice(invoice);

        // Domain → DTO konvertieren und zurückgeben
        return ResponseEntity.ok(InvoiceMapper.toDTO(updated));
    }

    /**
     * DELETE /api/invoices/{id} - Rechnung löschen
     *
     * WICHTIG: Löscht auch alle zugehörigen OpenItems (siehe InvoiceService)!
     *
     * SICHERHEIT:
     * - Rechnung darf keine offenen Posten (OPEN) haben
     * - Erst stornieren, dann löschen!
     *
     * Path-Parameter:
     * - id: Die Rechnungs-UUID
     *
     * Response:
     * - 204 NO CONTENT: Rechnung erfolgreich gelöscht
     * - 404 NOT FOUND: Rechnung existiert nicht
     * - 400 BAD REQUEST: Rechnung hat noch offene Posten
     *
     * @param id Die Rechnungs-ID
     * @return 204 NO CONTENT bei Erfolg
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        logger.info("DELETE /api/invoices/{} - Deleting invoice", id);

        // Service aufrufen (löscht auch OpenItems!)
        invoiceService.deleteInvoice(id);

        // 204 NO CONTENT zurückgeben (erfolgreiche Löschung ohne Body)
        return ResponseEntity.noContent().build();
    }

    // ==============================
    // 2. STATUS-MANAGEMENT
    // ==============================

    /**
     * PATCH /api/invoices/{id}/cancel - Rechnung stornieren
     *
     * WICHTIG: Storniert auch alle zugehörigen OpenItems (siehe InvoiceService)!
     *
     * Dies ist die HAUPTMETHODE für Rechnungsstornierung.
     * - Rechnung → Status CANCELLED
     * - Alle OpenItems (OPEN) → Status CANCELLED
     *
     * Path-Parameter:
     * - id: Die Rechnungs-UUID
     *
     * Response:
     * - 200 OK: Rechnung erfolgreich storniert
     * - 404 NOT FOUND: Rechnung existiert nicht
     *
     * Beispiel:
     * PATCH /api/invoices/123e4567-e89b-12d3-a456-426614174000/cancel
     *
     * @param id Die Rechnungs-ID
     * @return Die stornierte Rechnung (200 OK)
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<InvoiceDTO> cancelInvoice(@PathVariable UUID id) {
        logger.info("PATCH /api/invoices/{}/cancel - Cancelling invoice", id);

        // Service aufrufen (storniert auch OpenItems!)
        Invoice invoice = invoiceService.cancelInvoice(id);

        // Domain → DTO konvertieren und zurückgeben
        return ResponseEntity.ok(InvoiceMapper.toDTO(invoice));
    }

    /**
     * PATCH /api/invoices/{id}/send - Rechnung versenden
     *
     * Setzt den Status auf SENT (versendet).
     *
     * Path-Parameter:
     * - id: Die Rechnungs-UUID
     *
     * Response:
     * - 200 OK: Status erfolgreich geändert
     * - 404 NOT FOUND: Rechnung existiert nicht
     *
     * Beispiel:
     * PATCH /api/invoices/123e4567-e89b-12d3-a456-426614174000/send
     *
     * @param id Die Rechnungs-ID
     * @return Die aktualisierte Rechnung (200 OK)
     */
    @PatchMapping("/{id}/send")
    public ResponseEntity<InvoiceDTO> sendInvoice(@PathVariable UUID id) {
        logger.info("PATCH /api/invoices/{}/send - Sending invoice", id);

        // Service aufrufen
        Invoice invoice = invoiceService.sendInvoice(id);

        // Domain → DTO konvertieren und zurückgeben
        return ResponseEntity.ok(InvoiceMapper.toDTO(invoice));
    }

    /**
     * PATCH /api/invoices/{id}/status - Status manuell ändern
     *
     * Universelle Methode zum Ändern des Status.
     *
     * Path-Parameter:
     * - id: Die Rechnungs-UUID
     *
     * Query-Parameter:
     * - status: Der neue Status (DRAFT, SENT, PAID, CANCELLED)
     *
     * Response:
     * - 200 OK: Status erfolgreich geändert
     * - 404 NOT FOUND: Rechnung existiert nicht
     *
     * Beispiel:
     * PATCH /api/invoices/123e4567-e89b-12d3-a456-426614174000/status?status=PAID
     *
     * HINWEIS: Für Stornierung besser /cancel verwenden (storniert auch OpenItems)!
     *
     * @param id Die Rechnungs-ID
     * @param status Der neue Status
     * @return Die aktualisierte Rechnung (200 OK)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<InvoiceDTO> changeStatus(
            @PathVariable UUID id,
            @RequestParam Invoice.InvoiceStatus status) {

        logger.info("PATCH /api/invoices/{}/status - Changing status to {}", id, status);

        // Service aufrufen
        Invoice invoice = invoiceService.changeStatus(id, status);

        // Domain → DTO konvertieren und zurückgeben
        return ResponseEntity.ok(InvoiceMapper.toDTO(invoice));
    }

    // ==============================
    // 3. ITEM-MANAGEMENT
    // ==============================

    /**
     * POST /api/invoices/{id}/items - Item zu Rechnung hinzufügen
     *
     * Fügt ein neues InvoiceItem zu einer bestehenden Rechnung hinzu.
     *
     * Path-Parameter:
     * - id: Die Rechnungs-UUID
     *
     * Request-Body: InvoiceItemDTO (JSON)
     * {
     *   "description": "Produkt XYZ",
     *   "quantity": 2,
     *   "unitPrice": 49.99,
     *   "productCode": "PROD-123",
     *   "productName": "Premium Package"
     * }
     *
     * Response:
     * - 200 OK: Item erfolgreich hinzugefügt
     * - 404 NOT FOUND: Rechnung existiert nicht
     *
     * @param id Die Rechnungs-ID
     * @param itemDTO Die Item-Daten
     * @return Die aktualisierte Rechnung mit neuem Item (200 OK)
     */
    @PostMapping("/{id}/items")
    public ResponseEntity<InvoiceDTO> addItem(
            @PathVariable UUID id,
            @RequestBody InvoiceItemDTO itemDTO) {

        logger.info("POST /api/invoices/{}/items - Adding item: {}", id, itemDTO.getDescription());

        // ItemDTO → Domain-Objekt konvertieren
        com.erp.backend.domain.InvoiceItem item = new com.erp.backend.domain.InvoiceItem();
        item.setDescription(itemDTO.getDescription());
        item.setQuantity(itemDTO.getQuantity());
        item.setUnitPrice(itemDTO.getUnitPrice());
        item.setProductCode(itemDTO.getProductCode());
        item.setProductName(itemDTO.getProductName());

        // Service aufrufen
        Invoice updated = invoiceService.addInvoiceItem(id, item);

        // Domain → DTO konvertieren und zurückgeben
        return ResponseEntity.ok(InvoiceMapper.toDTO(updated));
    }

    /**
     * DELETE /api/invoices/{id}/items/{itemId} - Item von Rechnung entfernen
     *
     * Entfernt ein InvoiceItem von einer Rechnung.
     *
     * Path-Parameter:
     * - id: Die Rechnungs-UUID
     * - itemId: Die Item-UUID
     *
     * Response:
     * - 200 OK: Item erfolgreich entfernt
     * - 404 NOT FOUND: Rechnung oder Item existiert nicht
     *
     * Beispiel:
     * DELETE /api/invoices/{invoiceId}/items/{itemId}
     *
     * @param id Die Rechnungs-ID
     * @param itemId Die Item-ID
     * @return Die aktualisierte Rechnung ohne das Item (200 OK)
     */
    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<InvoiceDTO> removeItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {

        logger.info("DELETE /api/invoices/{}/items/{} - Removing item", id, itemId);

        // Service aufrufen
        Invoice updated = invoiceService.removeInvoiceItem(id, itemId);

        // Domain → DTO konvertieren und zurückgeben
        return ResponseEntity.ok(InvoiceMapper.toDTO(updated));
    }

    // ==============================
    // 4. FILTERING / QUERIES
    // ==============================

    /**
     * GET /api/invoices/customer/{customerId} - Rechnungen eines Kunden
     *
     * Findet alle Rechnungen eines bestimmten Kunden.
     *
     * Path-Parameter:
     * - customerId: Die Kunden-UUID
     *
     * Response:
     * - 200 OK: Liste von Rechnungen (kann leer sein)
     * - 404 NOT FOUND: Kunde existiert nicht
     *
     * Beispiel:
     * GET /api/invoices/customer/123e4567-e89b-12d3-a456-426614174000
     *
     * @param customerId Die Kunden-ID
     * @return Liste von InvoiceDTOs
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByCustomer(@PathVariable UUID customerId) {
        logger.info("GET /api/invoices/customer/{} - Fetching invoices for customer", customerId);

        // Service aufrufen und in DTOs konvertieren
        List<InvoiceDTO> dtos = invoiceService.getInvoicesByCustomer(customerId).stream()
                .map(InvoiceMapper::toDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/invoices/status/{status} - Rechnungen nach Status
     *
     * Findet alle Rechnungen mit einem bestimmten Status.
     *
     * Path-Parameter:
     * - status: Der Status (DRAFT, SENT, PAID, CANCELLED)
     *
     * Response:
     * - 200 OK: Liste von Rechnungen (kann leer sein)
     *
     * Beispiele:
     * GET /api/invoices/status/DRAFT
     * GET /api/invoices/status/SENT
     * GET /api/invoices/status/PAID
     * GET /api/invoices/status/CANCELLED
     *
     * @param status Der gewünschte Status
     * @return Liste von InvoiceDTOs
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByStatus(
            @PathVariable Invoice.InvoiceStatus status) {

        logger.info("GET /api/invoices/status/{} - Fetching invoices by status", status);

        // Service aufrufen und in DTOs konvertieren
        List<InvoiceDTO> dtos = invoiceService.getInvoicesByStatus(status).stream()
                .map(InvoiceMapper::toDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/invoices/date-range - Rechnungen in Datumsbereich
     *
     * Findet alle Rechnungen zwischen zwei Daten (inklusive).
     *
     * Query-Parameter:
     * - start: Startdatum (ISO-Format: yyyy-MM-dd)
     * - end: Enddatum (ISO-Format: yyyy-MM-dd)
     *
     * Response:
     * - 200 OK: Liste von Rechnungen (kann leer sein)
     *
     * Beispiel:
     * GET /api/invoices/date-range?start=2025-01-01&end=2025-01-31
     *
     * @param start Startdatum (inklusive)
     * @param end Enddatum (inklusive)
     * @return Liste von InvoiceDTOs
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        logger.info("GET /api/invoices/date-range - Fetching invoices from {} to {}", start, end);

        // Service aufrufen und in DTOs konvertieren
        List<InvoiceDTO> dtos = invoiceService.getInvoicesByDateRange(start, end).stream()
                .map(InvoiceMapper::toDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }
}

/*
 * ===============================================================================
 * REST-API BEST PRACTICES
 * ===============================================================================
 *
 * 1. HTTP-Methoden richtig nutzen:
 *    - GET: Daten abrufen (read-only)
 *    - POST: Neue Ressourcen erstellen
 *    - PUT: Vollständiges Update
 *    - PATCH: Partielles Update (z.B. nur Status ändern)
 *    - DELETE: Ressourcen löschen
 *
 * 2. HTTP-Status-Codes:
 *    - 200 OK: Erfolgreiche Anfrage mit Body
 *    - 201 CREATED: Ressource erfolgreich erstellt
 *    - 204 NO CONTENT: Erfolgreiche Anfrage ohne Body (z.B. DELETE)
 *    - 404 NOT FOUND: Ressource existiert nicht
 *    - 400 BAD REQUEST: Ungültige Anfrage
 *
 * 3. DTOs verwenden:
 *    - Niemals Domain-Objekte direkt zurückgeben
 *    - DTOs entkoppeln API von interner Struktur
 *    - InvoiceMapper macht die Konvertierung
 *
 * 4. Logging:
 *    - Jeder Endpunkt loggt die Anfrage
 *    - Hilft bei Debugging und Monitoring
 *
 * 5. CORS aktivieren:
 *    - @CrossOrigin erlaubt Frontend-Zugriff
 *    - In Production: Spezifische Origins angeben
 *
 * 6. Konsistente URL-Struktur:
 *    - /api/invoices - Haupt-Ressource
 *    - /api/invoices/{id} - Einzelne Ressource
 *    - /api/invoices/{id}/items - Sub-Ressourcen
 *    - /api/invoices/{id}/cancel - Actions
 *    - /api/invoices/customer/{id} - Queries
 */