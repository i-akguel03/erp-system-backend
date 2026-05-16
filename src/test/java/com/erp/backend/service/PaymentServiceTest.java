package com.erp.backend.service;

import com.erp.backend.domain.Order;
import com.erp.backend.domain.Payment;
import com.erp.backend.domain.PaymentStatus;
import com.erp.backend.repository.PaymentRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: PaymentService")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        Order order = new Order();
        order.setId(1L);
        payment = new Payment(order, 150.0, "CREDIT_CARD", PaymentStatus.PAID);
        payment.setId(1L);
    }

    @Test
    @DisplayName("getAllPayments gibt alle Zahlungen zurück")
    void getAllPayments_returnsAll() {
        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        List<Payment> result = paymentService.getAllPayments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualTo(150.0);
        verify(paymentRepository).findAll();
    }

    @Test
    @DisplayName("getAllPayments mit Pageable gibt Page zurück")
    void getAllPayments_pageable_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(payment));
        when(paymentRepository.findAll(pageable)).thenReturn(page);

        Page<Payment> result = paymentService.getAllPayments(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(paymentRepository).findAll(pageable);
    }

    @Test
    @DisplayName("getPaymentById gibt Zahlung zurück wenn vorhanden")
    void getPaymentById_found() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        Optional<Payment> result = paymentService.getPaymentById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getMethod()).isEqualTo("CREDIT_CARD");
    }

    @Test
    @DisplayName("getPaymentById gibt leeres Optional zurück wenn nicht vorhanden")
    void getPaymentById_notFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Payment> result = paymentService.getPaymentById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("createOrUpdatePayment speichert und gibt Zahlung zurück")
    void createOrUpdatePayment_persists() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Payment result = paymentService.createOrUpdatePayment(payment);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("deletePaymentById ruft Repository-Delete auf")
    void deletePaymentById_callsRepository() {
        doNothing().when(paymentRepository).deleteById(1L);

        paymentService.deletePaymentById(1L);

        verify(paymentRepository).deleteById(1L);
    }
}
