package com.erp.backend.service;

import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.OpenItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Factory für OpenItem-Erstellung.
 * Verantwortlich für: OpenItem-Assembly, Business-Rules für OpenItems
 */
@Service
public class OpenItemFactory {

    public OpenItem createOpenItemForInvoice(Invoice invoice, boolean wasOverdue) {
        // VALIDIERUNG: Invoice muss subscription_id haben
        if (invoice.getSubscriptionId() == null) {
            throw new IllegalArgumentException("Invoice muss eine subscription_id haben für OpenItem-Erstellung: "
                    + invoice.getInvoiceNumber());
        }

        OpenItem openItem = new OpenItem();
        openItem.setInvoice(invoice);

        // KRITISCH: subscription_id aus der Invoice übernehmen
        openItem.setSubscriptionId(invoice.getSubscriptionId());

        openItem.setDescription(String.format("Offener Posten für Rechnung %s%s",
                invoice.getInvoiceNumber(),
                wasOverdue ? " (aus überfälliger Fälligkeit)" : ""));
        openItem.setAmount(invoice.getTotalAmount());
        openItem.setDueDate(invoice.getDueDate());
        openItem.setStatus(determineInitialStatus(invoice));
        openItem.setPaidAmount(BigDecimal.ZERO);

        return openItem;
    }

    private OpenItem.OpenItemStatus determineInitialStatus(Invoice invoice) {
        // Wenn bereits bei Erstellung überfällig
        if (invoice.getDueDate().isBefore(LocalDate.now())) {
            return OpenItem.OpenItemStatus.OVERDUE;
        }
        return OpenItem.OpenItemStatus.OPEN;
    }
}