package com.erp.backend.repository;

import com.erp.backend.domain.*;
import org.springframework.data.domain.Page;
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

    // Suche nach Abo-Nummer
    Optional<Subscription> findBySubscriptionNumber(String subscriptionNumber);

    // Alle Abos eines Vertrags
    List<Subscription> findByContract(Contract contract);

    // Abos nach Status
    List<Subscription> findBySubscriptionStatus(SubscriptionStatus status);

    // Aktive Abos eines Vertrags
    List<Subscription> findByContractAndSubscriptionStatus(Contract contract, SubscriptionStatus status);

    // Alle Abos eines Kunden (über Contract-Beziehung)
    @Query("SELECT s FROM Subscription s WHERE s.contract.customer = :customer")
    List<Subscription> findByCustomer(@Param("customer") Customer customer);

    // Aktive Abos eines Kunden
    @Query("SELECT s FROM Subscription s WHERE s.contract.customer = :customer AND s.subscriptionStatus = 'ACTIVE'")
    List<Subscription> findActiveSubscriptionsByCustomer(@Param("customer") Customer customer);

    // Abos die bald ablaufen
    @Query("SELECT s FROM Subscription s WHERE s.endDate BETWEEN :startDate AND :endDate AND s.autoRenewal = false")
    List<Subscription> findSubscriptionsExpiringBetween(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    // Abos zur automatischen Verlängerung
    @Query("SELECT s FROM Subscription s WHERE s.endDate BETWEEN :startDate AND :endDate AND s.autoRenewal = true")
    List<Subscription> findSubscriptionsForAutoRenewal(@Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    // Abos nach Produktname
    List<Subscription> findByProductNameContainingIgnoreCase(String productName);

    // Abos nach Abrechnungszyklus
    List<Subscription> findByBillingCycle(BillingCycle billingCycle);

    // Abos in einem Preisbereich
    List<Subscription> findByMonthlyPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // Teuerste Abos
    @Query("SELECT s FROM Subscription s ORDER BY s.monthlyPrice DESC")
    List<Subscription> findTopSubscriptionsByPrice(Pageable pageable);

    // Gesamtumsatz aller aktiven Abos
    @Query("SELECT SUM(s.monthlyPrice) FROM Subscription s WHERE s.subscriptionStatus = 'ACTIVE'")
    BigDecimal calculateTotalActiveRevenue();

    // Gesamtumsatz aller aktiven Abos eines Kunden
    @Query("SELECT SUM(s.monthlyPrice) FROM Subscription s WHERE s.contract.customer = :customer AND s.subscriptionStatus = 'ACTIVE'")
    BigDecimal calculateActiveRevenueByCustomer(@Param("customer") Customer customer);

    // Anzahl Abos nach Status
    Long countBySubscriptionStatus(SubscriptionStatus status);

    // Abgelaufene Abos ohne Auto-Renewal
    @Query("SELECT s FROM Subscription s WHERE s.endDate < :currentDate AND s.autoRenewal = false")
    List<Subscription> findExpiredSubscriptionsWithoutAutoRenewal(@Param("currentDate") LocalDate currentDate);

    // Abos ohne Enddatum (unbefristet)
    List<Subscription> findByEndDateIsNull();

    // Abos die heute starten
    List<Subscription> findByStartDate(LocalDate startDate);

    // Top-Produkte nach Anzahl aktiver Abos
    @Query("SELECT s.productName, COUNT(s) FROM Subscription s WHERE s.subscriptionStatus = 'ACTIVE' GROUP BY s.productName ORDER BY COUNT(s) DESC")
    List<Object[]> findTopProductsByActiveSubscriptions();
}