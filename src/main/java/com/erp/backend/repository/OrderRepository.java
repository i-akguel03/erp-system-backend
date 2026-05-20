package com.erp.backend.repository;

import com.erp.backend.domain.Order;
import com.erp.backend.domain.OrderSource;
import com.erp.backend.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerId(UUID customerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByOrderSource(OrderSource orderSource);

    Optional<Order> findByExternalOrderId(String externalOrderId);

    boolean existsByOrderNumber(String orderNumber);

    boolean existsByExternalOrderId(String externalOrderId);
}