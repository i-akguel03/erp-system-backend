package com.erp.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.notifications")
public class NotificationProperties {

    private Email email = new Email();
    private Triggers triggers = new Triggers();

    public Email getEmail() { return email; }
    public void setEmail(Email email) { this.email = email; }

    public Triggers getTriggers() { return triggers; }
    public void setTriggers(Triggers triggers) { this.triggers = triggers; }

    public static class Email {
        private boolean enabled = false;
        private boolean customerEmailsEnabled = false;
        private String from = "noreply@erp.local";
        private String adminAddress = "admin@erp.local";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public boolean isCustomerEmailsEnabled() { return customerEmailsEnabled; }
        public void setCustomerEmailsEnabled(boolean customerEmailsEnabled) { this.customerEmailsEnabled = customerEmailsEnabled; }

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }

        public String getAdminAddress() { return adminAddress; }
        public void setAdminAddress(String adminAddress) { this.adminAddress = adminAddress; }
    }

    public static class Triggers {
        private boolean overdueEnabled = true;
        private String overdueCron = "0 0 8 * * *";
        private int expiringDays = 30;
        private boolean expiringEnabled = true;
        private boolean invoiceBatchEnabled = true;
        private boolean newEntityEnabled = true;

        public boolean isOverdueEnabled() { return overdueEnabled; }
        public void setOverdueEnabled(boolean overdueEnabled) { this.overdueEnabled = overdueEnabled; }

        public String getOverdueCron() { return overdueCron; }
        public void setOverdueCron(String overdueCron) { this.overdueCron = overdueCron; }

        public int getExpiringDays() { return expiringDays; }
        public void setExpiringDays(int expiringDays) { this.expiringDays = expiringDays; }

        public boolean isExpiringEnabled() { return expiringEnabled; }
        public void setExpiringEnabled(boolean expiringEnabled) { this.expiringEnabled = expiringEnabled; }

        public boolean isInvoiceBatchEnabled() { return invoiceBatchEnabled; }
        public void setInvoiceBatchEnabled(boolean invoiceBatchEnabled) { this.invoiceBatchEnabled = invoiceBatchEnabled; }

        public boolean isNewEntityEnabled() { return newEntityEnabled; }
        public void setNewEntityEnabled(boolean newEntityEnabled) { this.newEntityEnabled = newEntityEnabled; }
    }
}
