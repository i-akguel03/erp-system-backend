package com.erp.backend.service;

import com.erp.backend.domain.Customer;
import com.erp.backend.domain.Order;
import com.erp.backend.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order order;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer("Max", "Mustermann", "max@test.de", "+49123");
        order = new Order(customer, List.of(), LocalDateTime.now(), 99.99);
        order.setId(1L);
    }

    @Test
    @DisplayName("findAll gibt alle Bestellungen zurück")
    void findAll_returnsAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<Order> result = orderService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalPrice()).isEqualTo(99.99);
        verify(orderRepository).findAll();
    }

    @Test
    @DisplayName("findAll mit Pageable gibt Page zurück")
    void findAll_pageable_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(pageable)).thenReturn(page);

        Page<Order> result = orderService.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(orderRepository).findAll(pageable);
    }

    @Test
    @DisplayName("findById gibt Bestellung zurück wenn vorhanden")
    void findById_found() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Optional<Order> result = orderService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById gibt leeres Optional zurück wenn nicht vorhanden")
    void findById_notFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Order> result = orderService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save speichert Bestellung und gibt sie zurück")
    void save_persistsOrder() {
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.save(order);

        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("deleteById ruft Repository-Delete auf")
    void deleteById_callsRepository() {
        doNothing().when(orderRepository).deleteById(1L);

        orderService.deleteById(1L);

        verify(orderRepository).deleteById(1L);
    }
}
