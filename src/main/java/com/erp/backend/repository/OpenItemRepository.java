package com.erp.backend.repository;

import com.erp.backend.domain.OpenItem;
import com.erp.backend.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface OpenItemRepository extends JpaRepository<OpenItem, UUID> {

    // ========================================
    // 1. Basic Queries
    // ========================================

    @Query("SELECT oi FROM OpenItem oi JOIN FETCH oi.invoice i JOIN FETCH i.customer ORDER BY oi.dueDate ASC")
    List<OpenItem> findAllWithInvoiceAndCustomer();

    @Query("SELECT oi FROM OpenItem oi JOIN FETCH oi.invoice i JOIN FETCH i.customer WHERE oi.id = :id")
    OpenItem findByIdWithInvoiceAndCustomer(@Param("id") UUID id);

    // ========================================
    // 2. Status-based Queries
    // ========================================

    List<OpenItem> findByStatus(OpenItem.OpenItemStatus status);

    @Query("SELECT oi FROM OpenItem oi WHERE oi.status IN ('OPEN', 'PARTIALLY_PAID', 'OVERDUE')")
    List<OpenItem> findAllOpenItems();

    @Query("SELECT oi FROM OpenItem oi WHERE oi.status = 'PAID'")
    List<OpenItem> findAllPaidItems();

    @Query("SELECT oi FROM OpenItem oi WHERE oi.status IN ('OPEN', 'OVERDUE') AND oi.dueDate < :currentDate")
    List<OpenItem> findOverdueItems(@Param("currentDate") LocalDate currentDate);

    // ========================================
    // 3. Invoice-related Queries
    // ========================================

    List<OpenItem> findByInvoice(Invoice invoice);

    @Query("SELECT oi FROM OpenItem oi WHERE oi.invoice.id = :invoiceId")
    List<OpenItem> findByInvoiceId(@Param("invoiceId") UUID invoiceId);

    @Query("SELECT oi FROM OpenItem oi WHERE oi.invoice.id IN :invoiceIds")
    List<OpenItem> findByInvoiceIds(@Param("invoiceIds") List<UUID> invoiceIds);

    // ========================================
    // 4. Customer-related Queries
    // ========================================

    @Query("SELECT oi FROM OpenItem oi WHERE oi.invoice.customer.id = :customerId")
    List<OpenItem> findByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT oi FROM OpenItem oi WHERE oi.invoice.customer.id = :customerId AND oi.status IN ('OPEN', 'PARTIALLY_PAID', 'OVERDUE')")
    List<OpenItem> findOpenItemsByCustomerId(@Param("customerId") UUID customerId);

    // ========================================
    // 5. Date-based Queries
    // ========================================

    List<OpenItem> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT oi FROM OpenItem oi WHERE oi.dueDate <= :date AND oi.status IN ('OPEN', 'PARTIALLY_PAID')")
    List<OpenItem> findItemsDueByDate(@Param("date") LocalDate date);

    @Query("SELECT oi FROM OpenItem oi WHERE oi.paidDate BETWEEN :startDate AND :endDate")
    List<OpenItem> findItemsPaidBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // ========================================
    // 6. Amount-based Queries
    // ========================================

    @Query("SELECT SUM(oi.amount - COALESCE(oi.paidAmount, 0)) FROM OpenItem oi WHERE oi.status IN ('OPEN', 'PARTIALLY_PAID', 'OVERDUE')")
    BigDecimal getTotalOutstandingAmount();

    @Query("SELECT SUM(oi.paidAmount) FROM OpenItem oi WHERE oi.status IN ('PAID', 'PARTIALLY_PAID')")
    BigDecimal getTotalPaidAmount();

    @Query("SELECT SUM(oi.amount - COALESCE(oi.paidAmount, 0)) FROM OpenItem oi WHERE oi.invoice.customer.id = :customerId AND oi.status IN ('OPEN', 'PARTIALLY_PAID', 'OVERDUE')")
    BigDecimal getOutstandingAmountByCustomer(@Param("customerId") UUID customerId);

    // ========================================
    // 7. Reminder Queries
    // ========================================

    @Query("SELECT oi FROM OpenItem oi WHERE oi.status IN ('OPEN', 'OVERDUE') AND (oi.lastReminderDate IS NULL OR oi.lastReminderDate < :reminderThreshold)")
    List<OpenItem> findItemsNeedingReminder(@Param("reminderThreshold") LocalDate reminderThreshold);

    List<OpenItem> findByReminderCountGreaterThan(int reminderCount);

    // ========================================
    // 8. Subscription-related Queries
    // ========================================

    @Query("SELECT oi FROM OpenItem oi JOIN oi.invoice i WHERE i.subscription.id = :subscriptionId")
    List<OpenItem> findBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);

    @Query("SELECT oi FROM OpenItem oi JOIN oi.invoice i WHERE i.subscription.id IN :subscriptionIds")
    List<OpenItem> findBySubscriptionIds(@Param("subscriptionIds") List<UUID> subscriptionIds);

    @Query("SELECT oi FROM OpenItem oi JOIN FETCH oi.invoice inv JOIN FETCH inv.customer WHERE inv.subscription.id = :subscriptionId ORDER BY oi.dueDate ASC")
    List<OpenItem> findBySubscriptionIdWithInvoiceAndCustomer(@Param("subscriptionId") UUID subscriptionId);

    @Query("SELECT oi FROM OpenItem oi JOIN FETCH oi.invoice inv JOIN FETCH inv.customer WHERE inv.subscription.id IN :subscriptionIds ORDER BY oi.dueDate ASC")
    List<OpenItem> findBySubscriptionIdsWithInvoiceAndCustomer(@Param("subscriptionIds") List<UUID> subscriptionIds);

    // ========================================
    // 9. Statistics Queries
    // ========================================

    @Query("SELECT COUNT(oi) FROM OpenItem oi WHERE oi.status = :status")
    long countByStatus(@Param("status") OpenItem.OpenItemStatus status);

    @Query("SELECT AVG(oi.amount) FROM OpenItem oi WHERE oi.status = :status")
    BigDecimal getAverageAmountByStatus(@Param("status") OpenItem.OpenItemStatus status);

    @Query("SELECT COUNT(oi) FROM OpenItem oi WHERE oi.dueDate < :currentDate AND oi.status IN ('OPEN', 'PARTIALLY_PAID', 'OVERDUE')")
    long countOverdueItems(@Param("currentDate") LocalDate currentDate);
}