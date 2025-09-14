package com.erp.backend.mapper;

import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.InvoiceItem;
import com.erp.backend.dto.InvoiceDTO;
import com.erp.backend.dto.InvoiceItemDTO;

import java.util.List;
import java.util.stream.Collectors;

public class InvoiceMapper {

    public static InvoiceDTO toDTO(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setStatus(invoice.getStatus() != null ? invoice.getStatus().name() : null);
        dto.setInvoiceType(invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : null);
        dto.setSubtotal(invoice.getSubtotal());
        dto.setTaxAmount(invoice.getTaxAmount());
        dto.setTotalAmount(invoice.getTotalAmount());

        // Customer mapping
        if (invoice.getCustomer() != null) {
            dto.setCustomerId(invoice.getCustomer().getId().toString());
            dto.setCustomerName(invoice.getCustomer().getName()); // Assuming Customer has getName()
        }

        // *** CRITICAL: Map invoice items ***
        if (invoice.getInvoiceItems() != null && !invoice.getInvoiceItems().isEmpty()) {
            List<InvoiceItemDTO> itemDTOs = invoice.getInvoiceItems().stream()
                    .map(InvoiceMapper::toInvoiceItemDTO)
                    .collect(Collectors.toList());
            dto.setItems(itemDTOs);
        }

        return dto;
    }

    public static InvoiceItemDTO toInvoiceItemDTO(InvoiceItem item) {
        if (item == null) {
            return null;
        }

        InvoiceItemDTO dto = new InvoiceItemDTO();
        dto.setId(item.getId());
        dto.setDescription(item.getDescription());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setProductCode(item.getProductCode());
        dto.setProductName(item.getProductName());
        // Calculate line total if not stored
        if (item.getQuantity() != null && item.getUnitPrice() != null) {
            dto.setLineTotal(item.getQuantity().multiply(item.getUnitPrice()));
        }

        return dto;
    }

    public static Invoice toEntity(InvoiceDTO dto) {
        if (dto == null) {
            return null;
        }

        Invoice invoice = new Invoice();
        invoice.setId(dto.getId());
        invoice.setInvoiceNumber(dto.getInvoiceNumber());
        invoice.setInvoiceDate(dto.getInvoiceDate());
        invoice.setDueDate(dto.getDueDate());

        // Status mapping
        if (dto.getStatus() != null) {
            invoice.setStatus(Invoice.InvoiceStatus.valueOf(dto.getStatus()));
        }

        // Invoice type mapping
        if (dto.getInvoiceType() != null) {
            invoice.setInvoiceType(Invoice.InvoiceType.valueOf(dto.getInvoiceType()));
        }

        return invoice;
    }
}