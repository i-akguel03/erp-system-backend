package com.erp.backend.repository;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.domain.DueStatus;
import com.erp.backend.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository für DueSchedule-Entitäten.
 *
 * Hinweis: DueSchedule ist ein reiner Zeitraumplan.
 * Preise oder Zahlungen werden NICHT hier gespeichert.
 * Der Rechnungslauf verwendet nur ACTIVE-DueSchedules, um Rechnungen zu erzeugen.
 */
@Repository
public interface DueScheduleRepository extends JpaRepository<DueSchedule, UUID> {

    // === Basis-Abfragen ===

    /** Findet eine Fälligkeit anhand der DueNumber */
    Optional<DueSchedule> findByDueNumber(String dueNumber);

    /** Holt alle Fälligkeiten eines Abos */
    List<DueSchedule> findBySubscription(Subscription subscription);

    /** Holt alle Fälligkeiten eines Abos aufsteigend nach DueDate */
    List<DueSchedule> findBySubscriptionOrderByDueDateAsc(Subscription subscription);

    /** Holt Fälligkeiten eines Abos nach Status aufsteigend nach DueDate */
    List<DueSchedule> findBySubscriptionAndStatusOrderByDueDateAsc(
            Subscription subscription, DueStatus status);

    /** Holt alle Fälligkeiten mit bestimmtem Status */
    List<DueSchedule> findByStatus(DueStatus status);

    // === Subscription ID basierte Abfragen ===

    List<DueSchedule> findBySubscriptionId(UUID subscriptionId);

    List<DueSchedule> findBySubscriptionIdOrderByPeriodStart(UUID subscriptionId);

    List<DueSchedule> findBySubscriptionIdOrderByDueDateDesc(UUID subscriptionId);

    List<DueSchedule> findBySubscriptionIdAndStatusOrderByDueDateAsc(UUID subscriptionId, DueStatus status);

    List<DueSchedule> findBySubscriptionAndDueDateAfter(Subscription subscription, LocalDate date);

    List<DueSchedule> findBySubscriptionAndStatusAndDueDateAfterOrderByDueDateAsc(
            Subscription subscription, DueStatus status, LocalDate date);

    // === Datum-basierte Abfragen ===

    List<DueSchedule> findByDueDate(LocalDate dueDate);

    List<DueSchedule> findByStatusAndDueDateBefore(DueStatus status, LocalDate dueDate);

    List<DueSchedule> findByStatusAndDueDateLessThanEqual(DueStatus status, LocalDate dueDate);

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

    // === Kunden-bezogene Abfragen (über Subscription → Contract → Customer) ===

    @Query("SELECT ds FROM DueSchedule ds JOIN ds.subscription s JOIN s.contract c WHERE c.customer.id = :customerId")
    List<DueSchedule> findByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT ds FROM DueSchedule ds JOIN ds.subscription s JOIN s.contract c " +
            "WHERE c.customer.id = :customerId AND ds.status = :status")
    List<DueSchedule> findByCustomerIdAndStatus(@Param("customerId") UUID customerId,
                                                @Param("status") DueStatus status);

    @Query("SELECT ds FROM DueSchedule ds JOIN ds.subscription s JOIN s.contract c WHERE c.customer.id = :customerId")
    List<DueSchedule> findBySubscriptionCustomerId(@Param("customerId") UUID customerId);

    // === Statistik-Abfragen ===

    Long countByStatus(DueStatus status);

    @Query("SELECT COUNT(ds) FROM DueSchedule ds WHERE ds.dueDate < :currentDate AND ds.status = 'ACTIVE'")
    Long countOverdue(@Param("currentDate") LocalDate currentDate);

    long countBySubscriptionAndStatus(Subscription subscription, DueStatus status);

    @Query("SELECT COUNT(ds) FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.status = 'ACTIVE'")
    long countActiveBySubscription(@Param("subscription") Subscription subscription);

    long countBySubscriptionIdAndStatus(UUID subscriptionId, DueStatus status);

    // === Neue Count-Methoden für Rechnungslauf ===

    /** Zählt Fälligkeiten nach Status und DueDate <= billingDate */
    long countByStatusAndDueDateLessThanEqual(DueStatus status, LocalDate dueDate);

    /** Zählt Fälligkeiten nach Status und DueDate < billingDate */
    long countByStatusAndDueDateBefore(DueStatus status, LocalDate dueDate);

    /** Zählt aktive Fälligkeiten bis zu einem bestimmten Datum (alternative Query-Methode) */
    @Query("SELECT COUNT(ds) FROM DueSchedule ds WHERE ds.status = :status AND ds.dueDate <= :dueDate")
    long countSchedulesForBilling(@Param("status") DueStatus status, @Param("dueDate") LocalDate dueDate);

    /** Zählt aktive Fälligkeiten für einen bestimmten Zeitraum */
    @Query("SELECT COUNT(ds) FROM DueSchedule ds WHERE ds.status = 'ACTIVE' AND ds.dueDate BETWEEN :startDate AND :endDate")
    long countActiveSchedulesBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

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

    // === Neue Queries für ACTIVE-Zeitraumplan / Rechnungslauf ===

    @Query("SELECT ds FROM DueSchedule ds WHERE ds.periodStart >= :periodStart AND ds.periodEnd <= :periodEnd AND ds.status = 'ACTIVE'")
    List<DueSchedule> findActiveByPeriod(@Param("periodStart") LocalDate periodStart,
                                         @Param("periodEnd") LocalDate periodEnd);

    @Query("SELECT ds FROM DueSchedule ds WHERE ds.status = 'ACTIVE' AND ds.periodStart <= :date AND ds.periodEnd >= :date")
    List<DueSchedule> findDueSchedulesForBilling(@Param("date") LocalDate date);

    @Query("SELECT ds FROM DueSchedule ds WHERE ds.status = 'ACTIVE' AND ds.periodEnd < :currentDate")
    List<DueSchedule> findAllOverdueForBilling(@Param("currentDate") LocalDate currentDate);

    // === Custom Delete-Abfragen ===

    void deleteBySubscriptionAndStatus(Subscription subscription, DueStatus status);

    @Query("DELETE FROM DueSchedule ds WHERE ds.subscription = :subscription AND ds.dueDate > :cutoffDate AND ds.status = 'ACTIVE'")
    void deleteFutureActiveBySubscription(@Param("subscription") Subscription subscription,
                                          @Param("cutoffDate") LocalDate cutoffDate);

    @Query("DELETE FROM DueSchedule ds WHERE ds.periodStart = :periodStart AND ds.periodEnd = :periodEnd")
    void deleteByPeriod(@Param("periodStart") LocalDate periodStart,
                        @Param("periodEnd") LocalDate periodEnd);



    // ===============================================================================================
// ERGÄNZUNGEN FÜR DueScheduleRepository
// ===============================================================================================

// === Fehlende Basis-Abfragen für Service-Methoden ===

    /** Holt Fälligkeiten nach Status und Fälligkeitsdatum (für getDueSchedulesByStatus mit Datum) */
    List<DueSchedule> findByStatusAndDueDate(DueStatus status, LocalDate dueDate);

    /** Holt Fälligkeiten nach Status zwischen zwei Daten (für getUpcomingDueSchedules) */
    List<DueSchedule> findByStatusAndDueDateBetween(DueStatus status, LocalDate startDate, LocalDate endDate);

    /** Holt alle Fälligkeiten eines Abos nach Perioden-Ende absteigend sortiert (für generateAdditionalDueSchedules) */
    List<DueSchedule> findBySubscriptionIdOrderByPeriodEndDesc(UUID subscriptionId);

    /** Holt Fälligkeiten eines Abos nach bestimmtem Status (für updateDueScheduleStatusForSubscription) */
    List<DueSchedule> findBySubscriptionIdAndStatus(UUID subscriptionId, DueStatus status);

// === Invoice/Batch-Verknüpfungen (für InvoiceBatchService) ===

    /** Holt alle Fälligkeiten eines bestimmten Rechnungslaufs */
    List<DueSchedule> findByInvoiceBatchId(String invoiceBatchId);

    /** Holt alle Fälligkeiten einer bestimmten Rechnung */
    List<DueSchedule> findByInvoiceId(UUID invoiceId);

    /** Prüft ob es Fälligkeiten mit einer bestimmten Rechnungs-ID gibt */
    boolean existsByInvoiceId(UUID invoiceId);

    /** Prüft ob es Fälligkeiten mit einer bestimmten Batch-ID gibt */
    boolean existsByInvoiceBatchId(String invoiceBatchId);

// === Erweiterte Statistik-Queries für Dashboard ===

    /** Zählt alle ACTIVE Fälligkeiten die bereits überfällig sind */
    @Query("SELECT COUNT(ds) FROM DueSchedule ds WHERE ds.status = 'ACTIVE' AND ds.dueDate < :currentDate")
    long countOverdueActive(@Param("currentDate") LocalDate currentDate);

    /** Zählt alle ACTIVE Fälligkeiten für heute */
    @Query("SELECT COUNT(ds) FROM DueSchedule ds WHERE ds.status = 'ACTIVE' AND ds.dueDate = :currentDate")
    long countDueTodayActive(@Param("currentDate") LocalDate currentDate);

    /** Zählt alle ACTIVE Fälligkeiten in den nächsten X Tagen */
    @Query("SELECT COUNT(ds) FROM DueSchedule ds WHERE ds.status = 'ACTIVE' AND ds.dueDate BETWEEN :startDate AND :endDate")
    long countUpcomingActive(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

// === Bulk-Operationen für Subscription Status-Änderungen ===

    /** Bulk-Update aller offenen Fälligkeiten eines Abos */
    @Query("UPDATE DueSchedule ds SET ds.status = :newStatus WHERE ds.subscription.id = :subscriptionId AND ds.status IN ('ACTIVE', 'PAUSED')")
    int bulkUpdateStatusBySubscription(@Param("subscriptionId") UUID subscriptionId, @Param("newStatus") DueStatus newStatus);

    /** Bulk-Update aller Fälligkeiten mit bestimmtem alten Status */
    @Query("UPDATE DueSchedule ds SET ds.status = :newStatus WHERE ds.subscription.id = :subscriptionId AND ds.status = :oldStatus")
    int bulkUpdateSpecificStatus(@Param("subscriptionId") UUID subscriptionId,
                                 @Param("oldStatus") DueStatus oldStatus,
                                 @Param("newStatus") DueStatus newStatus);

// === Perioden-Validierung (für überschneidende Zeiträume) ===

    /** Findet Fälligkeiten die sich mit einem gegebenen Zeitraum überschneiden */
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.subscription.id = :subscriptionId " +
            "AND NOT (ds.periodEnd < :periodStart OR ds.periodStart > :periodEnd)")
    List<DueSchedule> findOverlappingPeriods(@Param("subscriptionId") UUID subscriptionId,
                                             @Param("periodStart") LocalDate periodStart,
                                             @Param("periodEnd") LocalDate periodEnd);

    /** Findet Fälligkeiten die sich überschneiden, außer der mit der angegebenen ID */
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.subscription.id = :subscriptionId " +
            "AND ds.id != :excludeId " +
            "AND NOT (ds.periodEnd < :periodStart OR ds.periodStart > :periodEnd)")
    List<DueSchedule> findOverlappingPeriodsExcluding(@Param("subscriptionId") UUID subscriptionId,
                                                      @Param("periodStart") LocalDate periodStart,
                                                      @Param("periodEnd") LocalDate periodEnd,
                                                      @Param("excludeId") UUID excludeId);

// === Erweiterte Kunden-Abfragen ===

    /** Holt aktive Fälligkeiten eines Kunden */
    @Query("SELECT ds FROM DueSchedule ds JOIN ds.subscription s JOIN s.contract c " +
            "WHERE c.customer.id = :customerId AND ds.status = 'ACTIVE' ORDER BY ds.dueDate")
    List<DueSchedule> findActiveByCustomerId(@Param("customerId") UUID customerId);

    /** Holt überfällige Fälligkeiten eines Kunden */
    @Query("SELECT ds FROM DueSchedule ds JOIN ds.subscription s JOIN s.contract c " +
            "WHERE c.customer.id = :customerId AND ds.status = 'ACTIVE' AND ds.dueDate < :currentDate")
    List<DueSchedule> findOverdueByCustomerId(@Param("customerId") UUID customerId, @Param("currentDate") LocalDate currentDate);

// === Audit/Cleanup Queries ===

    /** Findet verwaiste Fälligkeiten ohne Subscription (für Data-Cleanup) */
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.subscription IS NULL")
    List<DueSchedule> findOrphanedDueSchedules();

    /** Findet COMPLETED Fälligkeiten ohne Invoice-Verknüpfung (Data-Integrity Check) */
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.status = 'COMPLETED' AND ds.invoiceId IS NULL")
    List<DueSchedule> findCompletedWithoutInvoice();

    /** Findet Fälligkeiten mit Invoice-Verknüpfung aber nicht COMPLETED Status */
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.invoiceId IS NOT NULL AND ds.status != 'COMPLETED'")
    List<DueSchedule> findInconsistentInvoiceStatus();

    /**
     * Zählt überfällige DueSchedules (ACTIVE Status aber Fälligkeitsdatum überschritten)
     */
    @Query("SELECT COUNT(ds) FROM DueSchedule ds WHERE ds.status = 'ACTIVE' AND ds.dueDate < :date")
    long countOverdueDueSchedules(@Param("date") LocalDate date);

    /**
     * Findet überfällige DueSchedules
     */
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.status = 'ACTIVE' AND ds.dueDate < :date")
    List<DueSchedule> findOverdueDueSchedules(@Param("date") LocalDate date);

    /**
     * Findet DueSchedules die bis zu einem bestimmten Datum fällig sind
     */
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.status = 'ACTIVE' AND ds.dueDate <= :date")
    List<DueSchedule> findDueSchedulesUntilDate(@Param("date") LocalDate date);

    /**
     * Findet DueSchedules einer bestimmten Subscription
     */
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.subscription.id = :subscriptionId")
    List<DueSchedule> findBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    /**
     * Findet aktive DueSchedules einer bestimmten Subscription
     */
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.subscription.id = :subscriptionId AND ds.status = 'ACTIVE'")
    List<DueSchedule> findActiveBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    /**
     * Findet DueSchedules in einem Datumsbereich
     */
    @Query("SELECT ds FROM DueSchedule ds WHERE ds.dueDate BETWEEN :startDate AND :endDate")
    List<DueSchedule> findByDueDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Findet die letzten 5 DueSchedules
     */
    @Query("SELECT ds FROM DueSchedule ds ORDER BY ds.createdDate DESC")
    List<DueSchedule> findTop5ByOrderByCreatedAtDesc();

    /**
     * Alternative falls createdAt nicht vorhanden ist
     */
    List<DueSchedule> findTop5ByOrderByIdDesc();

}