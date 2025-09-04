package com.erp.backend.service.event;

// Importiere alle Subscription-Events, die wir behandeln wollen
import com.erp.backend.event.SubscriptionCancelledEvent;
import com.erp.backend.event.SubscriptionCreatedEvent;
import com.erp.backend.event.SubscriptionExpiredEvent;
import com.erp.backend.event.SubscriptionRenewedEvent;
import com.erp.backend.event.SubscriptionUpdatedEvent;

// Importiere unseren Service, der die Fälligkeitspläne verwaltet
import com.erp.backend.service.DueScheduleService;

// Logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Spring-spezifische Imports
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// Hilfsklassen für Datum
import java.time.LocalDate;

/**
 * SubscriptionEventHandler ist ein Spring-Component, der auf verschiedene
 * Subscription-Events reagiert und automatisch die Fälligkeitspläne erstellt,
 * aktualisiert oder löscht.
 *
 * Wichtige Konzepte:
 * - Event-Listener: Methoden, die auf Events reagieren.
 * - @Async: Methoden laufen asynchron im Hintergrund.
 */
@Component // Kennzeichnet die Klasse als Spring-Bean, damit sie automatisch erkannt wird
public class SubscriptionEventHandler {

    // Logger für Ausgaben, z.B. Info oder Fehler
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionEventHandler.class);

    // Wir brauchen unseren DueScheduleService, um Fälligkeitspläne zu erstellen/ändern
    @Autowired // Spring injectet automatisch die passende Instanz
    private DueScheduleService dueScheduleService;

    /**
     * EventListener für neue Abonnements.
     * Erstellt automatisch Fälligkeitspläne für das neue Abonnement.
     *
     * @param event SubscriptionCreatedEvent
     */
    @EventListener // Spring erkennt diese Methode als Listener für ein Event
    @Async // Läuft asynchron, damit die Hauptoperation nicht blockiert wird
    public void handleSubscriptionCreated(SubscriptionCreatedEvent event) {
        try {
            // Berechne, wie viele Monate initial erzeugt werden sollen
            int monthsToGenerate = calculateInitialMonths(event.getSubscription());

            // Wenn mehr als 0 Monate erzeugt werden sollen
            if (monthsToGenerate > 0) {
                // Erzeuge die Fälligkeitspläne
                dueScheduleService.generateDueSchedulesForSubscription(
                        event.getSubscription().getId(), monthsToGenerate);

                // Log-Ausgabe
                logger.info("Automatically generated {} months of due schedules for new subscription {}",
                        monthsToGenerate, event.getSubscription().getId());
            }
        } catch (Exception e) {
            // Fehlerbehandlung, damit das System nicht abstürzt
            logger.error("Failed to create due schedules for new subscription {}: {}",
                    event.getSubscription().getId(), e.getMessage());
        }
    }

    /**
     * EventListener für Updates von Abonnements.
     * Prüft, ob Preis, Abrechnungszyklus oder Enddatum geändert wurde.
     * Dann synchronisiert es die Fälligkeitspläne.
     */
    @EventListener
    @Async
    public void handleSubscriptionUpdated(SubscriptionUpdatedEvent event) {
        try {
            // Nur reagieren, wenn relevante Änderungen stattgefunden haben
            if (event.isPriceChanged() || event.isBillingCycleChanged() || event.isEndDateChanged()) {

                // Preisänderung -> zukünftige Fälligkeiten anpassen
                if (event.isPriceChanged()) {
                    updateFutureDueSchedulePrices(event.getNewSubscription());
                }

                // Enddatum geändert -> zusätzliche oder überschüssige Pläne behandeln
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
     * EventListener für gekündigte Abonnements.
     * Storniert alle zukünftigen Fälligkeiten ab dem Kündigungsdatum.
     */
    @EventListener
    @Async
    public void handleSubscriptionCancelled(SubscriptionCancelledEvent event) {
        try {
            cancelFutureDueSchedules(event.getSubscription().getId(), event.getCancellationDate());

            logger.info("Cancelled future due schedules for cancelled subscription {} from date {}",
                    event.getSubscription().getId(), event.getCancellationDate());
        } catch (Exception e) {
            logger.error("Failed to cancel due schedules for cancelled subscription {}: {}",
                    event.getSubscription().getId(), e.getMessage());
        }
    }

    /**
     * EventListener für verlängerte Abonnements.
     * Erstellt zusätzliche Fälligkeiten für die Verlängerung.
     */
    @EventListener
    @Async
    public void handleSubscriptionRenewed(SubscriptionRenewedEvent event) {
        try {
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
     * EventListener für abgelaufene Abonnements.
     * Storniert offene zukünftige Fälligkeiten nach Ablauf.
     */
    @EventListener
    @Async
    public void handleSubscriptionExpired(SubscriptionExpiredEvent event) {
        try {
            handleExpiredSubscriptionDueSchedules(event.getSubscription().getId());

            logger.info("Handled due schedules for expired subscription {}",
                    event.getSubscription().getId());
        } catch (Exception e) {
            logger.error("Failed to handle due schedules for expired subscription {}: {}",
                    event.getSubscription().getId(), e.getMessage());
        }
    }

    // ========================================================================
    // Private Hilfsmethoden (werden nur intern verwendet)
    // ========================================================================

    /**
     * Berechnet die initialen Monate für die Fälligkeitspläne.
     * - Maximal 12 Monate
     * - Standard 12 Monate, falls Enddatum nicht gesetzt
     */
    private int calculateInitialMonths(com.erp.backend.domain.Subscription subscription) {
        if (subscription.getEndDate() == null) {
            return 12; // Standard: 1 Jahr
        }

        LocalDate startDate = subscription.getStartDate();
        LocalDate endDate = subscription.getEndDate();

        long totalMonths = startDate.until(endDate).toTotalMonths();
        return Math.max(1, Math.min(12, (int) totalMonths)); // 1 bis 12 Monate
    }

    /**
     * Aktualisiert zukünftige Fälligkeiten, wenn der Preis geändert wurde.
     */
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

    /**
     * Behandelt Änderungen des Enddatums:
     * - Verlängerung -> zusätzliche Fälligkeiten erstellen
     * - Verkürzung -> überschüssige Fälligkeiten stornieren
     */
    private void handleEndDateChange(com.erp.backend.domain.Subscription oldSubscription,
                                     com.erp.backend.domain.Subscription newSubscription) {

        if (newSubscription.getEndDate().isAfter(oldSubscription.getEndDate())) {
            // Verlängerung
            int additionalMonths = (int) oldSubscription.getEndDate().until(newSubscription.getEndDate()).toTotalMonths();
            if (additionalMonths > 0) {
                try {
                    dueScheduleService.generateDueSchedulesForSubscription(newSubscription.getId(), additionalMonths);
                } catch (Exception e) {
                    logger.warn("Could not generate additional due schedules: {}", e.getMessage());
                }
            }
        } else if (newSubscription.getEndDate().isBefore(oldSubscription.getEndDate())) {
            // Verkürzung
            cancelFutureDueSchedules(newSubscription.getId(), newSubscription.getEndDate());
        }
    }

    /**
     * Storniert alle zukünftigen Fälligkeiten ab einem bestimmten Datum.
     */
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

    /**
     * Behandelt abgelaufene Abonnements:
     * Storniert alle offenen zukünftigen Fälligkeiten.
     */
    private void handleExpiredSubscriptionDueSchedules(java.util.UUID subscriptionId) {
        try {
            var schedules = dueScheduleService.getDueSchedulesBySubscription(subscriptionId);

            for (var schedule : schedules) {
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
