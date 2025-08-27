package com.erp.backend.controller;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.ContractStatus;
import com.erp.backend.service.ContractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@CrossOrigin
public class ContractController {

    private static final Logger logger = LoggerFactory.getLogger(ContractController.class);

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @PostMapping("/init")
    public ResponseEntity<String> initTestContracts() {
        service.initTestContracts();
        return ResponseEntity.ok("15 Testverträge wurden erstellt.");
    }

    @GetMapping
    public ResponseEntity<List<Contract>> getAllContracts(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logger.info("GET /api/contracts - Fetching contracts (paginated: {})", paginated);

        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Contract> contractPage = service.getAllContracts(pageable);

            logger.debug("Found {} contracts on page {}/{}",
                    contractPage.getNumberOfElements(), page + 1, contractPage.getTotalPages());

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(contractPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(contractPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(contractPage.getContent());
        } else {
            List<Contract> contracts = service.getAllContracts();
            logger.debug("Found {} contracts", contracts.size());
            return ResponseEntity.ok(contracts);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable UUID id) {
        logger.info("GET /api/contracts/{} - Fetching contract by ID", id);
        return service.getContractById(id)
                .map(contract -> {
                    logger.debug("Contract found: {}", contract.getContractNumber());
                    return ResponseEntity.ok(contract);
                })
                .orElseGet(() -> {
                    logger.warn("Contract with ID {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/by-number/{contractNumber}")
    public ResponseEntity<Contract> getContractByNumber(@PathVariable String contractNumber) {
        logger.info("GET /api/contracts/by-number/{} - Fetching contract by number", contractNumber);
        return service.getContractByNumber(contractNumber)
                .map(contract -> {
                    logger.debug("Contract found with number: {}", contractNumber);
                    return ResponseEntity.ok(contract);
                })
                .orElseGet(() -> {
                    logger.warn("Contract with number {} not found", contractNumber);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Contract>> getContractsByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        logger.info("GET /api/contracts/customer/{} - Fetching contracts by customer (activeOnly: {})",
                customerId, activeOnly);

        try {
            List<Contract> contracts = activeOnly ?
                    service.getActiveContractsByCustomer(customerId) :
                    service.getContractsByCustomer(customerId);

            logger.debug("Found {} contracts for customer {}", contracts.size(), customerId);
            return ResponseEntity.ok(contracts);
        } catch (IllegalArgumentException e) {
            logger.error("Customer not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Contract>> getContractsByStatus(@PathVariable ContractStatus status) {
        logger.info("GET /api/contracts/status/{} - Fetching contracts by status", status);
        List<Contract> contracts = service.getContractsByStatus(status);
        logger.debug("Found {} contracts with status {}", contracts.size(), status);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<Contract>> getContractsExpiringInDays(
            @RequestParam(defaultValue = "30") int days) {
        logger.info("GET /api/contracts/expiring?days={} - Fetching contracts expiring in {} days", days, days);
        List<Contract> contracts = service.getContractsExpiringInDays(days);
        logger.debug("Found {} contracts expiring in next {} days", contracts.size(), days);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/expired")
    public ResponseEntity<List<Contract>> getExpiredContracts() {
        logger.info("GET /api/contracts/expired - Fetching expired contracts");
        List<Contract> contracts = service.getExpiredContracts();
        logger.debug("Found {} expired contracts", contracts.size());
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Contract>> searchContracts(@RequestParam String q) {
        logger.info("GET /api/contracts/search?q={} - Searching contracts by title", q);
        List<Contract> contracts = service.searchContractsByTitle(q);
        logger.debug("Found {} contracts matching search term: '{}'", contracts.size(), q);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/with-active-subscriptions")
    public ResponseEntity<List<Contract>> getContractsWithActiveSubscriptions() {
        logger.info("GET /api/contracts/with-active-subscriptions - Fetching contracts with active subscriptions");
        List<Contract> contracts = service.getContractsWithActiveSubscriptions();
        logger.debug("Found {} contracts with active subscriptions", contracts.size());
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getContractCount() {
        logger.info("GET /api/contracts/count - Fetching contract count");
        long count = service.getTotalContractCount();
        logger.debug("Total contract count: {}", count);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/customer/{customerId}/active-count")
    public ResponseEntity<Long> getActiveContractCountByCustomer(@PathVariable UUID customerId) {
        logger.info("GET /api/contracts/customer/{}/active-count - Fetching active contract count", customerId);
        try {
            Long count = service.getActiveContractCountByCustomer(customerId);
            logger.debug("Active contract count for customer {}: {}", customerId, count);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            logger.error("Customer not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Contract> createContract(@RequestBody Contract contract) {
        logger.info("POST /api/contracts - Creating new contract for customer {}",
                contract.getCustomer() != null ? contract.getCustomer().getId() : "null");
        logger.debug("RequestBody {}", contract.toString());

        try {
            Contract created = service.createContract(contract);
            logger.info("Created contract with ID {} and number {}", created.getId(), created.getContractNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error creating contract: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating contract: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contract> updateContract(@PathVariable UUID id, @RequestBody Contract updated) {
        logger.info("PUT /api/contracts/{} - Updating contract", id);

        try {
            updated.setId(id); // Ensure ID is set
            Contract saved = service.updateContract(updated);
            logger.info("Updated contract with ID {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            logger.error("Contract not found or validation error: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating contract: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Contract> activateContract(@PathVariable UUID id) {
        logger.info("PATCH /api/contracts/{}/activate - Activating contract", id);
        try {
            Contract activated = service.activateContract(id);
            logger.info("Activated contract with ID {}", activated.getId());
            return ResponseEntity.ok(activated);
        } catch (IllegalArgumentException e) {
            logger.error("Contract not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error activating contract: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/terminate")
    public ResponseEntity<Contract> terminateContract(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDate) {

        logger.info("PATCH /api/contracts/{}/terminate - Terminating contract (date: {})", id, terminationDate);
        try {
            Contract terminated = service.terminateContract(id, terminationDate);
            logger.info("Terminated contract with ID {} on {}", terminated.getId(), terminated.getEndDate());
            return ResponseEntity.ok(terminated);
        } catch (IllegalArgumentException e) {
            logger.error("Contract not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error terminating contract: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<Contract> suspendContract(@PathVariable UUID id) {
        logger.info("PATCH /api/contracts/{}/suspend - Suspending contract", id);
        try {
            Contract suspended = service.suspendContract(id);
            logger.info("Suspended contract with ID {}", suspended.getId());
            return ResponseEntity.ok(suspended);
        } catch (IllegalArgumentException e) {
            logger.error("Contract not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error suspending contract: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContract(@PathVariable UUID id) {
        logger.info("DELETE /api/contracts/{} - Deleting contract", id);
        try {
            service.deleteContract(id);
            logger.info("Deleted contract with ID {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("Contract not found for deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting contract: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}