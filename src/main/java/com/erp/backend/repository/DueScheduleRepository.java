package com.erp.backend.repository;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.domain.DueStatus;
import com.erp.backend.domain.Subscription;
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

    // Basis-Abfragen
    Optional<DueSchedule> findByDueNumber(String dueNumber);

    List<DueSchedule> findBySubscription(Subscription subscription);

    List<DueSchedule> findBySubscriptionOrderByDueDateAsc(Subscription subscription);

    List<DueSchedule> findBySubscriptionAndStatusOrderByDueDateAsc(Subscription subscription, DueStatus status);

    List<DueSchedule> findByStatus(DueStatus status);

    // Subscription ID basierte Abfragen
    List<DueSchedule> findBySubscriptionId(UUID subscriptionId);

    List<DueSchedule> findBySubscriptionIdOrderByPeriodStart(UUID subscriptionId);

    List<DueSchedule> findBySubscriptionAndDueDateAfter(Subscription subscription, LocalDate date);

    List<DueSchedule> findByStatusAndDueDateBefore(DueStatus status, LocalDate date);

    List<DueSchedule> findBySubscriptionAndStatusAndDueDateAfterOrderByDueDateAsc(
            Subscription subscription, DueStatus status, LocalDate date);

    // Datum-basierte Abfragen
    List<DueSchedule> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT ds FROM DueSchedule ds WHERE ds.dueDate < :currentDate AND ds.status IN (:pendingStatus, :partialStatus)")
    List<DueSchedule> findOverdueSchedules(@Param("currentDate") LocalDate currentDate,
                                           @Param("pendingStatus") DueStatus pendingStatus,
                                           @Param("partialStatus") DueStatus partialStatus);

    // Alternative ohne Parameter (falls DueStatus als Konstanten definiert sind)
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.dueDate < :currentDate AND (ds.status = 'PENDING' OR ds.status = 'PARTIAL_PAID')")
    List<DueSchedule> findOverdueSchedules(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT ds FROM DueSchedule ds WHERE ds.dueDate = :currentDate AND (ds.status = 'PENDING' OR ds.status = 'PARTIAL_PAID')")
    List<DueSchedule> findDueTodaySchedules(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT ds FROM DueSchedule ds WHERE ds.dueDate BETWEEN :startDate AND :endDate AND (ds.status = 'PENDING' OR ds.status = 'PARTIAL_PAID')")
    List<DueSchedule> findUpcomingDueSchedules(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // Mahnungs-bezogene Abfragen
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.dueDate < :currentDate AND " +
            "(ds.reminderSent = false OR ds.lastReminderDate IS NULL OR ds.lastReminderDate < :reminderThreshold) AND " +
            "(ds.status = 'PENDING' OR ds.status = 'PARTIAL_PAID')")
    List<DueSchedule> findSchedulesNeedingReminder(@Param("currentDate") LocalDate currentDate,
                                                   @Param("reminderThreshold") LocalDate reminderThreshold);

    // Vereinfachte Version
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.dueDate < :currentDate AND ds.reminderSent = false AND (ds.status = 'PENDING' OR ds.status = 'PARTIAL_PAID')")
    List<DueSchedule> findSchedulesNeedingReminder(@Param("currentDate") LocalDate currentDate);

    // Nächste Fälligkeit für Subscription
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.status = 'PENDING' AND ds.dueDate >= :currentDate ORDER BY ds.dueDate ASC")
    List<DueSchedule> findNextDueScheduleBySubscription(@Param("subscription") Subscription subscription,
                                                        @Param("currentDate") LocalDate currentDate);

    // Vereinfachte Version ohne Datum-Parameter
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.status = 'PENDING' ORDER BY ds.dueDate ASC")
    List<DueSchedule> findNextDueScheduleBySubscription(@Param("subscription") Subscription subscription);

    // Kunden-bezogene Abfragen (über Subscription-Relation)
    @Query("SELECT ds FROM DueSchedule ds JOIN ds.subscription s JOIN s.contract c WHERE c.customer.id = :customerId")
    List<DueSchedule> findByCustomerId(@Param("customerId") UUID customerId);

    // Statistik-Abfragen
    Long countByStatus(DueStatus status);

    @Query("SELECT COUNT(ds) FROM DueSchedule ds WHERE ds.dueDate < :currentDate AND (ds.status = 'PENDING' OR ds.status = 'PARTIAL_PAID')")
    Long countOverdue(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT COUNT(ds) FROM DueSchedule ds WHERE ds.dueDate < :currentDate AND ds.reminderSent = false AND (ds.status = 'PENDING' OR ds.status = 'PARTIAL_PAID')")
    Long countSchedulesNeedingReminder(@Param("currentDate") LocalDate currentDate);

    // Summen-Abfragen
    @Query("SELECT SUM(ds.amount) FROM DueSchedule ds WHERE ds.status = :status")
    Optional<BigDecimal> sumAmountByStatus(@Param("status") DueStatus status);

    @Query("SELECT SUM(ds.paidAmount) FROM DueSchedule ds WHERE ds.paidAmount IS NOT NULL")
    Optional<BigDecimal> sumPaidAmount();

    @Query("SELECT SUM(ds.amount - COALESCE(ds.paidAmount, 0)) FROM DueSchedule ds WHERE ds.dueDate < :currentDate AND (ds.status = 'PENDING' OR ds.status = 'PARTIAL_PAID')")
    Optional<BigDecimal> sumOverdueAmount(@Param("currentDate") LocalDate currentDate);

    // Subscription-spezifische Summen
    @Query("SELECT SUM(ds.amount - COALESCE(ds.paidAmount, 0)) FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.status = 'PENDING'")
    BigDecimal sumPendingAmountBySubscription(@Param("subscription") Subscription subscription);

    @Query("SELECT SUM(ds.paidAmount) FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.paidAmount IS NOT NULL")
    BigDecimal sumPaidAmountBySubscription(@Param("subscription") Subscription subscription);

    // Bulk-Updates (optional, für Performance bei großen Datenmengen)
    @Query("UPDATE DueSchedule ds SET ds.status = :newStatus WHERE ds.id IN :ids")
    void bulkUpdateStatus(@Param("ids") List<UUID> ids, @Param("newStatus") DueStatus newStatus);

    // Custom Delete-Abfragen für spezielle Fälle
    void deleteBySubscriptionAndStatus(Subscription subscription, DueStatus status);

    @Query("DELETE FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.dueDate > :cutoffDate AND ds.status = 'PENDING'")
    void deleteFuturePendingBySubscription(@Param("subscription") Subscription subscription,
                                           @Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Findet Fälligkeitspläne nach Abonnement und mehreren Status
     */
    List<DueSchedule> findBySubscriptionAndStatusIn(Subscription subscription, List<DueStatus> statuses);


}