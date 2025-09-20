package com.erp.backend.controller;

import com.erp.backend.service.init.InitDataOrchestrator;
import com.erp.backend.service.init.InitConfig;
import com.erp.backend.service.init.InitMode;
import com.erp.backend.service.init.DataStatusReporter;
import com.erp.backend.service.init.DataManagementUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/init")
public class InitController {

    @Autowired
    private InitDataOrchestrator initDataOrchestrator;

    @Autowired
    private DataStatusReporter dataStatusReporter;

    @Autowired
    private DataManagementUtils dataManagementUtils;

    // ===============================================================================================
    // STANDARD-ENDPUNKTE (Alles ACTIVE)
    // ===============================================================================================

    /**
     * Nur Basisdaten (Adressen, Kunden, Produkte)
     */
    @PostMapping("/basic")
    public ResponseEntity<String> initBasic() {
        initDataOrchestrator.initializeData(InitMode.BASIC, null, null);
        return ResponseEntity.ok("✓ Basisdaten initialisiert (Adressen, Kunden, Produkte)");
    }

    /**
     * Bis Verträge und Abonnements - STANDARD: Alles ACTIVE
     */
    @PostMapping("/contracts")
    public ResponseEntity<String> initContracts() {
        initDataOrchestrator.initializeData(InitMode.CONTRACTS, null, InitConfig.allActive());
        return ResponseEntity.ok("✓ Daten bis Verträge und Abonnements initialisiert (STANDARD: Alles ACTIVE)");
    }

    /**
     * Bis Fälligkeitspläne - STANDARD: Alles ACTIVE
     */
    @PostMapping("/schedules")
    public ResponseEntity<String> initSchedules() {
        initDataOrchestrator.initializeData(InitMode.SCHEDULES, null, InitConfig.allActive());
        return ResponseEntity.ok("✓ Daten bis Fälligkeitspläne initialisiert (STANDARD: Alles ACTIVE)");
    }

    /**
     * Mit manuellen Sample-Rechnungen - STANDARD: Alles ACTIVE
     */
    @PostMapping("/invoices")
    public ResponseEntity<String> initInvoices() {
        initDataOrchestrator.initializeData(InitMode.INVOICES_MANUAL, null, InitConfig.allActive());
        return ResponseEntity.ok("✓ Daten mit manuellen Sample-Rechnungen initialisiert (STANDARD: Alles ACTIVE)");
    }

    /**
     * Komplette Initialisierung mit Rechnungslauf heute - STANDARD: Alles ACTIVE
     */
    @PostMapping("/full")
    public ResponseEntity<String> initFull() {
        initDataOrchestrator.initializeData(InitMode.FULL, LocalDate.now(), InitConfig.allActive());
        return ResponseEntity.ok("✓ Komplette Initialisierung mit Rechnungslauf durchgeführt (STANDARD: Alles ACTIVE)");
    }

    /**
     * Mit Rechnungslauf zu bestimmtem Datum - STANDARD: Alles ACTIVE
     */
    @PostMapping("/full-with-billing")
    public ResponseEntity<String> initWithBilling(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingDate) {
        initDataOrchestrator.initializeData(InitMode.FULL_WITH_BILLING, billingDate, InitConfig.allActive());
        return ResponseEntity.ok("✓ Initialisierung mit Rechnungslauf bis " + billingDate + " durchgeführt (STANDARD: Alles ACTIVE)");
    }

    // ===============================================================================================
    // VORKONFIGURIERTE SZENARIEN
    // ===============================================================================================

    /**
     * Realistische Testdaten (gemischte Status für Testing)
     * - Verträge: 80% aktiv, 20% beendet
     * - Abos: 85% aktiv, 10% storniert, 5% pausiert
     * - Rechnungen: 60% aktiv, 20% versendet, 15% entwurf, 5% storniert
     */
    @PostMapping("/realistic")
    public ResponseEntity<String> initRealistic() {
        initDataOrchestrator.initRealisticTestData();
        return ResponseEntity.ok("✓ Realistische Testdaten initialisiert (80% Verträge aktiv, 85% Abos aktiv, gemischte Rechnungen)");
    }

    /**
     * Development-Daten (viele aktive Daten für Development)
     * - Verträge: 95% aktiv, 5% beendet
     * - Abos: 90% aktiv, 5% storniert, 5% pausiert
     * - Rechnungen: 80% aktiv, 15% versendet, 5% entwurf
     */
    @PostMapping("/development")
    public ResponseEntity<String> initDevelopment() {
        initDataOrchestrator.initDevelopmentData();
        return ResponseEntity.ok("✓ Development-Daten initialisiert (95% Verträge aktiv, 90% Abos aktiv, meist aktive Rechnungen)");
    }

    /**
     * Demo-Daten (ausgewogene Verteilung für Präsentationen)
     */
    @PostMapping("/demo")
    public ResponseEntity<String> initDemo() {
        initDataOrchestrator.initializeData(InitMode.FULL, LocalDate.now().minusDays(5), InitConfig.demo());
        return ResponseEntity.ok("✓ Demo-Setup erstellt: Ausgewogene Daten mit Rechnungslauf vor 5 Tagen");
    }

    // ===============================================================================================
    // ERWEITERTE KONFIGURATION MIT PARAMETERN
    // ===============================================================================================

    /**
     * Erweiterte Konfiguration für Verträge und Abonnements
     */
    @PostMapping("/custom-contracts")
    public ResponseEntity<String> initCustomContracts(
            @RequestParam(defaultValue = "1.0") double activeContractRatio,
            @RequestParam(defaultValue = "1.0") double activeSubscriptionRatio,
            @RequestParam(defaultValue = "0.0") double cancelledSubscriptionRatio) {

        if (activeContractRatio < 0 || activeContractRatio > 1 ||
                activeSubscriptionRatio < 0 || activeSubscriptionRatio > 1 ||
                cancelledSubscriptionRatio < 0 || cancelledSubscriptionRatio > 1) {
            return ResponseEntity.badRequest().body("Alle Ratios müssen zwischen 0.0 und 1.0 liegen");
        }

        if (activeSubscriptionRatio + cancelledSubscriptionRatio > 1) {
            return ResponseEntity.badRequest().body("Subscription-Ratios dürfen zusammen nicht größer als 1.0 sein");
        }

        InitConfig config = InitConfig.builder()
                .contractStatus(activeContractRatio, 1.0 - activeContractRatio)
                .subscriptionStatus(activeSubscriptionRatio, cancelledSubscriptionRatio,
                        1.0 - activeSubscriptionRatio - cancelledSubscriptionRatio)
                .build();

        initDataOrchestrator.initializeData(InitMode.CONTRACTS, null, config);

        return ResponseEntity.ok(String.format("✓ Custom Verträge/Abos initialisiert (%.0f%% Verträge aktiv, %.0f%% Abos aktiv)",
                activeContractRatio * 100, activeSubscriptionRatio * 100));
    }

    /**
     * Erweiterte Konfiguration für Rechnungen
     */
    @PostMapping("/custom-invoices")
    public ResponseEntity<String> initCustomInvoices(
            @RequestParam(defaultValue = "1.0") double activeInvoiceRatio,
            @RequestParam(defaultValue = "0.0") double draftInvoiceRatio,
            @RequestParam(defaultValue = "0.0") double sentInvoiceRatio) {

        double totalRatio = activeInvoiceRatio + draftInvoiceRatio + sentInvoiceRatio;
        if (totalRatio > 1.0) {
            return ResponseEntity.badRequest().body("Ratios dürfen zusammen nicht größer als 1.0 sein");
        }

        double cancelledRatio = 1.0 - totalRatio;

        InitConfig config = InitConfig.builder()
                .invoiceStatus(activeInvoiceRatio, draftInvoiceRatio, sentInvoiceRatio, cancelledRatio)
                .build();

        initDataOrchestrator.initializeData(InitMode.INVOICES_MANUAL, null, config);

        return ResponseEntity.ok(String.format("✓ Custom Rechnungen initialisiert (%.0f%% aktiv, %.0f%% entwurf, %.0f%% versendet)",
                activeInvoiceRatio * 100, draftInvoiceRatio * 100, sentInvoiceRatio * 100));
    }

    /**
     * Vollständige Custom-Konfiguration mit Builder-Pattern
     */
    @PostMapping("/custom-full")
    public ResponseEntity<String> initCustomFull(
            @RequestParam(defaultValue = "1.0") double activeContractRatio,
            @RequestParam(defaultValue = "1.0") double activeSubscriptionRatio,
            @RequestParam(defaultValue = "1.0") double activeInvoiceRatio,
            @RequestParam(defaultValue = "1.0") double activeDueScheduleRatio,
            @RequestParam(defaultValue = "1.0") double openOpenItemRatio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingDate) {

        try {
            InitConfig config = InitConfig.builder()
                    .contractStatus(activeContractRatio, 1.0 - activeContractRatio)
                    .subscriptionStatus(activeSubscriptionRatio,
                            (1.0 - activeSubscriptionRatio) * 0.8,
                            (1.0 - activeSubscriptionRatio) * 0.2)
                    .invoiceStatus(activeInvoiceRatio,
                            (1.0 - activeInvoiceRatio) * 0.4,
                            (1.0 - activeInvoiceRatio) * 0.4,
                            (1.0 - activeInvoiceRatio) * 0.2)
                    .dueScheduleStatus(activeDueScheduleRatio,
                            (1.0 - activeDueScheduleRatio) * 0.8,
                            (1.0 - activeDueScheduleRatio) * 0.2)
                    .openItemStatus(openOpenItemRatio,
                            (1.0 - openOpenItemRatio) * 0.7,
                            (1.0 - openOpenItemRatio) * 0.3)
                    .build();

            LocalDate effectiveBillingDate = billingDate != null ? billingDate : LocalDate.now();
            initDataOrchestrator.initializeData(InitMode.FULL_WITH_BILLING, effectiveBillingDate, config);

            return ResponseEntity.ok(String.format("✓ Custom Vollinitialisierung durchgeführt (%.0f%% Verträge, %.0f%% Abos, %.0f%% Rechnungen, %.0f%% Fälligkeiten aktiv)",
                    activeContractRatio * 100, activeSubscriptionRatio * 100, activeInvoiceRatio * 100, activeDueScheduleRatio * 100));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Ungültige Konfiguration: " + e.getMessage());
        }
    }

    /**
     * Erweiterte Konfiguration mit vorgefertigten Presets
     */
    @PostMapping("/preset")
    public ResponseEntity<String> initWithPreset(
            @RequestParam String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingDate) {

        InitConfig config;
        String description;

        switch (preset.toLowerCase()) {
            case "all-active":
                config = InitConfig.allActive();
                description = "Alle Entitäten auf ACTIVE";
                break;
            case "realistic":
                config = InitConfig.realistic();
                description = "Realistische Testdaten-Verteilung";
                break;
            case "development":
                config = InitConfig.development();
                description = "Development-optimierte Daten";
                break;
            case "demo":
                config = InitConfig.demo();
                description = "Demo-optimierte Daten";
                break;
            case "mostly-active":
                config = InitConfig.allActive().withMostlyActiveStatus();
                description = "90% aller Entitäten aktiv";
                break;
            case "balanced":
                config = InitConfig.allActive().withBalancedStatus();
                description = "Ausgewogene Status-Verteilung";
                break;
            default:
                return ResponseEntity.badRequest().body("Unbekanntes Preset: " + preset +
                        ". Verfügbar: all-active, realistic, development, demo, mostly-active, balanced");
        }

        LocalDate effectiveBillingDate = billingDate != null ? billingDate : LocalDate.now();
        initDataOrchestrator.initializeData(InitMode.FULL_WITH_BILLING, effectiveBillingDate, config);

        return ResponseEntity.ok(String.format("✓ Initialisierung mit Preset '%s' durchgeführt (%s)", preset, description));
    }

    // ===============================================================================================
    // UTILITY-ENDPUNKTE
    // ===============================================================================================

    /**
     * Alle Daten löschen
     */
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearAllData() {
        dataManagementUtils.clearAllTestData();
        return ResponseEntity.ok("✓ Alle Testdaten wurden gelöscht");
    }

    /**
     * Nur Geschäftsdaten löschen (Stammdaten bleiben)
     */
    @DeleteMapping("/clear-business")
    public ResponseEntity<String> clearBusinessData() {
        dataManagementUtils.clearBusinessDataOnly();
        return ResponseEntity.ok("✓ Geschäftsdaten wurden gelöscht, Stammdaten erhalten");
    }

    /**
     * Daten-Status anzeigen
     */
    @GetMapping("/status")
    public ResponseEntity<String> getDataStatus() {
        dataStatusReporter.logCurrentDataStatus();
        return ResponseEntity.ok("✓ Aktueller Datenbestand wurde in Logs ausgegeben");
    }

    /**
     * Detail-Status für eine spezifische Entität
     */
    @GetMapping("/status/{entity}")
    public ResponseEntity<String> getDetailStatus(@PathVariable String entity) {
        try {
            Class<?> entityClass;
            switch (entity.toLowerCase()) {
                case "contracts":
                    entityClass = com.erp.backend.domain.Contract.class;
                    break;
                case "subscriptions":
                    entityClass = com.erp.backend.domain.Subscription.class;
                    break;
                case "invoices":
                    entityClass = com.erp.backend.domain.Invoice.class;
                    break;
                case "openitems":
                    entityClass = com.erp.backend.domain.OpenItem.class;
                    break;
                default:
                    return ResponseEntity.badRequest().body("Unbekannte Entität: " + entity +
                            ". Verfügbar: contracts, subscriptions, invoices, openitems");
            }

            dataStatusReporter.logDetailedStatusFor(entityClass);
            return ResponseEntity.ok("✓ Detail-Status für " + entity + " wurde in Logs ausgegeben");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Fehler beim Abrufen des Detail-Status: " + e.getMessage());
        }
    }

    /**
     * Daten-Konsistenz reparieren
     */
    @PostMapping("/repair")
    public ResponseEntity<String> repairData() {
        dataManagementUtils.repairDataConsistency();
        return ResponseEntity.ok("✓ Daten-Konsistenz-Reparatur durchgeführt");
    }

    /**
     * Standard-Maintenance-Aufgaben ausführen
     */
    @PostMapping("/maintenance")
    public ResponseEntity<String> performMaintenance() {
        dataManagementUtils.performMaintenanceTasks();
        return ResponseEntity.ok("✓ Standard-Maintenance-Aufgaben durchgeführt");
    }

    // ===============================================================================================
    // CONVENIENCE-ENDPUNKTE FÜR HÄUFIGE SZENARIEN
    // ===============================================================================================

    /**
     * Schnell-Setup für Development (alles aktiv, wenig Daten)
     */
    @PostMapping("/quick-dev")
    public ResponseEntity<String> quickDev() {
        initDataOrchestrator.initializeData(InitMode.SCHEDULES, null, InitConfig.allActive());
        return ResponseEntity.ok("✓ Schnell-Setup für Development: Basisdaten + aktive Verträge/Abos/Fälligkeiten (ohne Rechnungen)");
    }

    /**
     * Test-Setup (viele aktive Daten für automatisierte Tests)
     */
    @PostMapping("/test")
    public ResponseEntity<String> test() {
        initDataOrchestrator.initializeData(InitMode.FULL_WITH_BILLING, LocalDate.now(), InitConfig.development());
        return ResponseEntity.ok("✓ Test-Setup erstellt: Viele aktive Daten mit aktuellem Rechnungslauf");
    }

    /**
     * Minimal-Setup (nur Stammdaten)
     */
    @PostMapping("/minimal")
    public ResponseEntity<String> minimal() {
        initDataOrchestrator.initializeData(InitMode.BASIC, null, null);
        return ResponseEntity.ok("✓ Minimal-Setup erstellt: Nur Stammdaten (Adressen, Kunden, Produkte)");
    }

    // ===============================================================================================
    // INFORMATIONS-ENDPUNKTE
    // ===============================================================================================

    /**
     * API-Dokumentation
     */
    @GetMapping("/info")
    public ResponseEntity<InitApiInfo> getApiInfo() {
        return ResponseEntity.ok(new InitApiInfo());
    }

    /**
     * Verfügbare Presets anzeigen
     */
    @GetMapping("/presets")
    public ResponseEntity<PresetInfo> getPresets() {
        return ResponseEntity.ok(new PresetInfo());
    }

    // ===============================================================================================
    // DTO-KLASSEN FÜR API-DOKUMENTATION
    // ===============================================================================================

    /**
     * DTO für API-Informationen
     */
    public static class InitApiInfo {
        private final String[] standardEndpoints;
        private final String[] scenarioEndpoints;
        private final String[] customEndpoints;
        private final String[] utilityEndpoints;
        private final String[] convenienceEndpoints;
        private final String newArchitecture;
        private final String newStandardBehavior;

        public InitApiInfo() {
            this.newArchitecture = "NEU: Modulare Architektur mit InitDataOrchestrator, separaten Initializern und erweiterten Utility-Services";
            this.newStandardBehavior = "NEU: Alle Entitäten werden standardmäßig auf ACTIVE erstellt (Verträge, Abonnements, Rechnungen, etc.)";

            this.standardEndpoints = new String[]{
                    "POST /api/init/basic - Nur Stammdaten (Adressen, Kunden, Produkte)",
                    "POST /api/init/contracts - Bis Verträge/Abos (alles ACTIVE)",
                    "POST /api/init/schedules - Bis Fälligkeitspläne (alles ACTIVE)",
                    "POST /api/init/invoices - Mit Sample-Rechnungen (alles ACTIVE)",
                    "POST /api/init/full - Komplett mit Rechnungslauf (alles ACTIVE)",
                    "POST /api/init/full-with-billing?billingDate=2025-03-31 - Mit Stichtag"
            };

            this.scenarioEndpoints = new String[]{
                    "POST /api/init/realistic - Realistische Testdaten (80% Verträge aktiv, etc.)",
                    "POST /api/init/development - Development-Daten (95% aktiv)",
                    "POST /api/init/demo - Demo-Daten (ausgewogene Verteilung)",
                    "POST /api/init/preset?preset=realistic&billingDate=2025-03-31 - Mit Preset"
            };

            this.customEndpoints = new String[]{
                    "POST /api/init/custom-contracts?activeContractRatio=0.8&activeSubscriptionRatio=0.9",
                    "POST /api/init/custom-invoices?activeInvoiceRatio=0.7&draftInvoiceRatio=0.2",
                    "POST /api/init/custom-full?activeContractRatio=0.9&activeSubscriptionRatio=0.8&..."
            };

            this.utilityEndpoints = new String[]{
                    "DELETE /api/init/clear - Alle Daten löschen",
                    "DELETE /api/init/clear-business - Nur Geschäftsdaten löschen",
                    "GET /api/init/status - Datenbestand anzeigen",
                    "GET /api/init/status/{entity} - Detail-Status (contracts/subscriptions/invoices/openitems)",
                    "POST /api/init/repair - Konsistenz reparieren",
                    "POST /api/init/maintenance - Standard-Maintenance",
                    "GET /api/init/info - Diese Dokumentation",
                    "GET /api/init/presets - Verfügbare Presets"
            };

            this.convenienceEndpoints = new String[]{
                    "POST /api/init/quick-dev - Schnell-Setup ohne Rechnungen",
                    "POST /api/init/test - Test-Setup mit aktuellem Rechnungslauf",
                    "POST /api/init/minimal - Nur Stammdaten"
            };
        }

        // Getters
        public String getNewArchitecture() { return newArchitecture; }
        public String getNewStandardBehavior() { return newStandardBehavior; }
        public String[] getStandardEndpoints() { return standardEndpoints; }
        public String[] getScenarioEndpoints() { return scenarioEndpoints; }
        public String[] getCustomEndpoints() { return customEndpoints; }
        public String[] getUtilityEndpoints() { return utilityEndpoints; }
        public String[] getConvenienceEndpoints() { return convenienceEndpoints; }
    }

    /**
     * DTO für Preset-Informationen
     */
    public static class PresetInfo {
        private final PresetDescription[] presets;

        public PresetInfo() {
            this.presets = new PresetDescription[]{
                    new PresetDescription("all-active", "Alle Entitäten auf ACTIVE (100%)", "Standard für Production"),
                    new PresetDescription("realistic", "Realistische Testdaten", "80% Verträge aktiv, 85% Abos aktiv, gemischte Rechnungen"),
                    new PresetDescription("development", "Development-optimiert", "95% Verträge aktiv, 90% Abos aktiv, meist aktive Daten"),
                    new PresetDescription("demo", "Demo-optimiert", "Ausgewogene Verteilung für Präsentationen"),
                    new PresetDescription("mostly-active", "Meist aktiv", "90% aller Entitäten aktiv, 10% andere Status"),
                    new PresetDescription("balanced", "Ausgewogen", "Gleichmäßige Verteilung aller Status")
            };
        }

        public PresetDescription[] getPresets() { return presets; }
    }

    public static class PresetDescription {
        private final String name;
        private final String title;
        private final String description;

        public PresetDescription(String name, String title, String description) {
            this.name = name;
            this.title = title;
            this.description = description;
        }

        public String getName() { return name; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
    }
}