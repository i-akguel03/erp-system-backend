package com.erp.backend.mapper;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.dto.DueScheduleDto;
import org.springframework.stereotype.Component;

@Component
public class DueScheduleMapper {

    public DueScheduleDto toDto(DueSchedule dueSchedule) {
        if (dueSchedule == null) {
            return null;
        }

        DueScheduleDto dto = new DueScheduleDto();
        dto.setId(dueSchedule.getId());
        dto.setDueNumber(dueSchedule.getDueNumber());
        dto.setDueDate(dueSchedule.getDueDate());
        dto.setAmount(dueSchedule.getAmount());
        dto.setPeriodStart(dueSchedule.getPeriodStart());
        dto.setPeriodEnd(dueSchedule.getPeriodEnd());
        dto.setStatus(dueSchedule.getStatus());
        dto.setPaidDate(dueSchedule.getPaidDate());
        dto.setPaidAmount(dueSchedule.getPaidAmount());
        dto.setPaymentMethod(dueSchedule.getPaymentMethod());
        dto.setPaymentReference(dueSchedule.getPaymentReference());
        dto.setCreatedDate(dueSchedule.getCreatedDate());
        dto.setUpdatedDate(dueSchedule.getUpdatedDate());
        dto.setNotes(dueSchedule.getNotes());
        dto.setReminderSent(dueSchedule.getReminderSent());
        dto.setReminderCount(dueSchedule.getReminderCount());
        dto.setLastReminderDate(dueSchedule.getLastReminderDate());

        // Subscription-Informationen
        if (dueSchedule.getSubscription() != null) {
            dto.setSubscriptionId(dueSchedule.getSubscription().getId());
            dto.setSubscriptionProductName(dueSchedule.getSubscription().getProductName());

            // Contract- und Customer-Informationen über Subscription
            if (dueSchedule.getSubscription().getContract() != null) {
                dto.setContractNumber(dueSchedule.getSubscription().getContract().getContractNumber());

                if (dueSchedule.getSubscription().getContract().getCustomer() != null) {
                    String customerName = dueSchedule.getSubscription().getContract().getCustomer().getFirstName()
                            + " " + dueSchedule.getSubscription().getContract().getCustomer().getLastName();
                    dto.setCustomerName(customerName);
                }
            }
        }

        return dto;
    }

    public DueSchedule toEntity(DueScheduleDto dto) {
        if (dto == null) {
            return null;
        }

        DueSchedule dueSchedule = new DueSchedule();
        dueSchedule.setId(dto.getId());
        dueSchedule.setDueNumber(dto.getDueNumber());
        dueSchedule.setDueDate(dto.getDueDate());
        dueSchedule.setAmount(dto.getAmount());
        dueSchedule.setPeriodStart(dto.getPeriodStart());
        dueSchedule.setPeriodEnd(dto.getPeriodEnd());
        dueSchedule.setStatus(dto.getStatus() != null ? dto.getStatus() : com.erp.backend.domain.DueStatus.PENDING);
        dueSchedule.setPaidDate(dto.getPaidDate());
        dueSchedule.setPaidAmount(dto.getPaidAmount());
        dueSchedule.setPaymentMethod(dto.getPaymentMethod());
        dueSchedule.setPaymentReference(dto.getPaymentReference());
        dueSchedule.setNotes(dto.getNotes());
        dueSchedule.setReminderSent(dto.getReminderSent() != null ? dto.getReminderSent() : false);
        dueSchedule.setReminderCount(dto.getReminderCount() != null ? dto.getReminderCount() : 0);
        dueSchedule.setLastReminderDate(dto.getLastReminderDate());

        // Subscription wird im Service gesetzt, da wir hier nur die ID haben

        return dueSchedule;
    }
}