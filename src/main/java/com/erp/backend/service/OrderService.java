package com.erp.backend.service;

import com.erp.backend.domain.Order;
import com.erp.backend.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public List<Order> findAll() {
        List<Order> orders = repository.findAll();
        logger.info("Fetched {} orders", orders.size());
        return orders;
    }

    public Optional<Order> findById(Long id) {
        Optional<Order> order = repository.findById(id);
        if (order.isPresent()) {
            logger.info("Found order with id={}", id);
        } else {
            logger.warn("No order found with id={}", id);
        }
        return order;
    }

    public Order save(Order order) {
        Order saved = repository.save(order);
        logger.info("Saved order with id={} and totalPrice={}", saved.getId(), saved.getTotalPrice());
        return saved;
    }


    public void deleteById(Long id) {
        repository.deleteById(id);
        logger.info("Deleted order with id={}", id);
    }
}
