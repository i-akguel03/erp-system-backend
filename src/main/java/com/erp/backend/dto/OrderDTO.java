package com.erp.backend.dto;

import com.erp.backend.domain.Order;
import com.erp.backend.domain.OrderSource;
import com.erp.backend.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrderDTO {

    private Long id;
    private String orderNumber;
    private UUID customerId;
    private String customerName;
    private OrderStatus status;
    private OrderSource orderSource;
    private String externalOrderId;
    private UUID angebotId;
    private UUID invoiceId;
    private List<OrderItemDTO> items;
    private LocalDateTime orderDate;
    private BigDecimal nettobetrag;
    private BigDecimal steuerbetrag;
    private BigDecimal bruttobetrag;
    private LocalDate lieferDatum;
    private String notizen;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderDTO() {}

    public static OrderDTO fromEntity(Order o) {
        OrderDTO dto = new OrderDTO();
        dto.setId(o.getId());
        dto.setOrderNumber(o.getOrderNumber());
        dto.setStatus(o.getStatus());
        dto.setOrderSource(o.getOrderSource());
        dto.setExternalOrderId(o.getExternalOrderId());
        dto.setAngebotId(o.getAngebotId());
        dto.setInvoiceId(o.getInvoiceId());
        dto.setOrderDate(o.getOrderDate());
        dto.setNettobetrag(o.getNettobetrag());
        dto.setSteuerbetrag(o.getSteuerbetrag());
        dto.setBruttobetrag(o.getBruttobetrag());
        dto.setLieferDatum(o.getLieferDatum());
        dto.setNotizen(o.getNotizen());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setUpdatedAt(o.getUpdatedAt());

        if (o.getCustomer() != null) {
            dto.setCustomerId(o.getCustomer().getId());
            dto.setCustomerName(o.getCustomer().getName());
        }
        if (o.getItems() != null) {
            dto.setItems(o.getItems().stream().map(item -> {
                OrderItemDTO idto = new OrderItemDTO();
                idto.setId(item.getId());
                idto.setQuantity(item.getQuantity());
                idto.setUnitPrice(item.getUnitPrice());
                if (item.getProduct() != null) {
                    idto.setProductId(item.getProduct().getId());
                    idto.setProductName(item.getProduct().getName());
                }
                return idto;
            }).collect(Collectors.toList()));
        }
        return dto;
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public OrderSource getOrderSource() { return orderSource; }
    public void setOrderSource(OrderSource orderSource) { this.orderSource = orderSource; }

    public String getExternalOrderId() { return externalOrderId; }
    public void setExternalOrderId(String externalOrderId) { this.externalOrderId = externalOrderId; }

    public UUID getAngebotId() { return angebotId; }
    public void setAngebotId(UUID angebotId) { this.angebotId = angebotId; }

    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }

    public List<OrderItemDTO> getItems() { return items; }
    public void setItems(List<OrderItemDTO> items) { this.items = items; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public BigDecimal getNettobetrag() { return nettobetrag; }
    public void setNettobetrag(BigDecimal nettobetrag) { this.nettobetrag = nettobetrag; }

    public BigDecimal getSteuerbetrag() { return steuerbetrag; }
    public void setSteuerbetrag(BigDecimal steuerbetrag) { this.steuerbetrag = steuerbetrag; }

    public BigDecimal getBruttobetrag() { return bruttobetrag; }
    public void setBruttobetrag(BigDecimal bruttobetrag) { this.bruttobetrag = bruttobetrag; }

    public LocalDate getLieferDatum() { return lieferDatum; }
    public void setLieferDatum(LocalDate lieferDatum) { this.lieferDatum = lieferDatum; }

    public String getNotizen() { return notizen; }
    public void setNotizen(String notizen) { this.notizen = notizen; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}