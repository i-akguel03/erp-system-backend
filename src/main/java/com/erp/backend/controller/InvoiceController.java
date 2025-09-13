package com.erp.backend.controller;

import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.Customer;
import com.erp.backend.repository.CustomerRepository;
import com.erp.backend.service.InvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

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

    // --- GET Endpoints ---

    @GetMapping
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        List<Invoice> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    //@GetMapping("/paged")
    //public ResponseEntity<Page<Invoice>> getAllInvoices(Pageable pageable) {
    //    Page<Invoice> invoices = invoiceService.getAllInvoices(pageable);
    //    return ResponseEntity.ok(invoices);
    //}

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable UUID id) {
        Optional<Invoice> invoice = invoiceService.getInvoiceById(id);
        return invoice.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<Invoice> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        Optional<Invoice> invoice = null;//invoiceService.getInvoiceByNumber(invoiceNumber);
        return invoice.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Invoice>> getInvoicesByCustomer(@PathVariable UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            return ResponseEntity.badRequest().build();
        }
        List<Invoice> invoices = invoiceService.getInvoicesByCustomer(customerId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Invoice>> getInvoicesByStatus(@PathVariable Invoice.InvoiceStatus status) {
        List<Invoice> invoices = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Invoice>> getOverdueInvoices() {
        List<Invoice> invoices =  null;//invoiceService.getOverdueInvoices(LocalDate.now());
        return ResponseEntity.ok(invoices);
    }

    // --- POST & PUT ---

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@RequestBody Invoice invoice) {
        Invoice created = invoiceService.createInvoice(invoice);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Invoice> updateInvoice(@PathVariable UUID id, @RequestBody Invoice invoice) {
        if (!id.equals(invoice.getId())) {
            return ResponseEntity.badRequest().build();
        }
        Invoice updated = invoiceService.updateInvoice(invoice);
        return ResponseEntity.ok(updated);
    }

    // --- Status-Updates ---

    @PutMapping("/{id}/pay")
    public ResponseEntity<Invoice> markAsPaid(@PathVariable UUID id) {
        Invoice invoice = invoiceService.markAsPaid(id);
        return ResponseEntity.ok(invoice);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Invoice> cancelInvoice(@PathVariable UUID id) {
        Invoice invoice = invoiceService.cancelInvoice(id);
        return ResponseEntity.ok(invoice);
    }

    // --- DELETE ---

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
