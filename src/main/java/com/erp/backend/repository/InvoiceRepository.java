package com.erp.backend.repository;

import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    // Eindeutig durch Unique-Constraint
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    // Alle Rechnungen eines Kunden
    List<Invoice> findByCustomer(Customer customer);

    // Nach Status suchen
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);

    // Alle überfälligen Rechnungen (z. B. für Rechnungslauf)
    List<Invoice> findByDueDateBeforeAndStatusNot(LocalDate date, Invoice.InvoiceStatus status);

    // Überfällig UND nicht bezahlt/storniert
    List<Invoice> findByDueDateBeforeAndStatusNotIn(LocalDate date, List<Invoice.InvoiceStatus> excludedStatuses);
}
