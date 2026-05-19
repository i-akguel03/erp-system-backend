package com.erp.backend.service;

import com.erp.backend.config.NotificationProperties;
import com.erp.backend.domain.Notification;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationEmailService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEmailService.class);

    private final NotificationProperties properties;
    private final JavaMailSender mailSender;

    public NotificationEmailService(NotificationProperties properties,
                                    @org.springframework.beans.factory.annotation.Autowired(required = false)
                                    JavaMailSender mailSender) {
        this.properties = properties;
        this.mailSender = mailSender;
    }

    @Async
    public void sendIfEnabled(Notification notification) {
        if (!properties.getEmail().isEnabled()) return;
        if (mailSender == null) {
            logger.warn("E-Mail aktiviert, aber kein JavaMailSender konfiguriert (SMTP_HOST fehlt?)");
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(properties.getEmail().getFrom());
            helper.setTo(properties.getEmail().getAdminAddress());
            helper.setSubject("[ERP] " + severityLabel(notification.getSeverity()) + " " + notification.getTitle());
            helper.setText(buildHtml(notification), true);
            mailSender.send(msg);
            logger.debug("E-Mail gesendet: {}", notification.getTitle());
        } catch (Exception e) {
            logger.error("E-Mail konnte nicht gesendet werden: {}", e.getMessage());
        }
    }

    private String buildHtml(Notification n) {
        String color = switch (n.getSeverity()) {
            case ERROR -> "#d32f2f";
            case WARNING -> "#f57c00";
            case INFO -> "#1976d2";
        };
        return """
                <html><body style="font-family:Arial,sans-serif;margin:0;padding:20px;background:#f5f5f5">
                <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)">
                  <div style="background:%s;color:#fff;padding:16px 24px">
                    <h2 style="margin:0">%s</h2>
                  </div>
                  <div style="padding:24px">
                    <p style="font-size:15px;color:#333">%s</p>
                    %s
                    <hr style="border:none;border-top:1px solid #eee;margin:20px 0">
                    <p style="font-size:12px;color:#999">ERP-System Benachrichtigung • %s</p>
                  </div>
                </div>
                </body></html>
                """.formatted(
                color, n.getTitle(), n.getMessage(),
                n.getEntityId() != null ? "<p style='font-size:12px;color:#666'>Entität: " + n.getEntityType() + " / " + n.getEntityId() + "</p>" : "",
                n.getCreatedAt() != null ? n.getCreatedAt().toString() : ""
        );
    }

    private String severityLabel(Notification.NotificationSeverity severity) {
        return switch (severity) {
            case ERROR -> "🔴";
            case WARNING -> "🟡";
            case INFO -> "🔵";
        };
    }
}
