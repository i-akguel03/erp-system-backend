package com.erp.backend.domain;

public enum DueStatus {
    PENDING("Ausstehend"),
    PAID("Bezahlt"),
    OVERDUE("Überfällig"),
    CANCELLED("Storniert"),
    PARTIAL_PAID("Teilweise bezahlt"),
    REFUNDED("Erstattet");

    private final String displayName;

    DueStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}