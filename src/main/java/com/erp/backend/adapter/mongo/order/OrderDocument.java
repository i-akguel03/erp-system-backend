package com.erp.backend.adapter.mongo.order;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "orders")
public class OrderDocument {
    @Id
    private String id;
    private String customerId;
    private List<OrderItemDocument> items;
    private String orderDate;
    private double totalPrice;

    public OrderDocument() {}

    public OrderDocument(String id, String customerId, List<OrderItemDocument> items, String orderDate, double totalPrice) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.orderDate = orderDate;
        this.totalPrice = totalPrice;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public List<OrderItemDocument> getItems() { return items; }
    public String getOrderDate() { return orderDate; }
    public double getTotalPrice() { return totalPrice; }

    public void setId(String id) { this.id = id; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setItems(List<OrderItemDocument> items) { this.items = items; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}
