package com.erp.backend.service.init;

import com.erp.backend.domain.*;
import com.erp.backend.repository.*;
import com.erp.backend.service.VorgangService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * =====================================================================================
 * DATA MANAGEMENT UTILITIES SERVICE
 * =====================================================================================
 *
 * Diese Service-Klasse bietet umfassende Utilities für das Management von Testdaten
 * und die Wartung der Datenkonsistenz im ERP-System.
 *
 * HAUPTFUNKTIONALITÄTEN:
 * 1. DATENBEREINIGUNG:    Vollständiges oder partielles Löschen von Testdaten
 * 2. KONSISTENZREPARATUR: Automatische Behebung von Dateninkonsistenzen
 * 3. MAINTENANCE:         Regelmäßige Wartungsaufgaben
 * 4. VALIDIERUNG:         Überprüfung der Datenintegrität
 *
 * DESIGN-PRINZIPIEN:
 * - Transactional Safety: Alle Operationen sind transaktional abgesichert
 * - Dependency Order:     Löschung erfolgt in korrekter Abhängigkeitsreihenfolge
 * - Error Recovery:       Robuste Fehlerbehandlung mit Rollback-Mechanismen
 * - Audit Trail:          Vollständige Protokollierung über VorgangService
 * - Modular Design:       Einzelne Operationen können isoliert ausgeführt werden
 *
 * VERWENDUNG IN VERSCHIEDENEN KONTEXTEN:
 * - Development:          Schnelle Datenbereinigung zwischen Tests
 * - Testing:             Isolierte Testumgebungen schaffen
 * - Production Support:   Wartung und Konsistenzprüfungen
 * - Data Migration:       Bereinigung vor/nach Migrationen
 */
@Service
@Transactional
public class DataManagementUtils {

    private static final Logger logger = LoggerFactory.getLogger(DataManagementUtils.class);

    // =====================================================================================
    // DEPENDENCY INJECTION - REPOSITORIES
    // =====================================================================================

    /**
     * Repository-Dependencies für alle Entitäten des ERP-Systems.
     * Diese werden in der Reihenfolge der Abhängigkeiten organisiert:
     *
     * ABHÄNGIGKEITSHIERARCHIE (von abhängig zu unabhängig):
     * OpenItem -> Invoice -> DueSchedule -> Subscription -> Contract -> Customer/Product -> Address
     *
     * RATIONALE:
     * - OpenItems hängen von Invoices ab
     * - Invoices können von Subscriptions abhängen
     * - Subscriptions hängen von Contracts ab
     * - Contracts hängen von Customers ab
     * - Customers hängen von Addresses ab
     * - Products sind relativ unabhängig, werden aber von Subscriptions referenziert
     */

    @PersistenceContext
    private EntityManager entityManager;

    // Abhängige Entitäten (müssen zuerst gelöscht werden)
    private final OpenItemRepository openItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final DueScheduleRepository dueScheduleRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final NotificationRepository notificationRepository;
    private final InventoryRepository inventoryRepository;

    // Basis-Entitäten (können später gelöscht werden)
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;

    // =====================================================================================
    // DEPENDENCY INJECTION - SERVICES
    // =====================================================================================

    /**
     * Service-Dependencies für erweiterte Funktionalitäten:
     * - VorgangService:        Audit-Trail und Transaktionsmanagement
     * - DataStatusReporter:    Status-Reporting und Konsistenzprüfungen
     * - MasterDataInitializer: Benutzer nach Löschung neu anlegen
     */
    private final VorgangService vorgangService;
    private final DataStatusReporter dataStatusReporter;
    private final MasterDataInitializer masterDataInitializer;
    private final com.erp.backend.repository.VorgangRepository vorgangRepository;

    // =====================================================================================
    // KONSTRUKTOR UND DEPENDENCY INJECTION
    // =====================================================================================

    /**
     * Konstruktor mit vollständiger Dependency Injection.
     *
     * DESIGN-ENTSCHEIDUNG: Konstruktor-Injection statt Field-Injection
     * VORTEILE:
     * - Explizite Abhängigkeiten sichtbar
     * - Bessere Testbarkeit
     * - Immutable Dependencies nach Konstruktion
     * - Spring Boot Best Practices
     *
     * @param openItemRepository      Repository für offene Posten
     * @param invoiceRepository       Repository für Rechnungen
     * @param dueScheduleRepository   Repository für Fälligkeitspläne
     * @param subscriptionRepository  Repository für Abonnements
     * @param contractRepository      Repository für Verträge
     * @param customerRepository      Repository für Kunden
     * @param productRepository       Repository für Produkte
     * @param addressRepository       Repository für Adressen
     * @param vorgangService          Service für Vorgangsmanagement
     * @param dataStatusReporter      Service für Status-Reporting
     */
    public DataManagementUtils(OpenItemRepository openItemRepository,
                               InvoiceRepository invoiceRepository,
                               DueScheduleRepository dueScheduleRepository,
                               SubscriptionRepository subscriptionRepository,
                               ContractRepository contractRepository,
                               PaymentRepository paymentRepository,
                               OrderRepository orderRepository,
                               NotificationRepository notificationRepository,
                               InventoryRepository inventoryRepository,
                               CustomerRepository customerRepository,
                               ProductRepository productRepository,
                               AddressRepository addressRepository,
                               VorgangService vorgangService,
                               DataStatusReporter dataStatusReporter,
                               MasterDataInitializer masterDataInitializer,
                               com.erp.backend.repository.VorgangRepository vorgangRepository) {
        this.openItemRepository = openItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.dueScheduleRepository = dueScheduleRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.notificationRepository = notificationRepository;
        this.inventoryRepository = inventoryRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.vorgangService = vorgangService;
        this.dataStatusReporter = dataStatusReporter;
        this.masterDataInitializer = masterDataInitializer;
        this.vorgangRepository = vorgangRepository;
    }

    // =====================================================================================
    // HAUPTFUNKTIONALITÄTEN - DATENBEREINIGUNG
    // =====================================================================================

    /**
     * VOLLSTÄNDIGE DATENBEREINIGUNG
     *
     * Löscht ALLE Testdaten aus dem System in der korrekten Abhängigkeitsreihenfolge.
     *
     * ANWENDUNGSFÄLLE:
     * - Kompletter Reset einer Entwicklungsumgebung
     * - Vorbereitung für neue Testdatensets
     * - Bereinigung nach fehlgeschlagenen Tests
     *
     * SICHERHEITSMECHANISMEN:
     * - Transaktional abgesichert (bei Fehler wird alles zurückgerollt)
     * - Vollständige Protokollierung über VorgangService
     * - Detaillierte Zählung der gelöschten Datensätze
     * - Statusvalidierung vor und nach der Operation
     *
     * PERFORMANCE-ÜBERLEGUNGEN:
     * - Löschung erfolgt in optimaler Reihenfolge (Foreign Key Constraints)
     * - Batch-Operationen wo möglich
     * - Memory-effizient durch direkte Repository-Calls
     *
     * WARNUNG: Diese Operation ist IRREVERSIBEL!
     */
    public void clearAllTestData() {
        logger.warn("⚠️ KRITISCHE OPERATION: Vollständige Löschung aller Testdaten wird gestartet...");
        logger.warn("⚠️ Diese Operation ist IRREVERSIBEL und löscht ALLE Daten aus dem System!");

        // Vorgang für Audit-Trail starten
        Vorgang vorgang = vorgangService.starteAutomatischenVorgang(
                VorgangTyp.DATENMIGRATION,
                "VOLLSTÄNDIGER RESET: Löschung aller Testdaten"
        );

        try {
            // Status VOR der Löschung dokumentieren
            logger.info("=== STATUS VOR VOLLSTÄNDIGER LÖSCHUNG ===");
            long totalRecordsBefore = getTotalRecordCount();
            logger.info("Gesamtanzahl Datensätze vor Löschung: {}", totalRecordsBefore);

            // Detaillierte Zählung vor Löschung
            logDetailedCountsBefore();

            // Löschung in korrekter Abhängigkeitsreihenfolge durchführen
            int totalDeleted = 0;

            // STUFE 1: Kindtabellen ohne eigenes Repository (via JPQL)
            totalDeleted += deleteAllInvoiceItems();
            totalDeleted += deleteAllOrderItems();

            // STUFE 2: Abhängige Geschäftsdaten löschen
            totalDeleted += deleteAllOpenItems("Vollständiger Reset");
            totalDeleted += deleteAllInvoices("Vollständiger Reset");
            totalDeleted += deleteAllNotifications("Vollständiger Reset");
            totalDeleted += deleteAllPayments("Vollständiger Reset");
            totalDeleted += deleteAllDueSchedules("Vollständiger Reset");
            totalDeleted += deleteAllSubscriptions("Vollständiger Reset");
            totalDeleted += deleteAllContracts("Vollständiger Reset");
            totalDeleted += deleteAllOrders("Vollständiger Reset");

            // STUFE 3: Stammdaten löschen
            totalDeleted += deleteAllInventory("Vollständiger Reset");
            totalDeleted += deleteAllProducts("Vollständiger Reset");
            totalDeleted += deleteAllCustomers("Vollständiger Reset");
            totalDeleted += deleteAllAddresses("Vollständiger Reset");

            // STUFE 3: Standard-Benutzer neu anlegen
            logger.info("   👤 Lege Standard-Benutzer neu an...");
            masterDataInitializer.initializeUsersOnly();
            logger.info("   ✅ Standard-Benutzer neu angelegt");

            // Erfolgsmeldung und Audit-Trail
            vorgangService.vorgangErfolgreichAbschliessen(
                    vorgang.getId(),
                    8,           // Anzahl Operationen
                    8,           // Erfolgreiche Operationen
                    0,           // Fehlgeschlagene Operationen
                    new BigDecimal(0)
                    //String.format("Vollständiger Reset: %d Datensätze gelöscht", totalDeleted)
            );

            // Vorgänge und verbleibende Notifications nach Abschluss des Audit-Vorgangs löschen
            totalDeleted += deleteAllVorgaenge("Vollständiger Reset");
            totalDeleted += deleteAllNotifications("Vollständiger Reset - Final");

            // Status NACH der Löschung validieren
            logger.info("=== STATUS NACH VOLLSTÄNDIGER LÖSCHUNG ===");
            long totalRecordsAfter = getTotalRecordCount();
            logger.info("Gesamtanzahl Datensätze nach Löschung: {}", totalRecordsAfter);

            if (totalRecordsAfter == 0) {
                logger.info("✅ VOLLSTÄNDIGE LÖSCHUNG ERFOLGREICH: Alle Daten wurden entfernt");
            } else {
                logger.warn("⚠️ WARNUNG: {} Datensätze sind noch im System vorhanden", totalRecordsAfter);
            }

            // Zusammenfassung der Löschung
            logger.info("📊 LÖSCHUNGSSTATISTIK:");
            logger.info("   - Datensätze vor Löschung: {}", totalRecordsBefore);
            logger.info("   - Gelöschte Datensätze:    {}", totalDeleted);
            logger.info("   - Datensätze nach Löschung: {}", totalRecordsAfter);
            logger.info("   - Erfolgreich gelöscht:    {}%", totalRecordsBefore > 0 ?
                    (totalDeleted * 100 / totalRecordsBefore) : 100);

        } catch (Exception e) {
            logger.error("❌ KRITISCHER FEHLER bei vollständiger Datenlöschung", e);
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(),
                    "Vollständige Datenbereinigung fehlgeschlagen: " + e.getMessage());

            // Bei Fehler: Aktuellen Zustand dokumentieren
            logger.error("System-Status nach fehlgeschlagener Löschung:");
            dataStatusReporter.logCurrentDataStatus();

            throw new RuntimeException("Vollständige Datenbereinigung fehlgeschlagen", e);
        }
    }

    /**
     * PARTIELLE DATENBEREINIGUNG - NUR GESCHÄFTSDATEN
     *
     * Löscht nur die Geschäftsdaten und erhält die Stammdaten (Kunden, Produkte, Adressen).
     *
     * ANWENDUNGSFÄLLE:
     * - Reset zwischen verschiedenen Testszenarien
     * - Bereinigung vor neuem Rechnungslauf
     * - Entwicklungsumgebung: Neue Geschäftsdaten auf bestehenden Stammdaten
     *
     * VORTEILE:
     * - Schneller als vollständige Bereinigung
     * - Stammdaten bleiben für weitere Tests verfügbar
     * - Reduzierter Setup-Aufwand für nachfolgende Tests
     *
     * WAS WIRD GELÖSCHT:
     * - OpenItems (offene Posten)
     * - Invoices (Rechnungen)
     * - DueSchedules (Fälligkeitspläne)
     * - Subscriptions (Abonnements)
     * - Contracts (Verträge)
     *
     * WAS BLEIBT ERHALTEN:
     * - Addresses (Adressen)
     * - Customers (Kunden)
     * - Products (Produkte)
     */
    public void clearBusinessDataOnly() {
        logger.info("🔄 PARTIELLE BEREINIGUNG: Lösche nur Geschäftsdaten (Stammdaten bleiben erhalten)");

        Vorgang vorgang = vorgangService.starteAutomatischenVorgang(
                VorgangTyp.DATENMIGRATION,
                "PARTIELLE BEREINIGUNG: Reset der Geschäftsdaten"
        );

        try {
            // Status vor partieller Löschung
            logger.info("=== STATUS VOR PARTIELLER LÖSCHUNG ===");
            long businessDataBefore = getBusinessDataCount();
            long masterDataBefore = getMasterDataCount();
            logger.info("Geschäftsdaten vor Löschung: {}", businessDataBefore);
            logger.info("Stammdaten (bleiben erhalten): {}", masterDataBefore);

            // Nur Geschäftsdaten löschen
            int totalDeleted = 0;
            totalDeleted += deleteAllInvoiceItems();
            totalDeleted += deleteAllOrderItems();
            totalDeleted += deleteAllOpenItems("Partielle Bereinigung");
            totalDeleted += deleteAllInvoices("Partielle Bereinigung");
            totalDeleted += deleteAllNotifications("Partielle Bereinigung");
            totalDeleted += deleteAllPayments("Partielle Bereinigung");
            totalDeleted += deleteAllDueSchedules("Partielle Bereinigung");
            totalDeleted += deleteAllSubscriptions("Partielle Bereinigung");
            totalDeleted += deleteAllContracts("Partielle Bereinigung");
            totalDeleted += deleteAllOrders("Partielle Bereinigung");

            // Erfolg dokumentieren
            vorgangService.vorgangErfolgreichAbschliessen(
                    vorgang.getId(),
                    5,           // Anzahl Operationen
                    5,           // Erfolgreiche Operationen
                    0,           // Fehlgeschlagene Operationen
                    new BigDecimal(0)
                    //String.format("Partielle Bereinigung: %d Geschäftsdatensätze gelöscht", totalDeleted)
            );

            // Vorgänge und verbleibende Notifications nach Abschluss des Audit-Vorgangs löschen
            totalDeleted += deleteAllVorgaenge("Partielle Bereinigung");
            totalDeleted += deleteAllNotifications("Partielle Bereinigung - Final");

            // Status nach partieller Löschung validieren
            logger.info("=== STATUS NACH PARTIELLER LÖSCHUNG ===");
            long businessDataAfter = getBusinessDataCount();
            long masterDataAfter = getMasterDataCount();

            logger.info("✅ PARTIELLE BEREINIGUNG ABGESCHLOSSEN:");
            logger.info("   📋 GELÖSCHTE GESCHÄFTSDATEN:");
            logger.info("      - OpenItems, Invoices, DueSchedules, Subscriptions, Contracts: {} Datensätze", totalDeleted);
            logger.info("   📚 ERHALTENE STAMMDATEN:");
            logger.info("      - Addresses: {}", addressRepository.count());
            logger.info("      - Customers: {}", customerRepository.count());
            logger.info("      - Products:  {}", productRepository.count());

            // Validierung
            if (businessDataAfter == 0 && masterDataAfter > 0) {
                logger.info("✅ PARTIELLE BEREINIGUNG ERFOLGREICH: Geschäftsdaten gelöscht, Stammdaten erhalten");
            } else if (businessDataAfter > 0) {
                logger.warn("⚠️ WARNUNG: {} Geschäftsdaten sind noch im System vorhanden", businessDataAfter);
            }

        } catch (Exception e) {
            logger.error("❌ FEHLER bei partieller Datenbereinigung", e);
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(),
                    "Partielle Datenbereinigung fehlgeschlagen: " + e.getMessage());
            throw new RuntimeException("Partielle Datenbereinigung fehlgeschlagen", e);
        }
    }

    // =====================================================================================
    // KONSISTENZREPARATUR UND DATENINTEGRITÄT
    // =====================================================================================

    /**
     * UMFASSENDE KONSISTENZREPARATUR
     *
     * Analysiert und repariert alle bekannten Dateninkonsistenzen im System.
     * Diese Methode ist idempotent - sie kann sicher mehrfach ausgeführt werden.
     *
     * REPARATUR-KATEGORIEN:
     * 1. STRUKTURELLE KONSISTENZ:  Fehlende oder verwaiste Datensätze
     * 2. STATUS-KONSISTENZ:        Inkorrekte Status-Übergänge
     * 3. REFERENTIELLE INTEGRITÄT: Broken References zwischen Entitäten
     * 4. BUSINESS-LOGIC:           Verletzungen von Geschäftsregeln
     * 5. TEMPORALE KONSISTENZ:     Datum/Zeit-bezogene Inkonsistenzen
     *
     * REPARATUR-STRATEGIEN:
     * - CREATION:    Fehlende Datensätze erstellen (z.B. OpenItems für Invoices)
     * - DELETION:    Verwaiste Datensätze entfernen
     * - UPDATE:      Inkorrekte Status korrigieren
     * - VALIDATION:  Geschäftsregeln durchsetzen
     *
     * SICHERHEIT:
     * - Alle Reparaturen sind transaktional abgesichert
     * - Detaillierte Protokollierung aller Änderungen
     * - Rollback bei kritischen Fehlern
     */
    public void repairDataConsistency() {
        logger.info("🔧 KONSISTENZREPARATUR: Starte umfassende Analyse und Reparatur der Datenintegrität");

        Vorgang vorgang = vorgangService.starteAutomatischenVorgang(
                VorgangTyp.DATENMIGRATION,
                "KONSISTENZREPARATUR: Automatische Behebung von Dateninkonsistenzen"
        );

        try {
            // Status vor Reparatur dokumentieren
            logger.info("=== KONSISTENZANALYSE VOR REPARATUR ===");
            ConsistencyAnalysis beforeAnalysis = analyzeDataConsistency();
            logConsistencyAnalysis("VOR REPARATUR", beforeAnalysis);

            // Reparatur-Operationen durchführen
            int totalOperations = 0;
            int successfulOperations = 0;
            StringBuilder repairLog = new StringBuilder();

            // 1. STRUKTURELLE REPARATUREN
            logger.info("🔧 Phase 1: Strukturelle Konsistenzreparaturen");

            if (repairMissingOpenItems()) {
                successfulOperations++;
                repairLog.append("✅ Fehlende OpenItems erstellt; ");
            }
            totalOperations++;

            if (cleanupOrphanedOpenItems()) {
                successfulOperations++;
                repairLog.append("✅ Verwaiste OpenItems entfernt; ");
            }
            totalOperations++;

            // 2. STATUS-REPARATUREN
            logger.info("🔧 Phase 2: Status-Konsistenzreparaturen");

            if (updateOverdueOpenItems()) {
                successfulOperations++;
                repairLog.append("✅ Überfällige OpenItems aktualisiert; ");
            }
            totalOperations++;

            if (repairSubscriptionStatus()) {
                successfulOperations++;
                repairLog.append("✅ Subscription-Status repariert; ");
            }
            totalOperations++;

            // 3. REFERENTIELLE REPARATUREN
            logger.info("🔧 Phase 3: Referentielle Integritätsreparaturen");

            if (cleanupOrphanedDueSchedules()) {
                successfulOperations++;
                repairLog.append("✅ Verwaiste DueSchedules bereinigt; ");
            }
            totalOperations++;

            // 4. BUSINESS-LOGIC REPARATUREN
            logger.info("🔧 Phase 4: Business-Logic Konsistenzreparaturen");

            if (repairContractSubscriptionRelationships()) {
                successfulOperations++;
                repairLog.append("✅ Contract-Subscription Beziehungen repariert; ");
            }
            totalOperations++;

            if (synchronizeInvoiceOpenItemAmounts()) {
                successfulOperations++;
                repairLog.append("✅ Invoice-OpenItem Beträge synchronisiert; ");
            }
            totalOperations++;

            // Vorgang abschließen
            vorgangService.vorgangErfolgreichAbschliessen(
                    vorgang.getId(),
                    totalOperations,
                    successfulOperations,
                    totalOperations - successfulOperations,
                    new BigDecimal(0)
                    //repairLog.toString()
            );

            // Status nach Reparatur analysieren
            logger.info("=== KONSISTENZANALYSE NACH REPARATUR ===");
            ConsistencyAnalysis afterAnalysis = analyzeDataConsistency();
            logConsistencyAnalysis("NACH REPARATUR", afterAnalysis);

            // Reparatur-Ergebnis zusammenfassen
            logger.info("📊 REPARATUR-ERGEBNIS:");
            logger.info("   - Durchgeführte Operationen: {} von {}", successfulOperations, totalOperations);
            logger.info("   - Erfolgsrate: {}%", successfulOperations * 100 / totalOperations);
            logger.info("   - Verbesserungen:");
            logConsistencyImprovements(beforeAnalysis, afterAnalysis);

            // Finale Statusanzeige
            logger.info("✅ KONSISTENZREPARATUR ABGESCHLOSSEN");
            dataStatusReporter.logCurrentDataStatus();

        } catch (Exception e) {
            logger.error("❌ KRITISCHER FEHLER bei Konsistenzreparatur", e);
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(),
                    "Konsistenzreparatur fehlgeschlagen: " + e.getMessage());
            throw new RuntimeException("Konsistenzreparatur fehlgeschlagen", e);
        }
    }

    // =====================================================================================
    // MAINTENANCE-OPERATIONEN
    // =====================================================================================

    /**
     * STANDARD-MAINTENANCE-AUFGABEN
     *
     * Führt regelmäßige Wartungsaufgaben aus, die typischerweise in einem
     * produktiven ERP-System automatisch ausgeführt werden sollten.
     *
     * MAINTENANCE-KATEGORIEN:
     * 1. HOUSEKEEPING:     Bereinigung temporärer/veralteter Daten
     * 2. STATUS-UPDATES:   Automatische Status-Übergänge (z.B. OPEN -> OVERDUE)
     * 3. CACHE-REFRESH:    Aktualisierung berechneter/cached Werte
     * 4. OPTIMIZATION:     Performance-Optimierungen
     *
     * AUSFÜHRUNGSFREQUENZ:
     * - Täglich:    Überfällige Items aktualisieren
     * - Wöchentlich: Verwaiste Datensätze bereinigen
     * - Monatlich:  Umfassende Konsistenzprüfung
     *
     * AUTOMATISIERUNG:
     * Diese Methode kann über Scheduler (z.B. @Scheduled) automatisiert werden.
     */
    public void performMaintenanceTasks() {
        logger.info("🛠️ MAINTENANCE: Starte Standard-Wartungsaufgaben für ERP-System");

        Vorgang vorgang = vorgangService.starteAutomatischenVorgang(
                VorgangTyp.DATENMIGRATION,
                "MAINTENANCE: Regelmäßige Wartungsaufgaben"
        );

        try {
            logger.info("=== STATUS VOR MAINTENANCE ===");
            dataStatusReporter.logCurrentDataStatus();

            // MAINTENANCE-AUFGABEN AUSFÜHREN
            logger.info("🛠️ Führe Standard-Maintenance-Aufgaben aus...");

            // 1. Überfällige Items aktualisieren (täglich)
            updateOverdueOpenItems();

            // 2. Status-Synchronisation (täglich)
            repairSubscriptionStatus();

            // 3. Verwaiste Daten bereinigen (wöchentlich)
            cleanupOrphanedDueSchedules();

            // 4. Beträge synchronisieren (wöchentlich)
            synchronizeInvoiceOpenItemAmounts();

            vorgangService.vorgangErfolgreichAbschliessen(vorgang.getId());

            logger.info("=== STATUS NACH MAINTENANCE ===");
            dataStatusReporter.logCurrentDataStatus();

            logger.info("✅ MAINTENANCE ERFOLGREICH ABGESCHLOSSEN");

        } catch (Exception e) {
            logger.error("❌ FEHLER bei Standard-Maintenance-Aufgaben", e);
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(),
                    "Standard-Maintenance fehlgeschlagen: " + e.getMessage());
            throw new RuntimeException("Standard-Maintenance fehlgeschlagen", e);
        }
    }

    // =====================================================================================
    // PRIVATE HILFSMETHODEN - LÖSCHOPERATIONEN
    // =====================================================================================

    /**
     * Löscht alle OpenItems mit detaillierter Protokollierung.
     *
     * @param context Kontext der Löschung (für Logging)
     * @return Anzahl gelöschter Datensätze
     */
    private int deleteAllOpenItems(String context) {
        int count = (int) openItemRepository.count();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} OpenItems... (Kontext: {})", count, context);
            openItemRepository.deleteAllInBatch();
            logger.debug("   ✅ {} OpenItems gelöscht", count);
        } else {
            logger.debug("   ℹ️ Keine OpenItems zum Löschen vorhanden");
        }
        return count;
    }

    /**
     * Löscht alle Invoices mit detaillierter Protokollierung.
     */
    private int deleteAllInvoices(String context) {
        int count = (int) invoiceRepository.count();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} Invoices... (Kontext: {})", count, context);
            invoiceRepository.deleteAllInBatch();
            logger.debug("   ✅ {} Invoices gelöscht", count);
        } else {
            logger.debug("   ℹ️ Keine Invoices zum Löschen vorhanden");
        }
        return count;
    }

    /**
     * Löscht alle DueSchedules mit detaillierter Protokollierung.
     */
    private int deleteAllDueSchedules(String context) {
        int count = (int) dueScheduleRepository.count();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} DueSchedules... (Kontext: {})", count, context);
            dueScheduleRepository.deleteAllInBatch();
            logger.debug("   ✅ {} DueSchedules gelöscht", count);
        } else {
            logger.debug("   ℹ️ Keine DueSchedules zum Löschen vorhanden");
        }
        return count;
    }

    /**
     * Löscht alle Subscriptions mit detaillierter Protokollierung.
     */
    private int deleteAllSubscriptions(String context) {
        int count = (int) subscriptionRepository.count();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} Subscriptions... (Kontext: {})", count, context);
            subscriptionRepository.deleteAllInBatch();
            logger.debug("   ✅ {} Subscriptions gelöscht", count);
        } else {
            logger.debug("   ℹ️ Keine Subscriptions zum Löschen vorhanden");
        }
        return count;
    }

    /**
     * Löscht alle Contracts mit detaillierter Protokollierung.
     */
    private int deleteAllContracts(String context) {
        int count = (int) contractRepository.count();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} Contracts... (Kontext: {})", count, context);
            contractRepository.deleteAllInBatch();
            logger.debug("   ✅ {} Contracts gelöscht", count);
        } else {
            logger.debug("   ℹ️ Keine Contracts zum Löschen vorhanden");
        }
        return count;
    }

    /**
     * Löscht alle Products mit detaillierter Protokollierung.
     */
    private int deleteAllProducts(String context) {
        int count = (int) productRepository.count();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} Products... (Kontext: {})", count, context);
            productRepository.deleteAllInBatch();
            logger.debug("   ✅ {} Products gelöscht", count);
        } else {
            logger.debug("   ℹ️ Keine Products zum Löschen vorhanden");
        }
        return count;
    }

    /**
     * Löscht alle Customers mit detaillierter Protokollierung.
     */
    private int deleteAllCustomers(String context) {
        int count = (int) customerRepository.count();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} Customers... (Kontext: {})", count, context);
            customerRepository.deleteAllInBatch();
            logger.debug("   ✅ {} Customers gelöscht", count);
        } else {
            logger.debug("   ℹ️ Keine Customers zum Löschen vorhanden");
        }
        return count;
    }

    /**
     * Löscht alle Addresses mit detaillierter Protokollierung.
     */
    private int deleteAllAddresses(String context) {
        int count = (int) addressRepository.count();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} Addresses... (Kontext: {})", count, context);
            addressRepository.deleteAllInBatch();
            logger.debug("   ✅ {} Addresses gelöscht", count);
        } else {
            logger.debug("   ℹ️ Keine Addresses zum Löschen vorhanden");
        }
        return count;
    }

    private int deleteAllInvoiceItems() {
        int count = ((Number) entityManager.createQuery("SELECT COUNT(ii) FROM InvoiceItem ii").getSingleResult()).intValue();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} InvoiceItems...", count);
            entityManager.createQuery("DELETE FROM InvoiceItem").executeUpdate();
        }
        return count;
    }

    private int deleteAllOrderItems() {
        int count = ((Number) entityManager.createQuery("SELECT COUNT(oi) FROM OrderItem oi").getSingleResult()).intValue();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} OrderItems...", count);
            entityManager.createQuery("DELETE FROM OrderItem").executeUpdate();
        }
        return count;
    }

    private int deleteAllPayments(String context) {
        int count = (int) paymentRepository.count();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} Payments... (Kontext: {})", count, context);
            paymentRepository.deleteAllInBatch();
        }
        return count;
    }

    private int deleteAllOrders(String context) {
        int count = (int) orderRepository.count();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} Orders... (Kontext: {})", count, context);
            orderRepository.deleteAllInBatch();
        }
        return count;
    }

    private int deleteAllNotifications(String context) {
        int count = (int) notificationRepository.count();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} Notifications... (Kontext: {})", count, context);
            notificationRepository.deleteAllInBatch();
        }
        return count;
    }

    private int deleteAllInventory(String context) {
        int count = (int) inventoryRepository.count();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} Inventory-Einträge... (Kontext: {})", count, context);
            inventoryRepository.deleteAllInBatch();
        }
        return count;
    }

    private int deleteAllVorgaenge(String context) {
        int count = ((Number) entityManager.createQuery("SELECT COUNT(v) FROM Vorgang v").getSingleResult()).intValue();
        if (count > 0) {
            logger.info("   🗑️ Lösche {} Vorgänge... (Kontext: {})", count, context);
            entityManager.flush();
            entityManager.createQuery("DELETE FROM Vorgang v").executeUpdate();
            entityManager.clear();
        }
        return count;
    }

    // =====================================================================================
    // PRIVATE HILFSMETHODEN - KONSISTENZREPARATUREN
    // =====================================================================================

    /**
     * REPARATUR: Fehlende OpenItems für Invoices erstellen
     *
     * PROBLEM: Invoices ohne entsprechende OpenItems
     * LÖSUNG:  Automatische Erstellung fehlender OpenItems
     *
     * BUSINESS RULE: Jede aktive Invoice mit Betrag > 0 muss ein OpenItem haben
     *
     * @return true wenn Reparatur erfolgreich, false bei Fehlern
     */
    private boolean repairMissingOpenItems() {
        try {
            logger.info("   🔧 Suche nach Invoices ohne OpenItems...");

            // Alle Invoices finden, die OpenItems benötigen aber keine haben
            List<Invoice> invoicesWithoutOpenItems = invoiceRepository.findAll().stream()
                    .filter(invoice -> invoice.getOpenItems() == null || invoice.getOpenItems().isEmpty())
                    .filter(invoice -> invoice.getStatus() != Invoice.InvoiceStatus.CANCELLED)
                    .filter(invoice -> invoice.getTotalAmount() != null &&
                            invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList());

            if (invoicesWithoutOpenItems.isEmpty()) {
                logger.info("   ✅ Alle Invoices haben korrekte OpenItems");
                return true;
            }

            logger.info("   🔧 Erstelle {} fehlende OpenItems...", invoicesWithoutOpenItems.size());

            int created = 0;
            for (Invoice invoice : invoicesWithoutOpenItems) {
                try {
                    OpenItem openItem = new OpenItem(
                            invoice,
                            "Automatisch erstellt durch Konsistenzreparatur für Rechnung " + invoice.getInvoiceNumber(),
                            invoice.getTotalAmount(),
                            invoice.getDueDate()
                    );

                    // Status basierend auf Fälligkeitsdatum setzen
                    if (invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now())) {
                        openItem.setStatus(OpenItem.OpenItemStatus.OVERDUE);
                    }

                    openItemRepository.save(openItem);
                    created++;

                    logger.debug("   ✅ OpenItem erstellt für Invoice {}: {} EUR",
                            invoice.getInvoiceNumber(), invoice.getTotalAmount());

                } catch (Exception e) {
                    logger.error("   ❌ Fehler beim Erstellen des OpenItems für Invoice {}: {}",
                            invoice.getInvoiceNumber(), e.getMessage());
                }
            }

            logger.info("   ✅ REPARATUR ABGESCHLOSSEN: {} fehlende OpenItems erstellt", created);
            return true;

        } catch (Exception e) {
            logger.error("   ❌ FEHLER bei Reparatur fehlender OpenItems: {}", e.getMessage());
            return false;
        }
    }

    /**
     * REPARATUR: Verwaiste OpenItems entfernen
     *
     * PROBLEM: OpenItems ohne entsprechende Invoice
     * LÖSUNG:  Löschung verwaister OpenItems
     *
     * BUSINESS RULE: OpenItems müssen immer eine gültige Invoice-Referenz haben
     */
    private boolean cleanupOrphanedOpenItems() {
        try {
            logger.info("   🔧 Suche nach verwaisten OpenItems...");

            List<OpenItem> orphanedItems = openItemRepository.findAll().stream()
                    .filter(openItem -> openItem.getInvoice() == null)
                    .collect(Collectors.toList());

            if (orphanedItems.isEmpty()) {
                logger.info("   ✅ Keine verwaisten OpenItems gefunden");
                return true;
            }

            logger.info("   🔧 Entferne {} verwaiste OpenItems...", orphanedItems.size());
            openItemRepository.deleteAll(orphanedItems);

            logger.info("   ✅ REPARATUR ABGESCHLOSSEN: {} verwaiste OpenItems entfernt", orphanedItems.size());
            return true;

        } catch (Exception e) {
            logger.error("   ❌ FEHLER beim Entfernen verwaister OpenItems: {}", e.getMessage());
            return false;
        }
    }

    /**
     * REPARATUR: Überfällige OpenItems aktualisieren
     *
     * PROBLEM: OpenItems mit Status OPEN aber überschrittenem Fälligkeitsdatum
     * LÖSUNG:  Status-Update von OPEN auf OVERDUE
     *
     * BUSINESS RULE: OpenItems mit überschrittenem Fälligkeitsdatum müssen Status OVERDUE haben
     */
    private boolean updateOverdueOpenItems() {
        try {
            logger.info("   🔧 Suche nach überfälligen OpenItems...");

            List<OpenItem> overdueItems = openItemRepository.findAll().stream()
                    .filter(oi -> oi.getStatus() == OpenItem.OpenItemStatus.OPEN)
                    .filter(oi -> oi.getDueDate() != null && oi.getDueDate().isBefore(LocalDate.now()))
                    .collect(Collectors.toList());

            if (overdueItems.isEmpty()) {
                logger.info("   ✅ Alle OpenItems haben korrekte Status");
                return true;
            }

            logger.info("   🔧 Aktualisiere {} überfällige OpenItems...", overdueItems.size());

            for (OpenItem item : overdueItems) {
                item.setStatus(OpenItem.OpenItemStatus.OVERDUE);
                openItemRepository.save(item);
                logger.debug("   ✅ OpenItem {} auf OVERDUE aktualisiert (fällig seit: {})",
                        item.getId(), item.getDueDate());
            }

            logger.info("   ✅ REPARATUR ABGESCHLOSSEN: {} OpenItems auf OVERDUE aktualisiert", overdueItems.size());
            return true;

        } catch (Exception e) {
            logger.error("   ❌ FEHLER beim Aktualisieren überfälliger OpenItems: {}", e.getMessage());
            return false;
        }
    }

    /**
     * REPARATUR: Subscription-Status mit Contract-Status synchronisieren
     *
     * PROBLEM: Aktive Subscriptions bei beendeten Contracts
     * LÖSUNG:  Status-Synchronisation zwischen Contract und Subscription
     *
     * BUSINESS RULE: Subscriptions von beendeten Contracts müssen ebenfalls beendet sein
     */
    private boolean repairSubscriptionStatus() {
        try {
            logger.info("   🔧 Prüfe Contract-Subscription Status-Konsistenz...");

            List<Subscription> inconsistentSubscriptions = subscriptionRepository.findAll().stream()
                    .filter(sub -> sub.getContract().getContractStatus() == ContractStatus.TERMINATED)
                    .filter(sub -> sub.getSubscriptionStatus() == SubscriptionStatus.ACTIVE)
                    .collect(Collectors.toList());

            if (inconsistentSubscriptions.isEmpty()) {
                logger.info("   ✅ Alle Subscription-Status sind konsistent");
                return true;
            }

            logger.info("   🔧 Repariere {} inkonsistente Subscription-Status...", inconsistentSubscriptions.size());

            for (Subscription subscription : inconsistentSubscriptions) {
                subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
                subscription.setEndDate(subscription.getContract().getEndDate());
                subscriptionRepository.save(subscription);

                logger.debug("   ✅ Subscription {} auf CANCELLED gesetzt (Contract beendet: {})",
                        subscription.getSubscriptionNumber(), subscription.getContract().getEndDate());
            }

            logger.info("   ✅ REPARATUR ABGESCHLOSSEN: {} Subscription-Status repariert", inconsistentSubscriptions.size());
            return true;

        } catch (Exception e) {
            logger.error("   ❌ FEHLER beim Reparieren der Subscription-Status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * REPARATUR: Verwaiste DueSchedules bereinigen
     *
     * PROBLEM: Aktive DueSchedules für inaktive Subscriptions
     * LÖSUNG:  Status-Update oder Löschung verwaister DueSchedules
     *
     * BUSINESS RULE: Aktive DueSchedules benötigen aktive Subscriptions
     */
    private boolean cleanupOrphanedDueSchedules() {
        try {
            logger.info("   🔧 Suche nach verwaisten DueSchedules...");

            List<DueSchedule> orphanedSchedules = dueScheduleRepository.findAll().stream()
                    .filter(schedule -> schedule.getSubscription() == null ||
                            schedule.getSubscription().getSubscriptionStatus() != SubscriptionStatus.ACTIVE)
                    .filter(schedule -> schedule.getStatus() == DueStatus.ACTIVE)
                    .collect(Collectors.toList());

            if (orphanedSchedules.isEmpty()) {
                logger.info("   ✅ Alle DueSchedules sind korrekt verknüpft");
                return true;
            }

            logger.info("   🔧 Bereinige {} verwaiste DueSchedules...", orphanedSchedules.size());

            for (DueSchedule schedule : orphanedSchedules) {
                if (schedule.getSubscription() == null) {
                    // Komplett verwaist - löschen
                    dueScheduleRepository.delete(schedule);
                    logger.debug("   ✅ Verwaister DueSchedule {} gelöscht (keine Subscription)", schedule.getId());
                } else {
                    // Subscription inaktiv - pausieren
                    schedule.pause();
                    dueScheduleRepository.save(schedule);
                    logger.debug("   ✅ DueSchedule {} pausiert (Subscription inaktiv)", schedule.getId());
                }
            }

            logger.info("   ✅ REPARATUR ABGESCHLOSSEN: {} verwaiste DueSchedules bereinigt", orphanedSchedules.size());
            return true;

        } catch (Exception e) {
            logger.error("   ❌ FEHLER beim Bereinigen verwaister DueSchedules: {}", e.getMessage());
            return false;
        }
    }

    /**
     * REPARATUR: Contract-Subscription Beziehungen validieren und reparieren
     *
     * PROBLEM: Inkonsistente Beziehungen zwischen Contracts und Subscriptions
     * LÖSUNG:  Validierung und Reparatur der Beziehungslogik
     */
    private boolean repairContractSubscriptionRelationships() {
        try {
            logger.info("   🔧 Validiere Contract-Subscription Beziehungen...");

            int repairedCount = 0;

            // Alle Contracts mit problematischen Subscription-Beziehungen finden
            List<Contract> activeContractsWithoutSubscriptions = contractRepository.findAll().stream()
                    .filter(contract -> contract.getContractStatus() == ContractStatus.ACTIVE)
                    .filter(contract -> contract.getSubscriptions() == null || contract.getSubscriptions().isEmpty())
                    .collect(Collectors.toList());

            // Warnung für Contracts ohne Subscriptions (nicht automatisch reparierbar)
            if (!activeContractsWithoutSubscriptions.isEmpty()) {
                logger.warn("   ⚠️ {} aktive Contracts ohne Subscriptions gefunden (manueller Review erforderlich)",
                        activeContractsWithoutSubscriptions.size());

                for (Contract contract : activeContractsWithoutSubscriptions.stream().limit(5).collect(Collectors.toList())) {
                    logger.warn("   ⚠️ Contract {} (Kunde: {}) hat keine Subscriptions",
                            contract.getContractNumber(),
                            contract.getCustomer().getFirstName() + " " + contract.getCustomer().getLastName());
                }
            }

            logger.info("   ✅ VALIDIERUNG ABGESCHLOSSEN: Contract-Subscription Beziehungen geprüft");
            return true;

        } catch (Exception e) {
            logger.error("   ❌ FEHLER bei Contract-Subscription Beziehungs-Reparatur: {}", e.getMessage());
            return false;
        }
    }

    /**
     * REPARATUR: Invoice-OpenItem Beträge synchronisieren
     *
     * PROBLEM: Diskrepanzen zwischen Invoice.totalAmount und OpenItem.amount
     * LÖSUNG:  Synchronisation der Beträge
     */
    private boolean synchronizeInvoiceOpenItemAmounts() {
        try {
            logger.info("   🔧 Synchronisiere Invoice-OpenItem Beträge...");

            List<Invoice> invoicesWithDiscrepancies = invoiceRepository.findAll().stream()
                    .filter(invoice -> invoice.getTotalAmount() != null)
                    .filter(invoice -> invoice.getOpenItems() != null && !invoice.getOpenItems().isEmpty())
                    .filter(invoice -> {
                        BigDecimal invoiceAmount = invoice.getTotalAmount();
                        BigDecimal openItemAmount = invoice.getOpenItems().stream()
                                .map(OpenItem::getAmount)
                                .filter(amount -> amount != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        return invoiceAmount.compareTo(openItemAmount) != 0;
                    })
                    .collect(Collectors.toList());

            if (invoicesWithDiscrepancies.isEmpty()) {
                logger.info("   ✅ Alle Invoice-OpenItem Beträge sind synchron");
                return true;
            }

            logger.info("   🔧 Repariere {} Invoice-OpenItem Betragsdiskrepanzen...", invoicesWithDiscrepancies.size());

            int repairedCount = 0;
            for (Invoice invoice : invoicesWithDiscrepancies) {
                try {
                    BigDecimal invoiceAmount = invoice.getTotalAmount();

                    // Wenn nur ein OpenItem vorhanden ist, synchronisiere es
                    if (invoice.getOpenItems().size() == 1) {
                        OpenItem openItem = invoice.getOpenItems().iterator().next();
                        BigDecimal oldAmount = openItem.getAmount();
                        openItem.setAmount(invoiceAmount);
                        openItem.setPaidAmount(invoiceAmount.subtract(openItem.getPaidAmount() != null ? openItem.getPaidAmount() : BigDecimal.ZERO));

                        openItemRepository.save(openItem);
                        repairedCount++;

                        logger.debug("   ✅ OpenItem für Invoice {} synchronisiert: {} -> {} EUR",
                                invoice.getInvoiceNumber(), oldAmount, invoiceAmount);
                    } else {
                        // Komplexere Fälle loggen für manuellen Review
                        logger.warn("   ⚠️ Invoice {} hat {} OpenItems - manueller Review erforderlich",
                                invoice.getInvoiceNumber(), invoice.getOpenItems().size());
                    }

                } catch (Exception e) {
                    logger.error("   ❌ Fehler bei Betragssynchronisation für Invoice {}: {}",
                            invoice.getInvoiceNumber(), e.getMessage());
                }
            }

            logger.info("   ✅ REPARATUR ABGESCHLOSSEN: {} Invoice-OpenItem Beträge synchronisiert", repairedCount);
            return true;

        } catch (Exception e) {
            logger.error("   ❌ FEHLER bei Invoice-OpenItem Betragssynchronisation: {}", e.getMessage());
            return false;
        }
    }

    // =====================================================================================
    // PRIVATE HILFSMETHODEN - ANALYSE UND REPORTING
    // =====================================================================================

    /**
     * Zählt alle Datensätze im System
     */
    private long getTotalRecordCount() {
        return openItemRepository.count() +
                invoiceRepository.count() +
                dueScheduleRepository.count() +
                subscriptionRepository.count() +
                contractRepository.count() +
                customerRepository.count() +
                productRepository.count() +
                addressRepository.count();
    }

    /**
     * Zählt nur Geschäftsdaten (ohne Stammdaten)
     */
    private long getBusinessDataCount() {
        return openItemRepository.count() +
                invoiceRepository.count() +
                dueScheduleRepository.count() +
                subscriptionRepository.count() +
                contractRepository.count();
    }

    /**
     * Zählt nur Stammdaten
     */
    private long getMasterDataCount() {
        return customerRepository.count() +
                productRepository.count() +
                addressRepository.count();
    }

    /**
     * Detaillierte Zählung vor Löschung
     */
    private void logDetailedCountsBefore() {
        logger.info("   📊 Detaillierte Datensätze vor Löschung:");
        logger.info("      - OpenItems:     {}", openItemRepository.count());
        logger.info("      - Invoices:      {}", invoiceRepository.count());
        logger.info("      - DueSchedules:  {}", dueScheduleRepository.count());
        logger.info("      - Subscriptions: {}", subscriptionRepository.count());
        logger.info("      - Contracts:     {}", contractRepository.count());
        logger.info("      - Products:      {}", productRepository.count());
        logger.info("      - Customers:     {}", customerRepository.count());
        logger.info("      - Addresses:     {}", addressRepository.count());
    }

    /**
     * Konsistenzanalyse durchführen
     */
    private ConsistencyAnalysis analyzeDataConsistency() {
        ConsistencyAnalysis analysis = new ConsistencyAnalysis();

        // Strukturelle Konsistenz
        analysis.invoicesWithoutOpenItems = countInvoicesWithoutOpenItems();
        analysis.openItemsWithoutInvoices = countOpenItemsWithoutInvoices();

        // Status-Konsistenz
        analysis.overdueOpenItems = countOverdueOpenItems();
        analysis.inconsistentSubscriptions = countInconsistentSubscriptions();

        // Referentielle Konsistenz
        analysis.orphanedDueSchedules = countOrphanedDueSchedules();

        return analysis;
    }

    /**
     * Konsistenzanalyse ins Log ausgeben
     */
    private void logConsistencyAnalysis(String phase, ConsistencyAnalysis analysis) {
        logger.info("   📊 KONSISTENZANALYSE {}:", phase);
        logger.info("      Strukturelle Probleme:");
        logger.info("        - Invoices ohne OpenItems: {}", analysis.invoicesWithoutOpenItems);
        logger.info("        - OpenItems ohne Invoices: {}", analysis.openItemsWithoutInvoices);
        logger.info("      Status-Probleme:");
        logger.info("        - Überfällige OpenItems (noch OPEN): {}", analysis.overdueOpenItems);
        logger.info("        - Inkonsistente Subscription-Status: {}", analysis.inconsistentSubscriptions);
        logger.info("      Referentielle Probleme:");
        logger.info("        - Verwaiste DueSchedules: {}", analysis.orphanedDueSchedules);
        logger.info("      Gesamt-Inkonsistenzen: {}", analysis.getTotalInconsistencies());
    }

    /**
     * Verbesserungen zwischen zwei Analysen loggen
     */
    private void logConsistencyImprovements(ConsistencyAnalysis before, ConsistencyAnalysis after) {
        logger.info("      - Invoices ohne OpenItems: {} -> {} ({})",
                before.invoicesWithoutOpenItems, after.invoicesWithoutOpenItems,
                before.invoicesWithoutOpenItems - after.invoicesWithoutOpenItems >= 0 ? "✅" : "❌");
        logger.info("      - Überfällige OpenItems: {} -> {} ({})",
                before.overdueOpenItems, after.overdueOpenItems,
                before.overdueOpenItems - after.overdueOpenItems >= 0 ? "✅" : "❌");
        logger.info("      - Inkonsistente Subscriptions: {} -> {} ({})",
                before.inconsistentSubscriptions, after.inconsistentSubscriptions,
                before.inconsistentSubscriptions - after.inconsistentSubscriptions >= 0 ? "✅" : "❌");
    }

    // Hilfsmethoden für Konsistenz-Zählungen
    private long countInvoicesWithoutOpenItems() {
        try {
            return invoiceRepository.findAll().stream()
                    .filter(invoice -> invoice.getOpenItems() == null || invoice.getOpenItems().isEmpty())
                    .filter(invoice -> invoice.getStatus() != Invoice.InvoiceStatus.CANCELLED)
                    .filter(invoice -> invoice.getTotalAmount() != null &&
                            invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                    .count();
        } catch (Exception e) {
            logger.error("Fehler beim Zählen der Invoices ohne OpenItems: {}", e.getMessage());
            return -1;
        }
    }

    private long countOpenItemsWithoutInvoices() {
        return openItemRepository.findAll().stream()
                .filter(openItem -> openItem.getInvoice() == null)
                .count();
    }

    private long countOverdueOpenItems() {
        return openItemRepository.findAll().stream()
                .filter(oi -> oi.getStatus() == OpenItem.OpenItemStatus.OPEN)
                .filter(oi -> oi.getDueDate() != null && oi.getDueDate().isBefore(LocalDate.now()))
                .count();
    }

    private long countInconsistentSubscriptions() {
        return subscriptionRepository.findAll().stream()
                .filter(sub -> sub.getContract().getContractStatus() == ContractStatus.TERMINATED)
                .filter(sub -> sub.getSubscriptionStatus() == SubscriptionStatus.ACTIVE)
                .count();
    }

    private long countOrphanedDueSchedules() {
        return dueScheduleRepository.findAll().stream()
                .filter(schedule -> schedule.getSubscription() == null ||
                        schedule.getSubscription().getSubscriptionStatus() != SubscriptionStatus.ACTIVE)
                .filter(schedule -> schedule.getStatus() == DueStatus.ACTIVE)
                .count();
    }

    // =====================================================================================
    // INNER CLASS - KONSISTENZANALYSE
    // =====================================================================================

    /**
     * Container für Konsistenzanalyse-Ergebnisse
     */
    private static class ConsistencyAnalysis {
        long invoicesWithoutOpenItems = 0;
        long openItemsWithoutInvoices = 0;
        long overdueOpenItems = 0;
        long inconsistentSubscriptions = 0;
        long orphanedDueSchedules = 0;

        long getTotalInconsistencies() {
            return invoicesWithoutOpenItems + openItemsWithoutInvoices +
                    overdueOpenItems + inconsistentSubscriptions + orphanedDueSchedules;
        }
    }
}