package com.erp.backend.mapper;

import com.erp.backend.domain.OpenItem;
import com.erp.backend.dto.OpenItemDTO;

/**
 * Mapper für OpenItem Entity <-> OpenItemDTO Konvertierung.
 */
public class OpenItemMapper {

    private OpenItemMapper() {
        // Utility-Klasse - kein Konstruktor
    }

    /**
     * Konvertiert OpenItem Entity zu OpenItemDTO
     */
    public static OpenItemDTO toDTO(OpenItem openItem) {
        if (openItem == null) {
            return null;
        }

        OpenItemDTO dto = new OpenItemDTO();

        // Basic fields
        dto.setId(openItem.getId());
        dto.setDescription(openItem.getDescription());
        dto.setAmount(openItem.getAmount());
        dto.setDueDate(openItem.getDueDate());
        dto.setStatus(openItem.getStatus() != null ? openItem.getStatus().name() : null);
        dto.setPaidAmount(openItem.getPaidAmount());
        dto.setPaidDate(openItem.getPaidDate());
        dto.setPaymentMethod(openItem.getPaymentMethod());
        dto.setPaymentReference(openItem.getPaymentReference());
        dto.setCreatedDate(openItem.getCreatedDate());
        dto.setUpdatedDate(openItem.getUpdatedDate());
        dto.setNotes(openItem.getNotes());
        dto.setLastReminderDate(openItem.getLastReminderDate());
        dto.setReminderCount(openItem.getReminderCount());

        // Calculated fields
        dto.setOutstandingAmount(openItem.getOutstandingAmount());
        dto.setOverdue(openItem.isOverdue());

        // Invoice information
        if (openItem.getInvoice() != null) {
            dto.setInvoiceId(openItem.getInvoice().getId());
            dto.setInvoiceNumber(openItem.getInvoice().getInvoiceNumber());

            // Customer information (from invoice)
            if (openItem.getInvoice().getCustomer() != null) {
                dto.setCustomerId(openItem.getInvoice().getCustomer().getId().toString());
                // Passen Sie den Methodenaufruf an Ihre Customer-Klasse an:
                // dto.setCustomerName(openItem.getInvoice().getCustomer().getName());
                // oder
                // dto.setCustomerName(openItem.getInvoice().getCustomer().getFirstName() + " " + openItem.getInvoice().getCustomer().getLastName());
                // oder welche Methode auch immer in Ihrer Customer-Klasse existiert
                dto.setCustomerName("Customer " + openItem.getInvoice().getCustomer().getId()); // Fallback
            }
        }

        return dto;
    }

    /**
     * Konvertiert OpenItemDTO zu OpenItem Entity
     * Hinweis: Für vollständige Entity sollten Invoice und Customer separat geladen werden
     */
    public static OpenItem toEntity(OpenItemDTO dto) {
        if (dto == null) {
            return null;
        }

        OpenItem openItem = new OpenItem();

        // Basic fields
        openItem.setId(dto.getId());
        openItem.setDescription(dto.getDescription());
        openItem.setAmount(dto.getAmount());
        openItem.setDueDate(dto.getDueDate());

        // Status conversion
        if (dto.getStatus() != null) {
            try {
                openItem.setStatus(OpenItem.OpenItemStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                // Default to OPEN if invalid status
                openItem.setStatus(OpenItem.OpenItemStatus.OPEN);
            }
        }

        openItem.setPaidAmount(dto.getPaidAmount());
        openItem.setPaidDate(dto.getPaidDate());
        openItem.setPaymentMethod(dto.getPaymentMethod());
        openItem.setPaymentReference(dto.getPaymentReference());
        openItem.setCreatedDate(dto.getCreatedDate());
        openItem.setUpdatedDate(dto.getUpdatedDate());
        openItem.setNotes(dto.getNotes());
        openItem.setLastReminderDate(dto.getLastReminderDate());
        openItem.setReminderCount(dto.getReminderCount());

        // Invoice reference (nur ID wird gesetzt - Entity muss separat geladen werden)
        if (dto.getInvoiceId() != null) {
            com.erp.backend.domain.Invoice invoice = new com.erp.backend.domain.Invoice();
            invoice.setId(dto.getInvoiceId());
            openItem.setInvoice(invoice);
        }

        return openItem;
    }

    /**
     * Aktualisiert eine bestehende OpenItem Entity mit Daten aus DTO
     * (Behält Referenzen wie Invoice bei)
     */
    public static void updateEntity(OpenItem existingOpenItem, OpenItemDTO dto) {
        if (existingOpenItem == null || dto == null) {
            return;
        }

        // Basic fields update
        if (dto.getDescription() != null) {
            existingOpenItem.setDescription(dto.getDescription());
        }
        if (dto.getAmount() != null) {
            existingOpenItem.setAmount(dto.getAmount());
        }
        if (dto.getDueDate() != null) {
            existingOpenItem.setDueDate(dto.getDueDate());
        }
        if (dto.getStatus() != null) {
            try {
                existingOpenItem.setStatus(OpenItem.OpenItemStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                // Keep existing status if invalid
            }
        }
        if (dto.getPaidAmount() != null) {
            existingOpenItem.setPaidAmount(dto.getPaidAmount());
        }
        if (dto.getPaidDate() != null) {
            existingOpenItem.setPaidDate(dto.getPaidDate());
        }
        if (dto.getPaymentMethod() != null) {
            existingOpenItem.setPaymentMethod(dto.getPaymentMethod());
        }
        if (dto.getPaymentReference() != null) {
            existingOpenItem.setPaymentReference(dto.getPaymentReference());
        }
        if (dto.getNotes() != null) {
            existingOpenItem.setNotes(dto.getNotes());
        }
        if (dto.getLastReminderDate() != null) {
            existingOpenItem.setLastReminderDate(dto.getLastReminderDate());
        }
        if (dto.getReminderCount() != null) {
            existingOpenItem.setReminderCount(dto.getReminderCount());
        }
    }
}