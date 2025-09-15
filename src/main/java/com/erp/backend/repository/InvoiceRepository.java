package com.erp.backend.repository;

import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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


    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.invoiceItems LEFT JOIN FETCH i.customer ORDER BY i.invoiceDate DESC")
    List<Invoice> findAllWithItems();

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.invoiceItems LEFT JOIN FETCH i.customer WHERE i.id = :id")
    Optional<Invoice> findByIdWithItems(@Param("id") UUID id);

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.invoiceItems WHERE i.customer = :customer ORDER BY i.invoiceDate DESC")
    List<Invoice> findByCustomer(@Param("customer") Customer customer);

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.invoiceItems WHERE i.status = :status ORDER BY i.invoiceDate DESC")
    List<Invoice> findByStatus(@Param("status") Invoice.InvoiceStatus status);
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

}
