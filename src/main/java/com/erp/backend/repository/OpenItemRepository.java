package com.erp.backend.repository;

import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.OpenItem;
import com.erp.backend.domain.OpenItem.OpenItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository für offene Posten (OpenItem).
 *
 * Dieses Repository enthält alle wichtigen Methoden, um offene Posten:
 * - nach Status
 * - nach Fälligkeit
 * - nach Rechnung oder Kunde
 * zu filtern, zu summieren oder zu zählen.
 *
 * Ziel ist ein klarer, wartbarer Zugriff auf offene Posten ohne zu viele Referenzen.
 */
@Repository
public interface OpenItemRepository extends JpaRepository<OpenItem, UUID> {

    // ============================
    // Basis-Abfragen
    // ============================

    /**
     * Alle offenen Posten zu einer bestimmten Rechnung abrufen.
     */
    List<OpenItem> findByInvoice(Invoice invoice);

    /**
     * Alle offenen Posten mit einem bestimmten Status abrufen.
     */
    List<OpenItem> findByStatus(OpenItemStatus status);

    /**
     * Offene Posten nach Fälligkeit sortiert abrufen.
     */
    List<OpenItem> findByStatusOrderByDueDateAsc(OpenItemStatus status);

    /**
     * Offene Posten eines bestimmten Status, die vor einem bestimmten Datum fällig sind.
     */
    List<OpenItem> findByStatusAndDueDateBefore(OpenItemStatus status, LocalDate date);

    /**
     * Alle offenen Posten anhand der Rechnungs-ID abrufen.
     */
    List<OpenItem> findByInvoiceId(UUID invoiceId);

    // ============================
    // Summen-Abfragen
    // ============================

    /**
     * Gesamtsumme aller offenen Posten eines bestimmten Status.
     *
     * Beispiel: Summe aller OPEN-Posten
     */
    @Query("SELECT SUM(oi.amount - COALESCE(oi.paidAmount, 0)) FROM OpenItem oi WHERE oi.status = :status")
    BigDecimal sumOutstandingAmountByStatus(@Param("status") OpenItemStatus status);

    /**
     * Summe der offenen Posten für einen bestimmten Kunden und Status.
     */
    @Query("SELECT SUM(oi.amount - COALESCE(oi.paidAmount, 0)) FROM OpenItem oi " +
            "WHERE oi.invoice.customer.id = :customerId AND oi.status = :status")
    BigDecimal sumOutstandingAmountByCustomer(@Param("customerId") UUID customerId,
                                              @Param("status") OpenItemStatus status);

    /**
     * Summe der bereits bezahlten Beträge für einen Kunden.
     */
    @Query("SELECT SUM(oi.paidAmount) FROM OpenItem oi " +
            "WHERE oi.invoice.customer.id = :customerId AND oi.paidAmount IS NOT NULL")
    BigDecimal sumPaidAmountByCustomer(@Param("customerId") UUID customerId);

    // ============================
    // Datum-basierte Abfragen
    // ============================

    /**
     * Alle offenen Posten, die bis zu einem bestimmten Datum fällig sind.
     * Berücksichtigt Status OPEN und PARTIALLY_PAID.
     */
    @Query("SELECT oi FROM OpenItem oi WHERE oi.dueDate <= :cutoffDate AND oi.status IN ('OPEN','PARTIALLY_PAID') ORDER BY oi.dueDate")
    List<OpenItem> findOpenItemsDueBefore(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Alle offenen Posten, die genau heute fällig sind.
     */
    @Query("SELECT oi FROM OpenItem oi WHERE oi.dueDate = :targetDate AND oi.status IN ('OPEN','PARTIALLY_PAID')")
    List<OpenItem> findOpenItemsDueToday(@Param("targetDate") LocalDate targetDate);

    /**
     * Alle überfälligen offenen Posten (Fälligkeitsdatum < heute).
     */
    @Query("SELECT oi FROM OpenItem oi WHERE oi.dueDate < :currentDate AND oi.status IN ('OPEN','PARTIALLY_PAID')")
    List<OpenItem> findOverdueOpenItems(@Param("currentDate") LocalDate currentDate);

    // ============================
    // Bulk-Updates
    // ============================

    /**
     * Mehrere offene Posten gleichzeitig auf einen neuen Status setzen.
     * Nützlich z.B. beim Verbuchen von Zahlungen.
     */
    @Query("UPDATE OpenItem oi SET oi.status = :newStatus WHERE oi.id IN :ids")
    void bulkUpdateStatus(@Param("ids") List<UUID> ids, @Param("newStatus") OpenItemStatus newStatus);

    // ============================
    // Zählen
    // ============================

    /**
     * Anzahl der offenen Posten nach Status.
     */
    long countByStatus(OpenItemStatus status);

    /**
     * Anzahl der überfälligen offenen Posten.
     */
    @Query("SELECT COUNT(oi) FROM OpenItem oi WHERE oi.dueDate < :currentDate AND oi.status IN ('OPEN','PARTIALLY_PAID')")
    long countOverdue(@Param("currentDate") LocalDate currentDate);

    /**
     * Anzahl der offenen Posten eines bestimmten Kunden.
     */
    @Query("SELECT COUNT(oi) FROM OpenItem oi WHERE oi.invoice.customer.id = :customerId AND oi.status IN ('OPEN','PARTIALLY_PAID')")
    long countOpenByCustomer(@Param("customerId") UUID customerId);
}
