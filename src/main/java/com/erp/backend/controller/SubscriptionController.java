package com.erp.backend.controller;

import com.erp.backend.domain.Subscription;
import com.erp.backend.domain.SubscriptionStatus;
import com.erp.backend.dto.SubscriptionDto;
import com.erp.backend.exception.InvalidStatusTransitionException;
import com.erp.backend.service.SubscriptionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    // ================= GET =================
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUBSCRIPTIONS_READ')")
    @GetMapping
    public ResponseEntity<List<SubscriptionDto>> getAllSubscriptions(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        if (paginated) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
            Page<Subscription> subscriptionPage = service.getAllSubscriptions(pageable);
            List<SubscriptionDto> dtos = subscriptionPage.stream().map(service::toDto).collect(Collectors.toList());

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(subscriptionPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(subscriptionPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(dtos);
        } else {
            List<SubscriptionDto> dtos = service.getAllSubscriptions().stream().map(service::toDto).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUBSCRIPTIONS_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionDto> getSubscriptionById(@PathVariable UUID id) {
        return service.getSubscriptionById(id)
                .map(service::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUBSCRIPTIONS_READ')")
    @GetMapping("/by-number/{subscriptionNumber}")
    public ResponseEntity<SubscriptionDto> getSubscriptionByNumber(@PathVariable String subscriptionNumber) {
        return service.getSubscriptionByNumber(subscriptionNumber)
                .map(service::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUBSCRIPTIONS_READ')")
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionsByContract(
            @PathVariable UUID contractId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        try {
            List<Subscription> subscriptions = activeOnly ?
                    service.getActiveSubscriptionsByContract(contractId) :
                    service.getSubscriptionsByContract(contractId);
            return ResponseEntity.ok(subscriptions.stream().map(service::toDto).collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUBSCRIPTIONS_READ')")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionsByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        try {
            List<Subscription> subscriptions = activeOnly ?
                    service.getActiveSubscriptionsByCustomer(customerId) :
                    service.getSubscriptionsByCustomer(customerId);
            return ResponseEntity.ok(subscriptions.stream().map(service::toDto).collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUBSCRIPTIONS_READ')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionsByStatus(@PathVariable SubscriptionStatus status) {
        List<SubscriptionDto> dtos = service.getSubscriptionsByStatus(status)
                .stream().map(service::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUBSCRIPTIONS_READ')")
    @GetMapping("/revenue/total")
    public ResponseEntity<BigDecimal> getTotalActiveRevenue() {
        return ResponseEntity.ok(service.getTotalActiveRevenue());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUBSCRIPTIONS_READ')")
    @GetMapping("/revenue/customer/{customerId}")
    public ResponseEntity<BigDecimal> getActiveRevenueByCustomer(@PathVariable UUID customerId) {
        try {
            return ResponseEntity.ok(service.getActiveRevenueByCustomer(customerId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ================= POST / PUT / PATCH =================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createSubscription(@Valid @RequestBody SubscriptionDto dto) {
        try {
            Subscription created = service.createSubscriptionFromDto(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(service.toDto(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal server error"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubscription(@PathVariable UUID id, @Valid @RequestBody SubscriptionDto dto) {
        try {
            dto.setId(id);
            Subscription updated = service.updateSubscriptionFromDto(dto);
            return ResponseEntity.ok(service.toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal server error"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<?> activateSubscription(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.toDto(service.activateSubscription(id)));
        } catch (InvalidStatusTransitionException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<?> suspendSubscription(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.toDto(service.suspendSubscription(id)));
        } catch (InvalidStatusTransitionException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/terminate")
    public ResponseEntity<?> terminateSubscription(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDate) {
        try {
            return ResponseEntity.ok(service.toDto(service.terminateSubscription(id, terminationDate)));
        } catch (InvalidStatusTransitionException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reinstate")
    public ResponseEntity<?> reinstateSubscription(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.toDto(service.reinstateSubscription(id)));
        } catch (InvalidStatusTransitionException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelSubscription(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cancellationDate) {
        try {
            return ResponseEntity.ok(service.toDto(service.cancelSubscription(id, cancellationDate)));
        } catch (InvalidStatusTransitionException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/renew")
    public ResponseEntity<?> renewSubscription(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newEndDate) {
        try {
            return ResponseEntity.ok(service.toDto(service.renewSubscription(id, newEndDate)));
        } catch (InvalidStatusTransitionException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", e.getMessage()));
        }
    }

    // ================= DELETE =================
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable UUID id) {
            service.deleteSubscription(id);
            return ResponseEntity.noContent().build();
    }

}
