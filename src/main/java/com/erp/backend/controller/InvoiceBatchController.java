package com.erp.backend.controller;

import com.erp.backend.service.InvoiceBatchService;
import com.erp.backend.service.InvoiceBatchService.InvoiceBatchResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceBatchController {

    private final InvoiceBatchService invoiceBatchService;

    // Konstruktor-Injektion ist klarer und testbar
    public InvoiceBatchController(InvoiceBatchService invoiceBatchService) {
        this.invoiceBatchService = invoiceBatchService;
    }

    /**
     * Startet einen Rechnungslauf für ein bestimmtes Datum.
     * Erwartet als Request-Parameter ein Datum im Format YYYY-MM-DD.
     * Beispiel: POST /api/invoices/batch/run?billingDate=2024-09-01
     *
     * @param billingDate String-Repräsentation des Datums
     * @return Result des Rechnungslaufs (Anzahl Rechnungen/Fälligkeiten)
     */
    @PostMapping("/batch/run")
    public ResponseEntity<?> runInvoiceBatch(@RequestParam String billingDate) {
        try {
            // Versuche das Datum zu parsen
            LocalDate date = LocalDate.parse(billingDate);

            // Delegiere an den Service, der den Batch tatsächlich ausführt
            InvoiceBatchResult result = invoiceBatchService.runInvoiceBatch(date);

            // Ergebnis direkt an den Client zurückgeben
            return ResponseEntity.ok(result);

        } catch (DateTimeParseException e) {
            // Falls das Datum ungültig ist
            return ResponseEntity.badRequest()
                    .body("Ungültiges Datumsformat. Bitte verwenden Sie YYYY-MM-DD (z.B. 2024-09-01).");
        } catch (Exception e) {
            // Allgemeine Fehlerbehandlung, z.B. Datenbankprobleme
            return ResponseEntity.internalServerError()
                    .body("Fehler beim Ausführen des Rechnungslaufs: " + e.getMessage());
        }
    }

    /**
     * Startet einen Rechnungslauf für das heutige Datum.
     * Einfacher Shortcut ohne Request-Parameter.
     * Beispiel: POST /api/invoices/batch/run-today
     *
     * @return Result des Rechnungslaufs
     */
    @PostMapping("/batch/run-today")
    public ResponseEntity<?> runInvoiceBatchToday() {
        try {
            // Übergibt LocalDate.now() an den Batch-Service
            InvoiceBatchResult result = invoiceBatchService.runInvoiceBatch(LocalDate.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Fehler beim Ausführen des Rechnungslaufs: " + e.getMessage());
        }
    }

    /**
     * Vorschau-Funktion: zeigt an, wie viele DueSchedules verarbeitet würden,
     * ohne tatsächlich Rechnungen zu erstellen.
     * Kann z.B. für Frontend oder Reporting genutzt werden.
     *
     * @param billingDate Datum der Vorschau
     * @return Anzahl der DueSchedules, die verarbeitet würden
     */
    @GetMapping("/batch/preview")
    public ResponseEntity<?> previewInvoiceBatch(@RequestParam String billingDate) {
        try {
            LocalDate date = LocalDate.parse(billingDate);

            // Nutzt jetzt die Vorschau-Methode im Service
            // InvoiceBatchService.InvoiceBatchResult result = invoiceBatchService.previewInvoiceBatch(date);

            return ResponseEntity.ok(ResponseEntity.ok());

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body("Ungültiges Datumsformat. Bitte verwenden Sie YYYY-MM-DD (z.B. 2024-09-01).");
        }
    }

}
