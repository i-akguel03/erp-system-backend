package com.erp.backend.repository;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.Customer;
import com.erp.backend.domain.Subscription;
import com.erp.backend.domain.SubscriptionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    // ================= Basis =================
    Optional<Subscription> findBySubscriptionNumber(String subscriptionNumber);

    List<Subscription> findByContract(Contract contract);

    List<Subscription> findByContractAndSubscriptionStatus(Contract contract, SubscriptionStatus status);

    List<Subscription> findBySubscriptionStatus(SubscriptionStatus status);

    // ================= Kunden-bezogen =================
    @Query("SELECT s FROM Subscription s JOIN s.contract c WHERE c.customer.id = :customerId")
    List<Subscription> findByContractCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT s FROM Subscription s JOIN s.contract c WHERE c.customer.id = :customerId AND s.subscriptionStatus = :status")
    List<Subscription> findByContractCustomerIdAndSubscriptionStatus(@Param("customerId") UUID customerId,
                                                                     @Param("status") SubscriptionStatus status);

    // ================= Produkt-bezogen =================
    List<Subscription> findByProductNameContainingIgnoreCase(String productName);

    // ================= Datum / Auto-Renewal =================
    @Query("SELECT s FROM Subscription s WHERE s.endDate <= :threshold AND s.subscriptionStatus = 'ACTIVE'")
    List<Subscription> findSubscriptionsExpiringBefore(@Param("threshold") LocalDate threshold);

    @Query("SELECT s FROM Subscription s WHERE s.endDate <= :threshold AND s.subscriptionStatus = 'ACTIVE' AND s.autoRenewal = true")
    List<Subscription> findSubscriptionsForAutoRenewal(@Param("threshold") LocalDate threshold);

    @Query("SELECT s FROM Subscription s WHERE s.endDate < :currentDate AND s.subscriptionStatus = 'ACTIVE' AND s.autoRenewal = false")
    List<Subscription> findExpiredSubscriptionsWithoutAutoRenewal(@Param("currentDate") LocalDate currentDate);

    // ================= Statistik / Top =================
    @Query("SELECT s.productName, COUNT(s) FROM Subscription s WHERE s.subscriptionStatus = 'ACTIVE' GROUP BY s.productName ORDER BY COUNT(s) DESC")
    List<Object[]> findTopProductsByActiveSubscriptions();

    @Query("SELECT s FROM Subscription s WHERE s.subscriptionStatus = 'ACTIVE' ORDER BY s.monthlyPrice DESC")
    List<Subscription> findTopSubscriptionsByPrice(Pageable pageable);

    default List<Subscription> findTopBySubscriptionStatusOrderByMonthlyPriceDesc(SubscriptionStatus status, int limit) {
        return findTopSubscriptionsByPrice(Pageable.ofSize(limit));
    }

    // ================= Revenue =================
    @Query("SELECT SUM(s.monthlyPrice) FROM Subscription s WHERE s.subscriptionStatus = 'ACTIVE'")
    BigDecimal calculateTotalActiveRevenue();

    @Query("SELECT SUM(s.monthlyPrice) FROM Subscription s JOIN s.contract c WHERE c.customer.id = :customerId AND s.subscriptionStatus = 'ACTIVE'")
    BigDecimal calculateActiveRevenueByCustomer(@Param("customerId") UUID customerId);

    // ================= Bulk / Batch =================
    @Query("UPDATE Subscription s SET s.subscriptionStatus = :newStatus WHERE s.id IN :ids")
    void bulkUpdateStatus(@Param("ids") List<UUID> ids, @Param("newStatus") SubscriptionStatus newStatus);

    // ================= Contract / Status =================
    @Query("SELECT s FROM Subscription s WHERE s.contract.id = :contractId AND s.subscriptionStatus = :status")
    List<Subscription> findByContractIdAndStatus(@Param("contractId") UUID contractId,
                                                 @Param("status") SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.contract = :contract AND s.subscriptionStatus IN ('ACTIVE','PAUSED')")
    List<Subscription> findActiveOrPausedByContract(@Param("contract") Contract contract);

    long countBySubscriptionStatus(SubscriptionStatus status);
}
