// ===============================================================================================
// 4. INVOICE FACTORY (Rechnung-Erstellung)
// ===============================================================================================

package com.erp.backend.service;

import com.erp.backend.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Factory für Invoice-Erstellung.
 * Verantwortlich für: Rechnung-Assembly, Business-Rules für Invoice
 */
@Service
public class InvoiceFactory {

    private final InvoiceNumberGeneratorService invoiceNumberGenerator;

    public InvoiceFactory(InvoiceNumberGeneratorService invoiceNumberGenerator) {
        this.invoiceNumberGenerator = invoiceNumberGenerator;
    }

    public Invoice createInvoiceForDueSchedule(DueSchedule dueSchedule, LocalDate billingDate,
                                               String batchId, boolean isOverdue) {

        Subscription subscription = dueSchedule.getSubscription();
        Customer customer = subscription.getContract().getCustomer();

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumberGenerator.generateInvoiceNumber());
        invoice.setCustomer(customer);
        invoice.setSubscription(subscription);
        invoice.setBillingAddress(customer.getBillingAddress());
        invoice.setInvoiceDate(billingDate);
        invoice.setDueDate(billingDate.plusDays(14));
        invoice.setStatus(Invoice.InvoiceStatus.ACTIVE);
        invoice.setInvoiceType(Invoice.InvoiceType.AUTO_GENERATED);
        invoice.setInvoiceBatchId(batchId);
        invoice.setPaymentTerms("Zahlbar innerhalb von 14 Tagen");
        invoice.setTaxRate(BigDecimal.valueOf(19));

        // Invoice Item erstellen
        InvoiceItem item = createInvoiceItem(dueSchedule, isOverdue);
        invoice.addInvoiceItem(item);
        invoice.calculateTotals();

        // Notizen
        invoice.setNotes(createInvoiceNotes(dueSchedule, billingDate, isOverdue));

        return invoice;
    }

    private InvoiceItem createInvoiceItem(DueSchedule dueSchedule, boolean isOverdue) {
        Subscription subscription = dueSchedule.getSubscription();

        InvoiceItem item = new InvoiceItem();
        item.setDescription(createItemDescription(subscription, dueSchedule, isOverdue));
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(getPrice(subscription));
        item.setItemType(InvoiceItem.InvoiceItemType.SUBSCRIPTION);
        item.setTaxRate(BigDecimal.valueOf(19));
        item.setPeriodStart(dueSchedule.getPeriodStart());
        item.setPeriodEnd(dueSchedule.getPeriodEnd());

        if (subscription.getProduct() != null) {
            item.setProductCode(subscription.getProduct().getProductNumber());
            item.setProductName(subscription.getProduct().getName());
        } else {
            item.setProductName(subscription.getProductName());
        }

        item.calculateLineTotal();
        return item;
    }

    private BigDecimal getPrice(Subscription subscription) {
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

    private String createItemDescription(Subscription subscription, DueSchedule dueSchedule, boolean isOverdue) {
        String productName = subscription.getProductName() != null ?
                subscription.getProductName() :
                (subscription.getProduct() != null ? subscription.getProduct().getName() : "Abonnement");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String baseDescription = String.format("%s für Periode %s bis %s",
                productName,
                dueSchedule.getPeriodStart().format(formatter),
                dueSchedule.getPeriodEnd().format(formatter));

        if (isOverdue) {
            return String.format("%s ⚠ ÜBERFÄLLIG seit %s ⚠",
                    baseDescription, dueSchedule.getDueDate().format(formatter));
        }

        return baseDescription;
    }

    private String createInvoiceNotes(DueSchedule dueSchedule, LocalDate billingDate, boolean isOverdue) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        String template = isOverdue ?
                "Automatisch erstellt am %s für ÜBERFÄLLIGE Fälligkeit %s (ursprünglich fällig: %s, Periode: %s bis %s)" :
                "Automatisch erstellt am %s für Fälligkeit %s (Periode: %s bis %s)";

        if (isOverdue) {
            return String.format(template,
                    billingDate.format(formatter),
                    dueSchedule.getDueNumber(),
                    dueSchedule.getDueDate().format(formatter),
                    dueSchedule.getPeriodStart().format(formatter),
                    dueSchedule.getPeriodEnd().format(formatter));
        } else {
            return String.format(template,
                    billingDate.format(formatter),
                    dueSchedule.getDueNumber(),
                    dueSchedule.getPeriodStart().format(formatter),
                    dueSchedule.getPeriodEnd().format(formatter));
        }
    }
}