package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.repository.CustomerRepository;
import com.erp.backend.repository.InvoiceRepository;
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
    private final AtomicInteger counter = new AtomicInteger(1);

    public InvoiceService(InvoiceRepository invoiceRepository, CustomerRepository customerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public void initTestInvoicesFromSubscriptions() {
        if (invoiceRepository.count() > 0) return; // nur einmal

        List<Customer> customers = customerRepository.findAll();
        Random random = new Random();

        for (int i = 1; i <= 40; i++) { // 40 Rechnungen
            Customer customer = customers.get(random.nextInt(customers.size()));
            if (customer.getBillingAddress() == null) continue; // Kunde hat keine Rechnungsadresse

            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber("INV-" + String.format("%04d", i));
            invoice.setCustomer(customer);
            invoice.setInvoiceDate(LocalDate.now().minusDays(random.nextInt(30)));
            invoice.setDueDate(invoice.getInvoiceDate().plusDays(30));
            invoice.setStatus(Invoice.InvoiceStatus.DRAFT);

            // Rechnungsadresse vom Kunden übernehmen
            invoice.setBillingAddress(customer.getBillingAddress());

            // Alle Subscriptions des Kunden sammeln
            List<Subscription> subscriptions = customer.getContracts().stream()
                    .flatMap(contract -> contract.getSubscriptions().stream())
                    .filter(sub -> sub.getSubscriptionStatus() == SubscriptionStatus.ACTIVE)
                    .toList();

            if (subscriptions.isEmpty()) continue; // kein Abo vorhanden, nächste Rechnung

            // 1–3 Subscription-Items pro Rechnung
            int itemCount = 1 + random.nextInt(Math.min(3, subscriptions.size()));
            List<Subscription> chosenSubs = new ArrayList<>();
            for (int j = 0; j < itemCount; j++) {
                Subscription sub;
                do {
                    sub = subscriptions.get(random.nextInt(subscriptions.size()));
                } while (chosenSubs.contains(sub)); // keine Duplikate
                chosenSubs.add(sub);

                InvoiceItem item = new InvoiceItem();
                item.setDescription(sub.getProductName());
                item.setQuantity(BigDecimal.ONE);
                item.setUnitPrice(sub.getMonthlyPrice());
                item.setInvoice(invoice);
                item.setPosition(j + 1);
                item.setTaxRate(BigDecimal.valueOf(19)); // generischer Steuersatz
                invoice.addInvoiceItem(item);
            }

            invoice.calculateTotals();
            invoiceRepository.save(invoice);
        }

        logger.info("40 Test-Invoices basierend auf Subscriptions erstellt (Rechnungsadresse vom Kunden).");
    }


    // --- READ ---

    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        List<Invoice> invoices = invoiceRepository.findAll();
        logger.info("Fetched {} invoices", invoices.size());
        return invoices;
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceById(UUID id) {
        Optional<Invoice> invoice = invoiceRepository.findById(id);
        logger.info(invoice.isPresent() ? "Found invoice id={}" : "No invoice found id={}", id);
        return invoice;
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceByNumber(String invoiceNumber) {
        Optional<Invoice> invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber);
        logger.info(invoice.isPresent() ? "Found invoice number={}" : "No invoice found number={}", invoiceNumber);
        return invoice;
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        List<Invoice> invoices = invoiceRepository.findByCustomer(customer);
        logger.info("Found {} invoices for customer {}", invoices.size(), customerId);
        return invoices;
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status) {
        List<Invoice> invoices = invoiceRepository.findByStatus(status);
        logger.info("Found {} invoices with status {}", invoices.size(), status);
        return invoices;
    }

    @Transactional(readOnly = true)
    public List<Invoice> getOverdueInvoices(LocalDate now) {
        List<Invoice> invoices = invoiceRepository.findByDueDateBeforeAndStatusNotIn(
                LocalDate.now(),
                List.of(Invoice.InvoiceStatus.PAID, Invoice.InvoiceStatus.CANCELLED)
        );
        logger.info("Found {} overdue invoices", invoices.size());
        return invoices;
    }

    // --- WRITE ---

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

    public Invoice updateInvoice(Invoice invoice) {
        if (invoice.getId() == null || !invoiceRepository.existsById(invoice.getId())) {
            throw new IllegalArgumentException("Invoice not found for update: " + invoice.getId());
        }

        invoice.calculateTotals();
        Invoice saved = invoiceRepository.save(invoice);
        logger.info("Updated invoice: id={}, invoiceNumber={}", saved.getId(), saved.getInvoiceNumber());
        return saved;
    }

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
        Invoice saved = invoiceRepository.save(invoice);
        logger.info("Cancelled invoice: id={}, invoiceNumber={}", saved.getId(), saved.getInvoiceNumber());
        return saved;
    }

    public Invoice changeStatus(UUID invoiceId, Invoice.InvoiceStatus newStatus) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        invoice.setStatus(newStatus);
        Invoice saved = invoiceRepository.save(invoice);
        logger.info("Changed invoice status: id={}, invoiceNumber={}, newStatus={}",
                saved.getId(), saved.getInvoiceNumber(), newStatus);
        return saved;
    }

    public void deleteInvoice(UUID invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        }
        invoiceRepository.deleteById(invoiceId);
        logger.info("Deleted invoice: id={}", invoiceId);
    }

    // --- PRIVATE HELPER ---

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
