package com.erp.backend.controller;

import com.erp.backend.domain.Subscription;
import com.erp.backend.domain.SubscriptionStatus;
import com.erp.backend.domain.BillingCycle;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Subscription>> getAllSubscriptions(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logger.info("GET /api/subscriptions - Fetching subscriptions (paginated: {})", paginated);

        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Subscription> subscriptionPage = service.getAllSubscriptions(pageable);

            logger.debug("Found {} subscriptions on page {}/{}",
                    subscriptionPage.getNumberOfElements(), page + 1, subscriptionPage.getTotalPages());

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(subscriptionPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(subscriptionPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(subscriptionPage.getContent());
        } else {
            List<Subscription> subscriptions = service.getAllSubscriptions();
            logger.debug("Found {} subscriptions", subscriptions.size());
            return ResponseEntity.ok(subscriptions);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subscription> getSubscriptionById(@PathVariable UUID id) {
        logger.info("GET /api/subscriptions/{} - Fetching subscription by ID", id);
        return service.getSubscriptionById(id)
                .map(subscription -> {
                    logger.debug("Subscription found: {}", subscription.getSubscriptionNumber());
                    return ResponseEntity.ok(subscription);
                })
                .orElseGet(() -> {
                    logger.warn("Subscription with ID {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/by-number/{subscriptionNumber}")
    public ResponseEntity<Subscription> getSubscriptionByNumber(@PathVariable String subscriptionNumber) {
        logger.info("GET /api/subscriptions/by-number/{} - Fetching subscription by number", subscriptionNumber);
        return service.getSubscriptionByNumber(subscriptionNumber)
                .map(subscription -> {
                    logger.debug("Subscription found with number: {}", subscriptionNumber);
                    return ResponseEntity.ok(subscription);
                })
                .orElseGet(() -> {
                    logger.warn("Subscription with number {} not found", subscriptionNumber);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<List<Subscription>> getSubscriptionsByContract(
            @PathVariable UUID contractId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        logger.info("GET /api/subscriptions/contract/{} - Fetching subscriptions by contract (activeOnly: {})",
                contractId, activeOnly);

        try {
            List<Subscription> subscriptions = activeOnly ?
                    service.getActiveSubscriptionsByContract(contractId) :
                    service.getSubscriptionsByContract(contractId);

            logger.debug("Found {} subscriptions for contract {}", subscriptions.size(), contractId);
            return ResponseEntity.ok(subscriptions);
        } catch (IllegalArgumentException e) {
            logger.error("Contract not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Subscription>> getSubscriptionsByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        logger.info("GET /api/subscriptions/customer/{} - Fetching subscriptions by customer (activeOnly: {})",
                customerId, activeOnly);

        try {
            List<Subscription> subscriptions = activeOnly ?
                    service.getActiveSubscriptionsByCustomer(customerId) :
                    service.getSubscriptionsByCustomer(customerId);

            logger.debug("Found {} subscriptions for customer {}", subscriptions.size(), customerId);
            return ResponseEntity.ok(subscriptions);
        } catch (IllegalArgumentException e) {
            logger.error("Customer not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Subscription>> getSubscriptionsByStatus(@PathVariable SubscriptionStatus status) {
        logger.info("GET /api/subscriptions/status/{} - Fetching subscriptions by status", status);
        List<Subscription> subscriptions = service.getSubscriptionsByStatus(status);
        logger.debug("Found {} subscriptions with status {}", subscriptions.size(), status);
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<Subscription>> getSubscriptionsExpiringInDays(
            @RequestParam(defaultValue = "30") int days) {
        logger.info("GET /api/subscriptions/expiring?days={} - Fetching subscriptions expiring in {} days", days, days);
        List<Subscription> subscriptions = service.getSubscriptionsExpiringInDays(days);
        logger.debug("Found {} subscriptions expiring in next {} days", subscriptions.size(), days);
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/auto-renewal")
    public ResponseEntity<List<Subscription>> getSubscriptionsForAutoRenewal(
            @RequestParam(defaultValue = "7") int days) {
        logger.info("GET /api/subscriptions/auto-renewal?days={} - Fetching subscriptions for auto-renewal", days);
        List<Subscription> subscriptions = service.getSubscriptionsForAutoRenewal(days);
        logger.debug("Found {} subscriptions for auto-renewal in next {} days", subscriptions.size(), days);
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Subscription>> searchSubscriptions(@RequestParam String q) {
        logger.info("GET /api/subscriptions/search?q={} - Searching subscriptions by product", q);
        List<Subscription> subscriptions = service.searchSubscriptionsByProduct(q);
        logger.debug("Found {} subscriptions matching search term: '{}'", subscriptions.size(), q);
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/revenue/total")
    public ResponseEntity<BigDecimal> getTotalActiveRevenue() {
        logger.info("GET /api/subscriptions/revenue/total - Fetching total active revenue");
        BigDecimal revenue = service.getTotalActiveRevenue();
        logger.debug("Total active revenue: {}", revenue);
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/revenue/customer/{customerId}")
    public ResponseEntity<BigDecimal> getActiveRevenueByCustomer(@PathVariable UUID customerId) {
        logger.info("GET /api/subscriptions/revenue/customer/{} - Fetching active revenue by customer", customerId);
        try {
            BigDecimal revenue = service.getActiveRevenueByCustomer(customerId);
            logger.debug("Active revenue for customer {}: {}", customerId, revenue);
            return ResponseEntity.ok(revenue);
        } catch (IllegalArgumentException e) {
            logger.error("Customer not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/analytics/top-products")
    public ResponseEntity<List<Object[]>> getTopProductsByActiveSubscriptions() {
        logger.info("GET /api/subscriptions/analytics/top-products - Fetching top products");
        List<Object[]> topProducts = service.getTopProductsByActiveSubscriptions();
        logger.debug("Retrieved top products analytics");
        return ResponseEntity.ok(topProducts);
    }

    @GetMapping("/analytics/top-subscriptions")
    public ResponseEntity<List<Subscription>> getTopSubscriptionsByPrice(
            @RequestParam(defaultValue = "10") int limit) {
        logger.info("GET /api/subscriptions/analytics/top-subscriptions?limit={} - Fetching top subscriptions", limit);
        List<Subscription> topSubscriptions = service.getTopSubscriptionsByPrice(limit);
        logger.debug("Retrieved top {} subscriptions by price", limit);
        return ResponseEntity.ok(topSubscriptions);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getSubscriptionCount() {
        logger.info("GET /api/subscriptions/count - Fetching subscription count");
        long count = service.getTotalSubscriptionCount();
        logger.debug("Total subscription count: {}", count);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/by-status/{status}")
    public ResponseEntity<Long> getSubscriptionCountByStatus(@PathVariable SubscriptionStatus status) {
        logger.info("GET /api/subscriptions/count/by-status/{} - Fetching subscription count by status", status);
        Long count = service.getSubscriptionCountByStatus(status);
        logger.debug("Subscription count for status {}: {}", status, count);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        logger.info("GET /api/subscriptions/dashboard - Fetching dashboard data");

        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("totalCount", service.getTotalSubscriptionCount());
        dashboardData.put("activeCount", service.getSubscriptionCountByStatus(SubscriptionStatus.ACTIVE));
        dashboardData.put("pausedCount", service.getSubscriptionCountByStatus(SubscriptionStatus.PAUSED));
        dashboardData.put("cancelledCount", service.getSubscriptionCountByStatus(SubscriptionStatus.CANCELLED));
        dashboardData.put("totalRevenue", service.getTotalActiveRevenue());
        dashboardData.put("expiringIn30Days", service.getSubscriptionsExpiringInDays(30).size());
        dashboardData.put("topProducts", service.getTopProductsByActiveSubscriptions());

        logger.debug("Retrieved dashboard data");
        return ResponseEntity.ok(dashboardData);
    }

    @PostMapping
    public ResponseEntity<Subscription> createSubscription(@RequestBody Subscription subscription) {
        logger.info("POST /api/subscriptions - Creating new subscription for contract {}",
                subscription.getContract() != null ? subscription.getContract().getId() : "null");
        logger.debug("RequestBody {}", subscription.toString());

        try {
            Subscription created = service.createSubscription(subscription);
            logger.info("Created subscription with ID {} and number {}", created.getId(), created.getSubscriptionNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error creating subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating subscription: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Subscription> updateSubscription(@PathVariable UUID id, @RequestBody Subscription updated) {
        logger.info("PUT /api/subscriptions/{} - Updating subscription", id);

        try {
            updated.setId(id); // Ensure ID is set
            Subscription saved = service.updateSubscription(updated);
            logger.info("Updated subscription with ID {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            logger.error("Subscription not found or validation error: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating subscription: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Subscription> activateSubscription(@PathVariable UUID id) {
        logger.info("PATCH /api/subscriptions/{}/activate - Activating subscription", id);
        try {
            Subscription activated = service.activateSubscription(id);
            logger.info("Activated subscription with ID {}", activated.getId());
            return ResponseEntity.ok(activated);
        } catch (IllegalArgumentException e) {
            logger.error("Subscription not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error activating subscription: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Subscription> cancelSubscription(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cancellationDate) {

        logger.info("PATCH /api/subscriptions/{}/cancel - Cancelling subscription (date: {})", id, cancellationDate);
        try {
            Subscription cancelled = service.cancelSubscription(id, cancellationDate);
            logger.info("Cancelled subscription with ID {} on {}", cancelled.getId(), cancelled.getEndDate());
            return ResponseEntity.ok(cancelled);
        } catch (IllegalArgumentException e) {
            logger.error("Subscription not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error cancelling subscription: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<Subscription> pauseSubscription(@PathVariable UUID id) {
        logger.info("PATCH /api/subscriptions/{}/pause - Pausing subscription", id);
        try {
            Subscription paused = service.pauseSubscription(id);
            logger.info("Paused subscription with ID {}", paused.getId());
            return ResponseEntity.ok(paused);
        } catch (IllegalArgumentException e) {
            logger.error("Subscription not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error pausing subscription: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/renew")
    public ResponseEntity<Subscription> renewSubscription(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newEndDate) {

        logger.info("PATCH /api/subscriptions/{}/renew - Renewing subscription (newEndDate: {})", id, newEndDate);
        try {
            Subscription renewed = service.renewSubscription(id, newEndDate);
            logger.info("Renewed subscription with ID {} until {}", renewed.getId(), renewed.getEndDate());
            return ResponseEntity.ok(renewed);
        } catch (IllegalArgumentException e) {
            logger.error("Subscription not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error renewing subscription: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/process/auto-renewals")
    public ResponseEntity<Map<String, Object>> processAutoRenewals() {
        logger.info("POST /api/subscriptions/process/auto-renewals - Processing auto-renewals");
        try {
            List<Subscription> processed = service.processAutoRenewals();

            Map<String, Object> result = new HashMap<>();
            result.put("processedCount", processed.size());
            result.put("processedSubscriptions", processed);

            logger.info("Processed {} auto-renewals", processed.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error processing auto-renewals: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/process/expired")
    public ResponseEntity<Map<String, Object>> processExpiredSubscriptions() {
        logger.info("POST /api/subscriptions/process/expired - Processing expired subscriptions");
        try {
            List<Subscription> processed = service.processExpiredSubscriptions();

            Map<String, Object> result = new HashMap<>();
            result.put("processedCount", processed.size());
            result.put("processedSubscriptions", processed);

            logger.info("Processed {} expired subscriptions", processed.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error processing expired subscriptions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable UUID id) {
        logger.info("DELETE /api/subscriptions/{} - Deleting subscription", id);
        try {
            service.deleteSubscription(id);
            logger.info("Deleted subscription with ID {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("Subscription not found for deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting subscription: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}