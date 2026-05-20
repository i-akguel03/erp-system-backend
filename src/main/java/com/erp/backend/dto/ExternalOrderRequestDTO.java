package com.erp.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ExternalOrderRequestDTO {

    @NotBlank(message = "Externe Auftrags-ID darf nicht leer sein")
    private String externalOrderId;

    private String source;

    @Email(message = "Ungültige E-Mail-Adresse")
    @NotBlank(message = "E-Mail darf nicht leer sein")
    private String customerEmail;

    private String customerFirstName;
    private String customerLastName;
    private String customerTel;

    @NotEmpty(message = "Mindestens eine Position ist erforderlich")
    private List<ExternalOrderItemDTO> items;

    private AddressDTO shippingAddress;

    private String notizen;

    public ExternalOrderRequestDTO() {}

    // Getter & Setter
    public String getExternalOrderId() { return externalOrderId; }
    public void setExternalOrderId(String externalOrderId) { this.externalOrderId = externalOrderId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerFirstName() { return customerFirstName; }
    public void setCustomerFirstName(String customerFirstName) { this.customerFirstName = customerFirstName; }

    public String getCustomerLastName() { return customerLastName; }
    public void setCustomerLastName(String customerLastName) { this.customerLastName = customerLastName; }

    public String getCustomerTel() { return customerTel; }
    public void setCustomerTel(String customerTel) { this.customerTel = customerTel; }

    public List<ExternalOrderItemDTO> getItems() { return items; }
    public void setItems(List<ExternalOrderItemDTO> items) { this.items = items; }

    public AddressDTO getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(AddressDTO shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getNotizen() { return notizen; }
    public void setNotizen(String notizen) { this.notizen = notizen; }

    public static class ExternalOrderItemDTO {

        private UUID productId;
        private int quantity;
        private BigDecimal unitPrice;

        public ExternalOrderItemDTO() {}

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }
}