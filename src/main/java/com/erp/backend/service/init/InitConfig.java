package com.erp.backend.service.init;

/**
 * KONFIGURATION FÜR DATENINITIALISIERUNG
 * <p>
 * Diese Klasse steuert die Status-Verteilungen bei der Initialisierung.
 * <p>
 * STANDARD-VERHALTEN: Alle Entitäten werden auf ACTIVE erstellt (100%)
 * ALTERNATIVE: Realistische Verteilungen für Testing
 * <p>
 * Warum konfigurierbar?
 * - Production: Alle aktiv für saubere Startdaten
 * - Testing: Gemischte Status für realistische Szenarien
 * - Development: Viele aktive Daten für Feature-Entwicklung
 */
public class InitConfig {

    // ===============================================================================================
    // VERTRAG-STATUS-VERTEILUNG
    // ===============================================================================================
    private double activeContractRatio = 1.0;      // Standard: 100% ACTIVE
    private double terminatedContractRatio = 0.0;   // Standard: 0% TERMINATED

    // ===============================================================================================
    // ABONNEMENT-STATUS-VERTEILUNG
    // ===============================================================================================
    private double activeSubscriptionRatio = 1.0;   // Standard: 100% ACTIVE
    private double cancelledSubscriptionRatio = 0.0; // Standard: 0% CANCELLED
    private double pausedSubscriptionRatio = 0.0;    // Standard: 0% PAUSED

    // ===============================================================================================
    // RECHNUNG-STATUS-VERTEILUNG
    // ===============================================================================================
    private double activeInvoiceRatio = 1.0;        // Standard: 100% ACTIVE
    private double draftInvoiceRatio = 0.0;         // Standard: 0% DRAFT
    private double sentInvoiceRatio = 0.0;          // Standard: 0% SENT
    private double cancelledInvoiceRatio = 0.0;     // Standard: 0% CANCELLED

    // ===============================================================================================
    // DUE-SCHEDULE-STATUS-VERTEILUNG
    // ===============================================================================================
    private double activeDueScheduleRatio = 1.0;    // Standard: 100% ACTIVE
    private double completedDueScheduleRatio = 0.0; // Standard: 0% COMPLETED
    private double pausedDueScheduleRatio = 0.0;    // Standard: 0% PAUSED

    // ===============================================================================================
    // OPEN-ITEM-STATUS-VERTEILUNG
    // ===============================================================================================
    private double openOpenItemRatio = 1.0;         // Standard: 100% OPEN
    private double paidOpenItemRatio = 0.0;         // Standard: 0% PAID
    private double partiallyPaidOpenItemRatio = 0.0; // Standard: 0% PARTIALLY_PAID

    // ===============================================================================================
    // FACTORY-METHODEN FÜR VERSCHIEDENE SZENARIEN
    // ===============================================================================================

    /**
     * FACTORY-METHODE: Alles aktiv (Standard für Production)
     */
    public static InitConfig allActive() {
        return new InitConfig(); // Defaults sind bereits alle aktiv
    }

    /**
     * FACTORY-METHODE: Realistische Verteilung (für Testing)
     */
    public static InitConfig realistic() {
        InitConfig config = new InitConfig();

        // Verträge: 80% aktiv, 20% beendet
        config.setActiveContractRatio(0.8);
        config.setTerminatedContractRatio(0.2);

        // Abonnements: 85% aktiv, 10% storniert, 5% pausiert
        config.setActiveSubscriptionRatio(0.85);
        config.setCancelledSubscriptionRatio(0.10);
        config.setPausedSubscriptionRatio(0.05);

        // Rechnungen: 60% aktiv, 20% versendet, 15% entwurf, 5% storniert
        config.setActiveInvoiceRatio(0.60);
        config.setSentInvoiceRatio(0.20);
        config.setDraftInvoiceRatio(0.15);
        config.setCancelledInvoiceRatio(0.05);

        // DueSchedules: 40% aktiv, 50% abgerechnet, 10% pausiert
        config.setActiveDueScheduleRatio(0.40);
        config.setCompletedDueScheduleRatio(0.50);
        config.setPausedDueScheduleRatio(0.10);

        // OpenItems: 60% offen, 25% bezahlt, 15% teilweise bezahlt
        config.setOpenOpenItemRatio(0.60);
        config.setPaidOpenItemRatio(0.25);
        config.setPartiallyPaidOpenItemRatio(0.15);

        return config;
    }

    /**
     * FACTORY-METHODE: Development-Konfiguration (viele aktive Daten)
     */
    public static InitConfig development() {
        InitConfig config = new InitConfig();

        // Für Development: Mehr aktive Daten für Testing
        config.setActiveContractRatio(0.95);
        config.setTerminatedContractRatio(0.05);

        config.setActiveSubscriptionRatio(0.90);
        config.setCancelledSubscriptionRatio(0.05);
        config.setPausedSubscriptionRatio(0.05);

        config.setActiveInvoiceRatio(0.80);
        config.setSentInvoiceRatio(0.15);
        config.setDraftInvoiceRatio(0.05);

        config.setActiveDueScheduleRatio(0.70);
        config.setCompletedDueScheduleRatio(0.25);
        config.setPausedDueScheduleRatio(0.05);

        config.setOpenOpenItemRatio(0.80);
        config.setPaidOpenItemRatio(0.15);
        config.setPartiallyPaidOpenItemRatio(0.05);

        return config;
    }

    /**
     * FACTORY-METHODE: Demo-Konfiguration (für Präsentationen)
     */
    public static InitConfig demo() {
        InitConfig config = new InitConfig();

        // Demo: Ausgewogene Verteilung für Showcase
        config.setActiveContractRatio(0.70);
        config.setTerminatedContractRatio(0.30);

        config.setActiveSubscriptionRatio(0.75);
        config.setCancelledSubscriptionRatio(0.20);
        config.setPausedSubscriptionRatio(0.05);

        config.setActiveInvoiceRatio(0.50);
        config.setSentInvoiceRatio(0.30);
        config.setDraftInvoiceRatio(0.15);
        config.setCancelledInvoiceRatio(0.05);

        config.setActiveDueScheduleRatio(0.30);
        config.setCompletedDueScheduleRatio(0.60);
        config.setPausedDueScheduleRatio(0.10);

        config.setOpenOpenItemRatio(0.40);
        config.setPaidOpenItemRatio(0.40);
        config.setPartiallyPaidOpenItemRatio(0.20);

        return config;
    }

    // ===============================================================================================
    // HILFSMETHODEN
    // ===============================================================================================

    /**
     * Prüft ob Konfiguration "Alles aktiv" ist
     */
    public boolean isAllActive() {
        return activeContractRatio == 1.0 &&
                terminatedContractRatio == 0.0 &&
                activeSubscriptionRatio == 1.0 &&
                cancelledSubscriptionRatio == 0.0 &&
                pausedSubscriptionRatio == 0.0 &&
                activeInvoiceRatio == 1.0 &&
                draftInvoiceRatio == 0.0 &&
                sentInvoiceRatio == 0.0 &&
                cancelledInvoiceRatio == 0.0 &&
                activeDueScheduleRatio == 1.0 &&
                completedDueScheduleRatio == 0.0 &&
                pausedDueScheduleRatio == 0.0 &&
                openOpenItemRatio == 1.0 &&
                paidOpenItemRatio == 0.0 &&
                partiallyPaidOpenItemRatio == 0.0;
    }

    /**
     * Validiert dass alle Ratios zusammen 100% ergeben
     */
    public boolean isValid() {
        double contractTotal = activeContractRatio + terminatedContractRatio;
        double subscriptionTotal = activeSubscriptionRatio + cancelledSubscriptionRatio + pausedSubscriptionRatio;
        double invoiceTotal = activeInvoiceRatio + draftInvoiceRatio + sentInvoiceRatio + cancelledInvoiceRatio;
        double dueScheduleTotal = activeDueScheduleRatio + completedDueScheduleRatio + pausedDueScheduleRatio;
        double openItemTotal = openOpenItemRatio + paidOpenItemRatio + partiallyPaidOpenItemRatio;

        final double tolerance = 0.001; // Toleranz für Floating-Point-Vergleiche

        return Math.abs(contractTotal - 1.0) < tolerance &&
                Math.abs(subscriptionTotal - 1.0) < tolerance &&
                Math.abs(invoiceTotal - 1.0) < tolerance &&
                Math.abs(dueScheduleTotal - 1.0) < tolerance &&
                Math.abs(openItemTotal - 1.0) < tolerance;
    }

    /**
     * Beschreibung der Konfiguration für Logging
     */
    public String getDescription() {
        if (isAllActive()) {
            return "ALL-ACTIVE (Standard: alle Entitäten auf ACTIVE)";
        } else {
            return String.format("Custom (Verträge: %.0f%% aktiv, Abos: %.0f%% aktiv, Rechnungen: %.0f%% aktiv, OpenItems: %.0f%% offen)",
                    activeContractRatio * 100,
                    activeSubscriptionRatio * 100,
                    activeInvoiceRatio * 100,
                    openOpenItemRatio * 100);
        }
    }

    // ===============================================================================================
    // GETTERS UND SETTERS
    // ===============================================================================================

    // Vertrag-Status
    public double getActiveContractRatio() { return activeContractRatio; }
    public void setActiveContractRatio(double activeContractRatio) { this.activeContractRatio = activeContractRatio; }
    public double getTerminatedContractRatio() { return terminatedContractRatio; }
    public void setTerminatedContractRatio(double terminatedContractRatio) { this.terminatedContractRatio = terminatedContractRatio; }

    // Abonnement-Status
    public double getActiveSubscriptionRatio() { return activeSubscriptionRatio; }
    public void setActiveSubscriptionRatio(double activeSubscriptionRatio) { this.activeSubscriptionRatio = activeSubscriptionRatio; }
    public double getCancelledSubscriptionRatio() { return cancelledSubscriptionRatio; }
    public void setCancelledSubscriptionRatio(double cancelledSubscriptionRatio) { this.cancelledSubscriptionRatio = cancelledSubscriptionRatio; }
    public double getPausedSubscriptionRatio() { return pausedSubscriptionRatio; }
    public void setPausedSubscriptionRatio(double pausedSubscriptionRatio) { this.pausedSubscriptionRatio = pausedSubscriptionRatio; }

    // Rechnung-Status
    public double getActiveInvoiceRatio() { return activeInvoiceRatio; }
    public void setActiveInvoiceRatio(double activeInvoiceRatio) { this.activeInvoiceRatio = activeInvoiceRatio; }
    public double getDraftInvoiceRatio() { return draftInvoiceRatio; }
    public void setDraftInvoiceRatio(double draftInvoiceRatio) { this.draftInvoiceRatio = draftInvoiceRatio; }
    public double getSentInvoiceRatio() { return sentInvoiceRatio; }
    public void setSentInvoiceRatio(double sentInvoiceRatio) { this.sentInvoiceRatio = sentInvoiceRatio; }
    public double getCancelledInvoiceRatio() { return cancelledInvoiceRatio; }
    public void setCancelledInvoiceRatio(double cancelledInvoiceRatio) { this.cancelledInvoiceRatio = cancelledInvoiceRatio; }

    // DueSchedule-Status
    public double getActiveDueScheduleRatio() { return activeDueScheduleRatio; }
    public void setActiveDueScheduleRatio(double activeDueScheduleRatio) { this.activeDueScheduleRatio = activeDueScheduleRatio; }
    public double getCompletedDueScheduleRatio() { return completedDueScheduleRatio; }
    public void setCompletedDueScheduleRatio(double completedDueScheduleRatio) { this.completedDueScheduleRatio = completedDueScheduleRatio; }
    public double getPausedDueScheduleRatio() { return pausedDueScheduleRatio; }
    public void setPausedDueScheduleRatio(double pausedDueScheduleRatio) { this.pausedDueScheduleRatio = pausedDueScheduleRatio; }

    // OpenItem-Status
    public double getOpenOpenItemRatio() { return openOpenItemRatio; }
    public void setOpenOpenItemRatio(double openOpenItemRatio) { this.openOpenItemRatio = openOpenItemRatio; }
    public double getPaidOpenItemRatio() { return paidOpenItemRatio; }
    public void setPaidOpenItemRatio(double paidOpenItemRatio) { this.paidOpenItemRatio = paidOpenItemRatio; }
    public double getPartiallyPaidOpenItemRatio() { return partiallyPaidOpenItemRatio; }
    public void setPartiallyPaidOpenItemRatio(double partiallyPaidOpenItemRatio) { this.partiallyPaidOpenItemRatio = partiallyPaidOpenItemRatio; }

    // ===============================================================================================
    // BUILDER-PATTERN FÜR FLUENT API
    // ===============================================================================================

    /**
     * Builder-Klasse für fluente Konfiguration
     */
    public static class Builder {
        private InitConfig config = new InitConfig();

        public Builder contractStatus(double active, double terminated) {
            config.setActiveContractRatio(active);
            config.setTerminatedContractRatio(terminated);
            return this;
        }

        public Builder subscriptionStatus(double active, double cancelled, double paused) {
            config.setActiveSubscriptionRatio(active);
            config.setCancelledSubscriptionRatio(cancelled);
            config.setPausedSubscriptionRatio(paused);
            return this;
        }

        public Builder invoiceStatus(double active, double draft, double sent, double cancelled) {
            config.setActiveInvoiceRatio(active);
            config.setDraftInvoiceRatio(draft);
            config.setSentInvoiceRatio(sent);
            config.setCancelledInvoiceRatio(cancelled);
            return this;
        }

        public Builder dueScheduleStatus(double active, double completed, double paused) {
            config.setActiveDueScheduleRatio(active);
            config.setCompletedDueScheduleRatio(completed);
            config.setPausedDueScheduleRatio(paused);
            return this;
        }

        public Builder openItemStatus(double open, double paid, double partiallyPaid) {
            config.setOpenOpenItemRatio(open);
            config.setPaidOpenItemRatio(paid);
            config.setPartiallyPaidOpenItemRatio(partiallyPaid);
            return this;
        }

        public InitConfig build() {
            if (!config.isValid()) {
                throw new IllegalArgumentException("Ungültige Konfiguration: Ratios müssen für jede Entität zusammen 1.0 ergeben");
            }
            return config;
        }
    }

    /**
     * Statische Methode zum Erstellen eines Builders
     */
    public static Builder builder() {
        return new Builder();
    }

    // ===============================================================================================
    // ÜBERGEORDNETE KONFIGURATIONSMETHODEN
    // ===============================================================================================

    /**
     * Setzt alle Status auf "mostly active" (90% aktiv, 10% andere)
     */
    public InitConfig withMostlyActiveStatus() {
        setActiveContractRatio(0.9);
        setTerminatedContractRatio(0.1);

        setActiveSubscriptionRatio(0.9);
        setCancelledSubscriptionRatio(0.05);
        setPausedSubscriptionRatio(0.05);

        setActiveInvoiceRatio(0.9);
        setDraftInvoiceRatio(0.05);
        setSentInvoiceRatio(0.05);

        setActiveDueScheduleRatio(0.9);
        setCompletedDueScheduleRatio(0.05);
        setPausedDueScheduleRatio(0.05);

        setOpenOpenItemRatio(0.9);
        setPaidOpenItemRatio(0.05);
        setPartiallyPaidOpenItemRatio(0.05);

        return this;
    }

    /**
     * Setzt alle Status auf ausgewogene Verteilung
     */
    public InitConfig withBalancedStatus() {
        setActiveContractRatio(0.6);
        setTerminatedContractRatio(0.4);

        setActiveSubscriptionRatio(0.6);
        setCancelledSubscriptionRatio(0.3);
        setPausedSubscriptionRatio(0.1);

        setActiveInvoiceRatio(0.5);
        setDraftInvoiceRatio(0.2);
        setSentInvoiceRatio(0.2);
        setCancelledInvoiceRatio(0.1);

        setActiveDueScheduleRatio(0.4);
        setCompletedDueScheduleRatio(0.5);
        setPausedDueScheduleRatio(0.1);

        setOpenOpenItemRatio(0.4);
        setPaidOpenItemRatio(0.4);
        setPartiallyPaidOpenItemRatio(0.2);

        return this;
    }

    @Override
    public String toString() {
        return String.format("InitConfig{activeContract=%.0f%%, activeSubscription=%.0f%%, activeInvoice=%.0f%%, openItems=%.0f%%}",
                activeContractRatio * 100,
                activeSubscriptionRatio * 100,
                activeInvoiceRatio * 100,
                openOpenItemRatio * 100);
    }
}