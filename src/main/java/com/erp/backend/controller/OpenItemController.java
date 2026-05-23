package com.erp.backend.controller;

import com.erp.backend.domain.OpenItem;
import com.erp.backend.dto.OpenItemDTO;
import com.erp.backend.mapper.OpenItemMapper;
import com.erp.backend.service.OpenItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/openitems")
@CrossOrigin
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'OPEN_ITEMS_READ')")
@Tag(name = "Offene Posten")
public class OpenItemController {

    private static final Logger logger = LoggerFactory.getLogger(OpenItemController.class);
    private final OpenItemService openItemService;

    public OpenItemController(OpenItemService openItemService) {
        this.openItemService = openItemService;
    }

    // ==============================
    // 1. CRUD
    // ==============================

    @Operation(summary = "Alle offenen Posten abrufen — optional paginiert")
    @GetMapping
    public ResponseEntity<List<OpenItemDTO>> getAllOpenItems(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        logger.info("GET /api/openitems - Fetching open items (paginated: {})", paginated);

        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            List<OpenItem> allItems = openItemService.getAllOpenItems();
            int total = allItems.size();
            int totalPages = (int) Math.ceil((double) total / size);
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            List<OpenItem> pageContent = allItems.subList(fromIndex, toIndex);

            List<OpenItemDTO> dtoList = pageContent.stream()
                    .map(OpenItemMapper::toDTO)
                    .toList();

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(total))
                    .header("X-Total-Pages", String.valueOf(totalPages))
                    .header("X-Current-Page", String.valueOf(page))
                    .header("Access-Control-Expose-Headers", "X-Total-Count, X-Total-Pages, X-Current-Page")
                    .body(dtoList);
        } else {
            List<OpenItemDTO> dtoList = openItemService.getAllOpenItems().stream()
                    .map(OpenItemMapper::toDTO)
                    .toList();
            return ResponseEntity.ok(dtoList);
        }
    }

    @Operation(summary = "Offenen Posten nach ID abrufen")
    @GetMapping("/{id}")
    public ResponseEntity<OpenItemDTO> getOpenItemById(@PathVariable UUID id) {
        return openItemService.getOpenItemById(id)
                .map(OpenItemMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Neuen offenen Posten anlegen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<OpenItemDTO> createOpenItem(@Valid @RequestBody OpenItemDTO dto) {
        OpenItem openItem = new OpenItem();
        com.erp.backend.domain.Invoice invoice = new com.erp.backend.domain.Invoice();
        invoice.setId(dto.getInvoiceId());
        openItem.setInvoice(invoice);
        openItem.setDescription(dto.getDescription());
        openItem.setAmount(dto.getAmount());
        openItem.setDueDate(dto.getDueDate());
        openItem.setNotes(dto.getNotes());

        OpenItem created = openItemService.createOpenItem(openItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(OpenItemMapper.toDTO(created));
    }

    @Operation(summary = "Offenen Posten aktualisieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<OpenItemDTO> updateOpenItem(@PathVariable UUID id, @Valid @RequestBody OpenItemDTO dto) {
        OpenItem openItem = new OpenItem();
        openItem.setId(id);
        openItem.setDescription(dto.getDescription());
        openItem.setAmount(dto.getAmount());
        openItem.setDueDate(dto.getDueDate());
        openItem.setNotes(dto.getNotes());

        com.erp.backend.domain.Invoice invoice = new com.erp.backend.domain.Invoice();
        invoice.setId(dto.getInvoiceId());
        openItem.setInvoice(invoice);

        OpenItem updated = openItemService.updateOpenItem(openItem);
        return ResponseEntity.ok(OpenItemMapper.toDTO(updated));
    }

    @Operation(summary = "Offenen Posten löschen")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOpenItem(@PathVariable UUID id) {
        openItemService.deleteOpenItem(id);
        return ResponseEntity.noContent().build();
    }

    // ==============================
    // 2. Zahlungslogik
    // ==============================

    @Operation(summary = "Zahlung auf offenen Posten erfassen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/payments")
    public ResponseEntity<OpenItemDTO> recordPayment(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String paymentReference) {

        OpenItem updated = openItemService.recordPayment(id, amount, paymentMethod, paymentReference);
        return ResponseEntity.ok(OpenItemMapper.toDTO(updated));
    }

    @Operation(summary = "Zahlung auf offenem Posten stornieren")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/payments")
    public ResponseEntity<OpenItemDTO> reversePayment(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount) {

        OpenItem updated = openItemService.reversePayment(id, amount);
        return ResponseEntity.ok(OpenItemMapper.toDTO(updated));
    }

    @Operation(summary = "Offenen Posten stornieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OpenItemDTO> cancelOpenItem(@PathVariable UUID id) {
        OpenItem updated = openItemService.cancelOpenItem(id);
        return ResponseEntity.ok(OpenItemMapper.toDTO(updated));
    }

    // ==============================
    // 3. Status Management
    // ==============================

    @Operation(summary = "Überfälligkeiten aktualisieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/update-overdue")
    public ResponseEntity<Void> updateOverdueStatus() {
        openItemService.updateOverdueStatus();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Überfällige offene Posten abrufen")
    @GetMapping("/overdue")
    public ResponseEntity<List<OpenItemDTO>> getOverdueItems() {
        List<OpenItemDTO> dtos = openItemService.getOverdueItems().stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Offene Posten nach Fälligkeitsdatum abrufen")
    @GetMapping("/due-by-date")
    public ResponseEntity<List<OpenItemDTO>> getItemsDueByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
        List<OpenItemDTO> dtos = openItemService.getItemsDueByDate(dueDate).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // ==============================
    // 4. Filtering / Queries
    // ==============================

    @Operation(summary = "Offene Posten eines Kunden abrufen")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsByCustomer(@PathVariable UUID customerId) {
        List<OpenItemDTO> dtos = openItemService.getOpenItemsByCustomer(customerId).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Nur offene Posten (Status OPEN) eines Kunden")
    @GetMapping("/customer/{customerId}/open")
    public ResponseEntity<List<OpenItemDTO>> getOpenOpenItemsByCustomer(@PathVariable UUID customerId) {
        List<OpenItemDTO> dtos = openItemService.getOpenOpenItemsByCustomer(customerId).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Offene Posten einer Rechnung abrufen")
    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsByInvoice(@PathVariable UUID invoiceId) {
        List<OpenItemDTO> dtos = openItemService.getOpenItemsByInvoice(invoiceId).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Offene Posten eines Abonnements abrufen")
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsBySubscription(@PathVariable UUID subscriptionId) {
        List<OpenItemDTO> dtos = openItemService.getOpenItemsBySubscriptionId(subscriptionId).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Offene Posten nach Abonnement-IDs abrufen")
    @GetMapping("/by-subscriptions")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsBySubscriptions(
            @RequestParam List<UUID> subscriptionIds) {
        List<OpenItemDTO> dtos = openItemService.getOpenItemsBySubscriptionIds(subscriptionIds).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Offene Posten nach Rechnungs-IDs abrufen")
    @GetMapping("/by-invoices")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsByInvoices(
            @RequestParam List<UUID> invoiceIds) {
        List<OpenItemDTO> dtos = openItemService.getOpenItemsByInvoices(invoiceIds).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Offene Posten nach Status filtern")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsByStatus(@PathVariable OpenItem.OpenItemStatus status) {
        List<OpenItemDTO> dtos = openItemService.getOpenItemsByStatus(status).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Offene Posten in Datumsbereich abrufen")
    @GetMapping("/date-range")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        List<OpenItemDTO> dtos = openItemService.getOpenItemsByDateRange(start, end).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Bezahlte offene Posten in Zeitraum abrufen")
    @GetMapping("/paid-between")
    public ResponseEntity<List<OpenItemDTO>> getItemsPaidBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        List<OpenItemDTO> dtos = openItemService.getItemsPaidBetween(start, end).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // ==============================
    // 5. Reminder Management
    // ==============================

    @Operation(summary = "Mahnung zu offenem Posten hinzufügen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reminders")
    public ResponseEntity<OpenItemDTO> addReminder(@PathVariable UUID id) {
        OpenItem updated = openItemService.addReminder(id);
        return ResponseEntity.ok(OpenItemMapper.toDTO(updated));
    }

    @Operation(summary = "Offene Posten die Mahnungen benötigen")
    @GetMapping("/reminders/needed")
    public ResponseEntity<List<OpenItemDTO>> getItemsNeedingReminder(
            @RequestParam(defaultValue = "30") int daysSinceLastReminder) {
        List<OpenItemDTO> dtos = openItemService.getItemsNeedingReminder(daysSinceLastReminder).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Offene Posten mit mehreren Mahnungen")
    @GetMapping("/reminders/multiple")
    public ResponseEntity<List<OpenItemDTO>> getItemsWithMultipleReminders(
            @RequestParam(defaultValue = "2") int minimumReminderCount) {
        List<OpenItemDTO> dtos = openItemService.getItemsWithMultipleReminders(minimumReminderCount).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // ==============================
    // 6. Statistics
    // ==============================

    @Operation(summary = "Gesamtausstehender Betrag")
    @GetMapping("/statistics/outstanding-amount")
    public ResponseEntity<BigDecimal> getTotalOutstandingAmount() {
        return ResponseEntity.ok(openItemService.getTotalOutstandingAmount());
    }

    @Operation(summary = "Gesamtbezahlter Betrag")
    @GetMapping("/statistics/paid-amount")
    public ResponseEntity<BigDecimal> getTotalPaidAmount() {
        return ResponseEntity.ok(openItemService.getTotalPaidAmount());
    }

    @Operation(summary = "Ausstehender Betrag eines Kunden")
    @GetMapping("/statistics/customer/{customerId}/outstanding")
    public ResponseEntity<BigDecimal> getOutstandingAmountByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(openItemService.getOutstandingAmountByCustomer(customerId));
    }

    @Operation(summary = "Anzahl offener Posten nach Status")
    @GetMapping("/statistics/count/{status}")
    public ResponseEntity<Long> getOpenItemCountByStatus(@PathVariable OpenItem.OpenItemStatus status) {
        return ResponseEntity.ok(openItemService.getOpenItemCountByStatus(status));
    }

    @Operation(summary = "Durchschnittsbetrag nach Status")
    @GetMapping("/statistics/average/{status}")
    public ResponseEntity<BigDecimal> getAverageAmountByStatus(@PathVariable OpenItem.OpenItemStatus status) {
        return ResponseEntity.ok(openItemService.getAverageAmountByStatus(status));
    }

    @Operation(summary = "Anzahl überfälliger offener Posten")
    @GetMapping("/statistics/overdue-count")
    public ResponseEntity<Long> getOverdueItemCount() {
        return ResponseEntity.ok(openItemService.getOverdueItemCount());
    }

    // ==============================
    // 7. Bulk Operations
    // ==============================

    @Operation(summary = "Offene Posten für mehrere Rechnungen erstellen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bulk/create-for-invoices")
    public ResponseEntity<List<OpenItemDTO>> createOpenItemsForInvoices(
            @RequestBody List<UUID> invoiceIds) {
        List<OpenItemDTO> dtos = openItemService.createOpenItemsForInvoices(invoiceIds).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(dtos);
    }
}