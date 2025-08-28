package com.erp.backend.repository;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.domain.DueStatus;
import com.erp.backend.domain.Subscription;
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
public interface DueScheduleRepository extends JpaRepository<DueSchedule, UUID> {

    // Suche nach Fälligkeitsnummer
    Optional<DueSchedule> findByDueNumber(String dueNumber);

    // Alle Fälligkeiten eines Abonnements
    List<DueSchedule> findBySubscription(Subscription subscription);

    // Fälligkeiten mit Pagination für ein Abonnement
    Page<DueSchedule> findBySubscription(Subscription subscription, Pageable pageable);

    // Fälligkeiten nach Status
    List<DueSchedule> findByStatus(DueStatus status);

    // Fällige Zahlungen an einem bestimmten Datum
    List<DueSchedule> findByDueDate(LocalDate dueDate);

    // Überfällige Zahlungen
    @Query("SELECT d FROM DueSchedule d WHERE d.status = 'PENDING' AND d.dueDate < :currentDate")
    List<DueSchedule> findOverdueSchedules(@Param("currentDate") LocalDate currentDate);

    // Fälligkeiten in einem Zeitraum
    List<DueSchedule> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    // Alle ausstehenden Fälligkeiten eines Abonnements
    List<DueSchedule> findBySubscriptionAndStatus(Subscription subscription, DueStatus status);

    // Fälligkeiten eines Abonnements nach Fälligkeitsdatum sortiert
    List<DueSchedule> findBySubscriptionOrderByDueDateAsc(Subscription subscription);

    // Bezahlte Fälligkeiten in einem Zeitraum
    List<DueSchedule> findByStatusAndPaidDateBetween(DueStatus status, LocalDate startDate, LocalDate endDate);

    // Fälligkeiten die heute fällig sind
    @Query("SELECT d FROM DueSchedule d WHERE d.dueDate = :today AND d.status = 'PENDING'")
    List<DueSchedule> findDueTodaySchedules(@Param("today") LocalDate today);

    // Fälligkeiten die in den nächsten X Tagen fällig werden
    @Query("SELECT d FROM DueSchedule d WHERE d.dueDate BETWEEN :startDate AND :endDate AND d.status = 'PENDING'")
    List<DueSchedule> findUpcomingDueSchedules(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // Summe aller ausstehenden Beträge für ein Abonnement
    @Query("SELECT COALESCE(SUM(d.amount - d.paidAmount), 0) FROM DueSchedule d WHERE d.subscription = :subscription AND d.status = 'PENDING'")
    BigDecimal sumPendingAmountBySubscription(@Param("subscription") Subscription subscription);

    // Summe aller bezahlten Beträge für ein Abonnement
    @Query("SELECT COALESCE(SUM(d.paidAmount), 0) FROM DueSchedule d WHERE d.subscription = :subscription AND d.status = 'PAID'")
    BigDecimal sumPaidAmountBySubscription(@Param("subscription") Subscription subscription);

    // Alle Fälligkeiten eines Kunden über alle seine Abonnements
    @Query("SELECT d FROM DueSchedule d JOIN d.subscription s JOIN s.contract c WHERE c.customer.id = :customerId")
    List<DueSchedule> findByCustomerId(@Param("customerId") UUID customerId);

    // Mit Pagination
    @Query("SELECT d FROM DueSchedule d JOIN d.subscription s JOIN s.contract c WHERE c.customer.id = :customerId")
    Page<DueSchedule> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    // Summe aller offenen Beträge eines Kunden
    @Query("SELECT COALESCE(SUM(d.amount - d.paidAmount), 0) FROM DueSchedule d JOIN d.subscription s JOIN s.contract c WHERE c.customer.id = :customerId AND d.status = 'PENDING'")
    BigDecimal sumPendingAmountByCustomer(@Param("customerId") UUID customerId);

    // Summe aller bezahlten Beträge eines Kunden
    @Query("SELECT COALESCE(SUM(d.paidAmount), 0) FROM DueSchedule d JOIN d.subscription s JOIN s.contract c WHERE c.customer.id = :customerId AND d.status = 'PAID'")
    BigDecimal sumPaidAmountByCustomer(@Param("customerId") UUID customerId);

    // Fälligkeiten die eine Mahnung benötigen (überfällig und noch keine Mahnung gesendet)
    @Query("SELECT d FROM DueSchedule d WHERE d.status = 'PENDING' AND d.dueDate < :currentDate AND d.reminderSent = false")
    List<DueSchedule> findSchedulesNeedingReminder(@Param("currentDate") LocalDate currentDate);

    // Anzahl überfälliger Fälligkeiten pro Abonnement
    @Query("SELECT COUNT(d) FROM DueSchedule d WHERE d.subscription = :subscription AND d.status = 'PENDING' AND d.dueDate < :currentDate")
    Long countOverdueBySubscription(@Param("subscription") Subscription subscription, @Param("currentDate") LocalDate currentDate);

    // Nächste fällige Zahlung für ein Abonnement (nur 1)
    @Query("SELECT d FROM DueSchedule d WHERE d.subscription = :subscription AND d.status = 'PENDING' ORDER BY d.dueDate ASC")
    Optional<DueSchedule> findNextDueScheduleBySubscription(@Param("subscription") Subscription subscription);

    // Anzahl Fälligkeiten nach Status
    long countByStatus(DueStatus status);

    // Anzahl überfälliger Fälligkeiten gesamt
    @Query("SELECT COUNT(d) FROM DueSchedule d WHERE d.status = 'PENDING' AND d.dueDate < :currentDate")
    long countOverdue(@Param("currentDate") LocalDate currentDate);

    // Anzahl Fälligkeiten die eine Mahnung benötigen
    @Query("SELECT COUNT(d) FROM DueSchedule d WHERE d.status = 'PENDING' AND d.dueDate < :currentDate AND d.reminderSent = false")
    long countSchedulesNeedingReminder(@Param("currentDate") LocalDate currentDate);

    // Summe aller offenen Beträge nach Status
    @Query("SELECT COALESCE(SUM(d.amount - d.paidAmount), 0) FROM DueSchedule d WHERE d.status = :status")
    Optional<BigDecimal> sumAmountByStatus(@Param("status") DueStatus status);

    // Summe aller bezahlten Beträge gesamt
    @Query("SELECT COALESCE(SUM(d.paidAmount), 0) FROM DueSchedule d WHERE d.status = 'PAID'")
    Optional<BigDecimal> sumPaidAmount();

    // Summe aller überfälligen offenen Beträge
    @Query("SELECT COALESCE(SUM(d.amount - d.paidAmount), 0) FROM DueSchedule d WHERE d.status = 'PENDING' AND d.dueDate < :currentDate")
    Optional<BigDecimal> sumOverdueAmount(@Param("currentDate") LocalDate currentDate);

}
