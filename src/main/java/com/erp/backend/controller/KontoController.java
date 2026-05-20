package com.erp.backend.controller;

import com.erp.backend.dto.KontoDTO;
import com.erp.backend.service.KontoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/konten")
@CrossOrigin
@Tag(name = "Kontenplan")
public class KontoController {

    private final KontoService kontoService;

    public KontoController(KontoService kontoService) {
        this.kontoService = kontoService;
    }

    @Operation(summary = "Alle Konten abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BUCHHALTUNG_READ')")
    @GetMapping
    public ResponseEntity<List<KontoDTO>> getAll() {
        return ResponseEntity.ok(kontoService.findAll());
    }

    @Operation(summary = "Konto nach Nummer abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BUCHHALTUNG_READ')")
    @GetMapping("/{kontonummer}")
    public ResponseEntity<KontoDTO> getByNummer(@PathVariable Long kontonummer) {
        return ResponseEntity.ok(kontoService.findByKontonummer(kontonummer));
    }

    @Operation(summary = "Neues Konto anlegen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<KontoDTO> erstellen(@RequestBody KontoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kontoService.erstellen(dto));
    }

    @Operation(summary = "Konto aktualisieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{kontonummer}")
    public ResponseEntity<KontoDTO> aktualisieren(@PathVariable Long kontonummer, @RequestBody KontoDTO dto) {
        return ResponseEntity.ok(kontoService.aktualisieren(kontonummer, dto));
    }

    @Operation(
        summary = "SKR04-Standardkontenplan initialisieren",
        description = "Legt alle Standard-Konten nach SKR04 an. Bereits vorhandene werden übersprungen."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/init-skr04")
    public ResponseEntity<Map<String, Object>> initSkr04() {
        int created = kontoService.initSkr04();
        return ResponseEntity.ok(Map.of(
            "message", "SKR04 initialisiert",
            "angelegtKonten", created
        ));
    }
}