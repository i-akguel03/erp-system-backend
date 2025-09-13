package com.erp.backend.controller;

import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.InvoiceItem;
import com.erp.backend.domain.OpenItem;
import com.erp.backend.service.InvoiceService;
import com.erp.backend.repository.CustomerRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService invoiceService;
    private final CustomerRepository customerRepository;

    public InvoiceController(InvoiceService invoiceService, CustomerRepository customerRepository) {
        this.invoiceService = invoiceService;
        this.customerRepository = customerRepository;
    }

    // ========================================
    // 1. CRUD Endpoints
    // ========================================

    @GetMapping
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        List<Invoice> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable UUID id) {
        Optional<Invoice> invoice = invoiceService.getInvoiceById(id);
        return invoice.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@RequestBody Invoice invoice) {
        try {
            Invoice created = invoiceService.createInvoice(invoice);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            logger.error("Error creating invoice: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Invoice> updateInvoice(@PathVariable UUID id, @RequestBody Invoice invoice) {
        if (!id.equals(invoice.getId())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Invoice updated = invoiceService.updateInvoice(invoice);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating invoice: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        try {
            invoiceService.deleteInvoice(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.error("Cannot delete invoice: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ========================================
    // 2. Abfrage-Endpoints
    // ========================================

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Invoice>> getInvoicesByCustomer(@PathVariable UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            List<Invoice> invoices = invoiceService.getInvoicesByCustomer(customerId);
            return ResponseEntity.ok(invoices);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Invoice>> getInvoicesByStatus(@PathVariable Invoice.InvoiceStatus status) {
        List<Invoice> invoices = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<Invoice>> getInvoicesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Invoice> invoices = invoiceService.getInvoicesByDateRange(startDate, endDate);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<Invoice>> getInvoicesByBatchId(@PathVariable String batchId) {
        List<Invoice> invoices = invoiceService.getInvoicesByBatchId(batchId);
        return ResponseEntity.ok(invoices);
    }

    // ========================================
    // 3. Status-Management
    // ========================================

    @PutMapping("/{id}/status")
    public ResponseEntity<Invoice> changeStatus(
            @PathVariable UUID id,
            @RequestParam Invoice.InvoiceStatus status) {
        try {
            Invoice invoice = invoiceService.changeStatus(id, status);
            return ResponseEntity.ok(invoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Invoice> cancelInvoice(@PathVariable UUID id) {
        try {
            Invoice invoice = invoiceService.cancelInvoice(id);
            return ResponseEntity.ok(invoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/send")
    public ResponseEntity<Invoice> sendInvoice(@PathVariable UUID id) {
        try {
            Invoice invoice = invoiceService.sendInvoice(id);
            return ResponseEntity.ok(invoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========================================
    // 4. InvoiceItem-Management
    // ========================================

    @PostMapping("/{id}/items")
    public ResponseEntity<Invoice> addInvoiceItem(
            @PathVariable UUID id,
            @RequestBody InvoiceItem item) {
        try {
            Invoice invoice = invoiceService.addInvoiceItem(id, item);
            return ResponseEntity.ok(invoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{invoiceId}/items/{itemId}")
    public ResponseEntity<Invoice> removeInvoiceItem(
            @PathVariable UUID invoiceId,
            @PathVariable UUID itemId) {
        try {
            Invoice invoice = invoiceService.removeInvoiceItem(invoiceId, itemId);
            return ResponseEntity.ok(invoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========================================
    // 5. OpenItem-Integration
    // ========================================

    @PostMapping("/{id}/open-items")
    public ResponseEntity<List<OpenItem>> createOpenItemsForInvoice(@PathVariable UUID id) {
        try {
            List<OpenItem> openItems = invoiceService.createOpenItemsForInvoice(id);
            return ResponseEntity.ok(openItems);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/open-items")
    public ResponseEntity<List<OpenItem>> getOpenItemsForInvoice(@PathVariable UUID id) {
        try {
            List<OpenItem> openItems = invoiceService.getOpenItemsForInvoice(id);
            return ResponseEntity.ok(openItems);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========================================
    // 6. Gutschriften
    // ========================================

    @PostMapping("/{id}/credit-note")
    public ResponseEntity<Invoice> createCreditNote(@PathVariable UUID id) {
        try {
            Invoice creditNote = invoiceService.createCreditNote(id);
            return ResponseEntity.ok(creditNote);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========================================
    // 7. Statistiken
    // ========================================

    @GetMapping("/stats/total-amount")
    public ResponseEntity<BigDecimal> getTotalInvoiceAmount() {
        BigDecimal total = invoiceService.getTotalInvoiceAmount();
        return ResponseEntity.ok(total);
    }

    @GetMapping("/stats/count/{status}")
    public ResponseEntity<Long> getInvoiceCountByStatus(@PathVariable Invoice.InvoiceStatus status) {
        long count = invoiceService.getInvoiceCountByStatus(status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/amount/{status}")
    public ResponseEntity<BigDecimal> getInvoiceAmountByStatus(@PathVariable Invoice.InvoiceStatus status) {
        BigDecimal amount = invoiceService.getInvoiceAmountByStatus(status);
        return ResponseEntity.ok(amount);
    }
}