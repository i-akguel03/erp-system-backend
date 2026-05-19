package com.erp.backend.controller;

import com.erp.backend.domain.Order;
import com.erp.backend.domain.Payment;
import com.erp.backend.service.OrderService;
import com.erp.backend.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin
@Tag(name = "Zahlungen")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    public PaymentController(PaymentService paymentService, OrderService orderService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    @Operation(summary = "Alle Zahlungen abrufen — optional paginiert")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'PAYMENTS_READ')")
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Payment> paymentPage = paymentService.getAllPayments(pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(paymentPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(paymentPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(paymentPage.getContent());
        }
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @Operation(summary = "Zahlung nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'PAYMENTS_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Neue Zahlung erfassen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        if (payment.getOrder() != null && payment.getOrder().getId() != null) {
            Order order = orderService.findById(payment.getOrder().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Order not found"));
            payment.setOrder(order);
        } else {
            return ResponseEntity.badRequest().build();
        }

        Payment created = paymentService.createOrUpdatePayment(payment);
        return ResponseEntity.ok(created);
    }

    @Operation(summary = "Zahlung aktualisieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Payment> updatePayment(@PathVariable Long id, @RequestBody Payment updated) {
        return paymentService.getPaymentById(id)
                .map(existing -> {
                    if (updated.getOrder() != null && updated.getOrder().getId() != null) {
                        Order order = orderService.findById(updated.getOrder().getId())
                                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
                        existing.setOrder(order);
                    }
                    existing.setAmount(updated.getAmount());
                    existing.setMethod(updated.getMethod());
                    existing.setStatus(updated.getStatus());
                    return ResponseEntity.ok(paymentService.createOrUpdatePayment(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Zahlung löschen")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        paymentService.deletePaymentById(id);
        return ResponseEntity.noContent().build();
    }
}
