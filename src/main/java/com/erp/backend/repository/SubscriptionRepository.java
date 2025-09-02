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

    // Basis-Abfragen
    Optional<Subscription> findBySubscriptionNumber(String subscriptionNumber);

    List<Subscription> findByContract(Contract contract);

    List<Subscription> findByContractAndSubscriptionStatus(Contract contract, SubscriptionStatus status);

    List<Subscription> findBySubscriptionStatus(SubscriptionStatus status);

    // Kunden-bezogene Abfragen
    @Query("SELECT s FROM Subscription s JOIN s.contract c WHERE c.customer = :customer")
    List<Subscription> findByCustomer(@Param("customer") Customer customer);

    @Query("SELECT s FROM Subscription s JOIN s.contract c WHERE c.customer = :customer AND s.subscriptionStatus = 'ACTIVE'")
    List<Subscription> findActiveSubscriptionsByCustomer(@Param("customer") Customer customer);

    // Produkt-basierte Abfragen
    List<Subscription> findByProductNameContainingIgnoreCase(String productName);

    // Datum-basierte Abfragen für Ablauf und Verlängerung
    @Query("SELECT s FROM Subscription s WHERE s.endDate BETWEEN :startDate AND :endDate AND s.subscriptionStatus = 'ACTIVE'")
    List<Subscription> findSubscriptionsExpiringBetween(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Subscription s WHERE s.endDate BETWEEN :startDate AND :endDate AND s.autoRenewal = true AND s.subscriptionStatus = 'ACTIVE'")
    List<Subscription> findSubscriptionsForAutoRenewal(@Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Subscription s WHERE s.endDate < :currentDate AND s.autoRenewal = false AND s.subscriptionStatus = 'ACTIVE'")
    List<Subscription> findExpiredSubscriptionsWithoutAutoRenewal(@Param("currentDate") LocalDate currentDate);

    // Statistik-Abfragen
    Long countBySubscriptionStatus(SubscriptionStatus status);

    @Query("SELECT s.productName, COUNT(s) FROM Subscription s WHERE s.subscriptionStatus = 'ACTIVE' GROUP BY s.productName ORDER BY COUNT(s) DESC")
    List<Object[]> findTopProductsByActiveSubscriptions();

    @Query("SELECT s FROM Subscription s WHERE s.subscriptionStatus = 'ACTIVE' ORDER BY s.monthlyPrice DESC")
    List<Subscription> findTopSubscriptionsByPrice(Pageable pageable);

    // Revenue-Berechnungen
    @Query("SELECT SUM(s.monthlyPrice) FROM Subscription s WHERE s.subscriptionStatus = 'ACTIVE'")
    BigDecimal calculateTotalActiveRevenue();

    @Query("SELECT SUM(s.monthlyPrice) FROM Subscription s JOIN s.contract c WHERE c.customer = :customer AND s.subscriptionStatus = 'ACTIVE'")
    BigDecimal calculateActiveRevenueByCustomer(@Param("customer") Customer customer);

    // Erweiterte Abfragen für Reporting
    @Query("SELECT s FROM Subscription s WHERE s.startDate BETWEEN :startDate AND :endDate")
    List<Subscription> findSubscriptionsCreatedBetween(@Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Subscription s WHERE s.subscriptionStatus = :status AND s.endDate BETWEEN :startDate AND :endDate")
    List<Subscription> findSubscriptionsByStatusAndEndDateBetween(@Param("status") SubscriptionStatus status,
                                                                  @Param("startDate") LocalDate startDate,
                                                                  @Param("endDate") LocalDate endDate);

    // Bulk-Updates für Batch-Verarbeitung
    @Query("UPDATE Subscription s SET s.subscriptionStatus = :newStatus WHERE s.id IN :ids")
    void bulkUpdateStatus(@Param("ids") List<UUID> ids, @Param("newStatus") SubscriptionStatus newStatus);

    // Contract-basierte Abfragen
    @Query("SELECT s FROM Subscription s WHERE s.contract.id = :contractId")
    List<Subscription> findByContractId(@Param("contractId") UUID contractId);

    @Query("SELECT s FROM Subscription s WHERE s.contract.id = :contractId AND s.subscriptionStatus = :status")
    List<Subscription> findByContractIdAndStatus(@Param("contractId") UUID contractId,
                                                 @Param("status") SubscriptionStatus status);

    // Validation Queries
    @Query("SELECT COUNT(s) > 0 FROM Subscription s WHERE s.subscriptionNumber = :subscriptionNumber")
    boolean existsBySubscriptionNumber(@Param("subscriptionNumber") String subscriptionNumber);

    @Query("SELECT s FROM Subscription s WHERE s.contract = :contract AND s.subscriptionStatus IN ('ACTIVE', 'PAUSED')")
    List<Subscription> findActiveOrPausedByContract(@Param("contract") Contract contract);
}