package com.erp.backend.controller;

import com.erp.backend.service.InitDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/init")
public class InitController {

    private final InitDataService initDataService;

    public InitController(InitDataService initDataService) {
        this.initDataService = initDataService;
    }

    @PostMapping
    public ResponseEntity<String> initializeData() {
        try {
            initDataService.initAllData();
            return ResponseEntity.ok("Testdaten erfolgreich initialisiert.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Fehler bei der Initialisierung: " + e.getMessage());
        }
    }
}
