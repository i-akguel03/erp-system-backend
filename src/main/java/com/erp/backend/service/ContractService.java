package com.erp.backend.service;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.ContractStatus;
import com.erp.backend.domain.Customer;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public void initTestContracts() {
        if(contractRepository.count() > 0) return; // nur einmal

        List<Customer> customers = customerRepository.findAll();
        if(customers.isEmpty()) return; // Keine Kunden, nichts zu erstellen

        Random random = new Random();

        for(int i = 1; i <= 15; i++) {
            Customer randomCustomer = customers.get(random.nextInt(customers.size()));

            LocalDate startDate = LocalDate.now().minusDays(random.nextInt(365)); // innerhalb letzten Jahres
            Contract contract = new Contract(
                    "Testvertrag " + i,
                    startDate,
                    randomCustomer
            );

            // Eindeutige ID erzwingen (falls nicht automatisch generiert)
            contract.setId(null);

            // Zufälliger Status
            ContractStatus status = ContractStatus.values()[random.nextInt(ContractStatus.values().length)];
            contract.setContractStatus(status);

            // Optional: Enddatum bei TERMINATED setzen
            if(status == ContractStatus.TERMINATED) {
                contract.setEndDate(startDate.plusMonths(random.nextInt(12) + 1));
            }

            // Eindeutige Vertragsnummer generieren
            contract.setContractNumber(generateContractNumber());

            contractRepository.save(contract);
        }

        logger.info("Testcontracts initialized: {}", contractRepository.count());
    }


    @Transactional(readOnly = true)
    public List<Contract> getAllContracts() {
        List<Contract> contracts = contractRepository.findAll();
        logger.info("Fetched {} contracts", contracts.size());
        return contracts;
    }

    @Transactional(readOnly = true)
    public Page<Contract> getAllContracts(Pageable pageable) {
        Page<Contract> contracts = contractRepository.findAll(pageable);
        logger.info("Fetched {} contracts (page {}/{})",
                contracts.getNumberOfElements(), contracts.getNumber() + 1, contracts.getTotalPages());
        return contracts;
    }

    @Transactional(readOnly = true)
    public Optional<Contract> getContractById(UUID id) {
        Optional<Contract> contract = contractRepository.findById(id);
        if (contract.isPresent()) {
            logger.info("Found contract with id={}", id);
        } else {
            logger.warn("No contract found with id={}", id);
        }
        return contract;
    }

    @Transactional(readOnly = true)
    public Optional<Contract> getContractByNumber(String contractNumber) {
        Optional<Contract> contract = contractRepository.findByContractNumber(contractNumber);
        logger.info("Search for contract with number={}: {}", contractNumber, contract.isPresent() ? "found" : "not found");
        return contract;
    }

    @Transactional(readOnly = true)
    public List<Contract> getContractsByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        List<Contract> contracts = contractRepository.findByCustomer(customer);
        logger.info("Found {} contracts for customer {}", contracts.size(), customerId);
        return contracts;
    }

    @Transactional(readOnly = true)
    public List<Contract> getActiveContractsByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        List<Contract> contracts = contractRepository.findByCustomerAndContractStatus(customer, ContractStatus.ACTIVE);
        logger.info("Found {} active contracts for customer {}", contracts.size(), customerId);
        return contracts;
    }

    @Transactional(readOnly = true)
    public List<Contract> getContractsByStatus(ContractStatus status) {
        List<Contract> contracts = contractRepository.findByContractStatus(status);
        logger.info("Found {} contracts with status {}", contracts.size(), status);
        return contracts;
    }

    @Transactional(readOnly = true)
    public List<Contract> getContractsExpiringInDays(int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        List<Contract> contracts = contractRepository.findContractsExpiringBetween(startDate, endDate);
        logger.info("Found {} contracts expiring in next {} days", contracts.size(), days);
        return contracts;
    }

    @Transactional(readOnly = true)
    public List<Contract> getExpiredContracts() {
        List<Contract> contracts = contractRepository.findExpiredContracts(LocalDate.now());
        logger.info("Found {} expired contracts", contracts.size());
        return contracts;
    }

    @Transactional(readOnly = true)
    public List<Contract> searchContractsByTitle(String title) {
        List<Contract> contracts = contractRepository.findByContractTitleContainingIgnoreCase(title);
        logger.info("Found {} contracts matching title search: '{}'", contracts.size(), title);
        return contracts;
    }

    public Contract createContract(Contract contract) {
        validateContractForCreation(contract);

        // Keine ID setzen, wird von DB generiert
        contract.setId(null);

        // Vertragsnummer generieren
        contract.setContractNumber(generateContractNumber());

        // Status setzen falls nicht vorhanden
        if (contract.getContractStatus() == null) {
            contract.setContractStatus(ContractStatus.ACTIVE);
        }

        Contract saved = contractRepository.save(contract);
        logger.info("Created new contract: id={}, contractNumber={}, customer={}",
                saved.getId(), saved.getContractNumber(), saved.getCustomer().getId());
        return saved;
    }

    public Contract updateContract(Contract contract) {
        if (contract.getId() == null) {
            throw new IllegalArgumentException("Contract ID cannot be null for update");
        }

        if (!contractRepository.existsById(contract.getId())) {
            throw new IllegalArgumentException("Contract not found with ID: " + contract.getId());
        }

        Contract saved = contractRepository.save(contract);
        logger.info("Updated contract: id={}, contractNumber={}", saved.getId(), saved.getContractNumber());
        return saved;
    }

    public Contract activateContract(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with ID: " + contractId));

        contract.setContractStatus(ContractStatus.ACTIVE);
        if (contract.getStartDate() == null) {
            contract.setStartDate(LocalDate.now());
        }

        Contract saved = contractRepository.save(contract);
        logger.info("Activated contract: id={}, contractNumber={}", saved.getId(), saved.getContractNumber());
        return saved;
    }

    public Contract terminateContract(UUID contractId, LocalDate terminationDate) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with ID: " + contractId));

        contract.setContractStatus(ContractStatus.TERMINATED);
        contract.setEndDate(terminationDate != null ? terminationDate : LocalDate.now());

        Contract saved = contractRepository.save(contract);
        logger.info("Terminated contract: id={}, contractNumber={}, endDate={}",
                saved.getId(), saved.getContractNumber(), saved.getEndDate());
        return saved;
    }

    public Contract suspendContract(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with ID: " + contractId));

        contract.setContractStatus(ContractStatus.SUSPENDED);

        Contract saved = contractRepository.save(contract);
        logger.info("Suspended contract: id={}, contractNumber={}", saved.getId(), saved.getContractNumber());
        return saved;
    }

    public void deleteContract(UUID id) {
        if (!contractRepository.existsById(id)) {
            throw new IllegalArgumentException("Contract not found with ID: " + id);
        }
        contractRepository.deleteById(id);
        logger.info("Deleted contract with id={}", id);
    }

    @Transactional(readOnly = true)
    public long getTotalContractCount() {
        long count = contractRepository.count();
        logger.info("Total contract count: {}", count);
        return count;
    }

    @Transactional(readOnly = true)
    public Long getActiveContractCountByCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        Long count = contractRepository.countActiveContractsByCustomer(customer);
        logger.info("Active contract count for customer {}: {}", customerId, count);
        return count;
    }

    @Transactional(readOnly = true)
    public List<Contract> getContractsWithActiveSubscriptions() {
        List<Contract> contracts = contractRepository.findContractsWithActiveSubscriptions();
        logger.info("Found {} contracts with active subscriptions", contracts.size());
        return contracts;
    }

    // Private Hilfsmethoden

    private void validateContractForCreation(Contract contract) {
        if (contract.getContractTitle() == null || contract.getContractTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Contract title is required");
        }
        if (contract.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (contract.getCustomer() == null || contract.getCustomer().getId() == null) {
            throw new IllegalArgumentException("Customer is required");
        }

        // Kunde muss existieren
        if (!customerRepository.existsById(contract.getCustomer().getId())) {
            throw new IllegalArgumentException("Customer not found with ID: " + contract.getCustomer().getId());
        }
    }

    private String generateContractNumber() {
        String prefix = "CT";
        String year = String.valueOf(LocalDate.now().getYear());
        String randomPart;
        String contractNumber;

        do {
            int number = (int) (Math.random() * 999999) + 1;
            randomPart = String.format("%06d", number);
            contractNumber = prefix + year + randomPart;
        } while (contractRepository.findByContractNumber(contractNumber).isPresent());

        return contractNumber;
    }
}