package com.erp.backend.controller;

import com.erp.backend.domain.Payment;
import com.erp.backend.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Payment> getAllPayments() {
        return service.getAllPayments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String id) {
        return service.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        Payment created = service.createOrUpdatePayment(payment);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Payment> updatePayment(@PathVariable String id, @RequestBody Payment updated) {
        return service.getPaymentById(id)
                .map(existing -> {
                    existing.setOrderId(updated.getOrderId());
                    existing.setAmount(updated.getAmount());
                    existing.setMethod(updated.getMethod());
                    existing.setStatus(updated.getStatus());
                    return ResponseEntity.ok(service.createOrUpdatePayment(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable String id) {
        service.deletePaymentById(id);
        return ResponseEntity.noContent().build();
    }
}
