// ===============================================================================================
// 1. INIT DATA ORCHESTRATOR (Hauptklasse - nur Koordination)
// ===============================================================================================

package com.erp.backend.service.init;

import com.erp.backend.domain.VorgangTyp;
import com.erp.backend.domain.Vorgang;
import com.erp.backend.service.KontoService;
import com.erp.backend.service.VorgangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * HAUPTORCHESTRATOR FÜR DATENINITIALISIERUNG
 *
 * Diese Klasse koordiniert die gesamte Testdaten-Initialisierung.
 * Sie ist wie ein "Dirigent" der verschiedene Initializer orchestriert.
 *
 * Warum eine Orchestrator-Klasse?
 * - Separation of Concerns: Koordination vs. Detail-Implementierung
 * - Vorgang-Management: Zentrales Tracking aller Initialisierungsschritte
 * - Error Handling: Einheitliche Fehlerbehandlung
 * - Testability: Jeder Initializer kann isoliert getestet werden
 */
@Service
@ConditionalOnProperty(name = "app.init.enabled", havingValue = "true")
public class InitDataOrchestrator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(InitDataOrchestrator.class);

    // Neue Property für automatischen Start
    @Value("${app.init.auto-run-on-startup:false}")
    private boolean autoRunOnStartup;

    // DEPENDENCY INJECTION: Spring gibt uns automatisch die Services die wir brauchen
    private final MasterDataInitializer masterDataInitializer;    // Stammdaten (Adressen, Kunden, Produkte)
    private final BusinessDataInitializer businessDataInitializer; // Geschäftsdaten (Verträge, Abos)
    private final BillingDataInitializer billingDataInitializer;   // Abrechnungsdaten (Fälligkeiten, Rechnungen)
    private final VorgangService vorgangService;                   // Vorgang-Management
    private final DataStatusReporter dataStatusReporter;          // Status-Reporting
    private final KontoService kontoService;                       // Kontenplan (SKR04)

    /**
     * KONSTRUKTOR mit Dependency Injection
     * Spring injiziert automatisch alle benötigten Services
     */
    public InitDataOrchestrator(MasterDataInitializer masterDataInitializer,
                                BusinessDataInitializer businessDataInitializer,
                                BillingDataInitializer billingDataInitializer,
                                VorgangService vorgangService,
                                DataStatusReporter dataStatusReporter,
                                KontoService kontoService) {
        this.masterDataInitializer = masterDataInitializer;
        this.businessDataInitializer = businessDataInitializer;
        this.billingDataInitializer = billingDataInitializer;
        this.vorgangService = vorgangService;
        this.dataStatusReporter = dataStatusReporter;
        this.kontoService = kontoService;
    }

    /**
     * SPRING BOOT STARTUP-METHODE
     * Wird automatisch beim Application-Start ausgeführt
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {

        // SKR04 immer initialisieren — unabhängig von autoRunOnStartup
        try {
            int created = kontoService.initSkr04();
            if (created > 0) {
                logger.info("Buchhaltung: {} SKR04-Konten angelegt", created);
            } else {
                logger.info("Buchhaltung: Kontenplan bereits vorhanden");
            }
        } catch (Exception e) {
            logger.warn("SKR04-Initialisierung fehlgeschlagen (nicht kritisch): {}", e.getMessage());
        }

        if (!autoRunOnStartup) {
            logger.info("Auto-run-on-startup is disabled. Skipping automatic data initialization.");
            logger.info("Use REST endpoints at /api/init/* for manual initialization.");
            return;
        }

        // Hauptvorgang für Startup-Initialisierung starten
        Vorgang startupVorgang = vorgangService.starteAutomatischenVorgang(
                VorgangTyp.DATENMIGRATION,
                "Startup-Dateninitialisierung mit ALL-ACTIVE Standard"
        );

        try {
            logger.info("🚀 Starting automatic data initialization...");
            logger.info("Vorgang: {}", startupVorgang.getVorgangsnummer());

            // Standard-Initialisierung ausführen
            initializeData(InitMode.FULL, LocalDate.now(), InitConfig.allActive());

            // Vorgang erfolgreich abschließen
            vorgangService.vorgangErfolgreichAbschliessen(startupVorgang.getId());
            logger.info("✅ Data initialization completed successfully");

        } catch (Exception e) {
            logger.error("❌ Data initialization failed", e);
            vorgangService.vorgangMitFehlerAbschliessen(startupVorgang.getId(), e.getMessage());
            // Nicht weiterwerfen - App soll trotzdem starten können
        }
    }

    /**
     * HAUPTMETHODE FÜR DATENINITIALISIERUNG
     *
     * Koordiniert alle Initialisierungsschritte mit vollständigem Vorgang-Tracking
     *
     * @param mode Initialisierungsmodus (BASIC, FULL, etc.)
     * @param billingDate Stichtag für Rechnungslauf (optional)
     * @param config Konfiguration für Status-Verteilungen
     */
    @Transactional
    public void initializeData(InitMode mode, LocalDate billingDate, InitConfig config) {
        if (config == null) {
            config = InitConfig.allActive();
        }

        // Hauptvorgang für gesamte Initialisierung starten
        String titel = String.format("Dateninitialisierung: %s", mode.getDescription());
        String beschreibung = String.format("Modus: %s, Konfiguration: %s",
                mode, getConfigDescription(config));


        Vorgang hauptVorgang = vorgangService.starteVorgang(
                VorgangTyp.DATENMIGRATION, titel, beschreibung, "InitDataOrchestrator", true
        );

        try {
            logger.info("===========================================");
            logger.info("Starte Testdaten-Initialisierung");
            logger.info("Vorgang: {}", hauptVorgang.getVorgangsnummer());
            logger.info("Modus: {} - {}", mode, mode.getDescription());
            logger.info("===========================================");

            int totalSteps = 0;
            int completedSteps = 0;

            // SCHRITT 0: KONTENPLAN (SKR04) INITIALISIEREN
            if (executeInitializationStep("Kontenplan SKR04", () ->
                    kontoService.initSkr04())) {
                completedSteps++;
            }
            totalSteps++;

            // SCHRITT 1: STAMMDATEN INITIALISIEREN
            if (executeInitializationStep("Stammdaten", () ->
                    masterDataInitializer.initializeMasterData())) {
                completedSteps++;
            }
            totalSteps++;

            if (mode == InitMode.BASIC) {
                finishInitialization(hauptVorgang, completedSteps, totalSteps, "BASIC Mode");
                return;
            }

            // SCHRITT 2: GESCHÄFTSDATEN INITIALISIEREN
            InitConfig finalConfig1 = config;
            if (executeInitializationStep("Geschäftsdaten", () ->
                    businessDataInitializer.initializeBusinessData(finalConfig1))) {
                completedSteps++;
            }
            totalSteps++;

            if (mode == InitMode.CONTRACTS) {
                finishInitialization(hauptVorgang, completedSteps, totalSteps, "CONTRACTS Mode");
                return;
            }

            // SCHRITT 3: ABRECHNUNGSDATEN INITIALISIEREN
            InitConfig finalConfig = config;
            if (executeInitializationStep("Abrechnungsdaten", () ->
                    billingDataInitializer.initializeBillingData(finalConfig, mode, billingDate))) {
                completedSteps++;
            }
            totalSteps++;

            // Initialisierung abschließen
            finishInitialization(hauptVorgang, completedSteps, totalSteps, mode.getDescription());

        } catch (Exception e) {
            logger.error("Kritischer Fehler bei Dateninitialisierung", e);
            vorgangService.vorgangMitFehlerAbschliessen(hauptVorgang.getId(), e.getMessage());
            throw new RuntimeException("Dateninitialisierung fehlgeschlagen", e);
        }
    }

    /**
     * HILFSMETHODE: Einzelnen Initialisierungsschritt ausführen
     */
    private boolean executeInitializationStep(String stepName, Runnable initStep) {
        try {
            logger.info("Starte {}-Initialisierung...", stepName);
            initStep.run();
            logger.info("✓ {}-Initialisierung erfolgreich", stepName);
            return true;
        } catch (Exception e) {
            logger.error("✗ Fehler bei {}-Initialisierung: {}", stepName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * HILFSMETHODE: Initialisierung abschließen
     */
    private void finishInitialization(Vorgang vorgang, int completed, int total, String mode) {
        // Vorgang abschließen
        if (completed == total) {
            vorgangService.vorgangErfolgreichAbschliessen(vorgang.getId(),
                    total, completed, 0, null);
        } else {
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(),
                    String.format("%d von %d Schritten fehlgeschlagen", total - completed, total));
        }

        logger.info("{} abgeschlossen ({}/{} Schritte erfolgreich)", mode, completed, total);
        try {
            dataStatusReporter.logCurrentDataStatus();
        } catch (Exception e) {
            logger.warn("Status-Report nach Initialisierung fehlgeschlagen: {}", e.getMessage());
        }
    }

    /**
     * HILFSMETHODE: Config-Beschreibung erstellen
     */
    private String getConfigDescription(InitConfig config) {
        if (config.isAllActive()) {
            return "ALL-ACTIVE (Standard)";
        } else {
            return String.format("Custom (Verträge: %.0f%% aktiv, Abos: %.0f%% aktiv)",
                    config.getActiveContractRatio() * 100,
                    config.getActiveSubscriptionRatio() * 100);
        }
    }

    // ===============================================================================================
    // CONVENIENCE-METHODEN FÜR VERSCHIEDENE SZENARIEN
    // ===============================================================================================

    /**
     * Initialisierung mit realistischen Testdaten
     */
    @Transactional
    public void initRealisticTestData() {
        initializeData(InitMode.FULL, LocalDate.now(), InitConfig.realistic());
    }

    /**
     * Initialisierung mit Development-Daten (viele aktive Daten)
     */
    @Transactional
    public void initDevelopmentData() {
        initializeData(InitMode.FULL, LocalDate.now(), InitConfig.development());
    }

    /**
     * Standard-Initialisierung (alles aktiv)
     */
    @Transactional
    public void initAllActiveData() {
        initializeData(InitMode.FULL, LocalDate.now(), InitConfig.allActive());
    }
}
