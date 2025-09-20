// ===============================================================================================
// 7. INVOICE BATCH RESULT (Enhanced Builder Pattern)
// ===============================================================================================

package com.erp.backend.service.batch;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.OpenItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable Result Class für Rechnungslauf-Ergebnisse.
 * Verwendet Builder Pattern für saubere Konstruktion.
 */
public class InvoiceBatchResult {

    private final int createdInvoices;
    private final int createdOpenItems;
    private final int processedDueSchedules;
    private final BigDecimal totalAmount;
    private final String message;
    private final String vorgangsnummer;
    private final String batchId;
    private final List<String> errors;
    private final List<String> successMessages;

    // Package-private constructor (nicht private!) damit Builder darauf zugreifen kann
    InvoiceBatchResult(Builder builder) {
        this.createdInvoices = builder.createdInvoices;
        this.createdOpenItems = builder.createdOpenItems;
        this.processedDueSchedules = builder.processedDueSchedules;
        this.totalAmount = builder.totalAmount;
        this.message = builder.message;
        this.vorgangsnummer = builder.vorgangsnummer;
        this.batchId = builder.batchId;
        this.errors = List.copyOf(builder.errors);
        this.successMessages = List.copyOf(builder.successMessages);
    }

    // Getters
    public int getCreatedInvoices() { return createdInvoices; }
    public int getCreatedOpenItems() { return createdOpenItems; }
    public int getProcessedDueSchedules() { return processedDueSchedules; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getMessage() { return message; }
    public String getVorgangsnummer() { return vorgangsnummer; }
    public String getBatchId() { return batchId; }
    public List<String> getErrors() { return errors; }
    public List<String> getSuccessMessages() { return successMessages; }

    // Derived properties
    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasVorgang() { return vorgangsnummer != null && !vorgangsnummer.trim().isEmpty(); }
    public boolean isSuccessful() { return !hasErrors() && processedDueSchedules > 0; }
    public int getErrorCount() { return errors.size(); }

    @Override
    public String toString() {
        String base = message != null ? message :
                String.format("Rechnungslauf: %d Fälligkeiten → %d Rechnungen → %d OpenItems",
                        processedDueSchedules, createdInvoices, createdOpenItems);

        if (hasVorgang()) {
            base += " (Vorgang: " + vorgangsnummer + ")";
        }

        if (hasErrors()) {
            base += String.format(" [%d Fehler]", getErrorCount());
        }

        return base;
    }

    // ===============================================================================================
    // BUILDER CLASS
    // ===============================================================================================

    public static class Builder {
        private int createdInvoices = 0;
        private int createdOpenItems = 0;
        private int processedDueSchedules = 0;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private String message;
        private String vorgangsnummer;
        private String batchId;
        private final List<String> errors = new ArrayList<>();
        private final List<String> successMessages = new ArrayList<>();

        public Builder withVorgangsnummer(String vorgangsnummer) {
            this.vorgangsnummer = vorgangsnummer;
            return this;
        }

        public Builder withBatchId(String batchId) {
            this.batchId = batchId;
            return this;
        }

        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder addCreatedInvoice(Invoice invoice) {
            this.createdInvoices++;
            if (invoice.getTotalAmount() != null) {
                this.totalAmount = this.totalAmount.add(invoice.getTotalAmount());
            }
            return this;
        }

        public Builder addCreatedOpenItem(OpenItem openItem) {
            this.createdOpenItems++;
            return this;
        }

        public Builder addProcessedDueSchedule(DueSchedule dueSchedule) {
            this.processedDueSchedules++;
            return this;
        }

        public Builder addError(String error) {
            this.errors.add(error);
            return this;
        }

        public Builder addSuccessMessage(String message) {
            this.successMessages.add(message);
            return this;
        }

        public Builder addAmount(BigDecimal amount) {
            if (amount != null) {
                this.totalAmount = this.totalAmount.add(amount);
            }
            return this;
        }

        public InvoiceBatchResult build() {
            // Auto-generate message if not set
            if (this.message == null) {
                if (errors.isEmpty()) {
                    this.message = String.format(
                            "RECHNUNGSLAUF ERFOLGREICH: %d Fälligkeiten → %d Rechnungen → %d OpenItems, Gesamt: %.2f EUR",
                            processedDueSchedules, createdInvoices, createdOpenItems, totalAmount);
                } else {
                    this.message = String.format(
                            "RECHNUNGSLAUF MIT FEHLERN: %d Fälligkeiten → %d Rechnungen → %d OpenItems (%d Fehler), Gesamt: %.2f EUR",
                            processedDueSchedules, createdInvoices, createdOpenItems, errors.size(), totalAmount);
                }
            }

            return new InvoiceBatchResult(this);
        }
    }

    // Legacy constructor for backward compatibility
    @Deprecated
    public static InvoiceBatchResult createLegacy(int createdInvoices, int processedDueSchedules,
                                                  BigDecimal totalAmount, String message) {
        return new Builder()
                .withMessage(message)
                .addAmount(totalAmount)
                .build();
    }
}