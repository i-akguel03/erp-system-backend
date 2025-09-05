package com.erp.backend.mapper;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.dto.DueScheduleDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Mapper zwischen DueSchedule Entity und DTO
 */
@Component
public class DueScheduleMapper {

    /**
     * Konvertiert Entity zu DTO
     */
    public DueScheduleDto toDto(DueSchedule entity) {
        if (entity == null) return null;

        DueScheduleDto dto = new DueScheduleDto();

        dto.setId(entity.getId());
        dto.setDueNumber(entity.getDueNumber());
        dto.setAmount(entity.getAmount());
        dto.setPaidAmount(entity.getPaidAmount());
        dto.setDueDate(entity.getDueDate());
        dto.setPeriodStart(entity.getPeriodStart());
        dto.setPeriodEnd(entity.getPeriodEnd());
        dto.setStatus(entity.getStatus());
        dto.setPaidDate(entity.getPaidDate());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setPaymentReference(entity.getPaymentReference());
        dto.setNotes(entity.getNotes());
        dto.setReminderSent(entity.isReminderSent());
        dto.setReminderCount(entity.getReminderCount());
        dto.setLastReminderDate(entity.getLastReminderDate());

        // Subscription-bezogene Daten defensiv abfragen
        if (entity.getSubscription() != null) {
            dto.setSubscriptionId(entity.getSubscription().getId());
            dto.setSubscriptionNumber(entity.getSubscription().getSubscriptionNumber());

            // Produktname über Subscription optional
            dto.setProductName(entity.getSubscription().getProductName() != null ?
                    entity.getSubscription().getProductName() : "Unknown Product");

            // Customer über Contract defensiv abfragen
            try {
                if (entity.getSubscription().getContract() != null &&
                        entity.getSubscription().getContract().getCustomer() != null) {
                    dto.setCustomerName(entity.getSubscription().getContract().getCustomer().getName());
                } else {
                    dto.setCustomerName("Unknown Customer");
                }
            } catch (EntityNotFoundException ex) {
                dto.setCustomerName("Unknown Customer");
            }
        } else {
            dto.setSubscriptionId(null);
            dto.setSubscriptionNumber("Unknown Subscription");
            dto.setCustomerName("Unknown Customer");
            dto.setProductName("Unknown Product");
        }

        return dto;
    }

    /**
     * Konvertiert DTO zu Entity
     */
    public DueSchedule toEntity(DueScheduleDto dto) {
        if (dto == null) {
            return null;
        }

        DueSchedule entity = new DueSchedule();

        entity.setId(dto.getId());
        entity.setDueNumber(dto.getDueNumber());
        entity.setAmount(dto.getAmount());
        entity.setPaidAmount(dto.getPaidAmount());
        entity.setDueDate(dto.getDueDate());
        entity.setPeriodStart(dto.getPeriodStart());
        entity.setPeriodEnd(dto.getPeriodEnd());
        entity.setStatus(dto.getStatus());
        entity.setPaidDate(dto.getPaidDate());
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setPaymentReference(dto.getPaymentReference());
        entity.setNotes(dto.getNotes());
        entity.setReminderSent(dto.isReminderSent());
        entity.setReminderCount(dto.getReminderCount());
        entity.setLastReminderDate(dto.getLastReminderDate());

        // Subscription wird separat gesetzt (im Service)

        return entity;
    }

    /**
     * Aktualisiert eine bestehende Entity mit DTO-Daten
     */
    public void updateEntityFromDto(DueSchedule entity, DueScheduleDto dto) {
        if (entity == null || dto == null) {
            return;
        }

        // ID und DueNumber normalerweise nicht ändern
        entity.setAmount(dto.getAmount());
        entity.setPaidAmount(dto.getPaidAmount());
        entity.setDueDate(dto.getDueDate());
        entity.setPeriodStart(dto.getPeriodStart());
        entity.setPeriodEnd(dto.getPeriodEnd());
        entity.setStatus(dto.getStatus());
        entity.setPaidDate(dto.getPaidDate());
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setPaymentReference(dto.getPaymentReference());
        entity.setNotes(dto.getNotes());
        entity.setReminderSent(dto.isReminderSent());
        entity.setReminderCount(dto.getReminderCount());
        entity.setLastReminderDate(dto.getLastReminderDate());
    }

    /**
     * Erstellt eine Kopie einer Entity
     */
    public DueSchedule copyEntity(DueSchedule source) {
        if (source == null) {
            return null;
        }

        DueSchedule copy = new DueSchedule();
        copy.setDueNumber(source.getDueNumber());
        copy.setAmount(source.getAmount());
        copy.setPaidAmount(source.getPaidAmount());
        copy.setDueDate(source.getDueDate());
        copy.setPeriodStart(source.getPeriodStart());
        copy.setPeriodEnd(source.getPeriodEnd());
        copy.setStatus(source.getStatus());
        copy.setPaidDate(source.getPaidDate());
        copy.setPaymentMethod(source.getPaymentMethod());
        copy.setPaymentReference(source.getPaymentReference());
        copy.setNotes(source.getNotes());
        copy.setReminderSent(source.isReminderSent());
        copy.setReminderCount(source.getReminderCount());
        copy.setLastReminderDate(source.getLastReminderDate());
        copy.setSubscription(source.getSubscription());

        return copy;
    }
}