package com.erp.backend.service;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.Customer;
import com.erp.backend.domain.Subscription;
import com.erp.backend.domain.SubscriptionStatus;
import com.erp.backend.domain.BillingCycle;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.CustomerRepository;
import com.erp.backend.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               ContractRepository contractRepository,
                               CustomerRepository customerRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.contractRepository = contractRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        logger.info("Fetched {} subscriptions", subscriptions.size());
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public Page<Subscription> getAllSubscriptions(Pageable pageable) {
        Page<Subscription> subscriptions = subscriptionRepository.findAll(pageable);
        logger.info("Fetched {} subscriptions (page {}/{})",
                subscriptions.getNumberOfElements(), subscriptions.getNumber() + 1, subscriptions.getTotalPages());
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> getSubscriptionById(UUID id) {
        Optional<Subscription> subscription = subscriptionRepository.findById(id);
        if (subscription.isPresent()) {
            logger.info("Found subscription with id={}", id);
        } else {
            logger.warn("No subscription found with id={}", id);
        }
        return subscription;
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> getSubscriptionByNumber(String subscriptionNumber) {
        Optional<Subscription> subscription = subscriptionRepository.findBySubscriptionNumber(subscriptionNumber);
        logger.info("Search for subscription with number={}: {}", subscriptionNumber, subscription.isPresent() ? "found" : "not found");
        return subscription;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByContract(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with ID: " + contractId));
        List<Subscription> subscriptions = subscriptionRepository.findByContract(contract);
        logger.info("Found {} subscriptions for contract {}", subscriptions.size(), contractId);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getActiveSubscriptionsByContract(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with ID: " + contractId));
        List<Subscription> subscriptions = subscriptionRepository.findByContractAndSubscriptionStatus(contract, SubscriptionStatus.ACTIVE);
        logger.info("Found {} active subscriptions for contract {}", subscriptions.size(), contractId);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        List<Subscription> subscriptions = subscriptionRepository.findByCustomer(customer);
        logger.info("Found {} subscriptions for customer {}", subscriptions.size(), customerId);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getActiveSubscriptionsByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        List<Subscription> subscriptions = subscriptionRepository.findActiveSubscriptionsByCustomer(customer);
        logger.info("Found {} active subscriptions for customer {}", subscriptions.size(), customerId);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByStatus(SubscriptionStatus status) {
        List<Subscription> subscriptions = subscriptionRepository.findBySubscriptionStatus(status);
        logger.info("Found {} subscriptions with status {}", subscriptions.size(), status);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsExpiringInDays(int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        List<Subscription> subscriptions = subscriptionRepository.findSubscriptionsExpiringBetween(startDate, endDate);
        logger.info("Found {} subscriptions expiring in next {} days", subscriptions.size(), days);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsForAutoRenewal(int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        List<Subscription> subscriptions = subscriptionRepository.findSubscriptionsForAutoRenewal(startDate, endDate);
        logger.info("Found {} subscriptions for auto renewal in next {} days", subscriptions.size(), days);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<Subscription> searchSubscriptionsByProduct(String productName) {
        List<Subscription> subscriptions = subscriptionRepository.findByProductNameContainingIgnoreCase(productName);
        logger.info("Found {} subscriptions matching product search: '{}'", subscriptions.size(), productName);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalActiveRevenue() {
        BigDecimal revenue = subscriptionRepository.calculateTotalActiveRevenue();
        logger.info("Total active subscription revenue: {}", revenue);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getActiveRevenueByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        BigDecimal revenue = subscriptionRepository.calculateActiveRevenueByCustomer(customer);
        logger.info("Active subscription revenue for customer {}: {}", customerId, revenue);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public Subscription createSubscription(Subscription subscription) {
        validateSubscriptionForCreation(subscription);

        // Keine ID setzen, wird von DB generiert
        subscription.setId(null);

        // Abo-Nummer generieren
        subscription.setSubscriptionNumber(generateSubscriptionNumber());

        // Status setzen falls nicht vorhanden
        if (subscription.getSubscriptionStatus() == null) {
            subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        }

        // Billing Cycle setzen falls nicht vorhanden
        if (subscription.getBillingCycle() == null) {
            subscription.setBillingCycle(BillingCycle.MONTHLY);
        }

        // Auto-Renewal Standard-Wert
        if (subscription.getAutoRenewal() == null) {
            subscription.setAutoRenewal(true);
        }

        Subscription saved = subscriptionRepository.save(subscription);
        logger.info("Created new subscription: id={}, subscriptionNumber={}, product={}, contract={}",
                saved.getId(), saved.getSubscriptionNumber(), saved.getProductName(), saved.getContract().getId());
        return saved;
    }

    public Subscription updateSubscription(Subscription subscription) {
        if (subscription.getId() == null) {
            throw new IllegalArgumentException("Subscription ID cannot be null for update");
        }

        if (!subscriptionRepository.existsById(subscription.getId())) {
            throw new IllegalArgumentException("Subscription not found with ID: " + subscription.getId());
        }

        Subscription saved = subscriptionRepository.save(subscription);
        logger.info("Updated subscription: id={}, subscriptionNumber={}", saved.getId(), saved.getSubscriptionNumber());
        return saved;
    }

    public Subscription activateSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with ID: " + subscriptionId));

        subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        if (subscription.getStartDate() == null) {
            subscription.setStartDate(LocalDate.now());
        }

        Subscription saved = subscriptionRepository.save(subscription);
        logger.info("Activated subscription: id={}, subscriptionNumber={}", saved.getId(), saved.getSubscriptionNumber());
        return saved;
    }

    public Subscription cancelSubscription(UUID subscriptionId, LocalDate cancellationDate) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with ID: " + subscriptionId));

        subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
        subscription.setEndDate(cancellationDate != null ? cancellationDate : LocalDate.now());
        subscription.setAutoRenewal(false);

        Subscription saved = subscriptionRepository.save(subscription);
        logger.info("Cancelled subscription: id={}, subscriptionNumber={}, endDate={}",
                saved.getId(), saved.getSubscriptionNumber(), saved.getEndDate());
        return saved;
    }

    public Subscription pauseSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with ID: " + subscriptionId));

        subscription.setSubscriptionStatus(SubscriptionStatus.PAUSED);

        Subscription saved = subscriptionRepository.save(subscription);
        logger.info("Paused subscription: id={}, subscriptionNumber={}", saved.getId(), saved.getSubscriptionNumber());
        return saved;
    }

    public Subscription renewSubscription(UUID subscriptionId, LocalDate newEndDate) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with ID: " + subscriptionId));

        subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        subscription.setEndDate(newEndDate);

        Subscription saved = subscriptionRepository.save(subscription);
        logger.info("Renewed subscription: id={}, subscriptionNumber={}, newEndDate={}",
                saved.getId(), saved.getSubscriptionNumber(), saved.getEndDate());
        return saved;
    }

    public void deleteSubscription(UUID id) {
        if (!subscriptionRepository.existsById(id)) {
            throw new IllegalArgumentException("Subscription not found with ID: " + id);
        }
        subscriptionRepository.deleteById(id);
        logger.info("Deleted subscription with id={}", id);
    }

    @Transactional(readOnly = true)
    public long getTotalSubscriptionCount() {
        long count = subscriptionRepository.count();
        logger.info("Total subscription count: {}", count);
        return count;
    }

    @Transactional(readOnly = true)
    public Long getSubscriptionCountByStatus(SubscriptionStatus status) {
        Long count = subscriptionRepository.countBySubscriptionStatus(status);
        logger.info("Subscription count for status {}: {}", status, count);
        return count;
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopProductsByActiveSubscriptions() {
        List<Object[]> topProducts = subscriptionRepository.findTopProductsByActiveSubscriptions();
        logger.info("Retrieved top products by active subscriptions count");
        return topProducts;
    }

    @Transactional(readOnly = true)
    public List<Subscription> getTopSubscriptionsByPrice(int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        List<Subscription> topSubscriptions = subscriptionRepository.findTopSubscriptionsByPrice(pageable);
        logger.info("Retrieved top {} subscriptions by price", limit);
        return topSubscriptions;
    }

    public List<Subscription> processAutoRenewals() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        List<Subscription> subscriptionsToRenew = subscriptionRepository.findSubscriptionsForAutoRenewal(today, nextWeek);

        for (Subscription subscription : subscriptionsToRenew) {
            LocalDate newEndDate = calculateNewEndDate(subscription);
            subscription.setEndDate(newEndDate);
            subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);
            logger.info("Auto-renewed subscription: id={}, subscriptionNumber={}, newEndDate={}",
                    subscription.getId(), subscription.getSubscriptionNumber(), newEndDate);
        }

        logger.info("Processed {} auto-renewals", subscriptionsToRenew.size());
        return subscriptionsToRenew;
    }

    public List<Subscription> processExpiredSubscriptions() {
        LocalDate today = LocalDate.now();
        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredSubscriptionsWithoutAutoRenewal(today);

        for (Subscription subscription : expiredSubscriptions) {
            subscription.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            logger.info("Expired subscription: id={}, subscriptionNumber={}",
                    subscription.getId(), subscription.getSubscriptionNumber());
        }

        logger.info("Processed {} expired subscriptions", expiredSubscriptions.size());
        return expiredSubscriptions;
    }

    // Private Hilfsmethoden

    private void validateSubscriptionForCreation(Subscription subscription) {
        if (subscription.getProductName() == null || subscription.getProductName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (subscription.getMonthlyPrice() == null || subscription.getMonthlyPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid monthly price is required");
        }
        if (subscription.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (subscription.getContract() == null || subscription.getContract().getId() == null) {
            throw new IllegalArgumentException("Contract is required");
        }

        // Contract muss existieren
        if (!contractRepository.existsById(subscription.getContract().getId())) {
            throw new IllegalArgumentException("Contract not found with ID: " + subscription.getContract().getId());
        }
    }

    private String generateSubscriptionNumber() {
        String prefix = "SUB";
        String year = String.valueOf(LocalDate.now().getYear());
        String randomPart;
        String subscriptionNumber;

        do {
            int number = (int) (Math.random() * 999999) + 1;
            randomPart = String.format("%06d", number);
            subscriptionNumber = prefix + year + randomPart;
        } while (subscriptionRepository.findBySubscriptionNumber(subscriptionNumber).isPresent());

        return subscriptionNumber;
    }

    private LocalDate calculateNewEndDate(Subscription subscription) {
        LocalDate currentEndDate = subscription.getEndDate();
        if (currentEndDate == null) {
            currentEndDate = LocalDate.now();
        }

        return switch (subscription.getBillingCycle()) {
            case MONTHLY -> currentEndDate.plusMonths(1);
            case QUARTERLY -> currentEndDate.plusMonths(3);
            case SEMI_ANNUALLY -> currentEndDate.plusMonths(6);
            case ANNUALLY -> currentEndDate.plusYears(1);
        };
    }
}