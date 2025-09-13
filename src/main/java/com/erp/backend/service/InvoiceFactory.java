package com.erp.backend.service;

import com.erp.backend.domain.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Factory für die Erzeugung von Rechnungen und den zugehörigen offenen Posten (OpenItems).
 */
@Component
public class InvoiceFactory {

    private final InvoiceNumberGeneratorService invoiceNumberGenerator;

    public InvoiceFactory(InvoiceNumberGeneratorService invoiceNumberGenerator) {
        this.invoiceNumberGenerator = invoiceNumberGenerator;
    }

    /**
     * Erstellt eine Invoice inkl. InvoiceItems aus einer Liste von DueSchedules.
     *
     * @param subscription   Abonnement/Subscription
     * @param dueSchedules   Liste der offenen Fälligkeiten
     * @param billingRunDate Datum des Rechnungslaufs
     * @return fertige Invoice
     */
    public Invoice createInvoice(Subscription subscription,
                                 List<DueSchedule> dueSchedules,
                                 LocalDate billingRunDate) {

        Customer customer = subscription.getContract().getCustomer();

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumberGenerator.generateInvoiceNumber());
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(billingRunDate);
        invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
        invoice.setBillingAddress(customer.getBillingAddress());
        invoice.setInvoiceType(Invoice.InvoiceType.AUTO_GENERATED);

        // Für jede DueSchedule ein InvoiceItem erzeugen
        for (DueSchedule dueSchedule : dueSchedules) {
            InvoiceItem item = createInvoiceItemFromDueSchedule(dueSchedule, invoice);
            invoice.addInvoiceItem(item);
        }

        // Gesamtsummen berechnen
        invoice.calculateTotals();

        return invoice;
    }

    /**
     * Erstellt ein InvoiceItem aus einer DueSchedule.
     *
     * @param dueSchedule DueSchedule
     * @param invoice     Zugehörige Invoice
     * @return InvoiceItem
     */
    private InvoiceItem createInvoiceItemFromDueSchedule(DueSchedule dueSchedule, Invoice invoice) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);

        String description = createDescriptionForPeriod(
                dueSchedule.getSubscription(),
                dueSchedule.getPeriodStart(),
                dueSchedule.getPeriodEnd()
        );

        item.setDescription(description);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(dueSchedule.getAmount());
        item.calculateLineTotal();

        return item;
    }

    /**
     * Erstellt die Beschreibung für die InvoiceItem-Position (Produktname + Zeitraum).
     */
    private String createDescriptionForPeriod(Subscription subscription,
                                              LocalDate periodStart,
                                              LocalDate periodEnd) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        String subscriptionName = subscription.getProduct() != null ?
                subscription.getProduct().getName() : "Abonnement";

        if (periodEnd != null) {
            return String.format("%s für Zeitraum %s - %s",
                    subscriptionName,
                    periodStart.format(formatter),
                    periodEnd.format(formatter));
        } else {
            return String.format("%s ab %s",
                    subscriptionName,
                    periodStart.format(formatter));
        }
    }

    /**
     * Erzeugt einen OpenItem aus einem InvoiceItem.
     * Wird vom InvoiceBatchService verwendet.
     */
    public OpenItem createOpenItem(Invoice invoice, InvoiceItem item) {
        OpenItem openItem = new OpenItem();
        openItem.setInvoice(invoice);
        openItem.setDescription(item.getDescription());
        openItem.setAmount(item.getLineTotal());
        openItem.setDueDate(invoice.getDueDate());
        openItem.setStatus(OpenItem.OpenItemStatus.OPEN);
        return openItem;
    }
}
