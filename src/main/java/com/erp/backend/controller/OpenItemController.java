package com.erp.backend.controller;

import com.erp.backend.domain.OpenItem;
import com.erp.backend.dto.OpenItemDTO;
import com.erp.backend.mapper.OpenItemMapper;
import com.erp.backend.service.OpenItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
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
public class OpenItemController {

    private static final Logger logger = LoggerFactory.getLogger(OpenItemController.class);
    private final OpenItemService openItemService;

    public OpenItemController(OpenItemService openItemService) {
        this.openItemService = openItemService;
    }

    // ==============================
    // 1. CRUD
    // ==============================

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
            Pageable pageable = PageRequest.of(page, size, sort);
            List<OpenItem> allItems = openItemService.getAllOpenItems();
            Page<OpenItem> itemPage = new PageImpl<>(allItems, pageable, allItems.size());

            List<OpenItemDTO> dtoList = itemPage.getContent().stream()
                    .map(OpenItemMapper::toDTO)
                    .toList();

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(itemPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(itemPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(dtoList);
        } else {
            List<OpenItemDTO> dtoList = openItemService.getAllOpenItems().stream()
                    .map(OpenItemMapper::toDTO)
                    .toList();
            return ResponseEntity.ok(dtoList);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OpenItemDTO> getOpenItemById(@PathVariable UUID id) {
        return openItemService.getOpenItemById(id)
                .map(OpenItemMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OpenItemDTO> createOpenItem(@RequestBody OpenItemDTO dto) {
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

    @PutMapping("/{id}")
    public ResponseEntity<OpenItemDTO> updateOpenItem(@PathVariable UUID id, @RequestBody OpenItemDTO dto) {
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOpenItem(@PathVariable UUID id) {
        openItemService.deleteOpenItem(id);
        return ResponseEntity.noContent().build();
    }

    // ==============================
    // 2. Zahlungslogik
    // ==============================

    @PostMapping("/{id}/payments")
    public ResponseEntity<OpenItemDTO> recordPayment(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String paymentReference) {

        OpenItem updated = openItemService.recordPayment(id, amount, paymentMethod, paymentReference);
        return ResponseEntity.ok(OpenItemMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}/payments")
    public ResponseEntity<OpenItemDTO> reversePayment(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount) {

        OpenItem updated = openItemService.reversePayment(id, amount);
        return ResponseEntity.ok(OpenItemMapper.toDTO(updated));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OpenItemDTO> cancelOpenItem(@PathVariable UUID id) {
        OpenItem updated = openItemService.cancelOpenItem(id);
        return ResponseEntity.ok(OpenItemMapper.toDTO(updated));
    }

    // ==============================
    // 3. Status Management
    // ==============================

    @PatchMapping("/update-overdue")
    public ResponseEntity<Void> updateOverdueStatus() {
        openItemService.updateOverdueStatus();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<OpenItemDTO>> getOverdueItems() {
        List<OpenItemDTO> dtos = openItemService.getOverdueItems().stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

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

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsByCustomer(@PathVariable UUID customerId) {
        List<OpenItemDTO> dtos = openItemService.getOpenItemsByCustomer(customerId).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/customer/{customerId}/open")
    public ResponseEntity<List<OpenItemDTO>> getOpenOpenItemsByCustomer(@PathVariable UUID customerId) {
        List<OpenItemDTO> dtos = openItemService.getOpenOpenItemsByCustomer(customerId).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsByInvoice(@PathVariable UUID invoiceId) {
        List<OpenItemDTO> dtos = openItemService.getOpenItemsByInvoice(invoiceId).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsBySubscription(@PathVariable UUID subscriptionId) {
        List<OpenItemDTO> dtos = openItemService.getOpenItemsBySubscriptionId(subscriptionId).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/by-subscriptions")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsBySubscriptions(
            @RequestParam List<UUID> subscriptionIds) {
        List<OpenItemDTO> dtos = openItemService.getOpenItemsBySubscriptionIds(subscriptionIds).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/by-invoices")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsByInvoices(
            @RequestParam List<UUID> invoiceIds) {
        List<OpenItemDTO> dtos = openItemService.getOpenItemsByInvoices(invoiceIds).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsByStatus(@PathVariable OpenItem.OpenItemStatus status) {
        List<OpenItemDTO> dtos = openItemService.getOpenItemsByStatus(status).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<OpenItemDTO>> getOpenItemsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        List<OpenItemDTO> dtos = openItemService.getOpenItemsByDateRange(start, end).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

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

    @PostMapping("/{id}/reminders")
    public ResponseEntity<OpenItemDTO> addReminder(@PathVariable UUID id) {
        OpenItem updated = openItemService.addReminder(id);
        return ResponseEntity.ok(OpenItemMapper.toDTO(updated));
    }

    @GetMapping("/reminders/needed")
    public ResponseEntity<List<OpenItemDTO>> getItemsNeedingReminder(
            @RequestParam(defaultValue = "30") int daysSinceLastReminder) {
        List<OpenItemDTO> dtos = openItemService.getItemsNeedingReminder(daysSinceLastReminder).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

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

    @GetMapping("/statistics/outstanding-amount")
    public ResponseEntity<BigDecimal> getTotalOutstandingAmount() {
        return ResponseEntity.ok(openItemService.getTotalOutstandingAmount());
    }

    @GetMapping("/statistics/paid-amount")
    public ResponseEntity<BigDecimal> getTotalPaidAmount() {
        return ResponseEntity.ok(openItemService.getTotalPaidAmount());
    }

    @GetMapping("/statistics/customer/{customerId}/outstanding")
    public ResponseEntity<BigDecimal> getOutstandingAmountByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(openItemService.getOutstandingAmountByCustomer(customerId));
    }

    @GetMapping("/statistics/count/{status}")
    public ResponseEntity<Long> getOpenItemCountByStatus(@PathVariable OpenItem.OpenItemStatus status) {
        return ResponseEntity.ok(openItemService.getOpenItemCountByStatus(status));
    }

    @GetMapping("/statistics/average/{status}")
    public ResponseEntity<BigDecimal> getAverageAmountByStatus(@PathVariable OpenItem.OpenItemStatus status) {
        return ResponseEntity.ok(openItemService.getAverageAmountByStatus(status));
    }

    @GetMapping("/statistics/overdue-count")
    public ResponseEntity<Long> getOverdueItemCount() {
        return ResponseEntity.ok(openItemService.getOverdueItemCount());
    }

    // ==============================
    // 7. Bulk Operations
    // ==============================

    @PostMapping("/bulk/create-for-invoices")
    public ResponseEntity<List<OpenItemDTO>> createOpenItemsForInvoices(
            @RequestBody List<UUID> invoiceIds) {
        List<OpenItemDTO> dtos = openItemService.createOpenItemsForInvoices(invoiceIds).stream()
                .map(OpenItemMapper::toDTO)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(dtos);
    }
}