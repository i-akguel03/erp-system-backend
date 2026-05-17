package com.erp.backend.service;

import com.erp.backend.config.NotificationProperties;
import com.erp.backend.domain.Notification.NotificationSeverity;
import com.erp.backend.domain.Notification.NotificationType;
import com.erp.backend.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final NotificationProperties properties;
    private final CustomerEmailService customerEmailService;

    public NotificationEventListener(NotificationService notificationService,
                                     NotificationProperties properties,
                                     CustomerEmailService customerEmailService) {
        this.notificationService = notificationService;
        this.properties = properties;
        this.customerEmailService = customerEmailService;
    }

    @Async
    @EventListener
    public void onCustomerCreated(CustomerCreatedEvent event) {
        var c = event.getCustomer();
        if (properties.getTriggers().isNewEntityEnabled()) {
            notificationService.create(
                    NotificationType.NEW_CUSTOMER,
                    NotificationSeverity.INFO,
                    "Neuer Kunde angelegt",
                    "Kunde %s %s (%s) wurde erfolgreich angelegt."
                            .formatted(c.getFirstName(), c.getLastName(), c.getCustomerNumber()),
                    "CUSTOMER",
                    c.getId() != null ? c.getId().toString() : null
            );
        }
        customerEmailService.sendWelcomeEmail(c);
    }

    @Async
    @EventListener
    public void onContractCreated(ContractCreatedEvent event) {
        if (!properties.getTriggers().isNewEntityEnabled()) return;
        var c = event.getContract();
        notificationService.create(
                NotificationType.NEW_CONTRACT,
                NotificationSeverity.INFO,
                "Neuer Vertrag angelegt",
                "Vertrag \"%s\" (%s) wurde erfolgreich angelegt."
                        .formatted(c.getContractTitle(), c.getContractNumber()),
                "CONTRACT",
                c.getId() != null ? c.getId().toString() : null
        );
    }

    @Async
    @EventListener
    public void onSubscriptionCreated(SubscriptionCreatedEvent event) {
        if (!properties.getTriggers().isNewEntityEnabled()) return;
        var s = event.getSubscription();
        notificationService.create(
                NotificationType.NEW_SUBSCRIPTION,
                NotificationSeverity.INFO,
                "Neues Abonnement erstellt",
                "Abo \"%s\" (%s) wurde erstellt."
                        .formatted(s.getProductName(), s.getSubscriptionNumber()),
                "SUBSCRIPTION",
                s.getId() != null ? s.getId().toString() : null
        );
    }

    @Async
    @EventListener
    public void onInvoiceBatchCompleted(InvoiceBatchCompletedEvent event) {
        if (!properties.getTriggers().isInvoiceBatchEnabled()) return;

        if (event.isHasErrors()) {
            notificationService.create(
                    NotificationType.INVOICE_BATCH_FAILED,
                    NotificationSeverity.ERROR,
                    "Rechnungslauf mit Fehlern abgeschlossen",
                    "Rechnungslauf %s: %d Rechnungen erstellt, Fehler: %s"
                            .formatted(event.getVorgangsnummer(), event.getCreatedInvoices(), event.getErrorSummary()),
                    "VORGANG",
                    event.getVorgangsnummer()
            );
        } else {
            notificationService.create(
                    NotificationType.INVOICE_BATCH_COMPLETED,
                    NotificationSeverity.INFO,
                    "Rechnungslauf erfolgreich abgeschlossen",
                    "Rechnungslauf %s: %d Rechnungen erfolgreich erstellt."
                            .formatted(event.getVorgangsnummer(), event.getCreatedInvoices()),
                    "VORGANG",
                    event.getVorgangsnummer()
            );
        }
    }
}
