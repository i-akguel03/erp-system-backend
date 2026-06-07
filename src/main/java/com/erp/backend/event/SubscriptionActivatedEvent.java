package com.erp.backend.event;

import com.erp.backend.domain.Subscription;
import org.springframework.context.ApplicationEvent;

public class SubscriptionActivatedEvent extends ApplicationEvent {

    private final Subscription subscription;

    public SubscriptionActivatedEvent(Object source, Subscription subscription) {
        super(source);
        this.subscription = subscription;
    }

    public Subscription getSubscription() {
        return subscription;
    }
}
