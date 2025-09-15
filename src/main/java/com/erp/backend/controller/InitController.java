package com.erp.backend.controller;

import com.erp.backend.service.InitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/init")
public class InitController {

    @Autowired
    private InitDataService initDataService;

    // Nur Basisdaten
    @PostMapping("/init/basic")
    public ResponseEntity<String> initBasic() {
        initDataService.initData(InitDataService.InitMode.BASIC);
        return ResponseEntity.ok("Basisdaten initialisiert");
    }

    // Bis DueSchedules
    @PostMapping("/init/schedules")
    public ResponseEntity<String> initSchedules() {
        initDataService.initUpToSchedules();
        return ResponseEntity.ok("Daten bis Fälligkeitspläne initialisiert");
    }

    // Mit Rechnungslauf heute
    @PostMapping("/init/full")
    public ResponseEntity<String> initFull() {
        initDataService.initWithBillingToday();
        return ResponseEntity.ok("Komplette Initialisierung mit Rechnungslauf durchgeführt");
    }

    // Mit Rechnungslauf zu bestimmtem Datum
    @PostMapping("/init/full-with-billing")
    public ResponseEntity<String> initWithBilling(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingDate) {
        initDataService.initWithBilling(billingDate);
        return ResponseEntity.ok("Initialisierung mit Rechnungslauf bis " + billingDate + " durchgeführt");
    }

    // Daten löschen
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearData() {
        initDataService.clearAllTestData();
        return ResponseEntity.ok("Alle Testdaten gelöscht");
    }
}
