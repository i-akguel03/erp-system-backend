package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service für OpenItem-Verwaltung (CRUD + Zahlungslogik).
 *
 * Architektur:
 * - CRUD-Operationen für offene Posten
 * - Zahlungslogik (recordPayment, reversePayment)
 * - Status-Updates und Überfälligkeits-Prüfung
 * - Mahnung-Management
 * - Statistiken und Berichte
 */
@Service
@Transactional
public class OpenItemService {

    private static final Logger logger = LoggerFactory.getLogger(OpenItemService.class);

    private final OpenItemRepository openItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private BuchhaltungService buchhaltungService;

    public OpenItemService(OpenItemRepository openItemRepository,
                           InvoiceRepository invoiceRepository,
                           CustomerRepository customerRepository,
                           @org.springframework.beans.factory.annotation.Autowired(required = false)
                           BuchhaltungService buchhaltungService) {
        this.openItemRepository = openItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.buchhaltungService = buchhaltungService;
    }

    // ========================================
    // 1. CRUD-Operationen
    // ========================================

    @Transactional
    public OpenItem createOpenItem(OpenItem openItem) {
        validateOpenItem(openItem);

        openItem.setId(null);
        if (openItem.getStatus() == null) {
            openItem.setStatus(OpenItem.OpenItemStatus.OPEN);
        }
        if (openItem.getPaidAmount() == null) {
            openItem.setPaidAmount(BigDecimal.ZERO);
        }

        OpenItem saved = openItemRepository.save(openItem);
        logger.info("Created OpenItem: id={}, amount={}, invoice={}",
                saved.getId(), saved.getAmount(), saved.getInvoice().getInvoiceNumber());
        return saved;
    }

    @Transactional
    public OpenItem updateOpenItem(OpenItem openItem) {
        if (openItem.getId() == null || !openItemRepository.existsById(openItem.getId())) {
            throw new ResourceNotFoundException("Offener Posten nicht gefunden für Update: " + openItem.getId());
        }

        OpenItem saved = openItemRepository.save(openItem);
        logger.info("Updated OpenItem: id={}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getOpenItemsBySubscriptionId(UUID subscriptionId) {
        return openItemRepository.findBySubscriptionIdWithInvoiceAndCustomer(subscriptionId);
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getOpenItemsBySubscriptionIds(List<UUID> subscriptionIds) {
        return openItemRepository.findBySubscriptionIdsWithInvoiceAndCustomer(subscriptionIds);
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getAllOpenItems() {
        return openItemRepository.findAllWithInvoiceAndCustomer();
    }

    @Transactional(readOnly = true)
    public Page<OpenItem> getAllOpenItems(Pageable pageable) {
        return openItemRepository.findAllWithInvoiceAndCustomerPaged(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<OpenItem> getOpenItemById(UUID id) {
        OpenItem openItem = openItemRepository.findByIdWithInvoiceAndCustomer(id);
        return Optional.ofNullable(openItem);
    }

    @Transactional
    public void deleteOpenItem(UUID openItemId) {
        OpenItem openItem = openItemRepository.findById(openItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Offener Posten nicht gefunden: " + openItemId));

        if (openItem.getStatus() == OpenItem.OpenItemStatus.PAID ||
                openItem.getStatus() == OpenItem.OpenItemStatus.PARTIALLY_PAID) {
            throw new BusinessLogicException("Offener Posten mit Zahlungen kann nicht gelöscht werden");
        }

        openItemRepository.deleteById(openItemId);
        logger.info("Deleted OpenItem: id={}", openItemId);
    }

    // ========================================
    // 2. Zahlungslogik
    // ========================================

    @Transactional
    public OpenItem recordPayment(UUID openItemId, BigDecimal paymentAmount,
                                  String paymentMethod, String paymentReference) {
        OpenItem openItem = openItemRepository.findById(openItemId)
                .orElseThrow(() -> new IllegalArgumentException("OpenItem not found: " + openItemId));

        openItem.recordPayment(paymentAmount, paymentMethod, paymentReference);

        OpenItem saved = openItemRepository.save(openItem);
        logger.info("Recorded payment: openItemId={}, amount={}, method={}, newStatus={}",
                openItemId, paymentAmount, paymentMethod, saved.getStatus());

        // GL-Buchung: Zahlungseingang
        if (buchhaltungService != null) {
            buchhaltungService.bucheZahlungseingang(saved, paymentAmount);
        }

        return saved;
    }

    @Transactional
    public OpenItem reversePayment(UUID openItemId, BigDecimal reversalAmount) {
        OpenItem openItem = openItemRepository.findById(openItemId)
                .orElseThrow(() -> new IllegalArgumentException("OpenItem not found: " + openItemId));

        openItem.reversePayment(reversalAmount);

        OpenItem saved = openItemRepository.save(openItem);
        logger.info("Reversed payment: openItemId={}, amount={}, newStatus={}",
                openItemId, reversalAmount, saved.getStatus());

        return saved;
    }

    @Transactional
    public OpenItem cancelOpenItem(UUID openItemId) {
        OpenItem openItem = openItemRepository.findById(openItemId)
                .orElseThrow(() -> new IllegalArgumentException("OpenItem not found: " + openItemId));

        openItem.cancel();

        OpenItem saved = openItemRepository.save(openItem);
        logger.info("Cancelled OpenItem: id={}", openItemId);

        return saved;
    }

    // ========================================
    // 3. Status-Management
    // ========================================

    @Transactional
    public void updateOverdueStatus() {
        List<OpenItem> openItems = openItemRepository.findAllOpenItems();
        LocalDate today = LocalDate.now();

        int updatedCount = 0;
        for (OpenItem item : openItems) {
            if (item.getDueDate().isBefore(today) &&
                    item.getStatus() == OpenItem.OpenItemStatus.OPEN) {

                item.setStatus(OpenItem.OpenItemStatus.OVERDUE);
                openItemRepository.save(item);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            logger.info("Updated {} OpenItems to OVERDUE status", updatedCount);
        }
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getOverdueItems() {
        return openItemRepository.findOverdueItems(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getItemsDueByDate(LocalDate dueDate) {
        return openItemRepository.findItemsDueByDate(dueDate);
    }

    // ========================================
    // 4. Abfragen nach verschiedenen Kriterien
    // ========================================

    @Transactional(readOnly = true)
    public List<OpenItem> getOpenItemsByCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Kunde nicht gefunden: " + customerId);
        }
        return openItemRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getOpenOpenItemsByCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Kunde nicht gefunden: " + customerId);
        }
        return openItemRepository.findOpenItemsByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getOpenItemsByInvoice(UUID invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("Rechnung nicht gefunden: " + invoiceId);
        }
        return openItemRepository.findByInvoiceId(invoiceId);
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getOpenItemsByInvoices(List<UUID> invoiceIds) {
        return openItemRepository.findByInvoiceIds(invoiceIds);
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getOpenItemsByStatus(OpenItem.OpenItemStatus status) {
        return openItemRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getOpenItemsByDateRange(LocalDate startDate, LocalDate endDate) {
        return openItemRepository.findByDueDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getItemsPaidBetween(LocalDate startDate, LocalDate endDate) {
        return openItemRepository.findItemsPaidBetween(startDate, endDate);
    }

    // ========================================
    // 5. Mahnung-Management
    // ========================================

    @Transactional
    public OpenItem addReminder(UUID openItemId) {
        OpenItem openItem = openItemRepository.findById(openItemId)
                .orElseThrow(() -> new IllegalArgumentException("OpenItem not found: " + openItemId));

        openItem.addReminder();

        OpenItem saved = openItemRepository.save(openItem);
        logger.info("Added reminder to OpenItem: id={}, reminderCount={}",
                openItemId, saved.getReminderCount());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getItemsNeedingReminder(int daysSinceLastReminder) {
        LocalDate reminderThreshold = LocalDate.now().minusDays(daysSinceLastReminder);
        return openItemRepository.findItemsNeedingReminder(reminderThreshold);
    }

    @Transactional(readOnly = true)
    public List<OpenItem> getItemsWithMultipleReminders(int minimumReminderCount) {
        return openItemRepository.findByReminderCountGreaterThan(minimumReminderCount);
    }

    // ========================================
    // 6. Statistiken
    // ========================================

    @Transactional(readOnly = true)
    public BigDecimal getTotalOutstandingAmount() {
        BigDecimal total = openItemRepository.getTotalOutstandingAmount();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPaidAmount() {
        BigDecimal total = openItemRepository.getTotalPaidAmount();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getOutstandingAmountByCustomer(UUID customerId) {
        BigDecimal amount = openItemRepository.getOutstandingAmountByCustomer(customerId);
        return amount != null ? amount : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public long getOpenItemCountByStatus(OpenItem.OpenItemStatus status) {
        return openItemRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageAmountByStatus(OpenItem.OpenItemStatus status) {
        BigDecimal average = openItemRepository.getAverageAmountByStatus(status);
        return average != null ? average : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public long getOverdueItemCount() {
        return openItemRepository.countOverdueItems(LocalDate.now());
    }

    // ========================================
    // 7. Hilfsmethoden
    // ========================================

    private void validateOpenItem(OpenItem openItem) {
        if (openItem.getInvoice() == null || openItem.getInvoice().getId() == null) {
            throw new BusinessLogicException("Rechnung ist für einen offenen Posten erforderlich");
        }
        if (!invoiceRepository.existsById(openItem.getInvoice().getId())) {
            throw new ResourceNotFoundException("Rechnung nicht gefunden: " + openItem.getInvoice().getId());
        }
        if (openItem.getAmount() == null || openItem.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessLogicException("Betrag muss positiv sein");
        }
        if (openItem.getDueDate() == null) {
            throw new BusinessLogicException("Fälligkeitsdatum ist erforderlich");
        }
        if (openItem.getDescription() == null || openItem.getDescription().trim().isEmpty()) {
            throw new BusinessLogicException("Beschreibung ist erforderlich");
        }
    }

    /**
     * Erstellt automatisch OpenItems für alle versendeten Rechnungen ohne bestehende OpenItems
     */
    @Transactional
    public List<OpenItem> createOpenItemsForInvoicesWithoutOpenItems() {
        List<Invoice> invoicesWithoutOpenItems = invoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getOpenItems().isEmpty())
                .filter(invoice -> invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        List<OpenItem> createdItems = new ArrayList<>();

        for (Invoice invoice : invoicesWithoutOpenItems) {
            try {
                OpenItem openItem = new OpenItem(invoice,
                        "Rechnung " + invoice.getInvoiceNumber(),
                        invoice.getTotalAmount(),
                        invoice.getDueDate());

                OpenItem saved = openItemRepository.save(openItem);
                createdItems.add(saved);

                logger.info("OpenItem automatisch erstellt für Rechnung {}: {} EUR",
                        invoice.getInvoiceNumber(), invoice.getTotalAmount());

            } catch (Exception e) {
                logger.error("Fehler beim automatischen Erstellen des OpenItems für Rechnung {}: {}",
                        invoice.getInvoiceNumber(), e.getMessage());
            }
        }

        logger.info("Automatisch {} OpenItems für Rechnungen ohne OpenItems erstellt", createdItems.size());
        return createdItems;
    }

    // ========================================
    // 8. Bulk-Operationen
    // ========================================

    @Transactional
    public List<OpenItem> createOpenItemsForInvoices(List<UUID> invoiceIds) {
        List<OpenItem> createdItems = new ArrayList<>();

        for (UUID invoiceId : invoiceIds) {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

            // Prüfen ob bereits OpenItems existieren
            List<OpenItem> existingItems = openItemRepository.findByInvoiceId(invoiceId);
            if (existingItems.isEmpty()) {
                OpenItem openItem = new OpenItem(invoice,
                        "Rechnung " + invoice.getInvoiceNumber(),
                        invoice.getTotalAmount(),
                        invoice.getDueDate());

                OpenItem saved = openItemRepository.save(openItem);
                createdItems.add(saved);
            }
        }

        logger.info("Created {} OpenItems for {} invoices", createdItems.size(), invoiceIds.size());
        return createdItems;
    }
}