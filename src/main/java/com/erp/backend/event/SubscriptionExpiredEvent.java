package com.erp.backend.event;

import com.erp.backend.domain.Subscription;
import org.springframework.context.ApplicationEvent; /**
 * Event wird ausgelöst wenn ein Abonnement abläuft
 */
public class SubscriptionExpiredEvent extends ApplicationEvent {
    private final Subscription subscription;

    public SubscriptionExpiredEvent(Object source, Subscription subscription) {
        super(source);
        this.subscription = subscription;
    }

    public Subscription getSubscription() {
        return subscription;
    }
}
