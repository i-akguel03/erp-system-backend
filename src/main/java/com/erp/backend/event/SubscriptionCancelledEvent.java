package com.erp.backend.event;

import com.erp.backend.domain.Subscription;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate; /**
 * Event wird ausgelöst wenn ein Abonnement gekündigt wird
 */
public class SubscriptionCancelledEvent extends ApplicationEvent {
    private final Subscription subscription;
    private final LocalDate cancellationDate;

    public SubscriptionCancelledEvent(Object source, Subscription subscription, LocalDate cancellationDate) {
        super(source);
        this.subscription = subscription;
        this.cancellationDate = cancellationDate;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public LocalDate getCancellationDate() {
        return cancellationDate;
    }
}
