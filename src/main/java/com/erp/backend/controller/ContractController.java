package com.erp.backend.controller;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.ContractStatus;
import com.erp.backend.dto.ContractDTO;
import com.erp.backend.mapper.ContractMapper;
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
    public ResponseEntity<List<ContractDTO>> getAllContracts(
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

            List<ContractDTO> dtoList = contractPage.getContent().stream()
                    .map(ContractMapper::toDTO)
                    .toList();

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(contractPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(contractPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(dtoList);
        } else {
            List<ContractDTO> dtoList = service.getAllContracts().stream()
                    .map(ContractMapper::toDTO)
                    .toList();

            return ResponseEntity.ok(dtoList);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractDTO> getContractById(@PathVariable UUID id) {
        logger.info("GET /api/contracts/{} - Fetching contract by ID", id);
        return service.getContractById(id)
                .map(contract -> {
                    logger.debug("Contract found: {}", contract.getContractNumber());
                    return ResponseEntity.ok(ContractMapper.toDTO(contract));
                })
                .orElseGet(() -> {
                    logger.warn("Contract with ID {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/by-number/{contractNumber}")
    public ResponseEntity<ContractDTO> getContractByNumber(@PathVariable String contractNumber) {
        logger.info("GET /api/contracts/by-number/{} - Fetching contract by number", contractNumber);
        return service.getContractByNumber(contractNumber)
                .map(contract -> ResponseEntity.ok(ContractMapper.toDTO(contract)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ContractDTO>> getContractsByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        List<Contract> contracts = activeOnly ?
                service.getActiveContractsByCustomer(customerId) :
                service.getContractsByCustomer(customerId);

        return ResponseEntity.ok(
                contracts.stream().map(ContractMapper::toDTO).toList()
        );
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ContractDTO>> getContractsByStatus(@PathVariable ContractStatus status) {
        List<Contract> contracts = service.getContractsByStatus(status);
        return ResponseEntity.ok(
                contracts.stream().map(ContractMapper::toDTO).toList()
        );
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<ContractDTO>> getContractsExpiringInDays(
            @RequestParam(defaultValue = "30") int days) {
        List<Contract> contracts = service.getContractsExpiringInDays(days);
        return ResponseEntity.ok(
                contracts.stream().map(ContractMapper::toDTO).toList()
        );
    }

    @GetMapping("/expired")
    public ResponseEntity<List<ContractDTO>> getExpiredContracts() {
        List<Contract> contracts = service.getExpiredContracts();
        return ResponseEntity.ok(
                contracts.stream().map(ContractMapper::toDTO).toList()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<ContractDTO>> searchContracts(@RequestParam String q) {
        List<Contract> contracts = service.searchContractsByTitle(q);
        return ResponseEntity.ok(
                contracts.stream().map(ContractMapper::toDTO).toList()
        );
    }

    @GetMapping("/with-active-subscriptions")
    public ResponseEntity<List<ContractDTO>> getContractsWithActiveSubscriptions() {
        List<Contract> contracts = service.getContractsWithActiveSubscriptions();
        return ResponseEntity.ok(
                contracts.stream().map(ContractMapper::toDTO).toList()
        );
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getContractCount() {
        return ResponseEntity.ok(service.getTotalContractCount());
    }

    @GetMapping("/customer/{customerId}/active-count")
    public ResponseEntity<Long> getActiveContractCountByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(service.getActiveContractCountByCustomer(customerId));
    }

    @PostMapping
    public ResponseEntity<ContractDTO> createContract(@RequestBody ContractDTO dto) {
        Contract created = service.createContract(ContractMapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(ContractMapper.toDTO(created));
    }


    @PutMapping("/{id}")
    public ResponseEntity<ContractDTO> updateContract(@PathVariable UUID id, @RequestBody ContractDTO dto) {
        logger.info("=== CONTROLLER UPDATE DEBUG ===");
        logger.info("PUT /api/contracts/{} - Updating contract", id);
        logger.info("DTO Customer ID: {}", dto.getCustomerId());
        logger.info("DTO Contract Title: {}", dto.getContractTitle());
        logger.info("DTO Status: {}", dto.getContractStatus());

        // DTO zu Entity konvertieren
        Contract entity = ContractMapper.toEntity(dto);
        entity.setId(id);

        logger.info("Entity nach Mapping - Customer: {}", entity.getCustomer());
        if (entity.getCustomer() != null) {
            logger.info("Entity Customer ID: {}", entity.getCustomer().getId());
        }

        Contract updated = service.updateContract(entity);

        logger.info("Update abgeschlossen - Customer ID: {}", updated.getCustomer().getId());
        logger.info("=== CONTROLLER UPDATE DEBUG END ===");

        return ResponseEntity.ok(ContractMapper.toDTO(updated));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ContractDTO> activateContract(@PathVariable UUID id) {
        Contract activated = service.activateContract(id);
        return ResponseEntity.ok(ContractMapper.toDTO(activated));
    }

    @PatchMapping("/{id}/terminate")
    public ResponseEntity<ContractDTO> terminateContract(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDate) {
        Contract terminated = service.terminateContract(id, terminationDate);
        return ResponseEntity.ok(ContractMapper.toDTO(terminated));
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<ContractDTO> suspendContract(@PathVariable UUID id) {
        Contract suspended = service.suspendContract(id);
        return ResponseEntity.ok(ContractMapper.toDTO(suspended));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContract(@PathVariable UUID id) {
        service.deleteContract(id);
        return ResponseEntity.noContent().build();
    }
}
