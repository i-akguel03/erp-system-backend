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
    // 1. Rechnungen erzeugen (kompatibel)
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

    // ========================================
    // 2. OpenItems erzeugen
    // ========================================
    @Transactional
    public OpenItem createOpenItem(Invoice invoice, InvoiceItem invoiceItem) {
        if (invoice == null || invoiceItem == null) {
            throw new IllegalArgumentException("Invoice and InvoiceItem are required");
        }

        OpenItem openItem = new OpenItem();
        openItem.setInvoice(invoice);
        openItem.setDescription(invoiceItem.getDescription());
        openItem.setAmount(invoiceItem.getUnitPrice().multiply(invoiceItem.getQuantity()));
        openItem.setDueDate(invoice.getDueDate());
        openItem.setStatus(OpenItem.OpenItemStatus.OPEN);

        OpenItem saved = openItemRepository.save(openItem);
        logger.info("Created OpenItem for invoice: {}", invoice.getInvoiceNumber());
        return saved;
    }

    @Transactional
    public OpenItem markOpenItemAsPaid(OpenItem openItem, LocalDate paidDate) {
        if (openItem == null) throw new IllegalArgumentException("OpenItem is required");

        openItem.markAsPaid(openItem.getAmount(), "SEPA-Lastschrift", "REF-" + System.currentTimeMillis());
        if (paidDate != null) {
            openItem.setPaidDate(paidDate);
        }

        return openItemRepository.save(openItem);
    }

    // ========================================
    // 3. Rechnungen aus DueSchedules
    // ========================================
    @Transactional
    public Invoice createInvoiceFromDueSchedule(DueSchedule dueSchedule) {
        if (dueSchedule == null) {
            throw new IllegalArgumentException("DueSchedule darf nicht null sein");
        }
        if (dueSchedule.getSubscription() == null) {
            throw new IllegalArgumentException("DueSchedule hat keine Subscription");
        }
        if (dueSchedule.getSubscription().getContract() == null) {
            throw new IllegalArgumentException("Subscription hat keinen Contract");
        }
        if (dueSchedule.getSubscription().getContract().getCustomer() == null) {
            throw new IllegalArgumentException("Contract hat keinen Customer");
        }

        // Amount absichern
        BigDecimal amount = dueSchedule.getAmount();
        if (amount == null) {
            logger.warn("Amount ist null für DueSchedule {}, verwende 0.00", dueSchedule.getDueNumber());
            amount = BigDecimal.ZERO;
        }

        Customer customer = dueSchedule.getSubscription().getContract().getCustomer();
        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setBillingAddress(customer.getBillingAddress());
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(
                dueSchedule.getDueDate() != null ? dueSchedule.getDueDate() : LocalDate.now().plusDays(14)
        );
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setStatus(Invoice.InvoiceStatus.DRAFT);

        // InvoiceItem sicher erstellen
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(
                "Abonnement: " +
                        (dueSchedule.getSubscription().getProductName() != null
                                ? dueSchedule.getSubscription().getProductName()
                                : "Unbekanntes Produkt") +
                        " | Periode: " + dueSchedule.getPeriodStart() + " bis " + dueSchedule.getPeriodEnd()
        );
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(amount);
        item.setTaxRate(BigDecimal.valueOf(19));

        invoice.addInvoiceItem(item);

        // Totale neu berechnen (bereits null-safe in calculateTotals)
        invoice.calculateTotals();

        invoiceRepository.save(invoice);

        // OpenItem erstellen (hier könnte auch ein Null-Check für invoice & item sinnvoll sein)
        createOpenItem(invoice, item);

        // DueSchedule als verarbeitet markieren
        dueSchedule.setInvoice(invoice);
        dueSchedule.setProcessedForInvoicing(true);

        return invoice;
    }


    // ========================================
    // 4. Rechnungen nach Status / Kunde
    // ========================================
    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceById(UUID id) {
        return invoiceRepository.findById(id);
    }

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

    // ========================================
    // 5. Status ändern
    // ========================================
    public Invoice markAsPaid(UUID invoiceId) {
        return changeStatus(invoiceId, Invoice.InvoiceStatus.PAID);
    }

    public Invoice cancelInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Paid invoices cannot be cancelled");
        }

        invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
        return invoiceRepository.save(invoice);
    }

    public Invoice changeStatus(UUID invoiceId, Invoice.InvoiceStatus newStatus) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        invoice.setStatus(newStatus);
        return invoiceRepository.save(invoice);
    }

    public void deleteInvoice(UUID invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        }
        invoiceRepository.deleteById(invoiceId);
    }

    // ========================================
    // 6. Hilfsmethoden
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
