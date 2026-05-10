package com.erp.backend.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ContractStatus {

    DRAFT("Entwurf",
            EnumSet.of(ContractStatusRef.ACTIVE, ContractStatusRef.CANCELLED)),

    ACTIVE("Aktiv",
            EnumSet.of(ContractStatusRef.TERMINATED, ContractStatusRef.SUSPENDED,
                       ContractStatusRef.CANCELLED, ContractStatusRef.EXPIRED)),

    SUSPENDED("Pausiert",
            EnumSet.of(ContractStatusRef.ACTIVE, ContractStatusRef.TERMINATED,
                       ContractStatusRef.CANCELLED)),

    TERMINATED("Gekündigt",
            EnumSet.of(ContractStatusRef.ACTIVE)),   // Kündigung aufheben

    CANCELLED("Storniert",
            EnumSet.noneOf(ContractStatusRef.class)), // final – keine Übergänge

    EXPIRED("Abgelaufen",
            EnumSet.of(ContractStatusRef.ACTIVE)); // Verlängerung erlaubt

    private final String displayName;

    // Indirection enum, weil ein Enum sich nicht selbst in eigenen Konstanten referenzieren kann
    private final Set<ContractStatusRef> allowed;

    ContractStatus(String displayName, Set<ContractStatusRef> allowed) {
        this.displayName = displayName;
        this.allowed = allowed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canTransitionTo(ContractStatus target) {
        return allowed.contains(ContractStatusRef.valueOf(target.name()));
    }

    public boolean isFinal() {
        return this == CANCELLED || this == EXPIRED;
    }

    public boolean isEditLocked() {
        return isFinal();
    }

    // Indirektion nötig: Java-Enums können sich in Konstantendeklarationen nicht selbst referenzieren
    public enum ContractStatusRef {
        DRAFT, ACTIVE, SUSPENDED, TERMINATED, CANCELLED, EXPIRED
    }
}