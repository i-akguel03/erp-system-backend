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
 * Service für Invoice-Verwaltung.
 *
 * Korrigierte Architektur:
 * - Invoices werden aus DueSchedule + Subscription erstellt
 * - DueSchedule liefert Termine, Subscription liefert Preise
 * - OpenItems werden aus Invoices abgeleitet
 */
@Service
@Transactional
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final OpenItemRepository openItemRepository;
    private final DueScheduleRepository dueScheduleRepository;
    private final AtomicInteger counter = new AtomicInteger(1);

    public InvoiceService(InvoiceRepository invoiceRepository,
                          CustomerRepository customerRepository,
                          OpenItemRepository openItemRepository,
                          DueScheduleRepository dueScheduleRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.openItemRepository = openItemRepository;
        this.dueScheduleRepository = dueScheduleRepository;
    }

    // ========================================
    // 1. Manuelle Rechnungserstellung
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
    // 2. Automatische Rechnungserstellung aus DueSchedule
    // ========================================

    /**
     * Erstellt eine Rechnung aus einem DueSchedule und der zugehörigen Subscription.
     * Die Preise kommen aus der Subscription, die Termine aus dem DueSchedule.
     */
    @Transactional
    public Invoice createInvoiceFromDueSchedule(DueSchedule dueSchedule) {
        if (dueSchedule == null) {
            throw new IllegalArgumentException("DueSchedule darf nicht null sein");
        }

        Subscription subscription = dueSchedule.getSubscription();
        if (subscription == null) {
            throw new IllegalArgumentException("DueSchedule hat keine Subscription");
        }

        Contract contract = subscription.getContract();
        if (contract == null) {
            throw new IllegalArgumentException("Subscription hat keinen Contract");
        }

        Customer customer = contract.getCustomer();
        if (customer == null) {
            throw new IllegalArgumentException("Contract hat keinen Customer");
        }

        // Preis aus Subscription holen (nicht aus DueSchedule!)
        BigDecimal unitPrice = subscription.getMonthlyPrice() != null ?
                subscription.getMonthlyPrice() : BigDecimal.ZERO;

        // Rechnung erstellen
        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setBillingAddress(customer.getBillingAddress());
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(dueSchedule.getDueDate());
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setStatus(Invoice.InvoiceStatus.DRAFT);

        // InvoiceItem erstellen mit Preis aus Subscription
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(buildInvoiceItemDescription(subscription, dueSchedule));
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(unitPrice);
        item.setTaxRate(BigDecimal.valueOf(19)); // 19% MwSt

        invoice.addInvoiceItem(item);
        invoice.calculateTotals();

        Invoice saved = invoiceRepository.save(invoice);

        // DueSchedule als abgerechnet markieren
        dueSchedule.setInvoice(saved);
        dueSchedule.setProcessedForInvoicing(true);
        dueSchedule.setStatus(DueStatus.COMPLETED);
        dueScheduleRepository.save(dueSchedule);

        logger.info("Created invoice from DueSchedule: invoice={}, dueSchedule={}, amount={}",
                saved.getInvoiceNumber(), dueSchedule.getDueNumber(), unitPrice);

        return saved;
    }

    /**
     * Rechnungslauf: Verarbeitet alle fälligen DueSchedules.
     */
    @Transactional
    public List<Invoice> processBillingRun() {
        logger.info("Starte Rechnungslauf...");

        // Alle DueSchedules die zur Abrechnung bereit sind
        LocalDate today = LocalDate.now();
        List<DueSchedule> readyForBilling = dueScheduleRepository
                .findByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, today);

        List<Invoice> createdInvoices = new ArrayList<>();

        for (DueSchedule dueSchedule : readyForBilling) {
            try {
                // Prüfen ob bereits abgerechnet
                if (dueSchedule.getInvoice() != null || dueSchedule.isProcessedForInvoicing()) {
                    logger.debug("DueSchedule {} bereits abgerechnet, überspringe", dueSchedule.getDueNumber());
                    continue;
                }

                Invoice invoice = createInvoiceFromDueSchedule(dueSchedule);
                createdInvoices.add(invoice);

            } catch (Exception e) {
                logger.error("Fehler beim Verarbeiten von DueSchedule {}: {}",
                        dueSchedule.getDueNumber(), e.getMessage());
            }
        }

        logger.info("Rechnungslauf abgeschlossen: {} Rechnungen erstellt", createdInvoices.size());
        return createdInvoices;
    }

    // ========================================
    // 3. OpenItem-Verwaltung
    // ========================================

    @Transactional
    public OpenItem createOpenItem(Invoice invoice, BigDecimal amount, String description) {
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice is required");
        }

        OpenItem openItem = new OpenItem();
        openItem.setInvoice(invoice);
        openItem.setDescription(description);
        openItem.setAmount(amount);
        openItem.setDueDate(invoice.getDueDate());
        openItem.setStatus(OpenItem.OpenItemStatus.OPEN);

        OpenItem saved = openItemRepository.save(openItem);
        logger.info("Created OpenItem for invoice {}: amount={}", invoice.getInvoiceNumber(), amount);
        return saved;
    }

    @Transactional
    public OpenItem createOpenItemFromInvoiceItem(Invoice invoice, InvoiceItem invoiceItem) {
        if (invoiceItem == null) {
            throw new IllegalArgumentException("InvoiceItem is required");
        }

        BigDecimal amount = invoiceItem.getUnitPrice().multiply(invoiceItem.getQuantity());
        return createOpenItem(invoice, amount, invoiceItem.getDescription());
    }

    @Transactional
    public OpenItem markOpenItemAsPaid(UUID openItemId, LocalDate paidDate, String paymentMethod) {
        OpenItem openItem = openItemRepository.findById(openItemId)
                .orElseThrow(() -> new IllegalArgumentException("OpenItem not found: " + openItemId));

        openItem.markAsPaid(openItem.getAmount(), paymentMethod, "REF-" + System.currentTimeMillis());
        if (paidDate != null) {
            openItem.setPaidDate(paidDate);
        }

        return openItemRepository.save(openItem);
    }

    // ========================================
    // 4. Rechnungsabfragen
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

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return null; //invoiceRepository.findByInvoiceDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getOverdueInvoices() {
        LocalDate today = LocalDate.now();
        return null;//invoiceRepository.findByDueDateBeforeAndStatusIn(
                //today,
                //Arrays.asList(Invoice.InvoiceStatus.OPEN, Invoice.InvoiceStatus.PARTIALLY_PAID)
        //);
    }

    // ========================================
    // 5. Rechnungsstatus-Verwaltung
    // ========================================

    public Invoice markAsPaid(UUID invoiceId) {
        return changeStatus(invoiceId, Invoice.InvoiceStatus.PAID);
    }

    public Invoice markAsPartiallyPaid(UUID invoiceId) {
        return changeStatus(invoiceId, Invoice.InvoiceStatus.PARTIALLY_PAID);
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

        Invoice.InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(newStatus);

        Invoice saved = invoiceRepository.save(invoice);
        logger.info("Changed invoice status: id={}, oldStatus={}, newStatus={}",
                invoiceId, oldStatus, newStatus);

        return saved;
    }

    public void deleteInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        // Prüfen ob Rechnung bezahlt ist
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Paid invoices cannot be deleted");
        }

        // Zugehörige DueSchedule zurücksetzen
        //if (invoice.getDueSchedule() != null) {
        //    DueSchedule dueSchedule = invoice.getDueSchedule();
        //    dueSchedule.setInvoice(null);
        //    dueSchedule.setProcessedForInvoicing(false);
        //    dueSchedule.setStatus(DueStatus.ACTIVE);
        //    dueScheduleRepository.save(dueSchedule);
        //}

        invoiceRepository.deleteById(invoiceId);
        logger.info("Deleted invoice: id={}", invoiceId);
    }

    // ========================================
    // 6. Statistiken und Reports
    // ========================================

    @Transactional(readOnly = true)
    public BigDecimal getTotalOutstandingAmount() {
        return openItemRepository.findByStatus(OpenItem.OpenItemStatus.OPEN)
                .stream()
                .map(OpenItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public long getInvoiceCountByStatus(Invoice.InvoiceStatus status) {
        return 22222;//invoiceRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public BigDecimal getMonthlyRevenue(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        return  null;
                //invoiceRepository.findByInvoiceDateBetweenAndStatus(
                //        startDate, endDate, Invoice.InvoiceStatus.PAID)
                //.stream()
                //.map(Invoice::getTotalAmount)
                //.reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ========================================
    // 7. Hilfsmethoden
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

    private String buildInvoiceItemDescription(Subscription subscription, DueSchedule dueSchedule) {
        StringBuilder description = new StringBuilder();

        description.append("Abonnement: ");
        description.append(subscription.getProductName() != null ?
                subscription.getProductName() : "Unbekanntes Produkt");

        if (dueSchedule.getPeriodStart() != null && dueSchedule.getPeriodEnd() != null) {
            description.append(" | Periode: ");
            description.append(dueSchedule.getPeriodStart());
            description.append(" bis ");
            description.append(dueSchedule.getPeriodEnd());
        }

        return description.toString();
    }
}