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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin
public class InvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // ==============================
    // 1. CRUD
    // ==============================

    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "invoiceDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logger.info("GET /api/invoices - Fetching invoices (paginated: {})", paginated);

        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Invoice> invoicePage = new PageImpl<>(invoiceService.getAllInvoices(), pageable, invoiceService.getAllInvoices().size());

            List<InvoiceDTO> dtoList = invoicePage.getContent().stream()
                    .map(InvoiceMapper::toDTO)
                    .toList();

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(invoicePage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(invoicePage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(dtoList);
        } else {
            List<InvoiceDTO> dtoList = invoiceService.getAllInvoices().stream()
                    .map(InvoiceMapper::toDTO)
                    .toList();
            return ResponseEntity.ok(dtoList);
        }
    }

    @GetMapping("/by-subscriptions")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesBySubscriptionIds(
            @RequestParam List<UUID> subscriptionIds) {
        List<InvoiceDTO> dtos = invoiceService.getInvoicesBySubscriptionIds(subscriptionIds)
                .stream().map(InvoiceMapper::toDTO).toList();
        return ResponseEntity.ok(dtos);
    }


    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getInvoiceById(@PathVariable UUID id) {
        return invoiceService.getInvoiceById(id)
                .map(InvoiceMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<InvoiceDTO> createInvoice(@RequestBody InvoiceDTO dto) {
        Invoice invoice = new Invoice();
        invoice.setCustomer(new com.erp.backend.domain.Customer(UUID.fromString(dto.getCustomerId())));
        invoice.setInvoiceDate(dto.getInvoiceDate());
        invoice.setDueDate(dto.getDueDate());
        Invoice created = invoiceService.createInvoice(invoice);
        return ResponseEntity.status(HttpStatus.CREATED).body(InvoiceMapper.toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceDTO> updateInvoice(@PathVariable UUID id, @RequestBody InvoiceDTO dto) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setInvoiceDate(dto.getInvoiceDate());
        invoice.setDueDate(dto.getDueDate());
        invoice.setCustomer(new com.erp.backend.domain.Customer(UUID.fromString(dto.getCustomerId())));
        Invoice updated = invoiceService.updateInvoice(invoice);
        return ResponseEntity.ok(InvoiceMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    // ==============================
    // 2. Status Management
    // ==============================

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<InvoiceDTO> cancelInvoice(@PathVariable UUID id) {
        Invoice invoice = invoiceService.cancelInvoice(id);
        return ResponseEntity.ok(InvoiceMapper.toDTO(invoice));
    }

    @PatchMapping("/{id}/send")
    public ResponseEntity<InvoiceDTO> sendInvoice(@PathVariable UUID id) {
        Invoice invoice = invoiceService.sendInvoice(id);
        return ResponseEntity.ok(InvoiceMapper.toDTO(invoice));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InvoiceDTO> changeStatus(@PathVariable UUID id, @RequestParam Invoice.InvoiceStatus status) {
        Invoice invoice = invoiceService.changeStatus(id, status);
        return ResponseEntity.ok(InvoiceMapper.toDTO(invoice));
    }

    // ==============================
    // 3. Items Management
    // ==============================

    @PostMapping("/{id}/items")
    public ResponseEntity<InvoiceDTO> addItem(@PathVariable UUID id, @RequestBody InvoiceItemDTO itemDTO) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        com.erp.backend.domain.InvoiceItem item = new com.erp.backend.domain.InvoiceItem();
        item.setDescription(itemDTO.getDescription());
        item.setQuantity(itemDTO.getQuantity());
        item.setUnitPrice(itemDTO.getUnitPrice());
        item.setProductCode(itemDTO.getProductCode());
        item.setProductName(itemDTO.getProductName());
        Invoice updated = invoiceService.addInvoiceItem(id, item);
        return ResponseEntity.ok(InvoiceMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<InvoiceDTO> removeItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        Invoice updated = invoiceService.removeInvoiceItem(id, itemId);
        return ResponseEntity.ok(InvoiceMapper.toDTO(updated));
    }

    // ==============================
    // 4. Filtering / Queries
    // ==============================

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByCustomer(@PathVariable UUID customerId) {
        List<InvoiceDTO> dtos = invoiceService.getInvoicesByCustomer(customerId).stream()
                .map(InvoiceMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByStatus(@PathVariable Invoice.InvoiceStatus status) {
        List<InvoiceDTO> dtos = invoiceService.getInvoicesByStatus(status).stream()
                .map(InvoiceMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        List<InvoiceDTO> dtos = invoiceService.getInvoicesByDateRange(start, end).stream()
                .map(InvoiceMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }
}
