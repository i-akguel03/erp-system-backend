package com.erp.backend.domain;

import java.util.List;

public class Order {
    private String id;
    private String customerId;
    private List<OrderItem> items;
    private String orderDate;
    private double totalPrice;

    public Order() {}

    public Order(String id, String customerId, List<OrderItem> items, String orderDate, double totalPrice) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.orderDate = orderDate;
        this.totalPrice = totalPrice;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}
