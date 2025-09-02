package com.erp.backend.service.event;

import com.erp.backend.event.SubscriptionCancelledEvent;
import com.erp.backend.event.SubscriptionCreatedEvent;
import com.erp.backend.event.SubscriptionExpiredEvent;
import com.erp.backend.event.SubscriptionRenewedEvent;
import com.erp.backend.event.SubscriptionUpdatedEvent;
import com.erp.backend.service.DueScheduleService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Event Handler für Subscription-Events zur automatischen Verwaltung von Fälligkeitsplänen
 */
@Component
public class SubscriptionEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionEventHandler.class);

    @Autowired
    private DueScheduleService dueScheduleService;

    /**
     * Behandelt die Erstellung neuer Abonnements
     */
    @EventListener
    @Async
    public void handleSubscriptionCreated(SubscriptionCreatedEvent event) {
        try {
            // Initiale Fälligkeitspläne erstellen
            int monthsToGenerate = calculateInitialMonths(event.getSubscription());
            if (monthsToGenerate > 0) {
                dueScheduleService.generateDueSchedulesForSubscription(
                        event.getSubscription().getId(), monthsToGenerate);

                logger.info("Automatically generated {} months of due schedules for new subscription {}",
                        monthsToGenerate, event.getSubscription().getId());
            }
        } catch (Exception e) {
            logger.error("Failed to create due schedules for new subscription {}: {}",
                    event.getSubscription().getId(), e.getMessage());
        }
    }

    /**
     * Behandelt Abonnement-Updates
     */
    @EventListener
    @Async
    public void handleSubscriptionUpdated(SubscriptionUpdatedEvent event) {
        try {
            // Bei relevanten Änderungen Fälligkeitspläne synchronisieren
            if (event.isPriceChanged() || event.isBillingCycleChanged() || event.isEndDateChanged()) {

                // Zukünftige Fälligkeitspläne aktualisieren
                if (event.isPriceChanged()) {
                    updateFutureDueSchedulePrices(event.getNewSubscription());
                }

                // Bei Enddatum-Änderung zusätzliche oder überschüssige Pläne behandeln
                if (event.isEndDateChanged()) {
                    handleEndDateChange(event.getOldSubscription(), event.getNewSubscription());
                }

                logger.info("Synchronized due schedules for updated subscription {}",
                        event.getNewSubscription().getId());
            }
        } catch (Exception e) {
            logger.error("Failed to synchronize due schedules for updated subscription {}: {}",
                    event.getNewSubscription().getId(), e.getMessage());
        }
    }

    /**
     * Behandelt Abonnement-Kündigungen
     */
    @EventListener
    @Async
    public void handleSubscriptionCancelled(SubscriptionCancelledEvent event) {
        try {
            // Zukünftige Fälligkeitspläne nach Kündigungsdatum stornieren
            cancelFutureDueSchedules(event.getSubscription().getId(), event.getCancellationDate());

            logger.info("Cancelled future due schedules for cancelled subscription {} from date {}",
                    event.getSubscription().getId(), event.getCancellationDate());
        } catch (Exception e) {
            logger.error("Failed to cancel due schedules for cancelled subscription {}: {}",
                    event.getSubscription().getId(), e.getMessage());
        }
    }

    /**
     * Behandelt Abonnement-Verlängerungen
     */
    @EventListener
    @Async
    public void handleSubscriptionRenewed(SubscriptionRenewedEvent event) {
        try {
            // Zusätzliche Fälligkeitspläne für Verlängerung erstellen
            int additionalMonths = (int) event.getOldEndDate().until(event.getNewEndDate()).toTotalMonths();
            if (additionalMonths > 0) {
                dueScheduleService.generateDueSchedulesForSubscription(
                        event.getSubscription().getId(), additionalMonths);

                logger.info("Generated {} additional due schedules for renewed subscription {}",
                        additionalMonths, event.getSubscription().getId());
            }
        } catch (Exception e) {
            logger.error("Failed to generate additional due schedules for renewed subscription {}: {}",
                    event.getSubscription().getId(), e.getMessage());
        }
    }

    /**
     * Behandelt abgelaufene Abonnements
     */
    @EventListener
    @Async
    public void handleSubscriptionExpired(SubscriptionExpiredEvent event) {
        try {
            // Offene Fälligkeitspläne nach Ablauf behandeln
            handleExpiredSubscriptionDueSchedules(event.getSubscription().getId());

            logger.info("Handled due schedules for expired subscription {}",
                    event.getSubscription().getId());
        } catch (Exception e) {
            logger.error("Failed to handle due schedules for expired subscription {}: {}",
                    event.getSubscription().getId(), e.getMessage());
        }
    }

    // ========================================================================
    // Private Hilfsmethoden
    // ========================================================================

    private int calculateInitialMonths(com.erp.backend.domain.Subscription subscription) {
        if (subscription.getEndDate() == null) {
            return 12; // Standard: 1 Jahr voraus
        }

        LocalDate startDate = subscription.getStartDate();
        LocalDate endDate = subscription.getEndDate();

        long totalMonths = startDate.until(endDate).toTotalMonths();
        return Math.max(1, Math.min(12, (int) totalMonths)); // Max 12 Monate initial
    }

    private void updateFutureDueSchedulePrices(com.erp.backend.domain.Subscription subscription) {
        try {
            var pendingSchedules = dueScheduleService.getPendingDueSchedulesBySubscription(subscription.getId());

            for (var schedule : pendingSchedules) {
                if (schedule.getDueDate().isAfter(LocalDate.now())) {
                    schedule.setAmount(subscription.getMonthlyPrice());
                    dueScheduleService.updateDueSchedule(schedule.getId(), schedule);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not update future due schedule prices for subscription {}: {}",
                    subscription.getId(), e.getMessage());
        }
    }

    private void handleEndDateChange(com.erp.backend.domain.Subscription oldSubscription,
                                     com.erp.backend.domain.Subscription newSubscription) {

        if (newSubscription.getEndDate().isAfter(oldSubscription.getEndDate())) {
            // Verlängerung: zusätzliche Fälligkeitspläne erstellen
            int additionalMonths = (int) oldSubscription.getEndDate().until(newSubscription.getEndDate()).toTotalMonths();
            if (additionalMonths > 0) {
                try {
                    dueScheduleService.generateDueSchedulesForSubscription(newSubscription.getId(), additionalMonths);
                } catch (Exception e) {
                    logger.warn("Could not generate additional due schedules: {}", e.getMessage());
                }
            }
        } else if (newSubscription.getEndDate().isBefore(oldSubscription.getEndDate())) {
            // Verkürzung: überschüssige Fälligkeitspläne stornieren
            cancelFutureDueSchedules(newSubscription.getId(), newSubscription.getEndDate());
        }
    }

    private void cancelFutureDueSchedules(java.util.UUID subscriptionId, LocalDate cutoffDate) {
        try {
            var schedules = dueScheduleService.getDueSchedulesBySubscription(subscriptionId);

            for (var schedule : schedules) {
                if (schedule.getDueDate().isAfter(cutoffDate) &&
                        schedule.getStatus() == com.erp.backend.domain.DueStatus.PENDING) {
                    dueScheduleService.cancelDueSchedule(schedule.getId());
                }
            }
        } catch (Exception e) {
            logger.warn("Could not cancel future due schedules for subscription {}: {}",
                    subscriptionId, e.getMessage());
        }
    }

    private void handleExpiredSubscriptionDueSchedules(java.util.UUID subscriptionId) {
        try {
            var schedules = dueScheduleService.getDueSchedulesBySubscription(subscriptionId);

            for (var schedule : schedules) {
                // Zukünftige, unbezahlte Pläne stornieren
                if (schedule.getDueDate().isAfter(LocalDate.now()) &&
                        schedule.getStatus() == com.erp.backend.domain.DueStatus.PENDING) {
                    dueScheduleService.cancelDueSchedule(schedule.getId());
                }
            }
        } catch (Exception e) {
            logger.warn("Could not handle due schedules for expired subscription {}: {}",
                    subscriptionId, e.getMessage());
        }
    }
}