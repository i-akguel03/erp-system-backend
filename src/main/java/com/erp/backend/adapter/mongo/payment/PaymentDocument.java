package com.erp.backend.adapter.mongo.payment;

import com.erp.backend.domain.PaymentStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payments")
public class PaymentDocument {

    @Id
    private String id;
    private String orderId;
    private double amount;
    private String method;
    private PaymentStatus status;

    public PaymentDocument() {}

    public PaymentDocument(String id, String orderId, double amount, String method, PaymentStatus status) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.status = status;
    }

    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public double getAmount() { return amount; }
    public String getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }

    public void setId(String id) { this.id = id; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setMethod(String method) { this.method = method; }
    public void setStatus(PaymentStatus status) { this.status = status; }
}
