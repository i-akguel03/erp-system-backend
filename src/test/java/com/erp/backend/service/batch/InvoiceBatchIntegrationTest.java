// ===============================================================================================
// INTEGRATION TESTS - Testen das Zusammenspiel ALLER Komponenten mit echter Datenbank
// ===============================================================================================

package com.erp.backend.service.batch;

import com.erp.backend.domain.*;
import com.erp.backend.repository.*;
import com.erp.backend.service.CustomerEmailService;
import com.erp.backend.service.DueScheduleService;
import com.erp.backend.service.InvoiceFactory;
import com.erp.backend.service.OpenItemFactory;
import com.erp.backend.service.VorgangService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INTEGRATION TEST - Kompletter Rechnungslauf End-to-End
 *
 * Was ist anders als Unit Tests?
 * - @SpringBootTest startet die komplette Spring-Anwendung
 * - Echte Datenbank (H2 In-Memory für Tests)
 * - KEINE Mocks - alle echten Services und Repositories
 * - Testet das Zusammenspiel aller Komponenten
 * - Langsamer aber realistischer
 *
 * Wann Integration Tests?
 * - End-to-End Szenarien
 * - Datenbank-Interaktionen
 * - Transaktions-Verhalten
 * - Service-Zusammenspiel
 */
@SpringBootTest // Startet komplette Spring-Anwendung
@ActiveProfiles("test") // Verwendet application-test.yml (H2 In-Memory DB)
@Transactional // Jeder Test läuft in eigener Transaktion (automatisches Rollback nach Test)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // Tests in definierter Reihenfolge
@DisplayName("Integration Tests: Invoice Batch System (End-to-End)")
class InvoiceBatchIntegrationTest {

    // KEINE @Mock hier! Alles echte Spring Beans
    @Autowired private InvoiceBatchOrchestrator orchestrator;
    @Autowired private InvoiceBatchAnalyzer analyzer;
    @Autowired private DueScheduleRepository dueScheduleRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private OpenItemRepository openItemRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private ContractRepository contractRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private VorgangRepository vorgangRepository;

    private LocalDate billingDate;
    private Customer testCustomer;
    private Contract testContract;
    private Subscription testSubscription;

    /**
     * SETUP - Erstellt echte Testdaten in der Datenbank
     * Läuft VOR jedem Test
     */
    @BeforeEach
    void setUp() {
        billingDate = LocalDate.of(2025, 3, 15);

        // Cleanup: Alte Testdaten löschen
        cleanupDatabase();

        // Testdaten erstellen: Customer → Contract → Subscription → DueSchedule
        createTestData();
    }

    @Test
    @Order(1)
    @DisplayName("E2E: Sollte kompletten Rechnungslauf erfolgreich durchführen")
    void testCompleteInvoiceBatch_Success() {
        // GIVEN: 3 Fälligkeiten in der Datenbank
        createDueSchedule("DUE-001", LocalDate.of(2025, 1, 15), BigDecimal.valueOf(100));
        createDueSchedule("DUE-002", LocalDate.of(2025, 2, 15), BigDecimal.valueOf(200));
        createDueSchedule("DUE-003", LocalDate.of(2025, 3, 15), BigDecimal.valueOf(300));

        // Verify: Daten sind in DB
        assertEquals(3, dueScheduleRepository.count(), "Sollte 3 DueSchedules haben");
        assertEquals(0, invoiceRepository.count(), "Sollte noch keine Invoices haben");
        assertEquals(0, openItemRepository.count(), "Sollte noch keine OpenItems haben");

        // WHEN: Rechnungslauf durchführen
        InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate, true);

        // THEN: Prüfe Ergebnis-Objekt
        assertNotNull(result);
        assertEquals(3, result.getProcessedDueSchedules(), "Sollte 3 DueSchedules verarbeitet haben");
        assertEquals(3, result.getCreatedInvoices(), "Sollte 3 Invoices erstellt haben");
        assertEquals(3, result.getCreatedOpenItems(), "Sollte 3 OpenItems erstellt haben");
        assertEquals(BigDecimal.valueOf(600), result.getTotalAmount(), "Gesamtbetrag sollte 600 sein");
        assertFalse(result.hasErrors(), "Sollte keine Fehler haben");
        assertTrue(result.hasVorgang(), "Sollte Vorgang haben");

        // THEN: Prüfe Datenbank-Zustand
        assertEquals(3, invoiceRepository.count(), "DB sollte 3 Invoices haben");
        assertEquals(3, openItemRepository.count(), "DB sollte 3 OpenItems haben");

        // THEN: Prüfe DueSchedules sind auf COMPLETED gesetzt
        List<DueSchedule> completedSchedules = dueScheduleRepository
                .findByStatus(DueStatus.COMPLETED);
        assertEquals(3, completedSchedules.size(), "Alle DueSchedules sollten COMPLETED sein");

        // THEN: Prüfe dass alle DueSchedules eine Invoice-Referenz haben
        completedSchedules.forEach(ds -> {
            assertNotNull(ds.getInvoiceId(), "DueSchedule sollte Invoice-ID haben");
            assertNotNull(ds.isCompleted(), "DueSchedule sollte CompletedAt haben");
        });

        // THEN: Prüfe dass Vorgang korrekt erstellt wurde
        List<Vorgang> vorgaenge = vorgangRepository.findByTyp(VorgangTyp.RECHNUNGSLAUF);
        assertEquals(1, vorgaenge.size(), "Sollte genau 1 Rechnungslauf-Vorgang haben");

        Vorgang vorgang = vorgaenge.get(0);
        assertEquals(VorgangStatus.ERFOLGREICH, vorgang.getStatus());
        assertEquals(3, vorgang.getAnzahlErfolgreich());
        assertNotNull(vorgang.getEndeZeitpunkt());
    }

    @Test
    @Order(2)
    @DisplayName("E2E: Sollte nur überfällige Fälligkeiten verarbeiten")
    void testInvoiceBatch_OverdueOnly() {
        // GIVEN: 2 überfällige, 1 aktuelle, 1 zukünftige Fälligkeit
        createDueSchedule("DUE-OLD1", LocalDate.of(2025, 1, 15), BigDecimal.valueOf(100));
        createDueSchedule("DUE-OLD2", LocalDate.of(2025, 2, 15), BigDecimal.valueOf(200));
        createDueSchedule("DUE-CURRENT", LocalDate.of(2025, 3, 15), BigDecimal.valueOf(300));
        createDueSchedule("DUE-FUTURE", LocalDate.of(2025, 4, 15), BigDecimal.valueOf(400));

        // WHEN: Rechnungslauf bis zum 15.03.2025
        InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate, true);

        // THEN: Nur 3 sollten verarbeitet werden (2 überfällige + 1 aktuelle)
        assertEquals(3, result.getProcessedDueSchedules());
        assertEquals(3, result.getCreatedInvoices());

        // THEN: Die zukünftige Fälligkeit sollte ACTIVE bleiben
        DueSchedule futureDue = dueScheduleRepository.findByDueNumber("DUE-FUTURE").orElseThrow();
        assertEquals(DueStatus.ACTIVE, futureDue.getStatus(), "Zukünftige Fälligkeit sollte ACTIVE bleiben");
        assertNull(futureDue.getInvoiceId(), "Zukünftige Fälligkeit sollte keine Invoice haben");
    }

    @Test
    @Order(3)
    @DisplayName("E2E: Sollte nur exakte Fälligkeiten verarbeiten (ohne vorherige Monate)")
    void testInvoiceBatch_ExactDateOnly() {
        // GIVEN
        createDueSchedule("DUE-JAN", LocalDate.of(2025, 1, 15), BigDecimal.valueOf(100));
        createDueSchedule("DUE-FEB", LocalDate.of(2025, 2, 15), BigDecimal.valueOf(200));
        createDueSchedule("DUE-MAR1", LocalDate.of(2025, 3, 15), BigDecimal.valueOf(300));
        createDueSchedule("DUE-MAR2", LocalDate.of(2025, 3, 15), BigDecimal.valueOf(400));

        // WHEN: Rechnungslauf NUR für exaktes Datum (ohne vorherige Monate)
        InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate, false);

        // THEN: Nur die 2 März-Fälligkeiten sollten verarbeitet werden
        assertEquals(2, result.getProcessedDueSchedules());
        assertEquals(2, result.getCreatedInvoices());
        assertEquals(BigDecimal.valueOf(700), result.getTotalAmount());

        // THEN: Januar und Februar sollten ACTIVE bleiben
        DueSchedule janDue = dueScheduleRepository.findByDueNumber("DUE-JAN").orElseThrow();
        DueSchedule febDue = dueScheduleRepository.findByDueNumber("DUE-FEB").orElseThrow();

        assertEquals(DueStatus.ACTIVE, janDue.getStatus());
        assertEquals(DueStatus.ACTIVE, febDue.getStatus());
        assertNull(janDue.getInvoiceId());
        assertNull(febDue.getInvoiceId());
    }

    @Test
    @Order(4)
    @DisplayName("E2E: Sollte korrekt reagieren wenn keine Fälligkeiten vorhanden")
    void testInvoiceBatch_NoData() {
        // GIVEN: Keine Fälligkeiten in der DB
        assertEquals(0, dueScheduleRepository.count());

        // WHEN
        InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate, true);

        // THEN
        assertNotNull(result);
        assertEquals(0, result.getProcessedDueSchedules());
        assertEquals(0, result.getCreatedInvoices());
        assertEquals("Keine offenen Fälligkeiten gefunden", result.getMessage());
        assertFalse(result.hasErrors());

        // THEN: Vorgang sollte trotzdem erfolgreich abgeschlossen sein
        List<Vorgang> vorgaenge = vorgangRepository.findByTyp(VorgangTyp.RECHNUNGSLAUF);
        assertEquals(1, vorgaenge.size());
        assertEquals(VorgangStatus.ERFOLGREICH, vorgaenge.get(0).getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("E2E: Sollte Subscription-ID korrekt propagieren")
    void testInvoiceBatch_SubscriptionIdPropagation() {
        // GIVEN
        createDueSchedule("DUE-001", billingDate, BigDecimal.valueOf(100));

        // WHEN
        InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate, true);

        // THEN: Prüfe dass Invoice die richtige Subscription-ID hat
        List<Invoice> invoices = invoiceRepository.findAll();
        assertEquals(1, invoices.size());

        Invoice invoice = invoices.get(0);
        assertNotNull(invoice.getSubscriptionId(), "Invoice sollte Subscription-ID haben");
        assertEquals(testSubscription.getId(), invoice.getSubscriptionId());

        // THEN: Prüfe dass OpenItem auch die Subscription-ID hat (via Invoice)
        List<OpenItem> openItems = openItemRepository.findAll();
        assertEquals(1, openItems.size());

        OpenItem openItem = openItems.get(0);
        assertNotNull(openItem.getInvoice(), "OpenItem sollte Invoice haben");
        assertEquals(testSubscription.getId(), openItem.getInvoice().getSubscriptionId());
    }

    @Test
    @Order(6)
    @DisplayName("E2E: Sollte Batch-ID korrekt in alle Entitäten schreiben")
    void testInvoiceBatch_BatchIdPropagation() {
        // GIVEN
        createDueSchedule("DUE-001", billingDate, BigDecimal.valueOf(100));

        // WHEN
        InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate, true);

        // THEN: Prüfe Batch-ID in allen Entitäten
        String expectedBatchId = result.getBatchId();
        assertNotNull(expectedBatchId);
        assertTrue(expectedBatchId.startsWith("BATCH-"));

        // Invoice sollte Batch-ID haben
        Invoice invoice = invoiceRepository.findAll().get(0);
        assertEquals(expectedBatchId, invoice.getInvoiceBatchId());

        // DueSchedule sollte Batch-ID haben
        DueSchedule dueSchedule = dueScheduleRepository.findByDueNumber("DUE-001").orElseThrow();
        assertEquals(expectedBatchId, dueSchedule.getInvoiceBatchId());
    }

    @Test
    @Order(7)
    @DisplayName("E2E: Sollte Vorgang-Metadaten korrekt setzen")
    void testInvoiceBatch_VorgangMetadata() {
        // GIVEN
        createDueSchedule("DUE-JAN", LocalDate.of(2025, 1, 15), BigDecimal.valueOf(100));
        createDueSchedule("DUE-FEB", LocalDate.of(2025, 2, 15), BigDecimal.valueOf(200));
        createDueSchedule("DUE-MAR", LocalDate.of(2025, 3, 15), BigDecimal.valueOf(300));

        // WHEN
        InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate, true);

        // THEN: Prüfe Vorgang-Details
        List<Vorgang> vorgaenge = vorgangRepository.findByTyp(VorgangTyp.RECHNUNGSLAUF);
        assertEquals(1, vorgaenge.size());

        Vorgang vorgang = vorgaenge.get(0);

        // Status und Zeiten
        assertEquals(VorgangStatus.ERFOLGREICH, vorgang.getStatus());
        assertNotNull(vorgang.getStartZeitpunkt());
        assertNotNull(vorgang.getEndeZeitpunkt());
        assertTrue(vorgang.getDauerInMs() > 0);

        // Statistiken
        assertEquals(3, vorgang.getAnzahlVerarbeitet());
        assertEquals(BigDecimal.valueOf(600), vorgang.getGesamtbetrag());

        // Metadaten (JSON)
        assertNotNull(vorgang.getMetadaten());
        assertTrue(vorgang.getMetadaten().contains("billingDate"));
        assertTrue(vorgang.getMetadaten().contains("2025-03-15"));
        assertTrue(vorgang.getMetadaten().contains("batchId"));
        assertTrue(vorgang.getMetadaten().contains("monthsProcessed"));
    }

    @Test
    @Order(8)
    @DisplayName("E2E: Analyzer sollte korrekte Monats-Gruppierung liefern")
    void testAnalyzer_MonthGrouping() {
        // GIVEN: Fälligkeiten über 3 Monate verteilt
        createDueSchedule("DUE-JAN-1", LocalDate.of(2025, 1, 15), BigDecimal.valueOf(100));
        createDueSchedule("DUE-JAN-2", LocalDate.of(2025, 1, 20), BigDecimal.valueOf(150));
        createDueSchedule("DUE-FEB-1", LocalDate.of(2025, 2, 10), BigDecimal.valueOf(200));
        createDueSchedule("DUE-MAR-1", LocalDate.of(2025, 3, 5), BigDecimal.valueOf(250));
        createDueSchedule("DUE-MAR-2", LocalDate.of(2025, 3, 15), BigDecimal.valueOf(300));

        // WHEN: Analyse durchführen
        InvoiceBatchAnalysis analysis = analyzer.analyzeBillingScope(billingDate, true);

        // THEN: Prüfe Statistiken
        assertEquals(5, analysis.getTotalCount());
        assertEquals(3, analysis.getOverdueCount()); // JAN-1, JAN-2, FEB-1
        assertEquals(1, analysis.getCurrentCount()); // nur MAR-2 hat exakt 15.03
        assertEquals(3, analysis.getMonthCount()); // 3 verschiedene Monate

        // THEN: Prüfe Monats-Gruppen
        var monthGroups = analysis.getMonthGroups();
        assertEquals(2, monthGroups.get("2025-01").size());
        assertEquals(1, monthGroups.get("2025-02").size());
        assertEquals(2, monthGroups.get("2025-03").size());
    }

    @Test
    @Order(9)
    @DisplayName("E2E: Sollte bei mehrfachem Aufruf nicht duplizieren")
    void testInvoiceBatch_NoDoubleProcessing() {
        // GIVEN
        createDueSchedule("DUE-001", billingDate, BigDecimal.valueOf(100));

        // WHEN: Ersten Lauf durchführen
        InvoiceBatchResult result1 = orchestrator.runInvoiceBatch(billingDate, true);
        assertEquals(1, result1.getCreatedInvoices());

        // WHEN: Zweiten Lauf durchführen (sollte nichts mehr finden)
        InvoiceBatchResult result2 = orchestrator.runInvoiceBatch(billingDate, true);

        // THEN: Zweiter Lauf sollte nichts verarbeiten
        assertEquals(0, result2.getProcessedDueSchedules());
        assertEquals("Keine offenen Fälligkeiten gefunden", result2.getMessage());

        // THEN: Insgesamt sollte nur 1 Invoice existieren
        assertEquals(1, invoiceRepository.count());
        assertEquals(1, openItemRepository.count());
    }

    // ===============================================================================================
    // HELPER METHODS - Erstellen von Testdaten
    // ===============================================================================================

    private void cleanupDatabase() {
        openItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        dueScheduleRepository.deleteAll();
        subscriptionRepository.deleteAll();
        contractRepository.deleteAll();
        customerRepository.deleteAll();
        vorgangRepository.deleteAll();
    }

    private void createTestData() {
        // Customer erstellen
        testCustomer = new Customer();
        testCustomer.setCustomerNumber("CUST-001");
        testCustomer.setFirstName("Test");
        testCustomer.setLastName("Customer");
        testCustomer = customerRepository.save(testCustomer);

        // Contract erstellen
        testContract = new Contract();
        testContract.setContractNumber("CONTR-001");
        testContract.setContractTitle("Test-Vertrag");
        testContract.setCustomer(testCustomer);
        testContract.setStartDate(LocalDate.of(2024, 1, 1));
        testContract = contractRepository.save(testContract);

        // Subscription erstellen
        testSubscription = new Subscription();
        testSubscription.setSubscriptionNumber("SUB-001");
        testSubscription.setContract(testContract);
        testSubscription.setStartDate(LocalDate.of(2024, 1, 1));
        testSubscription.setProductName("Test-Abonnement");
        testSubscription.setMonthlyPrice(BigDecimal.valueOf(100));
        testSubscription = subscriptionRepository.save(testSubscription);
    }

    private DueSchedule createDueSchedule(String dueNumber, LocalDate dueDate, BigDecimal amount) {
        DueSchedule ds = new DueSchedule();
        ds.setDueNumber(dueNumber);
        ds.setDueDate(dueDate);
        //ds.getStatus(amount);
        ds.setStatus(DueStatus.ACTIVE);
        ds.setSubscription(testSubscription);
        ds.setPeriodStart(dueDate.withDayOfMonth(1));
        ds.setPeriodEnd(dueDate.withDayOfMonth(dueDate.lengthOfMonth()));

        return dueScheduleRepository.save(ds);
    }
}

// ===============================================================================================
// INTEGRATION TEST - Analyzer isoliert (aber mit echter DB)
// ===============================================================================================

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Tests: InvoiceBatchAnalyzer (mit DB)")
class InvoiceBatchAnalyzerIntegrationTest {

    @Autowired private InvoiceBatchAnalyzer analyzer;
    @Autowired private DueScheduleRepository dueScheduleRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private ContractRepository contractRepository;
    @Autowired private CustomerRepository customerRepository;

    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        cleanupDatabase();
        createBasicTestData();
    }

    @Test
    @DisplayName("Sollte Performance-optimierte Queries verwenden")
    void testAnalyzer_PerformanceOptimization() {
        // GIVEN: Viele Fälligkeiten erstellen
        for (int i = 1; i <= 50; i++) {
            createDueSchedule("DUE-" + i, LocalDate.of(2025, 1, i % 28 + 1),
                    BigDecimal.valueOf(100 + i));
        }

        // WHEN: Analyse durchführen
        long startTime = System.currentTimeMillis();
        InvoiceBatchAnalysis analysis = analyzer.analyzeBillingScope(
                LocalDate.of(2025, 1, 31), true);
        long duration = System.currentTimeMillis() - startTime;

        // THEN: Performance-Check
        assertEquals(50, analysis.getTotalCount());
        assertTrue(duration < 1000, "Analyse sollte unter 1 Sekunde dauern");
    }

    @Test
    @DisplayName("Sollte mit verschiedenen DueStatus korrekt umgehen")
    void testAnalyzer_DifferentStatuses() {
        // GIVEN: Fälligkeiten mit verschiedenen Status
        createDueSchedule("DUE-ACTIVE", LocalDate.of(2025, 1, 15),
                BigDecimal.valueOf(100));

        DueSchedule completed = createDueSchedule("DUE-COMPLETED",
                LocalDate.of(2025, 1, 20), BigDecimal.valueOf(200));
        completed.setStatus(DueStatus.COMPLETED);
        dueScheduleRepository.save(completed);

        DueSchedule cancelled = createDueSchedule("DUE-CANCELLED",
                LocalDate.of(2025, 1, 25), BigDecimal.valueOf(300));
        cancelled.setStatus(DueStatus.SUSPENDED);
        dueScheduleRepository.save(cancelled);

        // WHEN
        InvoiceBatchAnalysis analysis = analyzer.analyzeBillingScope(
                LocalDate.of(2025, 1, 31), true);

        // THEN: Nur ACTIVE sollten in der Analyse sein
        assertEquals(1, analysis.getTotalCount());
        assertEquals("DUE-ACTIVE", analysis.getDueSchedules().get(0).getDueNumber());
    }

    @Test
    @DisplayName("Sollte canRunBillingBatch korrekt prüfen")
    void testCanRunBillingBatch() {
        // GIVEN: Keine Fälligkeiten
        assertFalse(analyzer.canRunBillingBatch(LocalDate.of(2025, 3, 15), true));

        // GIVEN: Eine Fälligkeit hinzufügen
        createDueSchedule("DUE-001", LocalDate.of(2025, 3, 15), BigDecimal.valueOf(100));

        // THEN: Jetzt sollte Run möglich sein
        assertTrue(analyzer.canRunBillingBatch(LocalDate.of(2025, 3, 15), true));
    }

    // Helper methods
    private void cleanupDatabase() {
        dueScheduleRepository.deleteAll();
        subscriptionRepository.deleteAll();
        contractRepository.deleteAll();
        customerRepository.deleteAll();
    }

    private void createBasicTestData() {
        Customer customer = new Customer();
        customer.setCustomerNumber("CUST-001");
        customer.setFirstName("Test");
        customer.setLastName("Customer");
        customer = customerRepository.save(customer);

        Contract contract = new Contract();
        contract.setContractNumber("CONTR-001");
        contract.setContractTitle("Test-Vertrag");
        contract.setCustomer(customer);
        contract.setStartDate(LocalDate.of(2024, 1, 1));
        contract = contractRepository.save(contract);

        testSubscription = new Subscription();
        testSubscription.setSubscriptionNumber("SUB-001");
        testSubscription.setContract(contract);
        testSubscription.setStartDate(LocalDate.of(2024, 1, 1));
        testSubscription.setProductName("Test-Abonnement");
        testSubscription.setMonthlyPrice(BigDecimal.valueOf(100));
        testSubscription = subscriptionRepository.save(testSubscription);
    }

    private DueSchedule createDueSchedule(String dueNumber, LocalDate dueDate, BigDecimal amount) {
        DueSchedule ds = new DueSchedule();
        ds.setDueNumber(dueNumber);
        ds.setDueDate(dueDate);
        //ds.(amount);
        ds.setStatus(DueStatus.ACTIVE);
        ds.setSubscription(testSubscription);
        ds.setPeriodStart(dueDate.withDayOfMonth(1));
        ds.setPeriodEnd(dueDate.withDayOfMonth(dueDate.lengthOfMonth()));

        return dueScheduleRepository.save(ds);
    }
}

// ===============================================================================================
// INTEGRATION TEST - Sicherheitsmassnahmen (mit echter DB)
// ===============================================================================================

/**
 * Testet die Sicherheitsmassnahmen des Rechnungslaufs end-to-end:
 * - Gleichzeitige Batches werden blockiert
 * - Bereits abgerechnete DueSchedules werden als Fehler gemeldet
 * - Vorgang-Statistiken werden auch bei partiellem Fehler korrekt gesetzt
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integration Tests: Rechnungslauf-Sicherheitsmassnahmen")
class InvoiceBatchSecurityIntegrationTest {

    @Autowired private InvoiceBatchOrchestrator orchestrator;
    @Autowired private DueScheduleRepository dueScheduleRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private OpenItemRepository openItemRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private ContractRepository contractRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private VorgangRepository vorgangRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private LocalDate billingDate;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        billingDate = LocalDate.of(2025, 3, 15);
        cleanupDatabase();
        createBaseTestData();
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
    }

    @Test
    @DisplayName("Gleichzeitiger Batch: Sollte blockiert werden wenn bereits ein Lauf aktiv ist")
    void testGleichzeitigerBatch_WirdAbgelehnt() {
        // GIVEN: Manuell einen Vorgang mit Status LAUFEND anlegen
        Vorgang laufenderVorgang = new Vorgang(VorgangTyp.RECHNUNGSLAUF, "Laufender Test-Batch");
        laufenderVorgang.setVorgangsnummer("VG-TEST-LAUFEND");
        laufenderVorgang.starten(); // → Status LAUFEND
        vorgangRepository.save(laufenderVorgang);
        vorgangRepository.flush();

        // WHEN & THEN: Neuer Batch soll abgewiesen werden
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orchestrator.runInvoiceBatch(billingDate, true, "admin"));

        assertTrue(ex.getMessage().contains("läuft bereits"),
                "Fehlermeldung soll auf laufenden Batch hinweisen");

        // Kein neuer Vorgang ausser dem manuell angelegten
        assertEquals(1, vorgangRepository.count(), "Es darf kein weiterer Vorgang erstellt worden sein");
    }

    @Test
    @DisplayName("Doppelverarbeitung: DueSchedule mit gesetzter InvoiceId wird als Fehler gemeldet")
    void testDoppelverarbeitung_WirdAlsFehlerGemeldet() {
        // GIVEN: DueSchedule mit bereits gesetzter InvoiceId aber Status ACTIVE (inkonsistenter Zustand)
        DueSchedule problematischeDue = new DueSchedule();
        problematischeDue.setDueNumber("DUE-DOPPELT");
        problematischeDue.setDueDate(billingDate);
        problematischeDue.setStatus(DueStatus.ACTIVE);
        problematischeDue.setInvoiceId(UUID.randomUUID()); // bereits abgerechnet!
        problematischeDue.setInvoiceBatchId("BATCH-ALT");
        problematischeDue.setSubscription(testSubscription);
        problematischeDue.setPeriodStart(billingDate.withDayOfMonth(1));
        problematischeDue.setPeriodEnd(billingDate.withDayOfMonth(billingDate.lengthOfMonth()));
        dueScheduleRepository.save(problematischeDue);

        // WHEN: Batch läuft
        InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate, true, "admin");

        // THEN: Fehler für diese DueSchedule gemeldet, keine neue Rechnung erstellt
        assertTrue(result.hasErrors(), "Sollte Fehler für die doppelte DueSchedule melden");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("DUE-DOPPELT")),
                "Fehlermeldung soll die betroffene DueNumber enthalten");
        assertEquals(0, invoiceRepository.count(), "Keine neue Rechnung darf erstellt worden sein");

        // Vorgang mit FEHLER-Status abgeschlossen
        List<Vorgang> vorgaenge = vorgangRepository.findByTyp(VorgangTyp.RECHNUNGSLAUF);
        assertEquals(1, vorgaenge.size());
        assertEquals(VorgangStatus.FEHLER, vorgaenge.get(0).getStatus());
    }

    @Test
    @DisplayName("Partieller Fehler: Vorgang-Statistiken werden auch bei Teilerfolg korrekt gesetzt")
    void testPartialerFehler_VorgangStatistiken() {
        // GIVEN: Eine gültige DueSchedule + eine mit bereits gesetzter InvoiceId
        createDueSchedule("DUE-OK", billingDate, BigDecimal.valueOf(100));

        DueSchedule dueDoppelt = new DueSchedule();
        dueDoppelt.setDueNumber("DUE-DOPPELT");
        dueDoppelt.setDueDate(billingDate);
        dueDoppelt.setStatus(DueStatus.ACTIVE);
        dueDoppelt.setInvoiceId(UUID.randomUUID());
        dueDoppelt.setSubscription(testSubscription);
        dueDoppelt.setPeriodStart(billingDate.withDayOfMonth(1));
        dueDoppelt.setPeriodEnd(billingDate.withDayOfMonth(billingDate.lengthOfMonth()));
        dueScheduleRepository.save(dueDoppelt);

        // WHEN: Batch läuft — 1 Erfolg, 1 Fehler
        InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate, true, "admin");

        // THEN: Ergebnis korrekt
        assertEquals(1, result.getCreatedInvoices(), "Genau 1 Rechnung soll erstellt worden sein");
        assertEquals(1, result.getErrorCount(), "Genau 1 Fehler soll vorliegen");
        assertTrue(result.hasErrors());

        // THEN: Vorgang-Statistiken gesetzt
        List<Vorgang> vorgaenge = vorgangRepository.findByTyp(VorgangTyp.RECHNUNGSLAUF);
        assertEquals(1, vorgaenge.size());
        Vorgang vorgang = vorgaenge.get(0);

        assertEquals(VorgangStatus.FEHLER, vorgang.getStatus());
        assertEquals(2, vorgang.getAnzahlVerarbeitet(), "2 Versuche gesamt");
        assertEquals(1, vorgang.getAnzahlErfolgreich(), "1 erfolgreicher Abschluss");
        assertEquals(1, vorgang.getAnzahlFehler(), "1 Fehler");
        assertNotNull(vorgang.getFehlerprotokoll(), "Fehlerprotokoll soll gesetzt sein");
    }

    // Helpers

    // Natives SQL-Cleanup: bypassed Soft-Delete, sodass FK-Constraints nicht verletzt werden
    private void cleanupDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM open_items");
        jdbcTemplate.execute("DELETE FROM invoice_items");
        jdbcTemplate.execute("DELETE FROM invoices");
        jdbcTemplate.execute("DELETE FROM due_schedules");
        jdbcTemplate.execute("DELETE FROM subscriptions");
        jdbcTemplate.execute("DELETE FROM contracts");
        jdbcTemplate.execute("DELETE FROM customers");
        jdbcTemplate.execute("DELETE FROM vorgaenge");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private void createBaseTestData() {
        Customer customer = new Customer();
        customer.setCustomerNumber("CUST-SEC-001");
        customer.setFirstName("Security");
        customer.setLastName("Test");
        customer = customerRepository.save(customer);

        Contract contract = new Contract();
        contract.setContractNumber("CONTR-SEC-001");
        contract.setContractTitle("Security-Test-Vertrag");
        contract.setCustomer(customer);
        contract.setStartDate(LocalDate.of(2024, 1, 1));
        contract = contractRepository.save(contract);

        testSubscription = new Subscription();
        testSubscription.setSubscriptionNumber("SUB-SEC-001");
        testSubscription.setContract(contract);
        testSubscription.setStartDate(LocalDate.of(2024, 1, 1));
        testSubscription.setProductName("Test-Abonnement");
        testSubscription.setMonthlyPrice(BigDecimal.valueOf(100));
        testSubscription = subscriptionRepository.save(testSubscription);
    }

    private DueSchedule createDueSchedule(String dueNumber, LocalDate dueDate, BigDecimal amount) {
        DueSchedule ds = new DueSchedule();
        ds.setDueNumber(dueNumber);
        ds.setDueDate(dueDate);
        ds.setStatus(DueStatus.ACTIVE);
        ds.setSubscription(testSubscription);
        ds.setPeriodStart(dueDate.withDayOfMonth(1));
        ds.setPeriodEnd(dueDate.withDayOfMonth(dueDate.lengthOfMonth()));
        return dueScheduleRepository.save(ds);
    }
}

// ===============================================================================================
// INTEGRATION TEST - E-Mail-Fallback (mit MockBean)
// ===============================================================================================

/**
 * Testet dass ein E-Mail-Fehler die Buchung (Rechnung + OpenItem + DueSchedule) nicht verhindert.
 * Verwendet @MockBean um den E-Mail-Dienst kontrolliert zum Fehlschlagen zu bringen.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integration Tests: E-Mail-Fehler bricht Buchung nicht ab")
class InvoiceBatchEmailFallbackTest {

    @Autowired private InvoiceBatchOrchestrator orchestrator;
    @Autowired private DueScheduleRepository dueScheduleRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private OpenItemRepository openItemRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private ContractRepository contractRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private VorgangRepository vorgangRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockBean
    private CustomerEmailService customerEmailService;

    private LocalDate billingDate;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        billingDate = LocalDate.of(2025, 3, 15);
        cleanupDatabase();
        createBaseTestData();

        // E-Mail-Dienst wirft bei jedem Aufruf
        doThrow(new RuntimeException("SMTP-Server nicht erreichbar"))
                .when(customerEmailService).sendInvoiceEmail(any());
    }

    @Test
    @DisplayName("E-Mail-Fehler: Rechnung und OpenItem werden trotzdem gespeichert")
    void testEmailFehler_BrichtBuchungNichtAb() {
        // GIVEN: Eine fällige DueSchedule
        DueSchedule ds = new DueSchedule();
        ds.setDueNumber("DUE-EMAIL-TEST");
        ds.setDueDate(billingDate);
        ds.setStatus(DueStatus.ACTIVE);
        ds.setSubscription(testSubscription);
        ds.setPeriodStart(billingDate.withDayOfMonth(1));
        ds.setPeriodEnd(billingDate.withDayOfMonth(billingDate.lengthOfMonth()));
        dueScheduleRepository.save(ds);

        // WHEN: E-Mail-Dienst schlägt fehl
        InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate, true, "admin");

        // THEN: Rechnung und OpenItem wurden trotzdem angelegt
        assertEquals(1, result.getCreatedInvoices(), "Rechnung soll trotz E-Mail-Fehler erstellt worden sein");
        assertEquals(1, result.getCreatedOpenItems(), "OpenItem soll trotz E-Mail-Fehler erstellt worden sein");
        assertFalse(result.hasErrors(), "E-Mail-Fehler soll kein Batch-Fehler sein");

        // THEN: DueSchedule korrekt abgeschlossen
        DueSchedule verarbeitet = dueScheduleRepository.findByDueNumber("DUE-EMAIL-TEST").orElseThrow();
        assertEquals(DueStatus.COMPLETED, verarbeitet.getStatus());
        assertNotNull(verarbeitet.getInvoiceId());
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
    }

    // Helpers

    private void cleanupDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM open_items");
        jdbcTemplate.execute("DELETE FROM invoice_items");
        jdbcTemplate.execute("DELETE FROM invoices");
        jdbcTemplate.execute("DELETE FROM due_schedules");
        jdbcTemplate.execute("DELETE FROM subscriptions");
        jdbcTemplate.execute("DELETE FROM contracts");
        jdbcTemplate.execute("DELETE FROM customers");
        jdbcTemplate.execute("DELETE FROM vorgaenge");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private void createBaseTestData() {
        Customer customer = new Customer();
        customer.setCustomerNumber("CUST-EMAIL-001");
        customer.setFirstName("Email");
        customer.setLastName("Test");
        customer = customerRepository.save(customer);

        Contract contract = new Contract();
        contract.setContractNumber("CONTR-EMAIL-001");
        contract.setContractTitle("Email-Test-Vertrag");
        contract.setCustomer(customer);
        contract.setStartDate(LocalDate.of(2024, 1, 1));
        contract = contractRepository.save(contract);

        testSubscription = new Subscription();
        testSubscription.setSubscriptionNumber("SUB-EMAIL-001");
        testSubscription.setContract(contract);
        testSubscription.setStartDate(LocalDate.of(2024, 1, 1));
        testSubscription.setProductName("Test-Abonnement");
        testSubscription.setMonthlyPrice(BigDecimal.valueOf(100));
        testSubscription = subscriptionRepository.save(testSubscription);
    }
}

/*
 * ===============================================================================================
 * ZUSAMMENFASSUNG: UNIT VS INTEGRATION TESTS
 * ===============================================================================================
 *
 * UNIT TESTS:
 * ✓ Schnell (Millisekunden)
 * ✓ Isoliert (nur eine Klasse)
 * ✓ Verwendet Mocks (@Mock, @InjectMocks)
 * ✓ Testet Logik ohne externe Abhängigkeiten
 * ✓ Gut für: Algorithmen, Business-Logik, Edge-Cases
 * ✗ Findet keine Integrationsprobleme
 * ✗ Testet nicht das echte Zusammenspiel
 *
 * INTEGRATION TESTS:
 * ✓ Realistisch (echte DB, echte Services)
 * ✓ Findet Integrationsprobleme
 * ✓ Testet End-to-End Szenarien
 * ✓ Validiert Datenbank-Schema
 * ✓ Gut für: Workflows, Transaktionen, Service-Zusammenspiel
 * ✗ Langsamer (Sekunden)
 * ✗ Komplexer Setup
 * ✗ Schwieriger zu debuggen
 *
 * BESTE PRAXIS:
 * - Viele Unit Tests (80%) - für schnelles Feedback
 * - Wenige Integration Tests (20%) - für kritische Workflows
 * - Unit Tests für jede Klasse
 * - Integration Tests für wichtige User Stories
 *
 * TEST-PYRAMIDE:
 *        /\
 *       /  \     E2E Tests (wenige, langsam)
 *      /----\
 *     / Integ \   Integration Tests (einige, mittel)
 *    /--------\
 *   /   Unit   \  Unit Tests (viele, schnell)
 *  /------------\
 *
 * ===============================================================================================
 */