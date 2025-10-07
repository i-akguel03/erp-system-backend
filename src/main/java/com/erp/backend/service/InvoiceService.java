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
 * ===============================================================================
 * INVOICE SERVICE - Zentrale Verwaltung von Rechnungen
 * ===============================================================================
 *
 * Verantwortlichkeiten:
 * - CRUD-Operationen für Rechnungen
 * - Automatische OpenItem-Verwaltung (analog zum BatchProcessor)
 * - Status-Management (DRAFT, SENT, PAID, CANCELLED)
 * - DueSchedule-Rollback bei Stornierung
 * - Gutschriften-Erstellung
 * - Statistiken und Reports
 *
 * WICHTIGES PRINZIP (vom BatchProcessor übernommen):
 * - Jede Rechnung = Ein OpenItem (automatisch erstellt)
 * - Rechnung stornieren = OpenItems stornieren + DueSchedule zurücksetzen
 * - Rechnung löschen = OpenItems löschen
 * - Konsistenz zwischen Invoice, OpenItem und DueSchedule ist IMMER gewährleistet
 *
 * Architektur:
 * - Nutzt OpenItemFactory für konsistente OpenItem-Erstellung
 * - Nutzt DueScheduleService für Rollback-Logik
 * - Transaktionale Sicherheit durch @Transactional
 * - Detailliertes Logging für Nachvollziehbarkeit
 */
@Service
@Transactional
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    // Repositories für Datenbankzugriff
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final OpenItemRepository openItemRepository;
    private final DueScheduleRepository dueScheduleRepository;

    // Factory für konsistente OpenItem-Erstellung (wie im BatchProcessor)
    private final OpenItemFactory openItemFactory;

    // Service für DueSchedule-Verwaltung (Rollback bei Stornierung)
    private final DueScheduleService dueScheduleService;

    // Zähler für Rechnungsnummern-Generierung
    private final AtomicInteger counter = new AtomicInteger(1);

    /**
     * KONSTRUKTOR - Dependency Injection
     *
     * Spring injiziert automatisch alle benötigten Dependencies.
     * - OpenItemFactory für konsistente OpenItem-Erstellung
     * - DueScheduleService für Rollback bei Stornierung
     * - DueScheduleRepository um DueSchedule über Invoice zu finden
     */
    public InvoiceService(InvoiceRepository invoiceRepository,
                          CustomerRepository customerRepository,
                          OpenItemRepository openItemRepository,
                          DueScheduleRepository dueScheduleRepository,
                          OpenItemFactory openItemFactory,
                          DueScheduleService dueScheduleService) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.openItemRepository = openItemRepository;
        this.dueScheduleRepository = dueScheduleRepository;
        this.openItemFactory = openItemFactory;
        this.dueScheduleService = dueScheduleService;
    }

    // ========================================
    // 1. CRUD-OPERATIONEN
    // ========================================

    /**
     * Erstellt eine neue Rechnung UND das zugehörige OpenItem.
     *
     * WICHTIG: Gleiche Logik wie im BatchProcessor!
     * - Rechnung wird erstellt
     * - OpenItem wird AUTOMATISCH mit erstellt
     * - Beide sind konsistent verknüpft
     *
     * Ablauf:
     * 1. Validierung der Rechnung
     * 2. Defaults setzen (Rechnungsnummer, Status)
     * 3. Beträge berechnen
     * 4. Rechnung speichern
     * 5. OpenItem automatisch erstellen (NEU!)
     *
     * @param invoice Die zu erstellende Rechnung
     * @return Die gespeicherte Rechnung mit ID
     * @throws IllegalArgumentException bei ungültigen Daten
     */
    @Transactional
    public Invoice createInvoice(Invoice invoice) {
        // SCHRITT 1: Validierung
        validateInvoice(invoice);

        // SCHRITT 2: Defaults setzen
        invoice.setId(null); // Sicherstellen dass neue ID generiert wird

        // Rechnungsnummer generieren falls nicht vorhanden
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            invoice.setInvoiceNumber(generateInvoiceNumber());
        }

        // Status auf DRAFT setzen falls nicht angegeben
        if (invoice.getStatus() == null) {
            invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
        }

        // SCHRITT 3: Beträge berechnen (Summe aller Items)
        invoice.calculateTotals();

        // SCHRITT 4: Rechnung in Datenbank speichern
        Invoice saved = invoiceRepository.save(invoice);
        logger.info("Created invoice: id={}, invoiceNumber={}, amount={}",
                saved.getId(), saved.getInvoiceNumber(), saved.getTotalAmount());

        // SCHRITT 5: OpenItem automatisch erstellen (wie im BatchProcessor!)
        createOpenItemForInvoice(saved);

        return saved;
    }

    /**
     * Aktualisiert eine bestehende Rechnung.
     *
     * HINWEIS: OpenItems werden NICHT automatisch angepasst.
     * Nur bei Betragsänderungen sollten OpenItems manuell aktualisiert werden.
     *
     * @param invoice Die zu aktualisierende Rechnung (muss ID haben)
     * @return Die aktualisierte Rechnung
     * @throws IllegalArgumentException wenn Rechnung nicht gefunden
     */
    @Transactional
    public Invoice updateInvoice(Invoice invoice) {
        // Prüfen ob Rechnung existiert
        if (invoice.getId() == null || !invoiceRepository.existsById(invoice.getId())) {
            throw new IllegalArgumentException("Invoice not found for update: " + invoice.getId());
        }

        // Beträge neu berechnen
        invoice.calculateTotals();

        // Speichern und zurückgeben
        Invoice saved = invoiceRepository.save(invoice);
        logger.info("Updated invoice: id={}, invoiceNumber={}, amount={}",
                saved.getId(), saved.getInvoiceNumber(), saved.getTotalAmount());

        return saved;
    }

    /**
     * Gibt alle Rechnungen zurück (mit InvoiceItems geladen).
     *
     * @return Liste aller Rechnungen
     */
    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAllWithItems();
    }

    /**
     * Sucht eine Rechnung anhand ihrer ID.
     *
     * @param id Die Rechnungs-ID
     * @return Optional mit Rechnung (oder leer wenn nicht gefunden)
     */
    @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceById(UUID id) {
        return invoiceRepository.findByIdWithItems(id);
    }

    /**
     * Löscht eine Rechnung UND alle zugehörigen OpenItems.
     *
     * WICHTIG: Gleiche Logik wie im BatchProcessor!
     * - Erst werden alle OpenItems gelöscht
     * - Dann wird die Rechnung gelöscht
     * - Verhindert Inkonsistenzen in der Datenbank
     *
     * SICHERHEIT:
     * - Rechnung darf keine offenen Posten (OPEN) haben
     * - Erst stornieren (cancelInvoice), dann löschen!
     *
     * @param invoiceId Die ID der zu löschenden Rechnung
     * @throws IllegalArgumentException wenn Rechnung nicht gefunden
     * @throws IllegalStateException wenn noch offene Posten existieren
     */
    @Transactional
    public void deleteInvoice(UUID invoiceId) {
        // Rechnung laden
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        // SICHERHEITSPRÜFUNG: Keine offenen Posten erlaubt
        if (hasOpenItems(invoice)) {
            throw new IllegalStateException(
                    "Invoice cannot be deleted - has open items. Cancel the invoice first.");
        }

        // SCHRITT 1: Alle zugehörigen OpenItems löschen (auch CANCELLED)
        List<OpenItem> openItems = new ArrayList<>(invoice.getOpenItems());
        for (OpenItem openItem : openItems) {
            openItemRepository.delete(openItem);
            logger.debug("Deleted OpenItem: id={} for invoice {}",
                    openItem.getId(), invoice.getInvoiceNumber());
        }

        // SCHRITT 2: Rechnung löschen
        invoiceRepository.deleteById(invoiceId);

        logger.info("✓ Deleted invoice: id={}, invoiceNumber={}, deleted {} OpenItems",
                invoiceId, invoice.getInvoiceNumber(), openItems.size());
    }

    // ========================================
    // 2. STATUS-VERWALTUNG
    // ========================================

    /**
     * Ändert den Status einer Rechnung.
     *
     * Status-Flow:
     * DRAFT → SENT → PAID
     *        ↓
     *    CANCELLED (von jedem Status aus möglich)
     *
     * @param invoiceId Die Rechnungs-ID
     * @param newStatus Der neue Status
     * @return Die aktualisierte Rechnung
     */
    @Transactional
    public Invoice changeStatus(UUID invoiceId, Invoice.InvoiceStatus newStatus) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        Invoice.InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(newStatus);

        Invoice saved = invoiceRepository.save(invoice);
        logger.info("Changed invoice status: id={}, {} → {}",
                invoiceId, oldStatus, newStatus);

        return saved;
    }

    /**
     * Findet Rechnungen zu bestimmten Subscriptions.
     *
     * @param subscriptionIds Liste von Subscription-IDs
     * @return Liste von Rechnungen
     */
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesBySubscriptionIds(List<UUID> subscriptionIds) {
        return invoiceRepository.findBySubscriptionIds(subscriptionIds);
    }

    /**
     * Storniert eine Rechnung UND alle zugehörigen Daten (kompletter Rollback).
     *
     * WICHTIG: Vollständige Rollback-Logik wie im BatchProcessor!
     * - Rechnung → Status CANCELLED
     * - Alle OpenItems mit Status OPEN → Status CANCELLED
     * - DueSchedule → Status COMPLETED → ACTIVE (kann neu abgerechnet werden!)
     *
     * Dies ist die HAUPTMETHODE für Rechnungsstornierung.
     * Sie gewährleistet Konsistenz zwischen Invoice, OpenItems und DueSchedule.
     *
     * Ablauf:
     * 1. Rechnung auf CANCELLED setzen
     * 2. Alle offenen OpenItems auf CANCELLED setzen
     * 3. DueSchedule über Invoice-ID finden und zurücksetzen
     * 4. Detailliertes Logging
     *
     * @param invoiceId Die ID der zu stornierenden Rechnung
     * @return Die stornierte Rechnung
     * @throws IllegalArgumentException wenn Rechnung nicht gefunden
     */
    @Transactional
    public Invoice cancelInvoice(UUID invoiceId) {
        // Rechnung laden
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        // SCHRITT 1: Rechnung auf CANCELLED setzen
        invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
        Invoice saved = invoiceRepository.save(invoice);

        // SCHRITT 2: Alle offenen OpenItems stornieren (wie im BatchProcessor!)
        List<OpenItem> openItems = invoice.getOpenItems();
        int cancelledOpenItemsCount = 0;

        for (OpenItem openItem : openItems) {
            // Nur OPEN Items stornieren (bereits stornierte nicht nochmal)
            if (openItem.getStatus() == OpenItem.OpenItemStatus.OPEN) {
                openItem.setStatus(OpenItem.OpenItemStatus.CANCELLED);
                openItemRepository.save(openItem);
                cancelledOpenItemsCount++;

                logger.debug("Cancelled OpenItem: id={}, amount={}",
                        openItem.getId(), openItem.getAmount());
            }
        }

        // SCHRITT 3: DueSchedule zurücksetzen (COMPLETED → ACTIVE)
        // DueSchedule über Invoice-ID finden (gibt Liste zurück)
        List<DueSchedule> dueSchedules = dueScheduleRepository.findByInvoiceId(invoice.getId());

        if (!dueSchedules.isEmpty()) {
            // Normalerweise gibt es nur einen DueSchedule pro Invoice
            // Falls mehrere existieren, alle zurücksetzen
            for (DueSchedule dueSchedule : dueSchedules) {
                try {
                    // Rollback durchführen (wie im BatchProcessor bei Fehlern!)
                    dueScheduleService.rollbackCompleted(
                            dueSchedule.getId(),
                            "Invoice cancelled: " + invoice.getInvoiceNumber()
                    );

                    logger.debug("Reset DueSchedule: id={}, dueNumber={} (COMPLETED → ACTIVE)",
                            dueSchedule.getId(), dueSchedule.getDueNumber());

                } catch (Exception e) {
                    // Fehler beim Rollback loggen, aber Stornierung nicht abbrechen
                    logger.error("Failed to reset DueSchedule {} for invoice {}: {}",
                            dueSchedule.getId(), invoice.getInvoiceNumber(), e.getMessage());
                }
            }
        } else {
            // Keine DueSchedule gefunden (z.B. manuell erstellte Rechnung)
            logger.debug("No DueSchedule found for invoice {} - skipping rollback",
                    invoice.getInvoiceNumber());
        }

        // SCHRITT 4: Detailliertes Logging (✓ Symbol wie im BatchProcessor)
        logger.info("✓ Cancelled invoice: id={}, invoiceNumber={}, cancelled {} OpenItems, reset DueSchedule",
                invoiceId, invoice.getInvoiceNumber(), cancelledOpenItemsCount);

        return saved;
    }

    /**
     * Sendet eine Rechnung (setzt Status auf SENT).
     *
     * @param invoiceId Die Rechnungs-ID
     * @return Die aktualisierte Rechnung
     */
    @Transactional
    public Invoice sendInvoice(UUID invoiceId) {
        return changeStatus(invoiceId, Invoice.InvoiceStatus.SENT);
    }

    // ========================================
    // 3. ABFRAGEN / QUERIES
    // ========================================

    /**
     * Findet alle Rechnungen eines Kunden.
     *
     * @param customerId Die Kunden-ID
     * @return Liste von Rechnungen
     * @throws IllegalArgumentException wenn Kunde nicht gefunden
     */
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        return invoiceRepository.findByCustomer(customer);
    }

    /**
     * Findet alle Rechnungen mit einem bestimmten Status.
     *
     * @param status Der gewünschte Status
     * @return Liste von Rechnungen
     */
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatus(status);
    }

    /**
     * Findet Rechnungen in einem Datumsbereich.
     *
     * TODO: Repository-Methode implementieren für bessere Performance
     *
     * @param startDate Startdatum (inklusive)
     * @param endDate Enddatum (inklusive)
     * @return Liste von Rechnungen
     */
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findAll().stream()
                .filter(invoice -> !invoice.getInvoiceDate().isBefore(startDate) &&
                        !invoice.getInvoiceDate().isAfter(endDate))
                .toList();
    }

    /**
     * Findet alle Rechnungen eines Rechnungslaufs.
     *
     * @param batchId Die Batch-ID (z.B. "BATCH-V-2025-001")
     * @return Liste von Rechnungen
     */
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByBatchId(String batchId) {
        return invoiceRepository.findAll().stream()
                .filter(invoice -> Objects.equals(invoice.getInvoiceBatchId(), batchId))
                .toList();
    }

    // ========================================
    // 4. INVOICEITEM-VERWALTUNG
    // ========================================

    /**
     * Fügt ein Item zu einer Rechnung hinzu.
     *
     * @param invoiceId Die Rechnungs-ID
     * @param item Das hinzuzufügende Item
     * @return Die aktualisierte Rechnung
     */
    @Transactional
    public Invoice addInvoiceItem(UUID invoiceId, InvoiceItem item) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        invoice.addInvoiceItem(item);
        return invoiceRepository.save(invoice);
    }

    /**
     * Entfernt ein Item von einer Rechnung.
     *
     * @param invoiceId Die Rechnungs-ID
     * @param itemId Die Item-ID
     * @return Die aktualisierte Rechnung
     */
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
    // 5. OPENITEM-INTEGRATION
    // ========================================

    /**
     * Erstellt ein OpenItem für eine Rechnung.
     *
     * PRIVATE METHODE - wird automatisch bei createInvoice() aufgerufen!
     *
     * Nutzt die OpenItemFactory (wie im BatchProcessor) für konsistente Erstellung.
     *
     * Ablauf:
     * 1. Prüfen ob bereits OpenItems existieren
     * 2. OpenItem mit Factory erstellen
     * 3. Speichern und loggen
     *
     * @param invoice Die Rechnung für die ein OpenItem erstellt werden soll
     */
    private void createOpenItemForInvoice(Invoice invoice) {
        // Doppelte Erstellung verhindern
        if (!invoice.getOpenItems().isEmpty()) {
            logger.debug("Invoice {} already has OpenItems, skipping creation",
                    invoice.getInvoiceNumber());
            return;
        }

        // OpenItem mit Factory erstellen (wie im BatchProcessor!)
        boolean isOverdue = false; // Bei manueller Erstellung nicht überfällig
        OpenItem openItem = openItemFactory.createOpenItemForInvoice(invoice, isOverdue);

        // Speichern
        OpenItem saved = openItemRepository.save(openItem);

        // Detailliertes Logging (✓ Symbol wie im BatchProcessor)
        logger.info("✓ Created OpenItem {} for invoice {} ({}€)",
                saved.getId(), invoice.getInvoiceNumber(), saved.getAmount());
    }

    /**
     * Manuelle Methode zum Erstellen von OpenItems.
     *
     * LEGACY-METHODE für Kompatibilität.
     * Normalerweise wird createOpenItemForInvoice() automatisch aufgerufen.
     *
     * Use Cases:
     * - Migration von Alt-Rechnungen ohne OpenItems
     * - Reparatur von Inkonsistenzen
     * - Externe Integrationen
     *
     * @param invoiceId Die Rechnungs-ID
     * @return Liste der erstellten OpenItems (normalerweise 1)
     */
    @Transactional
    public List<OpenItem> createOpenItemsForInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        // Warnung wenn bereits OpenItems existieren
        if (!invoice.getOpenItems().isEmpty()) {
            logger.warn("Invoice {} already has {} OpenItems",
                    invoice.getInvoiceNumber(), invoice.getOpenItems().size());
            return invoice.getOpenItems();
        }

        // OpenItem erstellen
        List<OpenItem> openItems = new ArrayList<>();
        OpenItem openItem = openItemFactory.createOpenItemForInvoice(invoice, false);
        OpenItem saved = openItemRepository.save(openItem);
        openItems.add(saved);

        logger.info("✓ Created OpenItem for invoice {}: amount={}",
                invoice.getInvoiceNumber(), invoice.getTotalAmount());

        return openItems;
    }

    /**
     * Gibt alle OpenItems einer Rechnung zurück.
     *
     * @param invoiceId Die Rechnungs-ID
     * @return Liste von OpenItems
     */
    @Transactional(readOnly = true)
    public List<OpenItem> getOpenItemsForInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        return invoice.getOpenItems();
    }

    /**
     * Prüft ob eine Rechnung offene Posten (Status OPEN) hat.
     *
     * @param invoice Die zu prüfende Rechnung
     * @return true wenn offene Posten existieren
     */
    @Transactional(readOnly = true)
    public boolean hasOpenItems(Invoice invoice) {
        return !invoice.getOpenItems().isEmpty() &&
                invoice.getOpenItems().stream()
                        .anyMatch(item -> item.getStatus() == OpenItem.OpenItemStatus.OPEN);
    }

    // ========================================
    // 6. GUTSCHRIFTEN
    // ========================================

    /**
     * Erstellt eine Gutschrift für eine Rechnung.
     *
     * WICHTIG: Die Gutschrift bekommt automatisch ein OpenItem (wie im BatchProcessor)!
     *
     * Ablauf:
     * 1. Gutschrift aus Original-Rechnung erstellen (negative Beträge)
     * 2. Rechnungsnummer generieren
     * 3. Speichern
     * 4. OpenItem automatisch erstellen
     *
     * @param originalInvoiceId Die ID der Original-Rechnung
     * @return Die erstellte Gutschrift
     */
    @Transactional
    public Invoice createCreditNote(UUID originalInvoiceId) {
        // Original-Rechnung laden
        Invoice originalInvoice = invoiceRepository.findById(originalInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Original invoice not found: " + originalInvoiceId));

        // Gutschrift erstellen (Domain-Methode macht Beträge negativ)
        Invoice creditNote = originalInvoice.createCreditNote();
        creditNote.setInvoiceNumber(generateInvoiceNumber());

        // Speichern
        Invoice saved = invoiceRepository.save(creditNote);
        logger.info("Created credit note {} for original invoice {}",
                saved.getInvoiceNumber(), originalInvoice.getInvoiceNumber());

        // OpenItem für Gutschrift erstellen (wie im BatchProcessor!)
        createOpenItemForInvoice(saved);

        return saved;
    }

    // ========================================
    // 7. STATISTIKEN
    // ========================================

    /**
     * Berechnet die Gesamtsumme aller Rechnungen.
     *
     * @return Summe aller Rechnungsbeträge
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalInvoiceAmount() {
        return invoiceRepository.findAll().stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Zählt Rechnungen mit einem bestimmten Status.
     *
     * @param status Der Status
     * @return Anzahl der Rechnungen
     */
    @Transactional(readOnly = true)
    public long getInvoiceCountByStatus(Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatus(status).size();
    }

    /**
     * Berechnet Gesamtsumme nach Status.
     *
     * @param status Der Status
     * @return Summe aller Rechnungen mit diesem Status
     */
    @Transactional(readOnly = true)
    public BigDecimal getInvoiceAmountByStatus(Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatus(status).stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ========================================
    // 8. HILFSMETHODEN
    // ========================================

    /**
     * Validiert eine Rechnung vor dem Speichern.
     *
     * Prüfungen:
     * - Kunde ist angegeben und existiert
     * - Rechnungsdatum ist gesetzt
     * - Fälligkeitsdatum ist gesetzt
     *
     * @param invoice Die zu validierende Rechnung
     * @throws IllegalArgumentException bei Validierungsfehlern
     */
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

    /**
     * Generiert eine eindeutige Rechnungsnummer.
     *
     * Format: INV-YYYY-NNNN
     * Beispiel: INV-2025-0001
     *
     * TODO: Production-ready Implementierung mit Datenbank-Sequence
     *
     * @return Die generierte Rechnungsnummer
     */
    private String generateInvoiceNumber() {
        int number = counter.getAndIncrement();
        return String.format("INV-%d-%04d", LocalDate.now().getYear(), number);
    }
}