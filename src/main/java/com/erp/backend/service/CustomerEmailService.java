package com.erp.backend.service;

import com.erp.backend.config.NotificationProperties;
import com.erp.backend.domain.Contract;
import com.erp.backend.domain.Customer;
import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.OpenItem;
import com.erp.backend.domain.Subscription;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class CustomerEmailService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerEmailService.class);

    private final NotificationProperties properties;
    private final JavaMailSender mailSender;

    public CustomerEmailService(NotificationProperties properties,
                                @Autowired(required = false) JavaMailSender mailSender) {
        this.properties = properties;
        this.mailSender = mailSender;
    }

    public void sendWelcomeEmail(Customer customer) {
        if (!isEnabled()) return;
        send(
                customer.getEmail(),
                "Willkommen bei unserem ERP-System, " + customer.getFirstName() + "!",
                buildWelcomeHtml(customer)
        );
    }

    public void sendInvoiceEmail(Invoice invoice) {
        if (!isEnabled()) return;
        Customer customer = invoice.getCustomer();
        if (customer == null || customer.getEmail() == null) return;
        send(
                customer.getEmail(),
                "Ihre Rechnung " + invoice.getInvoiceNumber(),
                buildInvoiceHtml(invoice, customer)
        );
    }

    public void sendPaymentReminder(OpenItem item) {
        if (!isEnabled()) return;
        Invoice invoice = item.getInvoice();
        if (invoice == null) return;
        Customer customer = invoice.getCustomer();
        if (customer == null || customer.getEmail() == null) return;
        send(
                customer.getEmail(),
                "Zahlungserinnerung – Rechnung " + invoice.getInvoiceNumber(),
                buildPaymentReminderHtml(item, invoice, customer)
        );
    }

    public void sendContractExpiryNotice(Contract contract) {
        if (!isEnabled()) return;
        Customer customer = contract.getCustomer();
        if (customer == null || customer.getEmail() == null) return;
        send(
                customer.getEmail(),
                "Ihr Vertrag läuft bald ab – " + contract.getContractTitle(),
                buildContractExpiryHtml(contract, customer)
        );
    }

    public void sendSubscriptionExpiryNotice(Subscription subscription) {
        if (!isEnabled()) return;
        if (subscription.getContract() == null) return;
        Customer customer = subscription.getContract().getCustomer();
        if (customer == null || customer.getEmail() == null) return;
        send(
                customer.getEmail(),
                "Ihr Abonnement läuft bald ab – " + subscription.getProductName(),
                buildSubscriptionExpiryHtml(subscription, customer)
        );
    }

    // --- private helpers ---

    private boolean isEnabled() {
        if (!properties.getEmail().isCustomerEmailsEnabled()) return false;
        if (mailSender == null) {
            logger.warn("Kunden-E-Mail aktiviert, aber kein JavaMailSender konfiguriert");
            return false;
        }
        return true;
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(properties.getEmail().getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            logger.debug("Kunden-E-Mail gesendet an {}: {}", to, subject);
        } catch (Exception e) {
            logger.error("Kunden-E-Mail konnte nicht gesendet werden an {}: {}", to, e.getMessage());
        }
    }

    private String buildWelcomeHtml(Customer customer) {
        return """
                <html><body style="font-family:Arial,sans-serif;margin:0;padding:20px;background:#f5f5f5">
                <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)">
                  <div style="background:#1976d2;color:#fff;padding:16px 24px">
                    <h2 style="margin:0">Willkommen!</h2>
                  </div>
                  <div style="padding:24px">
                    <p>Hallo %s %s,</p>
                    <p>Ihr Kundenkonto wurde erfolgreich angelegt.</p>
                    <table style="border-collapse:collapse;width:100%%">
                      <tr><td style="padding:8px;color:#666">Kundennummer:</td><td style="padding:8px;font-weight:bold">%s</td></tr>
                      <tr><td style="padding:8px;color:#666">E-Mail:</td><td style="padding:8px">%s</td></tr>
                    </table>
                    <hr style="border:none;border-top:1px solid #eee;margin:20px 0">
                    <p style="font-size:12px;color:#999">ERP-System • %s</p>
                  </div>
                </div>
                </body></html>
                """.formatted(
                customer.getFirstName(), customer.getLastName(),
                customer.getCustomerNumber(), customer.getEmail(),
                LocalDate.now()
        );
    }

    private String buildInvoiceHtml(Invoice invoice, Customer customer) {
        BigDecimal total = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        return """
                <html><body style="font-family:Arial,sans-serif;margin:0;padding:20px;background:#f5f5f5">
                <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)">
                  <div style="background:#1976d2;color:#fff;padding:16px 24px">
                    <h2 style="margin:0">Rechnung %s</h2>
                  </div>
                  <div style="padding:24px">
                    <p>Hallo %s %s,</p>
                    <p>hiermit erhalten Sie Ihre Rechnung.</p>
                    <table style="border-collapse:collapse;width:100%%">
                      <tr><td style="padding:8px;color:#666">Rechnungsnummer:</td><td style="padding:8px;font-weight:bold">%s</td></tr>
                      <tr><td style="padding:8px;color:#666">Rechnungsdatum:</td><td style="padding:8px">%s</td></tr>
                      <tr><td style="padding:8px;color:#666">Fälligkeitsdatum:</td><td style="padding:8px">%s</td></tr>
                      <tr style="background:#e3f2fd"><td style="padding:8px;color:#666">Gesamtbetrag:</td><td style="padding:8px;font-weight:bold;font-size:16px">%.2f €</td></tr>
                    </table>
                    <hr style="border:none;border-top:1px solid #eee;margin:20px 0">
                    <p style="font-size:12px;color:#999">ERP-System • %s</p>
                  </div>
                </div>
                </body></html>
                """.formatted(
                invoice.getInvoiceNumber(),
                customer.getFirstName(), customer.getLastName(),
                invoice.getInvoiceNumber(), invoice.getInvoiceDate(), invoice.getDueDate(),
                total, LocalDate.now()
        );
    }

    private String buildPaymentReminderHtml(OpenItem item, Invoice invoice, Customer customer) {
        long daysOverdue = item.getDueDate() != null
                ? ChronoUnit.DAYS.between(item.getDueDate(), LocalDate.now())
                : 0;
        BigDecimal amount = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
        return """
                <html><body style="font-family:Arial,sans-serif;margin:0;padding:20px;background:#f5f5f5">
                <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)">
                  <div style="background:#f57c00;color:#fff;padding:16px 24px">
                    <h2 style="margin:0">Zahlungserinnerung</h2>
                  </div>
                  <div style="padding:24px">
                    <p>Hallo %s %s,</p>
                    <p>wir möchten Sie daran erinnern, dass folgende Zahlung noch aussteht:</p>
                    <table style="border-collapse:collapse;width:100%%">
                      <tr><td style="padding:8px;color:#666">Rechnungsnummer:</td><td style="padding:8px;font-weight:bold">%s</td></tr>
                      <tr><td style="padding:8px;color:#666">Fälligkeitsdatum:</td><td style="padding:8px">%s</td></tr>
                      <tr><td style="padding:8px;color:#666">Überfällig seit:</td><td style="padding:8px;color:#d32f2f;font-weight:bold">%d Tag(e)</td></tr>
                      <tr style="background:#fff3e0"><td style="padding:8px;color:#666">Offener Betrag:</td><td style="padding:8px;font-weight:bold;font-size:16px;color:#e65100">%.2f €</td></tr>
                    </table>
                    <p>Bitte begleichen Sie den ausstehenden Betrag schnellstmöglich.</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:20px 0">
                    <p style="font-size:12px;color:#999">ERP-System • %s</p>
                  </div>
                </div>
                </body></html>
                """.formatted(
                customer.getFirstName(), customer.getLastName(),
                invoice.getInvoiceNumber(), item.getDueDate(),
                daysOverdue, amount, LocalDate.now()
        );
    }

    private String buildContractExpiryHtml(Contract contract, Customer customer) {
        long daysLeft = contract.getEndDate() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate())
                : 0;
        return """
                <html><body style="font-family:Arial,sans-serif;margin:0;padding:20px;background:#f5f5f5">
                <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)">
                  <div style="background:#f57c00;color:#fff;padding:16px 24px">
                    <h2 style="margin:0">Ihr Vertrag läuft bald ab</h2>
                  </div>
                  <div style="padding:24px">
                    <p>Hallo %s %s,</p>
                    <p>Ihr Vertrag läuft in <strong>%d Tag(en)</strong> ab.</p>
                    <table style="border-collapse:collapse;width:100%%">
                      <tr><td style="padding:8px;color:#666">Vertragstitel:</td><td style="padding:8px;font-weight:bold">%s</td></tr>
                      <tr><td style="padding:8px;color:#666">Vertragsnummer:</td><td style="padding:8px">%s</td></tr>
                      <tr style="background:#fff3e0"><td style="padding:8px;color:#666">Ablaufdatum:</td><td style="padding:8px;font-weight:bold;color:#e65100">%s</td></tr>
                    </table>
                    <p>Bitte kontaktieren Sie uns für eine Verlängerung.</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:20px 0">
                    <p style="font-size:12px;color:#999">ERP-System • %s</p>
                  </div>
                </div>
                </body></html>
                """.formatted(
                customer.getFirstName(), customer.getLastName(),
                daysLeft, contract.getContractTitle(), contract.getContractNumber(),
                contract.getEndDate(), LocalDate.now()
        );
    }

    private String buildSubscriptionExpiryHtml(Subscription subscription, Customer customer) {
        long daysLeft = subscription.getEndDate() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), subscription.getEndDate())
                : 0;
        return """
                <html><body style="font-family:Arial,sans-serif;margin:0;padding:20px;background:#f5f5f5">
                <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)">
                  <div style="background:#f57c00;color:#fff;padding:16px 24px">
                    <h2 style="margin:0">Ihr Abonnement läuft bald ab</h2>
                  </div>
                  <div style="padding:24px">
                    <p>Hallo %s %s,</p>
                    <p>Ihr Abonnement läuft in <strong>%d Tag(en)</strong> ab.</p>
                    <table style="border-collapse:collapse;width:100%%">
                      <tr><td style="padding:8px;color:#666">Produkt:</td><td style="padding:8px;font-weight:bold">%s</td></tr>
                      <tr><td style="padding:8px;color:#666">Abonnementnummer:</td><td style="padding:8px">%s</td></tr>
                      <tr style="background:#fff3e0"><td style="padding:8px;color:#666">Ablaufdatum:</td><td style="padding:8px;font-weight:bold;color:#e65100">%s</td></tr>
                    </table>
                    <p>Bitte kontaktieren Sie uns für eine Verlängerung.</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:20px 0">
                    <p style="font-size:12px;color:#999">ERP-System • %s</p>
                  </div>
                </div>
                </body></html>
                """.formatted(
                customer.getFirstName(), customer.getLastName(),
                daysLeft, subscription.getProductName(), subscription.getSubscriptionNumber(),
                subscription.getEndDate(), LocalDate.now()
        );
    }
}
