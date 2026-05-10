package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.dto.ContractRenewalRequest;
import com.erp.backend.dto.ContractRenewalResult;
import com.erp.backend.dto.RenewalBatchResult;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ContractRenewalService {

    private static final Logger logger = LoggerFactory.getLogger(ContractRenewalService.class);

    private final ContractRepository contractRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    @Value("${app.renewal.default-extension-months:12}")
    private int defaultExtensionMonths;

    @Value("${app.renewal.batch-lookahead-days:30}")
    private int batchLookaheadDays;

    public ContractRenewalService(ContractRepository contractRepository,
                                   SubscriptionRepository subscriptionRepository,
                                   SubscriptionService subscriptionService) {
        this.contractRepository = contractRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
    }

    /**
     * Verlängert einen einzelnen Vertrag und alle Abonnements mit autoRenewal=true.
     * Funktioniert für ACTIVE, EXPIRED, SUSPENDED und TERMINATED Verträge.
     * CANCELLED Verträge können nicht verlängert werden.
     */
    @Transactional
    public ContractRenewalResult renewContract(UUID contractId, ContractRenewalRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Vertrag nicht gefunden: " + contractId));

        if (contract.getContractStatus() == ContractStatus.CANCELLED) {
            throw new BusinessLogicException("Stornierte Verträge können nicht verlängert werden.");
        }

        LocalDate oldEndDate = contract.getEndDate();
        LocalDate newEndDate = resolveNewEndDate(oldEndDate, request);
        LocalDate today = LocalDate.now();

        if (!newEndDate.isAfter(oldEndDate != null ? oldEndDate : today)) {
            throw new BusinessLogicException(
                    "Neues Enddatum (" + newEndDate + ") muss nach dem aktuellen Enddatum liegen.");
        }

        contract.setEndDate(newEndDate);
        if (contract.getContractStatus() == ContractStatus.EXPIRED) {
            contract.setContractStatus(ContractStatus.ACTIVE);
        }
        contractRepository.save(contract);
        logger.info("Contract {} verlängert: {} -> {}", contract.getContractNumber(), oldEndDate, newEndDate);

        int subscriptionsRenewed = 0;
        int dueSchedulesCreated = 0;

        List<Subscription> subscriptions = subscriptionRepository.findByContract(contract);
        for (Subscription sub : subscriptions) {
            if (!Boolean.TRUE.equals(sub.getAutoRenewal())) {
                continue;
            }
            if (sub.getSubscriptionStatus() == SubscriptionStatus.CANCELLED) {
                continue;
            }

            sub.setEndDate(newEndDate);
            if (sub.getSubscriptionStatus() == SubscriptionStatus.EXPIRED) {
                sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            }
            subscriptionRepository.save(sub);

            int created = subscriptionService.generateDueSchedulesUntilDate(sub.getId(), newEndDate);
            dueSchedulesCreated += created;
            subscriptionsRenewed++;

            logger.info("Subscription {} verlängert, {} neue Fälligkeitspläne erstellt",
                    sub.getSubscriptionNumber(), created);
        }

        return new ContractRenewalResult(
                contractId, contract.getContractNumber(), contract.getContractTitle(),
                oldEndDate, newEndDate, subscriptionsRenewed, dueSchedulesCreated, true, null);
    }

    /**
     * Verlängerungslauf: Verlängert alle Verträge mit renewable=true,
     * deren Enddatum bereits abgelaufen ist oder innerhalb der nächsten
     * app.renewal.batch-lookahead-days Tage liegt.
     */
    @Transactional
    public RenewalBatchResult runRenewalBatch() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(batchLookaheadDays);

        List<Contract> candidates = contractRepository.findRenewableContracts(threshold);
        logger.info("Verlängerungslauf gestartet: {} Verträge gefunden (threshold={})", candidates.size(), threshold);

        List<ContractRenewalResult> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;

        for (Contract contract : candidates) {
            try {
                ContractRenewalRequest request = new ContractRenewalRequest();
                LocalDate baseDate = (contract.getEndDate() != null && !contract.getEndDate().isBefore(today))
                        ? contract.getEndDate()
                        : today;
                request.setNewEndDate(baseDate.plusMonths(defaultExtensionMonths));

                ContractRenewalResult result = renewContract(contract.getId(), request);
                results.add(result);
                successful++;
            } catch (Exception e) {
                logger.error("Fehler bei Verlängerung von Vertrag {}: {}",
                        contract.getContractNumber(), e.getMessage());
                results.add(ContractRenewalResult.failure(
                        contract.getId(), contract.getContractNumber(),
                        contract.getContractTitle(), e.getMessage()));
                failed++;
            }
        }

        logger.info("Verlängerungslauf abgeschlossen: {} erfolgreich, {} fehlgeschlagen", successful, failed);
        return new RenewalBatchResult(candidates.size(), successful, failed, today, results);
    }

    private LocalDate resolveNewEndDate(LocalDate currentEndDate, ContractRenewalRequest request) {
        if (request != null && request.getNewEndDate() != null) {
            return request.getNewEndDate();
        }
        int months = (request != null && request.getExtensionMonths() != null)
                ? request.getExtensionMonths()
                : defaultExtensionMonths;
        LocalDate base = (currentEndDate != null && !currentEndDate.isBefore(LocalDate.now()))
                ? currentEndDate
                : LocalDate.now();
        return base.plusMonths(months);
    }
}
