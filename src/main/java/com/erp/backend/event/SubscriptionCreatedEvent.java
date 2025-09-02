package com.erp.backend.event;

import com.erp.backend.domain.Subscription;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Events für Subscription-Lifecycle zur besseren Entkopplung der Services
 */

/**
 * Event wird ausgelöst wenn ein neues Abonnement erstellt wird
 */
public class SubscriptionCreatedEvent extends ApplicationEvent {
    private final Subscription subscription;

    public SubscriptionCreatedEvent(Object source, Subscription subscription) {
        super(source);
        this.subscription = subscription;
    }

    public Subscription getSubscription() {
        return subscription;
    }
}

