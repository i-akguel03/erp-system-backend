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

    // *** WICHTIG: Alle Queries mit JOIN FETCH für Customer ***

    // Überschreibt findAll() mit eager loading
    @Query("SELECT c FROM Contract c JOIN FETCH c.customer")
    List<Contract> findAll();

    @Query("SELECT c FROM Contract c JOIN FETCH c.customer")
    Page<Contract> findAll(Pageable pageable);

    // Suche nach Vertragsnummer mit Customer
    @Query("SELECT c FROM Contract c JOIN FETCH c.customer WHERE c.contractNumber = :contractNumber")
    Optional<Contract> findByContractNumber(@Param("contractNumber") String contractNumber);

    // Alle Verträge eines Kunden
    @Query("SELECT c FROM Contract c JOIN FETCH c.customer WHERE c.customer = :customer")
    List<Contract> findByCustomer(@Param("customer") Customer customer);

    // Verträge nach Status mit Customer
    @Query("SELECT c FROM Contract c JOIN FETCH c.customer WHERE c.contractStatus = :status")
    List<Contract> findByContractStatus(@Param("status") ContractStatus status);

    // Aktive Verträge eines Kunden
    @Query("SELECT c FROM Contract c JOIN FETCH c.customer WHERE c.customer = :customer AND c.contractStatus = :status")
    List<Contract> findByCustomerAndContractStatus(@Param("customer") Customer customer, @Param("status") ContractStatus status);

    // Verträge die bald ablaufen (nächste X Tage)
    @Query("SELECT c FROM Contract c JOIN FETCH c.customer WHERE c.endDate BETWEEN :startDate AND :endDate")
    List<Contract> findContractsExpiringBetween(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    // Verträge die in einem Zeitraum starten
    @Query("SELECT c FROM Contract c JOIN FETCH c.customer WHERE c.startDate BETWEEN :startDate AND :endDate")
    List<Contract> findByStartDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Verträge ohne Enddatum (unbefristet)
    @Query("SELECT c FROM Contract c JOIN FETCH c.customer WHERE c.endDate IS NULL")
    List<Contract> findByEndDateIsNull();

    // Abgelaufene Verträge
    @Query("SELECT c FROM Contract c JOIN FETCH c.customer WHERE c.endDate < :currentDate")
    List<Contract> findExpiredContracts(@Param("currentDate") LocalDate currentDate);

    // Suche nach Vertragstitel (case-insensitive)
    @Query("SELECT c FROM Contract c JOIN FETCH c.customer WHERE LOWER(c.contractTitle) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Contract> findByContractTitleContainingIgnoreCase(@Param("title") String title);

    // Anzahl aktiver Verträge pro Kunde
    @Query("SELECT COUNT(c) FROM Contract c WHERE c.customer = :customer AND c.contractStatus = 'ACTIVE'")
    Long countActiveContractsByCustomer(@Param("customer") Customer customer);

    // Alle Verträge eines Kunden mit Paginierung
    @Query("SELECT c FROM Contract c JOIN FETCH c.customer WHERE c.customer = :customer")
    Page<Contract> findByCustomer(@Param("customer") Customer customer, Pageable pageable);

    // Verträge mit mindestens einem aktiven Abo
    @Query("SELECT DISTINCT c FROM Contract c JOIN FETCH c.customer LEFT JOIN c.subscriptions s WHERE s.subscriptionStatus = 'ACTIVE'")
    List<Contract> findContractsWithActiveSubscriptions();

    // Verträge ohne Abos
    @Query("SELECT c FROM Contract c JOIN FETCH c.customer WHERE c.subscriptions IS EMPTY OR SIZE(c.subscriptions) = 0")
    List<Contract> findContractsWithoutSubscriptions();
}