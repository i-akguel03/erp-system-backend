package com.erp.backend.controller;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.dto.InvoiceBatchPreviewDTO;
import com.erp.backend.service.*;
import com.erp.backend.service.batch.*;
import com.erp.backend.service.batch.InvoiceBatchResult;
import com.erp.backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Rechnungen")
public class InvoiceBatchController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceBatchController.class);

    private final InvoiceBatchOrchestrator orchestrator;
    private final InvoiceBatchAnalyzer analyzer;

    public InvoiceBatchController(InvoiceBatchOrchestrator orchestrator, InvoiceBatchAnalyzer analyzer) {
        this.orchestrator = orchestrator;
        this.analyzer = analyzer;
    }

    @Operation(summary = "Rechnungslauf zu einem Datum ausführen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/batch/run")
    public ResponseEntity<?> runInvoiceBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingDate,
            @RequestParam(defaultValue = "false") boolean exactDateOnly) {
        try {
            boolean includeAllPreviousMonths = !exactDateOnly;
            InvoiceBatchResult result = orchestrator.runInvoiceBatch(billingDate, includeAllPreviousMonths,
                    SecurityUtils.getCurrentUsername());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            // Fachliche Ablehnung (z.B. laufender Batch, Validierungsfehler)
            logger.warn("Rechnungslauf für {} abgelehnt: {}", billingDate, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unerwarteter Fehler beim Rechnungslauf für {}", billingDate, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Rechnungslauf fehlgeschlagen. Bitte Logs prüfen."));
        }
    }

    @Operation(summary = "Rechnungslauf für heute ausführen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/batch/run-today")
    public ResponseEntity<?> runInvoiceBatchToday(
            @RequestParam(defaultValue = "false") boolean exactDateOnly) {
        try {
            boolean includeAllPreviousMonths = !exactDateOnly;
            InvoiceBatchResult result = orchestrator.runInvoiceBatch(LocalDate.now(), includeAllPreviousMonths,
                    SecurityUtils.getCurrentUsername());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            logger.warn("Rechnungslauf für heute abgelehnt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unerwarteter Fehler beim Rechnungslauf für heute", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Rechnungslauf fehlgeschlagen. Bitte Logs prüfen."));
        }
    }

    @Operation(summary = "Vorschau eines Rechnungslaufs anzeigen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'INVOICES_READ')")
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
            logger.error("Fehler bei Vorschau für {}", billingDate, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Vorschau konnte nicht erstellt werden. Bitte Logs prüfen."));
        }
    }

    @Operation(summary = "Prüfen ob Rechnungslauf möglich ist")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'INVOICES_READ')")
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
            logger.error("Fehler bei Batch-Prüfung für {}", billingDate, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Prüfung fehlgeschlagen. Bitte Logs prüfen."));
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