package com.erp.backend.repository;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.Customer;
import com.erp.backend.domain.Subscription;
import com.erp.backend.domain.SubscriptionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // ===============================================================================================
// ERGÄNZUNGEN FÜR SubscriptionRepository
// ===============================================================================================

// === Fehlende Methoden für Service-Operationen ===

    /** Bulk-Update für Status-Änderungen (wird vom Service verwendet aber fehlt) */
    @Modifying
    @Query("UPDATE Subscription s SET s.subscriptionStatus = :newStatus WHERE s.id IN :ids")
    int bulkUpdateSubscriptionStatus(@Param("ids") List<UUID> ids, @Param("newStatus") SubscriptionStatus newStatus);

    /** Bulk-Update für Enddatum (für Verlängerungen) */
    @Modifying
    @Query("UPDATE Subscription s SET s.endDate = :newEndDate WHERE s.id IN :ids")
    int bulkUpdateEndDate(@Param("ids") List<UUID> ids, @Param("newEndDate") LocalDate newEndDate);

// === Erweiterte Kunden-Revenue Statistiken ===

    /** Berechnet monatliche Einnahmen pro Kunde (für detaillierte Statistiken) */
//    @Query("SELECT c.id, c.companyName, SUM(s.monthlyPrice) " +
//            "FROM Subscription s JOIN s.contract ct JOIN ct.customer c " +
//            "WHERE s.subscriptionStatus = 'ACTIVE' " +
//            "GROUP BY c.id, c.companyName " +
//            "ORDER BY SUM(s.monthlyPrice) DESC")
//    List<Object[]> findRevenueByCustomer();

    /** Berechnet Revenue nach Produkten */
//    @Query("SELECT s.productName, COUNT(s), SUM(s.monthlyPrice) " +
//            "FROM Subscription s " +
//            "WHERE s.subscriptionStatus = 'ACTIVE' " +
//            "GROUP BY s.productName " +
//            "ORDER BY SUM(s.monthlyPrice) DESC")
//    List<Object[]> findRevenueByProduct();

// === Contract-bezogene Validierungen ===

    /** Prüft ob ein Contract aktive Subscriptions hat (für Contract-Löschung) */
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.contract.id = :contractId AND s.subscriptionStatus IN ('ACTIVE', 'PAUSED')")
    long countActiveSubscriptionsByContract(@Param("contractId") UUID contractId);

    /** Holt alle Subscriptions eines Contracts unabhängig vom Status */
    List<Subscription> findByContractId(UUID contractId);

// === Datum-basierte Analysen ===

    /** Findet Subscriptions die in einem bestimmten Zeitraum starten */
    @Query("SELECT s FROM Subscription s WHERE s.startDate BETWEEN :startDate AND :endDate")
    List<Subscription> findByStartDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /** Findet Subscriptions die in einem bestimmten Zeitraum enden */
    @Query("SELECT s FROM Subscription s WHERE s.endDate BETWEEN :startDate AND :endDate")
    List<Subscription> findByEndDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /** Findet aktive Subscriptions ohne Enddatum (unbegrenzte Laufzeit) */
    @Query("SELECT s FROM Subscription s WHERE s.subscriptionStatus = 'ACTIVE' AND s.endDate IS NULL")
    List<Subscription> findActiveWithoutEndDate();

// === Erweiterte Produkt-Analysen ===

    /** Findet Subscriptions nach Preisbereich */
    @Query("SELECT s FROM Subscription s WHERE s.monthlyPrice BETWEEN :minPrice AND :maxPrice")
    List<Subscription> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    /** Findet teuerstes Subscription pro Kunde */
    @Query("SELECT c.id, MAX(s.monthlyPrice) " +
            "FROM Subscription s JOIN s.contract ct JOIN ct.customer c " +
            "WHERE s.subscriptionStatus = 'ACTIVE' " +
            "GROUP BY c.id")
    List<Object[]> findMaxPriceByCustomer();

// === Auto-Renewal Analysen ===

    /** Zählt Subscriptions nach Auto-Renewal Flag */
    long countByAutoRenewal(boolean autoRenewal);

    /** Findet Subscriptions mit Auto-Renewal die bald ablaufen */
    @Query("SELECT s FROM Subscription s WHERE s.autoRenewal = true " +
            "AND s.subscriptionStatus = 'ACTIVE' " +
            "AND s.endDate BETWEEN :startDate AND :endDate")
    List<Subscription> findAutoRenewalDueBetween(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

// === Billing-Cycle Analysen ===

    /** Zählt aktive Subscriptions nach Billing-Cycle */
    @Query("SELECT s.billingCycle, COUNT(s) FROM Subscription s WHERE s.subscriptionStatus = 'ACTIVE' GROUP BY s.billingCycle")
    List<Object[]> countActiveBillingCycles();

// === Cleanup/Maintenance Queries ===

    /** Findet Subscriptions ohne Contract (Data-Integrity Check) */
    @Query("SELECT s FROM Subscription s WHERE s.contract IS NULL")
    List<Subscription> findOrphanedSubscriptions();

    /** Findet Subscriptions mit ungültigen Daten (Enddatum vor Startdatum) */
    @Query("SELECT s FROM Subscription s WHERE s.endDate < s.startDate")
    List<Subscription> findInvalidDateRanges();

    /** Findet doppelte Subscription-Numbers (für Cleanup) */
    @Query("SELECT s.subscriptionNumber, COUNT(s) FROM Subscription s GROUP BY s.subscriptionNumber HAVING COUNT(s) > 1")
    List<Object[]> findDuplicateSubscriptionNumbers();

// === Status-Transition Queries (für Workflow-Validierung) ===

    /** Findet alle Subscriptions die vor X Tagen gecancelt wurden */
    @Query("SELECT s FROM Subscription s WHERE s.subscriptionStatus = 'CANCELLED' " +
            "AND s.endDate < :cutoffDate")
    List<Subscription> findCancelledBefore(@Param("cutoffDate") LocalDate cutoffDate);

    /** Findet pausierte Subscriptions die länger als X Tage pausiert sind */
    @Query("SELECT s FROM Subscription s WHERE s.subscriptionStatus = 'PAUSED' " +
            "AND s.startDate < :cutoffDate") // Vereinfacht: nutzt startDate als Proxy
    List<Subscription> findLongPausedSubscriptions(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Findet die letzten 5 Subscriptions nach Erstellungsdatum sortiert
     */
    @Query("SELECT s FROM Subscription s ORDER BY s.startDate DESC")
    List<Subscription> findTop5ByOrderByCreatedAtDesc();

    /**
     * Alternative falls createdAt nicht vorhanden ist - nach ID sortiert
     */
    List<Subscription> findTop5ByOrderByIdDesc();

    /**
     * Findet die letzten Subscriptions nach Startdatum sortiert
     */
    List<Subscription> findTop5ByOrderByStartDateDesc();

    /**
     * Zählt aktive Subscriptions ohne DueSchedules
     */
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.subscriptionStatus = 'ACTIVE' AND s.id NOT IN " +
            "(SELECT DISTINCT ds.subscription.id FROM DueSchedule ds WHERE ds.subscription.id IS NOT NULL)")
    long countActiveSubscriptionsWithoutDueSchedules();

    /**
     * Findet aktive Subscriptions ohne DueSchedules
     */
    @Query("SELECT s FROM Subscription s WHERE s.subscriptionStatus = 'ACTIVE' AND s.id NOT IN " +
            "(SELECT DISTINCT ds.subscription.id FROM DueSchedule ds WHERE ds.subscription.id IS NOT NULL)")
    List<Subscription> findActiveSubscriptionsWithoutDueSchedules();

    /**
     * Findet Subscriptions eines bestimmten Kunden
     */
    @Query("SELECT s FROM Subscription s WHERE s.contract.customer.id = :customerId")
    List<Subscription> findByCustomerId(@Param("customerId") Long customerId);

    /**
     * Findet aktive Subscriptions eines bestimmten Kunden
     */
    @Query("SELECT s FROM Subscription s WHERE s.contract.customer.id = :customerId AND s.subscriptionStatus = 'ACTIVE'")
    List<Subscription> findActiveByCustomerId(@Param("customerId") Long customerId);

    /**
     * Findet Subscriptions zu einem bestimmten Produkt
     */
    @Query("SELECT s FROM Subscription s WHERE s.product.id = :productId")
    List<Subscription> findByProductId(@Param("productId") Long productId);

}
