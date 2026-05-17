package com.erp.backend.service;

import com.erp.backend.config.NotificationProperties;
import com.erp.backend.domain.Contract;
import com.erp.backend.domain.Notification.NotificationSeverity;
import com.erp.backend.domain.Notification.NotificationType;
import com.erp.backend.domain.OpenItem;
import com.erp.backend.domain.Subscription;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.OpenItemRepository;
import com.erp.backend.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class NotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;
    private final NotificationProperties properties;
    private final OpenItemRepository openItemRepository;
    private final ContractRepository contractRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CustomerEmailService customerEmailService;

    public NotificationScheduler(NotificationService notificationService,
                                  NotificationProperties properties,
                                  OpenItemRepository openItemRepository,
                                  ContractRepository contractRepository,
                                  SubscriptionRepository subscriptionRepository,
                                  CustomerEmailService customerEmailService) {
        this.notificationService = notificationService;
        this.properties = properties;
        this.openItemRepository = openItemRepository;
        this.contractRepository = contractRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.customerEmailService = customerEmailService;
    }

    @Transactional
    @Scheduled(cron = "#{@notificationProperties.triggers.overdueCron}")
    public void checkOverdueOpenItems() {
        if (!properties.getTriggers().isOverdueEnabled()) return;

        List<OpenItem> overdueItems = openItemRepository.findOverdueItems(LocalDate.now());
        logger.info("Überfällige offene Posten gefunden: {}", overdueItems.size());

        for (OpenItem item : overdueItems) {
            notificationService.create(
                    NotificationType.OVERDUE_OPEN_ITEM,
                    NotificationSeverity.WARNING,
                    "Überfälliger offener Posten",
                    "Offener Posten %s ist seit %s überfällig (%.2f €)."
                            .formatted(item.getId(), item.getDueDate(), item.getAmount()),
                    "OPEN_ITEM",
                    item.getId() != null ? item.getId().toString() : null
            );
            customerEmailService.sendPaymentReminder(item);
        }
    }

    @Transactional
    @Scheduled(cron = "#{@notificationProperties.triggers.overdueCron}")
    public void checkExpiringContracts() {
        if (!properties.getTriggers().isExpiringEnabled()) return;

        int days = properties.getTriggers().getExpiringDays();
        LocalDate today = LocalDate.now();

        List<Contract> expiring = contractRepository.findContractsExpiringBetween(today, today.plusDays(days));
        logger.info("Ablaufende Verträge (nächste {} Tage): {}", days, expiring.size());

        for (Contract contract : expiring) {
            notificationService.create(
                    NotificationType.EXPIRING_CONTRACT,
                    NotificationSeverity.WARNING,
                    "Vertrag läuft bald ab",
                    "Vertrag \"%s\" (%s) läuft am %s ab."
                            .formatted(contract.getContractTitle(), contract.getContractNumber(), contract.getEndDate()),
                    "CONTRACT",
                    contract.getId() != null ? contract.getId().toString() : null
            );
            customerEmailService.sendContractExpiryNotice(contract);
        }
    }

    @Transactional
    @Scheduled(cron = "#{@notificationProperties.triggers.overdueCron}")
    public void checkExpiringSubscriptions() {
        if (!properties.getTriggers().isExpiringEnabled()) return;

        int days = properties.getTriggers().getExpiringDays();
        LocalDate today = LocalDate.now();

        List<Subscription> expiring = subscriptionRepository.findByEndDateBetween(today, today.plusDays(days));
        logger.info("Ablaufende Abonnements (nächste {} Tage): {}", days, expiring.size());

        for (Subscription sub : expiring) {
            notificationService.create(
                    NotificationType.EXPIRING_SUBSCRIPTION,
                    NotificationSeverity.WARNING,
                    "Abonnement läuft bald ab",
                    "Abo \"%s\" (%s) läuft am %s ab."
                            .formatted(sub.getProductName(), sub.getSubscriptionNumber(), sub.getEndDate()),
                    "SUBSCRIPTION",
                    sub.getId() != null ? sub.getId().toString() : null
            );
            customerEmailService.sendSubscriptionExpiryNotice(sub);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldNotifications() {
        int deleted = notificationService.deleteOldNotifications(90);
        if (deleted > 0) {
            logger.info("Alte Benachrichtigungen gelöscht: {}", deleted);
        }
    }
}
