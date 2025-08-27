package com.erp.backend.repository;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.ContractStatus;
import com.erp.backend.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {

    // Suche nach Vertragsnummer
    Optional<Contract> findByContractNumber(String contractNumber);

    // Alle Verträge eines Kunden
    List<Contract> findByCustomer(Customer customer);

    // Verträge nach Status
    List<Contract> findByContractStatus(ContractStatus status);

    // Aktive Verträge eines Kunden
    List<Contract> findByCustomerAndContractStatus(Customer customer, ContractStatus status);

    // Verträge die bald ablaufen (nächste X Tage)
    @Query("SELECT c FROM Contract c WHERE c.endDate BETWEEN :startDate AND :endDate")
    List<Contract> findContractsExpiringBetween(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    // Verträge die in einem Zeitraum starten
    List<Contract> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    // Verträge ohne Enddatum (unbefristet)
    List<Contract> findByEndDateIsNull();

    // Abgelaufene Verträge
    @Query("SELECT c FROM Contract c WHERE c.endDate < :currentDate")
    List<Contract> findExpiredContracts(@Param("currentDate") LocalDate currentDate);

    // Suche nach Vertragstitel (case-insensitive)
    List<Contract> findByContractTitleContainingIgnoreCase(String title);

    // Anzahl aktiver Verträge pro Kunde
    @Query("SELECT COUNT(c) FROM Contract c WHERE c.customer = :customer AND c.contractStatus = 'ACTIVE'")
    Long countActiveContractsByCustomer(@Param("customer") Customer customer);

    // Alle Verträge eines Kunden mit Paginierung
    Page<Contract> findByCustomer(Customer customer, Pageable pageable);

    // Verträge mit mindestens einem aktiven Abo
    @Query("SELECT DISTINCT c FROM Contract c JOIN c.subscriptions s WHERE s.subscriptionStatus = 'ACTIVE'")
    List<Contract> findContractsWithActiveSubscriptions();

    // Verträge ohne Abos
    @Query("SELECT c FROM Contract c WHERE c.subscriptions IS EMPTY")
    List<Contract> findContractsWithoutSubscriptions();
}