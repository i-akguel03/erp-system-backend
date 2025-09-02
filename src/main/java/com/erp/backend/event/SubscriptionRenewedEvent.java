package com.erp.backend.event;

import com.erp.backend.domain.Subscription;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate; /**
 * Event wird ausgelöst wenn ein Abonnement verlängert wird
 */
public class SubscriptionRenewedEvent extends ApplicationEvent {
    private final Subscription subscription;
    private final LocalDate oldEndDate;
    private final LocalDate newEndDate;

    public SubscriptionRenewedEvent(Object source, Subscription subscription, LocalDate oldEndDate, LocalDate newEndDate) {
        super(source);
        this.subscription = subscription;
        this.oldEndDate = oldEndDate;
        this.newEndDate = newEndDate;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public LocalDate getOldEndDate() {
        return oldEndDate;
    }

    public LocalDate getNewEndDate() {
        return newEndDate;
    }
}
