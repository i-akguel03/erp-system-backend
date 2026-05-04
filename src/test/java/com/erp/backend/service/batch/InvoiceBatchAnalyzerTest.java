// ===============================================================================================
// UNIT TESTS - Testen einzelne Klassen ISOLIERT mit Mocks
// ===============================================================================================

package com.erp.backend.service.batch;

import com.erp.backend.domain.*;
import com.erp.backend.repository.DueScheduleRepository;
import com.erp.backend.repository.InvoiceRepository;
import com.erp.backend.repository.OpenItemRepository;
import com.erp.backend.service.DueScheduleService;
import com.erp.backend.service.InvoiceFactory;
import com.erp.backend.service.OpenItemFactory;
import com.erp.backend.service.VorgangService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UNIT TEST - InvoiceBatchAnalyzer
 *
 * Was testen wir?
 * - Analyse-Logik für Fälligkeiten
 * - Gruppierung nach Monaten
 * - Zählung von überfälligen vs. aktuellen Fälligkeiten
 *
 * Warum Unit Test?
 * - Testet NUR die Analyzer-Logik
 * - Verwendet Mocks für Repository (keine echte Datenbank)
 * - Sehr schnell (keine DB-Operationen)
 * - Fokussiert auf eine Komponente
 */
@ExtendWith(MockitoExtension.class) // JUnit 5 + Mockito Integration
@DisplayName("Unit Tests: InvoiceBatchAnalyzer")
class InvoiceBatchAnalyzerTest {

    @Mock // Mock-Objekt: Simuliert das Repository ohne echte DB
    private DueScheduleRepository dueScheduleRepository;

    @InjectMocks // Automatisch erstellt mit den Mock-Dependencies
    private InvoiceBatchAnalyzer analyzer;

    private LocalDate billingDate;
    private List<DueSchedule> testSchedules;

    /**
     * SETUP - Läuft VOR jedem Test
     * Bereitet Testdaten vor
     */
    @BeforeEach
    void setUp() {
        billingDate = LocalDate.of(2025, 3, 15);
        testSchedules = createTestDueSchedules();
    }

    @Test
    @DisplayName("Sollte alle Fälligkeiten korrekt analysieren (mit vorherigen Monaten)")
    void testAnalyzeBillingScope_WithPreviousMonths() {
        // GIVEN: Mock so konfigurieren dass er unsere Testdaten zurückgibt
        when(dueScheduleRepository.findByStatusAndDueDateLessThanEqual(
                DueStatus.ACTIVE, billingDate))
                .thenReturn(testSchedules);

        // WHEN: Methode aufrufen die wir testen wollen
        InvoiceBatchAnalysis result = analyzer.analyzeBillingScope(billingDate, true);

        // THEN: Assertions - Prüfen ob das Ergebnis korrekt ist
        assertNotNull(result, "Analysis darf nicht null sein");
        assertEquals(5, result.getTotalCount(), "Sollte 5 Fälligkeiten haben");
        assertEquals(2, result.getOverdueCount(), "Sollte 2 überfällige haben");
        assertEquals(2, result.getCurrentCount(), "Sollte 2 aktuelle haben");
        assertEquals(3, result.getMonthCount(), "Sollte 3 verschiedene Monate haben");

        // Verify: Prüfen ob Mock richtig aufgerufen wurde
        verify(dueScheduleRepository, times(1))
                .findByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, billingDate);
    }

    @Test
    @DisplayName("Sollte nur exakte Fälligkeiten analysieren (ohne vorherige Monate)")
    void testAnalyzeBillingScope_ExactDateOnly() {
        // GIVEN: Mock für exaktes Datum
        List<DueSchedule> exactDateSchedules = testSchedules.stream()
                .filter(ds -> ds.getDueDate().equals(billingDate))
                .toList();

        when(dueScheduleRepository.findByStatusAndDueDate(
                DueStatus.ACTIVE, billingDate))
                .thenReturn(exactDateSchedules);

        // WHEN
        InvoiceBatchAnalysis result = analyzer.analyzeBillingScope(billingDate, false);

        // THEN
        assertEquals(2, result.getTotalCount(), "Sollte nur 2 Fälligkeiten für exaktes Datum haben");
        assertEquals(0, result.getOverdueCount(), "Sollte keine überfälligen haben");
        assertEquals(2, result.getCurrentCount(), "Sollte 2 aktuelle haben");

        verify(dueScheduleRepository, times(1))
                .findByStatusAndDueDate(DueStatus.ACTIVE, billingDate);
    }

    @Test
    @DisplayName("Sollte leere Analyse zurückgeben wenn keine Fälligkeiten vorhanden")
    void testAnalyzeBillingScope_Empty() {
        // GIVEN: Leere Liste
        when(dueScheduleRepository.findByStatusAndDueDateLessThanEqual(
                any(), any()))
                .thenReturn(List.of());

        // WHEN
        InvoiceBatchAnalysis result = analyzer.analyzeBillingScope(billingDate, true);

        // THEN
        assertEquals(0, result.getTotalCount());
        assertTrue(result.isEmpty());
        assertFalse(result.hasOverdueItems());
    }

    @Test
    @DisplayName("Sollte Monats-Gruppierung korrekt erstellen")
    void testMonthGrouping() {
        // GIVEN
        when(dueScheduleRepository.findByStatusAndDueDateLessThanEqual(
                DueStatus.ACTIVE, billingDate))
                .thenReturn(testSchedules);

        // WHEN
        InvoiceBatchAnalysis result = analyzer.analyzeBillingScope(billingDate, true);

        // THEN: Prüfe Monats-Gruppierung
        Map<String, List<DueSchedule>> monthGroups = result.getMonthGroups();

        assertTrue(monthGroups.containsKey("2025-01"), "Sollte Januar 2025 enthalten");
        assertTrue(monthGroups.containsKey("2025-02"), "Sollte Februar 2025 enthalten");
        assertTrue(monthGroups.containsKey("2025-03"), "Sollte März 2025 enthalten");

        assertEquals(2, monthGroups.get("2025-01").size(), "Januar sollte 2 Fälligkeiten haben");
        assertEquals(1, monthGroups.get("2025-02").size(), "Februar sollte 1 Fälligkeit haben");
        assertEquals(2, monthGroups.get("2025-03").size(), "März sollte 2 Fälligkeiten haben");
    }

    /**
     * HELPER: Erstellt Test-DueSchedules
     */
    private List<DueSchedule> createTestDueSchedules() {
        return List.of(
                createDueSchedule("DUE-001", LocalDate.of(2025, 1, 15), BigDecimal.valueOf(100)),
                createDueSchedule("DUE-002", LocalDate.of(2025, 1, 20), BigDecimal.valueOf(200)),
                createDueSchedule("DUE-003", LocalDate.of(2025, 2, 15), BigDecimal.valueOf(150)),
                createDueSchedule("DUE-004", LocalDate.of(2025, 3, 15), BigDecimal.valueOf(300)),
                createDueSchedule("DUE-005", LocalDate.of(2025, 3, 15), BigDecimal.valueOf(250))
        );
    }

    private DueSchedule createDueSchedule(String dueNumber, LocalDate dueDate, BigDecimal amount) {
        DueSchedule ds = new DueSchedule();
        ds.setDueNumber(dueNumber);
        ds.setDueDate(dueDate);
        //ds.setAmount(amount);
        ds.setStatus(DueStatus.ACTIVE);

        // Mock Subscription
        Subscription sub = new Subscription();
        sub.setId(UUID.randomUUID());
        ds.setSubscription(sub);

        return ds;
    }
}

// ===============================================================================================

/**
 * UNIT TEST - InvoiceBatchProcessor
 *
 * Was testen wir?
 * - Verarbeitung einzelner Fälligkeiten
 * - Invoice/OpenItem-Erstellung
 * - Fehler-Handling
 *
 * Besonderheit:
 * - Viele Mocks weil Processor mit vielen Services interagiert
 * - Test-Komplexität zeigt: Diese Klasse hat viele Dependencies (Code-Smell?)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: InvoiceBatchProcessor")
class InvoiceBatchProcessorTest {

    @Mock private InvoiceFactory invoiceFactory;
    @Mock private OpenItemFactory openItemFactory;
    @Mock private DueScheduleService dueScheduleService;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private OpenItemRepository openItemRepository;

    @InjectMocks
    private InvoiceBatchProcessor processor;

    private InvoiceBatchAnalysis mockAnalysis;
    private Vorgang mockVorgang;
    private LocalDate billingDate;

    @BeforeEach
    void setUp() {
        billingDate = LocalDate.of(2025, 3, 15);
        mockVorgang = createMockVorgang();
        mockAnalysis = createMockAnalysis();
    }

    @Test
    @DisplayName("Sollte erfolgreiche Batch-Verarbeitung durchführen")
    void testProcessBatch_Success() {
        // GIVEN: Alle Mocks konfigurieren
        DueSchedule dueSchedule = createTestDueSchedule();
        Invoice mockInvoice = createMockInvoice();
        OpenItem mockOpenItem = createMockOpenItem();

        when(invoiceFactory.createInvoiceForDueSchedule(any(), any(), any(), anyBoolean()))
                .thenReturn(mockInvoice);
        when(invoiceRepository.save(any(Invoice.class)))
                .thenReturn(mockInvoice);
        when(openItemFactory.createOpenItemForInvoice(any(), anyBoolean()))
                .thenReturn(mockOpenItem);
        when(openItemRepository.save(any(OpenItem.class)))
                .thenReturn(mockOpenItem);

        // WHEN
        InvoiceBatchResult result = processor.processBatch(mockAnalysis, mockVorgang, billingDate);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.getCreatedInvoices());
        assertEquals(1, result.getCreatedOpenItems());
        assertEquals(1, result.getProcessedDueSchedules());
        assertFalse(result.hasErrors());

        // Verify: Alle Services wurden aufgerufen
        verify(invoiceFactory, times(1)).createInvoiceForDueSchedule(any(), any(), any(), anyBoolean());
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
        verify(dueScheduleService, times(1)).markAsCompleted(any(UUID.class), any(UUID.class), anyString());
        verify(openItemFactory, times(1)).createOpenItemForInvoice(any(), anyBoolean());
        verify(openItemRepository, times(1)).save(any(OpenItem.class));
    }

    @Test
    @DisplayName("Sollte Fehler korrekt behandeln und Rollback durchführen")
    void testProcessBatch_WithError() {
        // GIVEN: InvoiceFactory wirft Exception
        DueSchedule dueSchedule = createTestDueSchedule();

        when(invoiceFactory.createInvoiceForDueSchedule(any(), any(), any(), anyBoolean()))
                .thenThrow(new RuntimeException("Invoice creation failed"));

        // WHEN
        InvoiceBatchResult result = processor.processBatch(mockAnalysis, mockVorgang, billingDate);

        // THEN
        assertTrue(result.hasErrors(), "Sollte Fehler enthalten");
        assertEquals(1, result.getErrorCount());
        assertEquals(0, result.getCreatedInvoices());

        // Verify: Rollback wurde aufgerufen
        verify(dueScheduleService, times(1))
                .rollbackCompleted(any(UUID.class), contains("Rollback due to billing error"));
    }

    // Helper Methods
    private InvoiceBatchAnalysis createMockAnalysis() {
        DueSchedule ds = createTestDueSchedule();
        return new InvoiceBatchAnalysis(
                List.of(ds),
                billingDate,
                true,
                0L, 1L,
                Map.of("2025-03", List.of(ds))
        );
    }

    private DueSchedule createTestDueSchedule() {
        DueSchedule ds = new DueSchedule();
        ds.setId(UUID.randomUUID());
        ds.setDueNumber("DUE-001");
        ds.setDueDate(billingDate);
        //ds.setAmount(BigDecimal.valueOf(100));
        ds.setStatus(DueStatus.ACTIVE);

        Subscription sub = new Subscription();
        sub.setId(UUID.randomUUID());
        ds.setSubscription(sub);

        return ds;
    }

    private Invoice createMockInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-001");
        invoice.setTotalAmount(BigDecimal.valueOf(100));
        invoice.setSubscriptionId(UUID.randomUUID());
        return invoice;
    }

    private OpenItem createMockOpenItem() {
        OpenItem oi = new OpenItem();
        oi.setId(UUID.randomUUID());
        oi.setAmount(BigDecimal.valueOf(100));
        return oi;
    }

    private Vorgang createMockVorgang() {
        Vorgang v = new Vorgang();
        v.setId(UUID.randomUUID());
        v.setVorgangsnummer("VG-001");
        v.setTyp(VorgangTyp.RECHNUNGSLAUF);
        return v;
    }
}

// ===============================================================================================

/**
 * UNIT TEST - InvoiceBatchOrchestrator
 *
 * Was testen wir?
 * - Orchestrierung der verschiedenen Services
 * - Vorgang-Handling (Start, Abschluss)
 * - Error-Handling auf Orchestrator-Ebene
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: InvoiceBatchOrchestrator")
class InvoiceBatchOrchestratorTest {

    @Mock private InvoiceBatchProcessor processor;
    @Mock private InvoiceBatchAnalyzer analyzer;
    @Mock private VorgangService vorgangService;

    @InjectMocks
    private InvoiceBatchOrchestrator orchestrator;

    private LocalDate billingDate;
    private Vorgang mockVorgang;

    @BeforeEach
    void setUp() {
        billingDate = LocalDate.of(2025, 3, 15);
        mockVorgang = createMockVorgang();
    }

    @Test
    @DisplayName("Sollte erfolgreichen Rechnungslauf orchestrieren")
    void testRunInvoiceBatch_Success() {
        // GIVEN
        InvoiceBatchAnalysis mockAnalysis = createMockAnalysis(5);
        InvoiceBatchResult mockResult = createSuccessResult();

        when(vorgangService.starteAutomatischenVorgang(any(VorgangTyp.class), anyString()))
                .thenReturn(mockVorgang);
        when(analyzer.analyzeBillingScope(any(LocalDate.class), anyBoolean()))
                .thenReturn(mockAnalysis);
        when(processor.processBatch(any(InvoiceBatchAnalysis.class), any(Vorgang.class), any(LocalDate.class)))
                .thenReturn(mockResult);

        // WHEN
        InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate);

        // THEN
        assertNotNull(result);
        assertEquals("VG-001", result.getVorgangsnummer());
        assertFalse(result.hasErrors());

        // Verify: Vorgang wurde gestartet und erfolgreich abgeschlossen
        verify(vorgangService, times(1))
                .starteAutomatischenVorgang(any(VorgangTyp.class), anyString());
        verify(vorgangService, times(1))
                .vorgangErfolgreichAbschliessen(any(UUID.class), anyInt(), anyInt(), eq(0), any());
        verify(vorgangService, never())
                .vorgangMitFehlerAbschliessen(any(UUID.class), anyString());
    }

    @Test
    @DisplayName("Sollte bei leerer Analyse früh beenden")
    void testRunInvoiceBatch_EmptyAnalysis() {
        // GIVEN: Keine Fälligkeiten
        InvoiceBatchAnalysis emptyAnalysis = createMockAnalysis(0);

        when(vorgangService.starteAutomatischenVorgang(any(VorgangTyp.class), anyString()))
                .thenReturn(mockVorgang);
        when(analyzer.analyzeBillingScope(any(LocalDate.class), anyBoolean()))
                .thenReturn(emptyAnalysis);

        // WHEN
        InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate);

        // THEN
        assertNotNull(result);
        assertEquals("Keine offenen Fälligkeiten gefunden", result.getMessage());

        // Verify: Processor wurde NICHT aufgerufen
        verify(processor, never()).processBatch(any(), any(), any());

        // Verify: Vorgang wurde trotzdem erfolgreich abgeschlossen
        verify(vorgangService, times(1))
                .vorgangErfolgreichAbschliessen(any(UUID.class), eq(0), eq(0), eq(0), any());
    }

    @Test
    @DisplayName("Sollte Exception korrekt behandeln")
    void testRunInvoiceBatch_WithException() {
        // GIVEN: Analyzer wirft Exception
        when(vorgangService.starteAutomatischenVorgang(any(VorgangTyp.class), anyString()))
                .thenReturn(mockVorgang);
        when(analyzer.analyzeBillingScope(any(LocalDate.class), anyBoolean()))
                .thenThrow(new RuntimeException("Database error"));

        // WHEN & THEN
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                orchestrator.runInvoiceBatch(billingDate));

        assertTrue(exception.getMessage().contains("Rechnungslauf fehlgeschlagen"));
        assertTrue(exception.getMessage().contains("VG-001"));

        // Verify: Vorgang wurde mit Fehler abgeschlossen
        verify(vorgangService, times(1))
                .vorgangMitFehlerAbschliessen(any(UUID.class), contains("Database error"));
    }

    // Helper Methods
    private InvoiceBatchAnalysis createMockAnalysis(int count) {
        List<DueSchedule> schedules = count > 0 ?
                List.of(createTestDueSchedule()) : List.of();

        return new InvoiceBatchAnalysis(
                schedules,
                billingDate,
                true,
                0L, (long) count,
                count > 0 ? Map.of("2025-03", schedules) : Map.of()
        );
    }

    private DueSchedule createTestDueSchedule() {
        DueSchedule ds = new DueSchedule();
        ds.setId(UUID.randomUUID());
        ds.setDueNumber("DUE-001");
        ds.setDueDate(billingDate);
        ds.setSubscription(new Subscription());
        return ds;
    }

    private InvoiceBatchResult createSuccessResult() {
        return new InvoiceBatchResult.Builder()
                .withVorgangsnummer("VG-001")
                .withBatchId("BATCH-001")
                .addAmount(BigDecimal.valueOf(100))
                .build();
    }

    private Vorgang createMockVorgang() {
        Vorgang v = new Vorgang();
        v.setId(UUID.randomUUID());
        v.setVorgangsnummer("VG-001");
        v.setTyp(VorgangTyp.RECHNUNGSLAUF);
        v.setStartZeitpunkt(LocalDateTime.now());
        return v;
    }
}