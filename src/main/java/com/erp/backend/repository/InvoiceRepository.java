package com.erp.backend.repository;

import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.Customer;
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
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {



    // Alle überfälligen Rechnungen (z. B. für Rechnungslauf)
    List<Invoice> findByDueDateBeforeAndStatusNot(LocalDate date, Invoice.InvoiceStatus status);

    // Überfällig UND nicht bezahlt/storniert
    List<Invoice> findByDueDateBeforeAndStatusNotIn(LocalDate date, List<Invoice.InvoiceStatus> excludedStatuses);

    /**
     * Findet Rechnung anhand der Rechnungsnummer
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Findet alle Rechnungen eines Kunden
     */
    List<Invoice> findByCustomerOrderByInvoiceDateDesc(Customer customer);

    /**
     * Findet Rechnungen nach Status
     */

    /**
     * Findet alle Rechnungen einer Subscription mit bestimmtem Status
     */
    List<Invoice> findBySubscriptionAndStatus(Subscription subscription, Invoice.InvoiceStatus status);

    /**
     * Alternative mit Subscription-ID (falls bevorzugt)
     */
    @Query("SELECT i FROM Invoice i WHERE i.subscription.id = :subscriptionId AND i.status = :status")
    List<Invoice> findBySubscriptionIdAndStatus(@Param("subscriptionId") UUID subscriptionId,
                                                @Param("status") Invoice.InvoiceStatus status);

    /**
     * Findet alle Rechnungen einer Subscription
     */
    List<Invoice> findBySubscription(Subscription subscription);

    /**
     * Zählt offene Rechnungen einer Subscription
     */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.subscription = :subscription AND i.status = :status")
    long countBySubscriptionAndStatus(@Param("subscription") Subscription subscription,
                                      @Param("status") Invoice.InvoiceStatus status);


    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.invoiceItems LEFT JOIN FETCH i.customer ORDER BY i.invoiceDate DESC")
    List<Invoice> findAllWithItems();

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.invoiceItems LEFT JOIN FETCH i.customer WHERE i.id = :id")
    Optional<Invoice> findByIdWithItems(@Param("id") UUID id);

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.invoiceItems WHERE i.customer = :customer ORDER BY i.invoiceDate DESC")
    List<Invoice> findByCustomer(@Param("customer") Customer customer);

    /**
     * Findet überfällige Rechnungen
     */
    @Query("SELECT i FROM Invoice i WHERE i.status != 'PAID' AND i.status != 'CANCELLED' AND i.dueDate < CURRENT_DATE")
    List<Invoice> findOverdueInvoices();

    /**
     * Findet die höchste Rechnungsnummer für die Generierung der nächsten Nummer
     */
    @Query("SELECT MAX(i.invoiceNumber) FROM Invoice i WHERE i.invoiceNumber LIKE :prefix%")
    Optional<String> findHighestInvoiceNumberWithPrefix(String prefix);

    @Query("SELECT i FROM Invoice i WHERE i.subscription.id IN :subscriptionIds")
    List<Invoice> findBySubscriptionIds(@Param("subscriptionIds") List<UUID> subscriptionIds);

    @Query("SELECT i FROM Invoice i WHERE i.id NOT IN (SELECT DISTINCT oi.invoice.id FROM OpenItem oi WHERE oi.invoice IS NOT NULL)")
    List<Invoice> findInvoicesWithoutOpenItems();


    /**
     * Findet die letzten 5 Invoices nach Rechnungsdatum
     */
    List<Invoice> findTop5ByOrderByInvoiceDateDesc();

    /**
     * Alternative nach Erstellungsdatum
     */
    @Query("SELECT i FROM Invoice i ORDER BY i.createdAt DESC")
    List<Invoice> findTop5ByOrderByCreatedAtDesc();

    /**
     * Summiert Gesamtbeträge nach Status
     */
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") Invoice.InvoiceStatus status);

    /**
     * Findet Invoices eines bestimmten Kunden
     */
    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId")
    List<Invoice> findByCustomerId(@Param("customerId") Long customerId);

    /**
     * Findet Invoices mit bestimmtem Status
     */
    @Query("SELECT i FROM Invoice i WHERE i.status = :status")
    List<Invoice> findByStatus(@Param("status") Invoice.InvoiceStatus status);

}
