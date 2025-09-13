package com.erp.backend.repository;

import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    // Alle Rechnungen eines Kunden
    List<Invoice> findByCustomer(Customer customer);


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
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);

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
}
