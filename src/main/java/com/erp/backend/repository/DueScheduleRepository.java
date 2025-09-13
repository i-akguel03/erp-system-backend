package com.erp.backend.repository;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.domain.DueStatus;
import com.erp.backend.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DueScheduleRepository extends JpaRepository<DueSchedule, UUID> {

    // === Basis-Abfragen ===
    Optional<DueSchedule> findByDueNumber(String dueNumber);

    List<DueSchedule> findBySubscription(Subscription subscription);

    List<DueSchedule> findBySubscriptionOrderByDueDateAsc(Subscription subscription);

    List<DueSchedule> findBySubscriptionAndStatusOrderByDueDateAsc(
            Subscription subscription, DueStatus status);

    List<DueSchedule> findByStatus(DueStatus status);

    // === Subscription ID basierte Abfragen ===
    List<DueSchedule> findBySubscriptionId(UUID subscriptionId);

    List<DueSchedule> findBySubscriptionIdOrderByPeriodStart(UUID subscriptionId);

    List<DueSchedule> findBySubscriptionAndDueDateAfter(Subscription subscription, LocalDate date);

    List<DueSchedule> findBySubscriptionAndStatusAndDueDateAfterOrderByDueDateAsc(
            Subscription subscription, DueStatus status, LocalDate date);

    // === Datum-basierte Abfragen ===
    List<DueSchedule> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT ds FROM DueSchedule ds WHERE ds.dueDate < :currentDate AND ds.status = 'ACTIVE'")
    List<DueSchedule> findOverdueSchedules(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT ds FROM DueSchedule ds WHERE ds.dueDate = :currentDate AND ds.status = 'ACTIVE'")
    List<DueSchedule> findDueTodaySchedules(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT ds FROM DueSchedule ds WHERE ds.dueDate BETWEEN :startDate AND :endDate AND ds.status = 'ACTIVE'")
    List<DueSchedule> findUpcomingDueSchedules(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // === Nächste fällige Fälligkeit ===
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.status = 'ACTIVE' " +
            "AND ds.dueDate >= :currentDate ORDER BY ds.dueDate ASC")
    List<DueSchedule> findNextActiveDueScheduleBySubscription(@Param("subscription") Subscription subscription,
                                                              @Param("currentDate") LocalDate currentDate);

    // === Kunden-bezogene Abfragen (über Subscription-Relation) ===
    @Query("SELECT ds FROM DueSchedule ds JOIN ds.subscription s JOIN s.contract c WHERE c.customer.id = :customerId")
    List<DueSchedule> findByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT ds FROM DueSchedule ds JOIN ds.subscription s JOIN s.contract c " +
            "WHERE c.customer.id = :customerId AND ds.status = :status")
    List<DueSchedule> findByCustomerIdAndStatus(@Param("customerId") UUID customerId,
                                                @Param("status") DueStatus status);

    // === Statistik-Abfragen ===
    Long countByStatus(DueStatus status);

    @Query("SELECT COUNT(ds) FROM DueSchedule ds WHERE ds.dueDate < :currentDate AND ds.status = 'ACTIVE'")
    Long countOverdue(@Param("currentDate") LocalDate currentDate);

    /**
     * Zählt DueSchedules nach Status für eine Subscription
     */
    long countBySubscriptionAndStatus(Subscription subscription, DueStatus status);

    /**
     * Zählt aktive DueSchedules für eine Subscription
     */
    @Query("SELECT COUNT(ds) FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.status = 'ACTIVE'")
    long countActiveBySubscription(@Param("subscription") Subscription subscription);

    // === Status-Management ===
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.status = 'PAUSED'")
    List<DueSchedule> findPausedBySubscription(@Param("subscription") Subscription subscription);

    @Query("SELECT ds FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.status = 'SUSPENDED'")
    List<DueSchedule> findSuspendedBySubscription(@Param("subscription") Subscription subscription);

    @Query("SELECT ds FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.status = 'COMPLETED'")
    List<DueSchedule> findCompletedBySubscription(@Param("subscription") Subscription subscription);

    // === Zeitraum-basierte Abfragen ===
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.periodStart >= :periodStart AND ds.periodEnd <= :periodEnd")
    List<DueSchedule> findByPeriod(@Param("periodStart") LocalDate periodStart,
                                   @Param("periodEnd") LocalDate periodEnd);

    @Query("SELECT ds FROM DueSchedule ds WHERE ds.subscription = :subscription " +
            "AND ds.periodStart >= :periodStart AND ds.periodEnd <= :periodEnd " +
            "ORDER BY ds.periodStart")
    List<DueSchedule> findBySubscriptionAndPeriod(@Param("subscription") Subscription subscription,
                                                  @Param("periodStart") LocalDate periodStart,
                                                  @Param("periodEnd") LocalDate periodEnd);

    // === Custom Delete-Abfragen ===
    void deleteBySubscriptionAndStatus(Subscription subscription, DueStatus status);

    @Query("DELETE FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.dueDate > :cutoffDate " +
            "AND ds.status = 'ACTIVE'")
    void deleteFutureActiveBySubscription(@Param("subscription") Subscription subscription,
                                          @Param("cutoffDate") LocalDate cutoffDate);

    @Query("DELETE FROM DueSchedule ds WHERE ds.periodStart = :periodStart AND ds.periodEnd = :periodEnd")
    void deleteByPeriod(@Param("periodStart") LocalDate periodStart,
                        @Param("periodEnd") LocalDate periodEnd);
}
