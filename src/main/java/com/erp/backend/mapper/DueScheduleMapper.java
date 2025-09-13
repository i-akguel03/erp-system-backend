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
        dto.setDueDate(entity.getDueDate());
        dto.setPeriodStart(entity.getPeriodStart());
        dto.setPeriodEnd(entity.getPeriodEnd());
        dto.setStatus(entity.getStatus());
        dto.setNotes(entity.getNotes());

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
        entity.setDueDate(dto.getDueDate());
        entity.setPeriodStart(dto.getPeriodStart());
        entity.setPeriodEnd(dto.getPeriodEnd());
        entity.setStatus(dto.getStatus());
        entity.setNotes(dto.getNotes());

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
        entity.setDueDate(dto.getDueDate());
        entity.setPeriodStart(dto.getPeriodStart());
        entity.setPeriodEnd(dto.getPeriodEnd());
        entity.setStatus(dto.getStatus());
        entity.setNotes(dto.getNotes());
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
        copy.setDueDate(source.getDueDate());
        copy.setPeriodStart(source.getPeriodStart());
        copy.setPeriodEnd(source.getPeriodEnd());
        copy.setStatus(source.getStatus());
        copy.setNotes(source.getNotes());
        copy.setSubscription(source.getSubscription());

        return copy;
    }
}