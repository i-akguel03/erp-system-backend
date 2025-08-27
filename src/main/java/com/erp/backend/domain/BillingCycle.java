package com.erp.backend.domain;

// Enum für Billing Cycle
public enum BillingCycle {
    MONTHLY("Monatlich"),
    QUARTERLY("Vierteljährlich"),
    SEMI_ANNUALLY("Halbjährlich"),
    ANNUALLY("Jährlich");

    private final String displayName;

    BillingCycle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
