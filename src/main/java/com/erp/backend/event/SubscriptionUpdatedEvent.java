package com.erp.backend.event;

import com.erp.backend.domain.Subscription;
import org.springframework.context.ApplicationEvent; /**
 * Event wird ausgelöst wenn ein Abonnement aktualisiert wird
 */
public class SubscriptionUpdatedEvent extends ApplicationEvent {
    private final Subscription oldSubscription;
    private final Subscription newSubscription;
    private final boolean priceChanged;
    private final boolean billingCycleChanged;
    private final boolean endDateChanged;

    public SubscriptionUpdatedEvent(Object source, Subscription oldSubscription, Subscription newSubscription,
                                    boolean priceChanged, boolean billingCycleChanged, boolean endDateChanged) {
        super(source);
        this.oldSubscription = oldSubscription;
        this.newSubscription = newSubscription;
        this.priceChanged = priceChanged;
        this.billingCycleChanged = billingCycleChanged;
        this.endDateChanged = endDateChanged;
    }

    public Subscription getOldSubscription() {
        return oldSubscription;
    }

    public Subscription getNewSubscription() {
        return newSubscription;
    }

    public boolean isPriceChanged() {
        return priceChanged;
    }

    public boolean isBillingCycleChanged() {
        return billingCycleChanged;
    }

    public boolean isEndDateChanged() {
        return endDateChanged;
    }
}
