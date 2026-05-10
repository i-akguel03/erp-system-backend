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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ContractRenewalService {

    private static final Logger logger = LoggerFactory.getLogger(ContractRenewalService.class);

    private final ContractRepository contractRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final VorgangService vorgangService;

    @Value("${app.renewal.default-extension-months:12}")
    private int defaultExtensionMonths;

    @Value("${app.renewal.batch-lookahead-days:30}")
    private int batchLookaheadDays;

    public ContractRenewalService(ContractRepository contractRepository,
                                   SubscriptionRepository subscriptionRepository,
                                   SubscriptionService subscriptionService,
                                   VorgangService vorgangService) {
        this.contractRepository = contractRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
        this.vorgangService = vorgangService;
    }

    /**
     * Verlängert einen einzelnen Vertrag und alle Abonnements mit autoRenewal=true.
     * Legt einen Vorgang an, der Ergebnis und Statistik festhält.
     */
    @Transactional
    public ContractRenewalResult renewContract(UUID contractId, ContractRenewalRequest request, String benutzer) {
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

        Vorgang vorgang = vorgangService.starteVorgang(
                VorgangTyp.VERTRAGSERNEUERUNG,
                "Vertragsverlängerung: " + contract.getContractNumber(),
                "Verlängerung von " + oldEndDate + " bis " + newEndDate,
                benutzer != null ? benutzer : "MANUELL",
                false
        );

        try {
            contract.setEndDate(newEndDate);
            if (contract.getContractStatus() == ContractStatus.EXPIRED) {
                contract.setContractStatus(ContractStatus.ACTIVE);
            }
            contract.setRenewalVorgang(vorgang);
            contract.setLastRenewedAt(LocalDateTime.now());
            contractRepository.save(contract);

            int subscriptionsRenewed = 0;
            int dueSchedulesCreated = 0;

            List<Subscription> subscriptions = subscriptionRepository.findByContract(contract);
            for (Subscription sub : subscriptions) {
                if (!Boolean.TRUE.equals(sub.getAutoRenewal())) continue;
                if (sub.getSubscriptionStatus() == SubscriptionStatus.CANCELLED) continue;

                sub.setEndDate(newEndDate);
                if (sub.getSubscriptionStatus() == SubscriptionStatus.EXPIRED) {
                    sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
                }
                subscriptionRepository.save(sub);

                int created = subscriptionService.generateDueSchedulesUntilDate(sub.getId(), newEndDate);
                dueSchedulesCreated += created;
                subscriptionsRenewed++;

                logger.info("Subscription {} verlängert, {} neue Fälligkeitspläne",
                        sub.getSubscriptionNumber(), created);
            }

            vorgangService.vorgangErfolgreichAbschliessen(
                    vorgang.getId(),
                    subscriptionsRenewed,
                    subscriptionsRenewed,
                    0,
                    null
            );

            logger.info("Vertrag {} verlängert: {} -> {} | Vorgang {}",
                    contract.getContractNumber(), oldEndDate, newEndDate, vorgang.getVorgangsnummer());

            ContractRenewalResult result = new ContractRenewalResult(
                    contractId, contract.getContractNumber(), contract.getContractTitle(),
                    oldEndDate, newEndDate, subscriptionsRenewed, dueSchedulesCreated, true, null);
            result.setVorgangId(vorgang.getId());
            result.setVorgangsnummer(vorgang.getVorgangsnummer());
            return result;

        } catch (Exception e) {
            vorgangService.vorgangMitFehlerAbschliessen(vorgang.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Verlängerungslauf: verarbeitet alle renewable=true Verträge, die ablaufen oder bereits abgelaufen sind.
     * Legt einen übergreifenden Vorgang mit Gesamtstatistik sowie pro Vertrag einen Einzelvorgang an.
     */
    @Transactional
    public RenewalBatchResult runRenewalBatch() {
        return runRenewalBatch("SYSTEM");
    }

    @Transactional
    public RenewalBatchResult runRenewalBatch(String benutzer) {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(batchLookaheadDays);

        List<Contract> candidates = contractRepository.findRenewableContracts(threshold);
        logger.info("Verlängerungslauf gestartet: {} Verträge (threshold={})", candidates.size(), threshold);

        boolean automatisch = "SYSTEM".equals(benutzer);
        Vorgang batchVorgang = vorgangService.starteVorgang(
                VorgangTyp.VERTRAGSERNEUERUNG,
                "Verlängerungslauf",
                "Verlängerungslauf für " + candidates.size() + " Verträge",
                benutzer,
                automatisch
        );

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

                ContractRenewalResult result = renewContract(contract.getId(), request, "SYSTEM");
                results.add(result);
                successful++;
            } catch (Exception e) {
                logger.error("Fehler bei Verlängerung von Vertrag {}: {}",
                        contract.getContractNumber(), e.getMessage());
                ContractRenewalResult failure = ContractRenewalResult.failure(
                        contract.getId(), contract.getContractNumber(),
                        contract.getContractTitle(), e.getMessage());
                results.add(failure);
                failed++;
            }
        }

        vorgangService.vorgangErfolgreichAbschliessen(
                batchVorgang.getId(),
                candidates.size(),
                successful,
                failed,
                null
        );

        logger.info("Verlängerungslauf abgeschlossen: {} erfolgreich, {} fehlgeschlagen | Vorgang {}",
                successful, failed, batchVorgang.getVorgangsnummer());

        RenewalBatchResult batchResult = new RenewalBatchResult(
                candidates.size(), successful, failed, today, results);
        batchResult.setVorgangId(batchVorgang.getId());
        batchResult.setVorgangsnummer(batchVorgang.getVorgangsnummer());
        return batchResult;
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
