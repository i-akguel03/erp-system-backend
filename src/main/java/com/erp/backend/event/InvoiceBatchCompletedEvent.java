package com.erp.backend.event;

import org.springframework.context.ApplicationEvent;

public class InvoiceBatchCompletedEvent extends ApplicationEvent {
    private final String vorgangsnummer;
    private final int createdInvoices;
    private final boolean hasErrors;
    private final String errorSummary;

    public InvoiceBatchCompletedEvent(Object source, String vorgangsnummer,
                                      int createdInvoices, boolean hasErrors, String errorSummary) {
        super(source);
        this.vorgangsnummer = vorgangsnummer;
        this.createdInvoices = createdInvoices;
        this.hasErrors = hasErrors;
        this.errorSummary = errorSummary;
    }

    public String getVorgangsnummer() { return vorgangsnummer; }
    public int getCreatedInvoices() { return createdInvoices; }
    public boolean isHasErrors() { return hasErrors; }
    public String getErrorSummary() { return errorSummary; }
}
