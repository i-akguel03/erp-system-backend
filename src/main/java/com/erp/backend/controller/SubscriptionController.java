package com.erp.backend.controller;

import com.erp.backend.domain.Subscription;
import com.erp.backend.domain.SubscriptionStatus;
import com.erp.backend.dto.SubscriptionDto;
import com.erp.backend.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    private SubscriptionDto toDto(Subscription s) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setId(s.getId());
        dto.setSubscriptionNumber(s.getSubscriptionNumber());
        dto.setProductName(s.getProductName());
        dto.setMonthlyPrice(s.getMonthlyPrice());
        dto.setStartDate(s.getStartDate());
        dto.setEndDate(s.getEndDate());
        dto.setBillingCycle(s.getBillingCycle());
        dto.setSubscriptionStatus(SubscriptionStatus.valueOf(s.getSubscriptionStatus().name()));
        //dto.setAutoRenewal(s.isAutoRenewal());
        dto.setContractId(s.getContract() != null ? s.getContract().getId() : null);
        return dto;
    }

    @PostMapping("/init")
    public ResponseEntity<String> initTestSubscriptions() {
        service.initTestSubscriptions();
        return ResponseEntity.ok("30 Test-Subscriptions wurden zufällig auf Contracts verteilt.");
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionDto>> getAllSubscriptions(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logger.info("GET /api/subscriptions - Fetching subscriptions (paginated: {})", paginated);

        List<SubscriptionDto> dtos;

        if (paginated) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
            Page<Subscription> subscriptionPage = service.getAllSubscriptions(pageable);
            dtos = subscriptionPage.stream().map(this::toDto).collect(Collectors.toList());

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(subscriptionPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(subscriptionPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(dtos);
        } else {
            dtos = service.getAllSubscriptions().stream().map(this::toDto).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionDto> getSubscriptionById(@PathVariable UUID id) {
        logger.info("GET /api/subscriptions/{} - Fetching subscription by ID", id);
        return service.getSubscriptionById(id)
                .map(subscription -> ResponseEntity.ok(toDto(subscription)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-number/{subscriptionNumber}")
    public ResponseEntity<SubscriptionDto> getSubscriptionByNumber(@PathVariable String subscriptionNumber) {
        logger.info("GET /api/subscriptions/by-number/{} - Fetching subscription by number", subscriptionNumber);
        return service.getSubscriptionByNumber(subscriptionNumber)
                .map(subscription -> ResponseEntity.ok(toDto(subscription)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionsByContract(
            @PathVariable UUID contractId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        logger.info("GET /api/subscriptions/contract/{} - Fetching subscriptions by contract (activeOnly: {})",
                contractId, activeOnly);

        try {
            List<Subscription> subscriptions = activeOnly ?
                    service.getActiveSubscriptionsByContract(contractId) :
                    service.getSubscriptionsByContract(contractId);
            List<SubscriptionDto> dtos = subscriptions.stream().map(this::toDto).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionsByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        try {
            List<Subscription> subscriptions = activeOnly ?
                    service.getActiveSubscriptionsByCustomer(customerId) :
                    service.getSubscriptionsByCustomer(customerId);
            List<SubscriptionDto> dtos = subscriptions.stream().map(this::toDto).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionsByStatus(@PathVariable SubscriptionStatus status) {
        List<SubscriptionDto> dtos = service.getSubscriptionsByStatus(status)
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionsExpiringInDays(
            @RequestParam(defaultValue = "30") int days) {
        List<SubscriptionDto> dtos = service.getSubscriptionsExpiringInDays(days)
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/auto-renewal")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionsForAutoRenewal(
            @RequestParam(defaultValue = "7") int days) {
        List<SubscriptionDto> dtos = service.getSubscriptionsForAutoRenewal(days)
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/search")
    public ResponseEntity<List<SubscriptionDto>> searchSubscriptions(@RequestParam String q) {
        List<SubscriptionDto> dtos = service.searchSubscriptionsByProduct(q)
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/revenue/total")
    public ResponseEntity<BigDecimal> getTotalActiveRevenue() {
        return ResponseEntity.ok(service.getTotalActiveRevenue());
    }

    @GetMapping("/revenue/customer/{customerId}")
    public ResponseEntity<BigDecimal> getActiveRevenueByCustomer(@PathVariable UUID customerId) {
        try {
            return ResponseEntity.ok(service.getActiveRevenueByCustomer(customerId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/analytics/top-products")
    public ResponseEntity<List<Object[]>> getTopProductsByActiveSubscriptions() {
        return ResponseEntity.ok(service.getTopProductsByActiveSubscriptions());
    }

    @GetMapping("/analytics/top-subscriptions")
    public ResponseEntity<List<SubscriptionDto>> getTopSubscriptionsByPrice(
            @RequestParam(defaultValue = "10") int limit) {
        List<SubscriptionDto> dtos = service.getTopSubscriptionsByPrice(limit)
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getSubscriptionCount() {
        return ResponseEntity.ok(service.getTotalSubscriptionCount());
    }

    @GetMapping("/count/by-status/{status}")
    public ResponseEntity<Long> getSubscriptionCountByStatus(@PathVariable SubscriptionStatus status) {
        return ResponseEntity.ok(service.getSubscriptionCountByStatus(status));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("totalCount", service.getTotalSubscriptionCount());
        dashboardData.put("activeCount", service.getSubscriptionCountByStatus(SubscriptionStatus.ACTIVE));
        dashboardData.put("pausedCount", service.getSubscriptionCountByStatus(SubscriptionStatus.PAUSED));
        dashboardData.put("cancelledCount", service.getSubscriptionCountByStatus(SubscriptionStatus.CANCELLED));
        dashboardData.put("totalRevenue", service.getTotalActiveRevenue());
        dashboardData.put("expiringIn30Days", service.getSubscriptionsExpiringInDays(30).size());
        dashboardData.put("topProducts", service.getTopProductsByActiveSubscriptions());
        return ResponseEntity.ok(dashboardData);
    }

    @PostMapping
    public ResponseEntity<SubscriptionDto> createSubscription(@RequestBody Subscription subscription) {
        try {
            Subscription created = service.createSubscription(subscription);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionDto> updateSubscription(@PathVariable UUID id, @RequestBody Subscription updated) {
        updated.setId(id);
        try {
            Subscription saved = service.updateSubscription(updated);
            return ResponseEntity.ok(toDto(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<SubscriptionDto> activateSubscription(@PathVariable UUID id) {
        try {
            Subscription activated = service.activateSubscription(id);
            return ResponseEntity.ok(toDto(activated));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<SubscriptionDto> cancelSubscription(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cancellationDate) {
        try {
            Subscription cancelled = service.cancelSubscription(id, cancellationDate);
            return ResponseEntity.ok(toDto(cancelled));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<SubscriptionDto> pauseSubscription(@PathVariable UUID id) {
        try {
            Subscription paused = service.pauseSubscription(id);
            return ResponseEntity.ok(toDto(paused));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/renew")
    public ResponseEntity<SubscriptionDto> renewSubscription(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newEndDate) {
        try {
            Subscription renewed = service.renewSubscription(id, newEndDate);
            return ResponseEntity.ok(toDto(renewed));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/process/auto-renewals")
    public ResponseEntity<Map<String, Object>> processAutoRenewals() {
        List<Subscription> processed = service.processAutoRenewals();
        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", processed.size());
        result.put("processedSubscriptions", processed.stream().map(this::toDto).collect(Collectors.toList()));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/process/expired")
    public ResponseEntity<Map<String, Object>> processExpiredSubscriptions() {
        List<Subscription> processed = service.processExpiredSubscriptions();
        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", processed.size());
        result.put("processedSubscriptions", processed.stream().map(this::toDto).collect(Collectors.toList()));
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable UUID id) {
        try {
            service.deleteSubscription(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
