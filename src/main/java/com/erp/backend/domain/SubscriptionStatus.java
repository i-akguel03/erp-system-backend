package com.erp.backend.domain;

// Enum für Subscription Status
public enum SubscriptionStatus {
    ACTIVE("Aktiv"),
    PAUSED("Pausiert"),
    CANCELLED("Gekündigt"),
    EXPIRED("Abgelaufen"),
    PENDING("Ausstehend");

    private final String displayName;

    SubscriptionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
