package com.erp.backend.controller;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.dto.InvoiceBatchPreviewDTO;
import com.erp.backend.service.*;
import com.erp.backend.service.batch.*;
import com.erp.backend.service.batch.InvoiceBatchResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceBatchController {

    private final InvoiceBatchOrchestrator orchestrator;
    private final InvoiceBatchAnalyzer analyzer;

    public InvoiceBatchController(InvoiceBatchOrchestrator orchestrator, InvoiceBatchAnalyzer analyzer) {
        this.orchestrator = orchestrator;
        this.analyzer = analyzer;
    }

    /**
     * Standard-Rechnungslauf: Alle offenen Monate
     */
    @PostMapping("/batch/run")
    public ResponseEntity<?> runInvoiceBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingDate,
            @RequestParam(defaultValue = "false") boolean exactDateOnly) {
        try {
            boolean includeAllPreviousMonths = !exactDateOnly;
            InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate, includeAllPreviousMonths);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Fehler beim Rechnungslauf: " + e.getMessage());
        }
    }

    /**
     * Rechnungslauf für heute
     */
    @PostMapping("/batch/run-today")
    public ResponseEntity<?> runInvoiceBatchToday(
            @RequestParam(defaultValue = "false") boolean exactDateOnly) {
        try {
            boolean includeAllPreviousMonths = !exactDateOnly;
            InvoiceBatchResult result = orchestrator.runInvoiceBatch(LocalDate.now(), includeAllPreviousMonths);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Fehler beim Rechnungslauf: " + e.getMessage());
        }
    }

    /**
     * Vorschau eines geplanten Rechnungslaufs
     */
    @GetMapping("/batch/preview")
    public ResponseEntity<?> previewInvoiceBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingDate,
            @RequestParam(defaultValue = "false") boolean exactDateOnly) {
        try {
            boolean includeAllPreviousMonths = !exactDateOnly;

            InvoiceBatchAnalysis analysis = analyzer.analyzeBillingScope(billingDate, includeAllPreviousMonths);

            // Estimate total amount
            BigDecimal estimatedTotal = analysis.getDueSchedules().stream()
                    .map(ds -> getEstimatedPrice(ds))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // DTO zurückgeben statt der Entity-Version
            InvoiceBatchPreviewDTO preview = new InvoiceBatchPreviewDTO(analysis, estimatedTotal);
            return ResponseEntity.ok(preview);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Fehler bei der Vorschau: " + e.getMessage());
        }
    }

    /**
     * Prüft ob Rechnungslauf möglich ist
     */
    @GetMapping("/batch/can-run")
    public ResponseEntity<?> canRunInvoiceBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingDate,
            @RequestParam(defaultValue = "false") boolean exactDateOnly) {
        try {
            boolean includeAllPreviousMonths = !exactDateOnly;
            boolean canRun = analyzer.canRunBillingBatch(billingDate, includeAllPreviousMonths);

            return ResponseEntity.ok(new CanRunResult(canRun, includeAllPreviousMonths ?
                    "Prüfung für alle offenen Monate bis " + billingDate :
                    "Prüfung nur für " + billingDate));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Fehler bei der Prüfung: " + e.getMessage());
        }
    }

    // Helper method to estimate price (simplified version of InvoiceFactory logic)
    private BigDecimal getEstimatedPrice(DueSchedule dueSchedule) {
        var subscription = dueSchedule.getSubscription();

        if (subscription.getMonthlyPrice() != null &&
                subscription.getMonthlyPrice().compareTo(BigDecimal.ZERO) > 0) {
            return subscription.getMonthlyPrice();
        }

        if (subscription.getProduct() != null &&
                subscription.getProduct().getPrice() != null &&
                subscription.getProduct().getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return subscription.getProduct().getPrice();
        }

        return BigDecimal.ZERO;
    }

    public static class CanRunResult {
        private final boolean canRun;
        private final String mode;

        public CanRunResult(boolean canRun, String mode) {
            this.canRun = canRun;
            this.mode = mode;
        }

        public boolean isCanRun() { return canRun; }
        public String getMode() { return mode; }
    }
}