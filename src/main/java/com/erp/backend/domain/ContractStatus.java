package com.erp.backend.domain;

// Enum für Contract Status
public enum ContractStatus {
    DRAFT("Entwurf"),
    ACTIVE("Aktiv"),
    SUSPENDED("Pausiert"),
    TERMINATED("Beendet"),
    EXPIRED("Abgelaufen");

    private final String displayName;

    ContractStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

