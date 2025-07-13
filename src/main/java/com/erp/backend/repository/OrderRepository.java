package com.erp.backend.repository;

import com.erp.backend.domain.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    List<Order> findAll();
    Optional<Order> findById(String id);
    void deleteById(String id);
}