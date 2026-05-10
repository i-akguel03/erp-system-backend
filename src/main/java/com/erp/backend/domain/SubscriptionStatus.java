package com.erp.backend.domain;

import java.util.EnumSet;
import java.util.Set;

public enum SubscriptionStatus {

    DRAFT("Entwurf",
            EnumSet.of(Ref.ACTIVE, Ref.CANCELLED)),

    ACTIVE("Aktiv",
            EnumSet.of(Ref.SUSPENDED, Ref.TERMINATED, Ref.CANCELLED, Ref.EXPIRED)),

    SUSPENDED("Pausiert",
            EnumSet.of(Ref.ACTIVE, Ref.TERMINATED, Ref.CANCELLED)),

    TERMINATED("Gekündigt",
            EnumSet.of(Ref.ACTIVE)),   // Kündigung aufheben

    CANCELLED("Storniert",
            EnumSet.noneOf(Ref.class)), // final

    EXPIRED("Abgelaufen",
            EnumSet.of(Ref.ACTIVE)); // Verlängerung erlaubt

    private final String displayName;
    private final Set<Ref> allowed;

    SubscriptionStatus(String displayName, Set<Ref> allowed) {
        this.displayName = displayName;
        this.allowed = allowed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canTransitionTo(SubscriptionStatus target) {
        return allowed.contains(Ref.valueOf(target.name()));
    }

    public boolean isFinal() {
        return this == CANCELLED || this == EXPIRED;
    }

    public boolean isEditLocked() {
        return isFinal();
    }

    // Indirektion nötig: Java-Enums können sich in Konstantendeklarationen nicht selbst referenzieren
    public enum Ref {
        DRAFT, ACTIVE, SUSPENDED, TERMINATED, CANCELLED, EXPIRED
    }
}
