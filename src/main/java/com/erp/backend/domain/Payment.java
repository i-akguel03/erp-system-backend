package com.erp.backend.domain;

public class Payment {

    private String id;
    private String orderId;
    private double amount;
    private String method; // z. B. "CREDIT_CARD", "PAYPAL"
    private PaymentStatus status;

    public Payment() {}

    public Payment(String id, String orderId, double amount, String method, PaymentStatus status) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
}
