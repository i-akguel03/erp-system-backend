package com.erp.backend.controller;

import com.erp.backend.service.init.DataManagementUtils;
import com.erp.backend.service.init.DataStatusReporter;
import com.erp.backend.service.init.InitConfig;
import com.erp.backend.service.init.InitDataOrchestrator;
import com.erp.backend.service.init.InitMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/init")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Init")
public class InitController {

    private final InitDataOrchestrator initDataOrchestrator;
    private final DataStatusReporter dataStatusReporter;
    private final DataManagementUtils dataManagementUtils;

    @Value("${app.init.delete-password}")
    private String deletePassword;

    public InitController(InitDataOrchestrator initDataOrchestrator,
                          DataStatusReporter dataStatusReporter,
                          DataManagementUtils dataManagementUtils) {
        this.initDataOrchestrator = initDataOrchestrator;
        this.dataStatusReporter = dataStatusReporter;
        this.dataManagementUtils = dataManagementUtils;
    }

    // ===============================================================================================
    // INITIALISIERUNG
    // ===============================================================================================

    @Operation(summary = "Komplett-Init — alle Entitäten ACTIVE, Rechnungslauf heute")
    @PostMapping("/full")
    public ResponseEntity<String> initFull() {
        initDataOrchestrator.initializeData(InitMode.FULL, LocalDate.now(), InitConfig.allActive());
        return ResponseEntity.ok("Komplett-Init abgeschlossen (alles ACTIVE, Rechnungslauf heute)");
    }

    @Operation(summary = "Komplett-Init mit Rechnungslauf zu bestimmtem Datum")
    @PostMapping("/full-with-billing")
    public ResponseEntity<String> initFullWithBilling(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingDate) {
        initDataOrchestrator.initializeData(InitMode.FULL_WITH_BILLING, billingDate, InitConfig.allActive());
        return ResponseEntity.ok("Komplett-Init abgeschlossen (Rechnungslauf bis " + billingDate + ")");
    }

    @Operation(summary = "Nur Stammdaten — Adressen, Kunden, Produkte, Lagerbestand")
    @PostMapping("/basic")
    public ResponseEntity<String> initBasic() {
        initDataOrchestrator.initializeData(InitMode.BASIC, null, null);
        return ResponseEntity.ok("Stammdaten initialisiert (Adressen, Kunden, Produkte, Lagerbestand)");
    }

    @Operation(summary = "Realistische Testdaten — gemischte Status (80% Verträge aktiv, 85% Abos aktiv)")
    @PostMapping("/realistic")
    public ResponseEntity<String> initRealistic() {
        initDataOrchestrator.initRealisticTestData();
        return ResponseEntity.ok("Realistische Testdaten initialisiert (80% Verträge aktiv, 85% Abos aktiv)");
    }

    @Operation(summary = "Development-Daten — viele aktive Daten (95% Verträge aktiv, 90% Abos aktiv)")
    @PostMapping("/development")
    public ResponseEntity<String> initDevelopment() {
        initDataOrchestrator.initDevelopmentData();
        return ResponseEntity.ok("Development-Daten initialisiert (95% Verträge aktiv, 90% Abos aktiv)");
    }

    @Operation(summary = "Demo-Daten — ausgewogene Verteilung für Präsentationen")
    @PostMapping("/demo")
    public ResponseEntity<String> initDemo() {
        initDataOrchestrator.initializeData(InitMode.FULL, LocalDate.now().minusDays(5), InitConfig.demo());
        return ResponseEntity.ok("Demo-Daten initialisiert (Rechnungslauf vor 5 Tagen)");
    }

    // ===============================================================================================
    // DATEN LÖSCHEN
    // ===============================================================================================

    @Operation(summary = "Alle Daten löschen — erfordert Bestätigungspasswort")
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearAll(@RequestParam String password) {
        if (!deletePassword.equals(password)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falsches Passwort");
        }
        dataManagementUtils.clearAllTestData();
        return ResponseEntity.ok("Alle Daten gelöscht");
    }

    @Operation(summary = "Nur Geschäftsdaten löschen — Stammdaten (Kunden, Produkte, Adressen) bleiben erhalten — erfordert Bestätigungspasswort")
    @DeleteMapping("/clear-business")
    public ResponseEntity<String> clearBusiness(@RequestParam String password) {
        if (!deletePassword.equals(password)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falsches Passwort");
        }
        dataManagementUtils.clearBusinessDataOnly();
        return ResponseEntity.ok("Geschäftsdaten gelöscht, Stammdaten erhalten");
    }

    // ===============================================================================================
    // STATUS & WARTUNG
    // ===============================================================================================

    @Operation(summary = "Aktuellen Datenbestand in Logs ausgeben")
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        dataStatusReporter.logCurrentDataStatus();
        return ResponseEntity.ok("Datenbestand wurde in Logs ausgegeben");
    }

    @Operation(summary = "Datenkonsistenz reparieren — fehlende OpenItems erstellen, Status synchronisieren")
    @PostMapping("/repair")
    public ResponseEntity<String> repair() {
        dataManagementUtils.repairDataConsistency();
        return ResponseEntity.ok("Konsistenz-Reparatur abgeschlossen");
    }

    @Operation(summary = "Routine-Wartung — überfällige Items aktualisieren, verwaiste Daten bereinigen")
    @PostMapping("/maintenance")
    public ResponseEntity<String> maintenance() {
        dataManagementUtils.performMaintenanceTasks();
        return ResponseEntity.ok("Wartungsaufgaben abgeschlossen");
    }
}
