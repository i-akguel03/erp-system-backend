package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service für Invoice-Verwaltung (CRUD-fokussiert).
 *
 * Architektur:
 * - Reine CRUD-Operationen für Rechnungen
 * - Keine Rechnungslauf-Logik (wird in separatem BillingService gemacht)
 * - Fokus auf Rechnungserstellung, -aktualisierung und -abfragen
 */
@Service
@Transactional
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final OpenItemRepository openItemRepository;
    private final AtomicInteger counter = new AtomicInteger(1);

    public InvoiceService(InvoiceRepository invoiceRepository,
                          CustomerRepository customerRepository,
                          OpenItemRepository openItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.openItemRepository = openItemRepository;
    }

    // ========================================
    // 1. CRUD-Operationen
    // ========================================

    @Transactional
    public Invoice createInvoice(Invoice invoice) {
        validateInvoice(invoice);

        invoice.setId(null);
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            invoice.setInvoiceNumber(generateInvoiceNumber());
        }

        if (invoice.getStatus() == null) {
            invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
        }

        invoice.calculateTotals();

        Invoice saved = invoiceRepository.save(invoice);
        logger.info("Created invoice: id={}, invoiceNumber={}", saved.getId(), saved.getInvoiceNumber());
        return saved;
    }

    @Transactional
    public Invoice updateInvoice(Invoice invoice) {
        if (invoice.getId() == null || !invoiceRepository.existsById(invoice.getId())) {
            throw new IllegalArgumentException("Invoice not found for update: " + invoice.getId());
        }

        invoice.calculateTotals();
        Invoice saved = invoiceRepository.save(invoice);
        logger.info("Updated invoice: id={}, invoiceNumber={}", saved.getId(), saved.getInvoiceNumber());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceById(UUID id) {
        return invoiceRepository.findById(id);
    }

    @Transactional
    public void deleteInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        // Prüfen ob Rechnung storniert werden kann
        if (hasOpenItems(invoice)) {
            throw new IllegalStateException("Invoice cannot be deleted - has open items");
        }

        invoiceRepository.deleteById(invoiceId);
        logger.info("Deleted invoice: id={}", invoiceId);
    }

    // ========================================
    // 2. Status-Verwaltung
    // ========================================

    @Transactional
    public Invoice changeStatus(UUID invoiceId, Invoice.InvoiceStatus newStatus) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        Invoice.InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(newStatus);

        Invoice saved = invoiceRepository.save(invoice);
        logger.info("Changed invoice status: id={}, oldStatus={}, newStatus={}",
                invoiceId, oldStatus, newStatus);

        return saved;
    }

    @Transactional
    public Invoice cancelInvoice(UUID invoiceId) {
        return changeStatus(invoiceId, Invoice.InvoiceStatus.CANCELLED);
    }

    @Transactional
    public Invoice sendInvoice(UUID invoiceId) {
        return changeStatus(invoiceId, Invoice.InvoiceStatus.SENT);
    }

    // ========================================
    // 3. Abfragen
    // ========================================

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        return invoiceRepository.findByCustomer(customer);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        // TODO: Repository-Methode implementieren
        return invoiceRepository.findAll().stream()
                .filter(invoice -> !invoice.getInvoiceDate().isBefore(startDate) &&
                        !invoice.getInvoiceDate().isAfter(endDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByBatchId(String batchId) {
        return invoiceRepository.findAll().stream()
                .filter(invoice -> Objects.equals(invoice.getInvoiceBatchId(), batchId))
                .toList();
    }

    // ========================================
    // 4. InvoiceItem-Verwaltung
    // ========================================

    @Transactional
    public Invoice addInvoiceItem(UUID invoiceId, InvoiceItem item) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        invoice.addInvoiceItem(item);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice removeInvoiceItem(UUID invoiceId, UUID itemId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        InvoiceItem itemToRemove = invoice.getInvoiceItems().stream()
                .filter(item -> Objects.equals(item.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("InvoiceItem not found: " + itemId));

        invoice.removeInvoiceItem(itemToRemove);
        return invoiceRepository.save(invoice);
    }

    // ========================================
    // 5. OpenItem-Integration
    // ========================================

    @Transactional
    public List<OpenItem> createOpenItemsForInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        List<OpenItem> openItems = new ArrayList<>();

        // Ein OpenItem für die gesamte Rechnung erstellen
        OpenItem openItem = new OpenItem();
        openItem.setInvoice(invoice);
        openItem.setDescription("Rechnung " + invoice.getInvoiceNumber());
        openItem.setAmount(invoice.getTotalAmount());
        openItem.setDueDate(invoice.getDueDate());
        openItem.setStatus(OpenItem.OpenItemStatus.OPEN);

        OpenItem saved = openItemRepository.save(openItem);
        openItems.add(saved);

        logger.info("Created OpenItem for invoice {}: amount={}",
                invoice.getInvoiceNumber(), invoice.getTotalAmount());

        return openItems;
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getOpenItemsForInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        return invoice.getOpenItems();
    }

    @Transactional(readOnly = true)
    public boolean hasOpenItems(Invoice invoice) {
        return !invoice.getOpenItems().isEmpty() &&
                invoice.getOpenItems().stream()
                        .anyMatch(item -> item.getStatus() == OpenItem.OpenItemStatus.OPEN);
    }

    // ========================================
    // 6. Gutschriften
    // ========================================

    @Transactional
    public Invoice createCreditNote(UUID originalInvoiceId) {
        Invoice originalInvoice = invoiceRepository.findById(originalInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Original invoice not found: " + originalInvoiceId));

        Invoice creditNote = originalInvoice.createCreditNote();
        creditNote.setInvoiceNumber(generateInvoiceNumber());

        Invoice saved = invoiceRepository.save(creditNote);
        logger.info("Created credit note {} for original invoice {}",
                saved.getInvoiceNumber(), originalInvoice.getInvoiceNumber());

        return saved;
    }

    // ========================================
    // 7. Statistiken
    // ========================================

    @Transactional(readOnly = true)
    public BigDecimal getTotalInvoiceAmount() {
        return invoiceRepository.findAll().stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public long getInvoiceCountByStatus(Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatus(status).size();
    }

    @Transactional(readOnly = true)
    public BigDecimal getInvoiceAmountByStatus(Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatus(status).stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ========================================
    // 8. Hilfsmethoden
    // ========================================

    private void validateInvoice(Invoice invoice) {
        if (invoice.getCustomer() == null || invoice.getCustomer().getId() == null) {
            throw new IllegalArgumentException("Customer is required for invoice");
        }
        if (!customerRepository.existsById(invoice.getCustomer().getId())) {
            throw new IllegalArgumentException("Customer not found: " + invoice.getCustomer().getId());
        }
        if (invoice.getInvoiceDate() == null) {
            throw new IllegalArgumentException("Invoice date is required");
        }
        if (invoice.getDueDate() == null) {
            throw new IllegalArgumentException("Due date is required");
        }
    }

    private String generateInvoiceNumber() {
        int number = counter.getAndIncrement();
        return String.format("INV-%d-%04d", LocalDate.now().getYear(), number);
    }
}