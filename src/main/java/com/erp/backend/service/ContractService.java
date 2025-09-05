package com.erp.backend.service;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.ContractStatus;
import com.erp.backend.domain.Customer;
import com.erp.backend.domain.SubscriptionStatus;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ContractService {

    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;

    public ContractService(ContractRepository contractRepository, CustomerRepository customerRepository) {
        this.contractRepository = contractRepository;
        this.customerRepository = customerRepository;
    }

    // --- Testdaten initialisieren ---
    public void initTestContracts() {
        if(contractRepository.count() > 0) return;

        List<Customer> customers = customerRepository.findAll();
        if(customers.isEmpty()) return;

        Random random = new Random();

        for(int i = 1; i <= 15; i++) {
            Customer randomCustomer = customers.get(random.nextInt(customers.size()));
            LocalDate startDate = LocalDate.now().minusDays(random.nextInt(365));
            Contract contract = new Contract(
                    "Testvertrag " + i,
                    startDate,
                    randomCustomer
            );
            contract.setId(null); // ID von DB generieren lassen
            contract.setContractStatus(ContractStatus.values()[random.nextInt(ContractStatus.values().length)]);
            if(contract.getContractStatus() == ContractStatus.TERMINATED) {
                contract.setEndDate(startDate.plusMonths(random.nextInt(12) + 1));
            }
            contract.setContractNumber(generateContractNumber());
            contractRepository.save(contract);
        }

        logger.info("Testcontracts initialized: {}", contractRepository.count());
    }

    // --- CRUD & Read ---
    @Transactional(readOnly = true)
    public List<Contract> getAllContracts() {
        List<Contract> contracts = contractRepository.findAll();
        contracts.forEach(c -> {
            c.getCustomer().getId(); // Customer initialisieren
            c.getSubscriptions().size(); // Subscriptions initialisieren
        });
        return contracts;
    }

    @Transactional(readOnly = true)
    public Page<Contract> getAllContracts(Pageable pageable) {
        return contractRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Contract> getContractById(UUID id) {
        return contractRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Contract> getContractByNumber(String contractNumber) {
        return contractRepository.findByContractNumber(contractNumber);
    }

    @Transactional(readOnly = true)
    public List<Contract> getContractsByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        return contractRepository.findByCustomer(customer);
    }

    @Transactional(readOnly = true)
    public List<Contract> getActiveContractsByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        return contractRepository.findByCustomerAndContractStatus(customer, ContractStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Contract> getContractsByStatus(ContractStatus status) {
        return contractRepository.findByContractStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Contract> getContractsExpiringInDays(int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        return contractRepository.findContractsExpiringBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Contract> getExpiredContracts() {
        return contractRepository.findExpiredContracts(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Contract> searchContractsByTitle(String title) {
        return contractRepository.findByContractTitleContainingIgnoreCase(title);
    }

    @Transactional(readOnly = true)
    public long getTotalContractCount() {
        return contractRepository.count();
    }

    @Transactional(readOnly = true)
    public Long getActiveContractCountByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        return contractRepository.countActiveContractsByCustomer(customer);
    }

    @Transactional(readOnly = true)
    public List<Contract> getContractsWithActiveSubscriptions() {
        return contractRepository.findContractsWithActiveSubscriptions();
    }

    // --- Create / Update ---
    public Contract createContract(Contract contract) {
        validateContractForCreation(contract);
        contract.setId(null);
        if(contract.getContractStatus() == null) contract.setContractStatus(ContractStatus.ACTIVE);
        contract.setContractNumber(generateContractNumber());
        return contractRepository.save(contract);
    }

    public Contract updateContract(Contract contract) {
        logger.info("=== CONTRACT UPDATE DEBUG START ===");
        logger.info("Contract ID: {}", contract.getId());
        logger.info("Contract Title: {}", contract.getContractTitle());

        if(contract.getId() == null) throw new IllegalArgumentException("Contract ID cannot be null for update");
        if(!contractRepository.existsById(contract.getId())) throw new IllegalArgumentException("Contract not found with ID: " + contract.getId());

        // Existierenden Vertrag laden
        Contract existing = contractRepository.findById(contract.getId()).get();
        logger.info("Existing Customer ID: {}", existing.getCustomer().getId());
        logger.info("Existing Customer Name: {} {}", existing.getCustomer().getFirstName(), existing.getCustomer().getLastName());

        // Customer aus dem übergebenen Contract prüfen
        logger.info("New Contract Customer: {}", contract.getCustomer());
        if (contract.getCustomer() != null) {
            logger.info("New Customer ID: {}", contract.getCustomer().getId());
        } else {
            logger.warn("New Contract Customer ist NULL!");
        }

        // KORRIGIERTE Customer-Logik
        if(contract.getCustomer() == null || contract.getCustomer().getId() == null) {
            logger.info("Behalte existierenden Customer");
            contract.setCustomer(existing.getCustomer());
        } else {
            // Neuen Customer aus der Datenbank laden
            UUID newCustomerId = contract.getCustomer().getId();
            logger.info("Lade neuen Customer mit ID: {}", newCustomerId);

            Customer newCustomer = customerRepository.findById(newCustomerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + newCustomerId));

            logger.info("Neuer Customer geladen: {} {}", newCustomer.getFirstName(), newCustomer.getLastName());
            contract.setCustomer(newCustomer);

            // Vergleich für Log
            if (!existing.getCustomer().getId().equals(newCustomer.getId())) {
                logger.info("Customer geändert von {} {} zu {} {}",
                        existing.getCustomer().getFirstName(),
                        existing.getCustomer().getLastName(),
                        newCustomer.getFirstName(),
                        newCustomer.getLastName());
            } else {
                logger.info("Customer bleibt unverändert");
            }
        }

        Contract updated = contractRepository.save(contract);
        logger.info("Contract gespeichert mit Customer ID: {}", updated.getCustomer().getId());
        logger.info("=== CONTRACT UPDATE DEBUG END ===");

        return updated;
    }

    public void deleteContract(UUID id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Contract not found with ID: " + id
                ));

        // Prüfen, ob aktive Subscriptions existieren
        boolean hasActiveSubscriptions = contract.getSubscriptions().stream()
                .anyMatch(sub -> sub.getSubscriptionStatus().equals(SubscriptionStatus.ACTIVE));

        if (hasActiveSubscriptions) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete contract with active subscriptions (id=" + id + ")"
            );
        }

        // Soft delete durchführen
        contractRepository.delete(contract);

        logger.info("Soft-deleted contract with id={}", id);
    }


    // --- Statusänderungen nur mit contractId ---
    public Contract activateContract(UUID contractId) {
        Contract contract = getContractForStatusUpdate(contractId);
        contract.setContractStatus(ContractStatus.ACTIVE);
        if(contract.getStartDate() == null) contract.setStartDate(LocalDate.now());
        return contractRepository.save(contract);
    }

    @Transactional
    public Contract suspendContract(UUID contractId) {
        Contract contract = getContractForStatusUpdate(contractId);

        // Contract auf SUSPENDED setzen
        contract.setContractStatus(ContractStatus.SUSPENDED);

        // Alle aktiven Subscriptions ebenfalls suspendieren
        if (contract.getSubscriptions() != null) {
            contract.getSubscriptions().forEach(subscription -> {
                if (subscription.getSubscriptionStatus().equals(SubscriptionStatus.ACTIVE)) { // nur aktive Subscriptions
                    subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED); // oder active = false
                }
            });
        }

        // Änderungen speichern (Contract + Subscriptions)
        return contractRepository.save(contract);
    }


    @Transactional
    public Contract terminateContract(UUID contractId, LocalDate terminationDate) {
        Contract contract = getContractForStatusUpdate(contractId);

        // Contract terminieren
        contract.setContractStatus(ContractStatus.TERMINATED);
        contract.setEndDate(terminationDate != null ? terminationDate : LocalDate.now());

        // Alle Subscriptions/Abo unter diesem Contract terminieren
        if (contract.getSubscriptions() != null) {
            contract.getSubscriptions().forEach(subscription -> {
                if (subscription.getSubscriptionStatus().equals(SubscriptionStatus.ACTIVE)) { // nur aktive Subscriptions
                    subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED); // oder active = false
                    subscription.setEndDate(contract.getEndDate());
                }
            });
        }

        // Änderungen speichern (Contract + Subscriptions)
        return contractRepository.save(contract);
    }


    // --- Private Hilfsmethoden ---
    private Contract getContractForStatusUpdate(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with ID: " + contractId));
    }

    private void validateContractForCreation(Contract contract) {
        if(contract.getContractTitle() == null || contract.getContractTitle().trim().isEmpty()) throw new IllegalArgumentException("Contract title is required");
        if(contract.getStartDate() == null) throw new IllegalArgumentException("Start date is required");
        if(contract.getCustomer() == null || contract.getCustomer().getId() == null) throw new IllegalArgumentException("Customer is required");
        if(!customerRepository.existsById(contract.getCustomer().getId())) throw new IllegalArgumentException("Customer not found with ID: " + contract.getCustomer().getId());
    }

    private String generateContractNumber() {
        String prefix = "CT";
        String year = String.valueOf(LocalDate.now().getYear());
        String contractNumber;
        do {
            int number = (int) (Math.random() * 999999) + 1;
            contractNumber = prefix + year + String.format("%06d", number);
        } while(contractRepository.findByContractNumber(contractNumber).isPresent());
        return contractNumber;
    }
}
