package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.dto.SubscriptionDto;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.DueScheduleRepository;
import com.erp.backend.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final ContractRepository contractRepository;
    private final DueScheduleRepository dueScheduleRepository;
    private final NumberGeneratorService numberGeneratorService;

    // Konfigurierbarer Default für Fälligkeitsmonate
    @Value("${app.billing.default-due-months:12}")
    private int defaultDueMonths;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               ContractRepository contractRepository,
                               DueScheduleRepository dueScheduleRepository,
                               NumberGeneratorService numberGeneratorService) {
        this.subscriptionRepository = subscriptionRepository;
        this.contractRepository = contractRepository;
        this.dueScheduleRepository = dueScheduleRepository;
        this.numberGeneratorService = numberGeneratorService;
    }

    // ================= DTO Mapping =================
    public SubscriptionDto toDto(Subscription s) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setId(s.getId());
        dto.setSubscriptionNumber(s.getSubscriptionNumber());
        dto.setProductId(s.getProductId());
        dto.setProductName(s.getProductName());
        dto.setMonthlyPrice(s.getMonthlyPrice());
        dto.setStartDate(s.getStartDate());
        dto.setEndDate(s.getEndDate());
        dto.setBillingCycle(s.getBillingCycle());
        dto.setSubscriptionStatus(s.getSubscriptionStatus());
        dto.setAutoRenewal(Boolean.TRUE.equals(s.getAutoRenewal()));
        dto.setContractId(s.getContract() != null ? s.getContract().getId() : null);
        return dto;
    }

    // ================= GET =================
    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public Page<Subscription> getAllSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAll(pageable);
    }

    public Optional<Subscription> getSubscriptionById(UUID id) {
        return subscriptionRepository.findById(id);
    }

    public Optional<Subscription> getSubscriptionByNumber(String subscriptionNumber) {
        return subscriptionRepository.findBySubscriptionNumber(subscriptionNumber);
    }

    public List<Subscription> getSubscriptionsByContract(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found"));
        return subscriptionRepository.findByContract(contract);
    }

    public List<Subscription> getActiveSubscriptionsByContract(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found"));
        return subscriptionRepository.findByContractAndSubscriptionStatus(contract, SubscriptionStatus.ACTIVE);
    }

    public List<Subscription> getSubscriptionsByCustomer(UUID customerId) {
        return subscriptionRepository.findByContractCustomerId(customerId);
    }

    public List<Subscription> getActiveSubscriptionsByCustomer(UUID customerId) {
        return subscriptionRepository.findByContractCustomerIdAndSubscriptionStatus(customerId, SubscriptionStatus.ACTIVE);
    }

    public List<Subscription> getSubscriptionsByStatus(SubscriptionStatus status) {
        return subscriptionRepository.findBySubscriptionStatus(status);
    }

    public BigDecimal getTotalActiveRevenue() {
        BigDecimal revenue = subscriptionRepository.calculateTotalActiveRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public BigDecimal getActiveRevenueByCustomer(UUID customerId) {
        BigDecimal revenue = subscriptionRepository.calculateActiveRevenueByCustomer(customerId);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public long getTotalSubscriptionCount() {
        return subscriptionRepository.count();
    }

    public long getSubscriptionCountByStatus(SubscriptionStatus status) {
        return subscriptionRepository.countBySubscriptionStatus(status);
    }

    // ================= CREATE / UPDATE =================
    @Transactional
    public Subscription createSubscriptionFromDto(SubscriptionDto dto) {
        return createSubscriptionFromDto(dto, null);
    }

    @Transactional
    public Subscription createSubscriptionFromDto(SubscriptionDto dto, Integer dueMonths) {
        Subscription subscription = new Subscription();

        // SubscriptionNumber generieren, falls leer
        if (dto.getSubscriptionNumber() == null || dto.getSubscriptionNumber().isBlank()) {
            dto.setSubscriptionNumber(generateSubscriptionNumber());
        }

        // monthlyPrice setzen, falls null
        if (dto.getMonthlyPrice() == null) {
            dto.setMonthlyPrice(BigDecimal.valueOf(10)); // Defaultwert
        }

        // startDate auf heute setzen, falls null
        if (dto.getStartDate() == null) {
            dto.setStartDate(LocalDate.now());
        }

        // endDate optional auf 1 Jahr später
        if (dto.getEndDate() == null) {
            dto.setEndDate(dto.getStartDate().plusYears(1));
        }

        // Defaults setzen
        if (dto.getBillingCycle() == null) {
            dto.setBillingCycle(BillingCycle.MONTHLY);
        }
        if (dto.getSubscriptionStatus() == null) {
            dto.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        }

        // Subscription speichern
        Subscription saved = saveFromDto(subscription, dto);

        // ===== Fälligkeitspläne automatisch erstellen =====
        int monthsToGenerate = dueMonths != null ? dueMonths : defaultDueMonths;
        createDueSchedulesForSubscription(saved, monthsToGenerate);

        logger.info("Created subscription {} with {} due schedules",
                saved.getSubscriptionNumber(), monthsToGenerate);

        return saved;
    }

    /**
     * Erzeugt automatisch DueSchedules für die Subscription.
     *
     * WICHTIGE REGELN:
     * - Erste Fälligkeit beginnt am Abo-Startdatum
     * - Fälligkeiten sind nur Terminpläne (keine Beträge)
     * - Status ist immer ACTIVE (außer bei deaktivierten Abos)
     * - Nur der Rechnungslauf darf Status auf COMPLETED setzen
     *
     * @param subscription Das Abonnement
     * @param months Anzahl Monate für die Fälligkeitspläne
     */
    private void createDueSchedulesForSubscription(Subscription subscription, int months) {
        LocalDate subscriptionStart = subscription.getStartDate();
        LocalDate subscriptionEnd = subscription.getEndDate();

        logger.info("Creating {} due schedules for subscription {} starting from {}",
                months, subscription.getSubscriptionNumber(), subscriptionStart);

        List<DueSchedule> schedules = new ArrayList<>();
        LocalDate currentPeriodStart = subscriptionStart;

        for (int i = 0; i < months; i++) {
            // Prüfe ob wir über das Abo-Ende hinausgehen würden
            if (currentPeriodStart.isAfter(subscriptionEnd)) {
                logger.info("Stopping schedule generation at {} months due to subscription end date", i);
                break;
            }

            // Periode Ende berechnen basierend auf BillingCycle
            LocalDate periodEnd = calculatePeriodEnd(currentPeriodStart, subscription.getBillingCycle());

            // Sicherstellen, dass die Periode nicht über das Subscription-Ende hinausgeht
            if (periodEnd.isAfter(subscriptionEnd)) {
                periodEnd = subscriptionEnd;
            }

            // Fälligkeitsdatum = erster Tag der Periode (sofort fällig)
            LocalDate dueDate = currentPeriodStart;

            DueSchedule schedule = new DueSchedule();
            schedule.setDueNumber(numberGeneratorService.generateDueNumber());
            schedule.setSubscription(subscription);
            schedule.setPeriodStart(currentPeriodStart);
            schedule.setPeriodEnd(periodEnd);
            schedule.setDueDate(dueDate);

            // Status abhängig vom Abo-Status
            if (subscription.getSubscriptionStatus() == SubscriptionStatus.ACTIVE) {
                schedule.setStatus(DueStatus.ACTIVE);
            } else {
                schedule.setStatus(DueStatus.PAUSED);
            }

            schedules.add(schedule);

            // Nächste Periode berechnen
            currentPeriodStart = calculateNextPeriodStart(currentPeriodStart, subscription.getBillingCycle());

            logger.debug("Created schedule {} for period {} to {} (due: {})",
                    schedule.getDueNumber(), schedule.getPeriodStart(),
                    schedule.getPeriodEnd(), schedule.getDueDate());
        }

        // Alle Fälligkeitspläne in einem Batch speichern
        List<DueSchedule> savedSchedules = dueScheduleRepository.saveAll(schedules);

        logger.info("Successfully created {} due schedules for subscription {}",
                savedSchedules.size(), subscription.getSubscriptionNumber());
    }

    /**
     * Berechnet das Perioden-Ende basierend auf Billing-Zyklus
     */
    private LocalDate calculatePeriodEnd(LocalDate periodStart, BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> periodStart.plusMonths(1).minusDays(1);
            case QUARTERLY -> periodStart.plusMonths(3).minusDays(1);
            case ANNUALLY -> periodStart.plusYears(1).minusDays(1);
            case SEMI_ANNUALLY -> periodStart.plusMonths(6).minusDays(1);
        };
    }

    /**
     * Berechnet den Start der nächsten Periode
     */
    private LocalDate calculateNextPeriodStart(LocalDate currentPeriodStart, BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> currentPeriodStart.plusMonths(1);
            case QUARTERLY -> currentPeriodStart.plusMonths(3);
            case ANNUALLY -> currentPeriodStart.plusYears(1);
            case SEMI_ANNUALLY -> currentPeriodStart.plusMonths(6);
        };
    }

    /**
     * Erstellt zusätzliche Fälligkeitspläne für ein bestehendes Abonnement
     */
    @Transactional
    public void generateAdditionalDueSchedules(UUID subscriptionId, int additionalMonths) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        // Finde die letzte bestehende Fälligkeit
        Optional<DueSchedule> lastSchedule = dueScheduleRepository
                .findBySubscriptionIdOrderByPeriodEndDesc(subscriptionId)
                .stream()
                .findFirst();

        LocalDate nextPeriodStart;
        if (lastSchedule.isPresent()) {
            // Starte nach der letzten Periode
            nextPeriodStart = lastSchedule.get().getPeriodEnd().plusDays(1);
        } else {
            // Falls keine Fälligkeiten vorhanden, starte am Abo-Beginn
            nextPeriodStart = subscription.getStartDate();
        }

        logger.info("Generating {} additional due schedules for subscription {} starting from {}",
                additionalMonths, subscription.getSubscriptionNumber(), nextPeriodStart);

        createDueSchedulesFromDate(subscription, nextPeriodStart, additionalMonths);
    }

    /**
     * Hilfsmethode zum Erstellen von Fälligkeitsplänen ab einem bestimmten Datum
     */
    private void createDueSchedulesFromDate(Subscription subscription, LocalDate startDate, int months) {
        List<DueSchedule> schedules = new ArrayList<>();
        LocalDate currentPeriodStart = startDate;
        LocalDate subscriptionEnd = subscription.getEndDate();

        for (int i = 0; i < months; i++) {
            if (currentPeriodStart.isAfter(subscriptionEnd)) {
                break;
            }

            LocalDate periodEnd = calculatePeriodEnd(currentPeriodStart, subscription.getBillingCycle());
            if (periodEnd.isAfter(subscriptionEnd)) {
                periodEnd = subscriptionEnd;
            }

            DueSchedule schedule = new DueSchedule();
            schedule.setDueNumber(numberGeneratorService.generateDueNumber());
            schedule.setSubscription(subscription);
            schedule.setPeriodStart(currentPeriodStart);
            schedule.setPeriodEnd(periodEnd);
            schedule.setDueDate(currentPeriodStart);
            schedule.setStatus(subscription.getSubscriptionStatus() == SubscriptionStatus.ACTIVE ?
                    DueStatus.ACTIVE : DueStatus.PAUSED);

            schedules.add(schedule);
            currentPeriodStart = calculateNextPeriodStart(currentPeriodStart, subscription.getBillingCycle());
        }

        dueScheduleRepository.saveAll(schedules);
        logger.info("Created {} additional due schedules", schedules.size());
    }

    @Transactional
    public Subscription updateSubscriptionFromDto(SubscriptionDto dto) {
        Subscription subscription = subscriptionRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        // Bei Status-Änderungen auch die Fälligkeiten anpassen
        SubscriptionStatus oldStatus = subscription.getSubscriptionStatus();
        Subscription updated = saveFromDto(subscription, dto);

        // Wenn Status geändert wurde, auch die offenen Fälligkeiten anpassen
        if (oldStatus != updated.getSubscriptionStatus()) {
            updateDueScheduleStatusForSubscription(updated.getId(), updated.getSubscriptionStatus());
        }

        return updated;
    }

    /**
     * Aktualisiert den Status aller offenen Fälligkeiten eines Abonnements
     */
    private void updateDueScheduleStatusForSubscription(UUID subscriptionId, SubscriptionStatus subscriptionStatus) {
        List<DueSchedule> activeDueSchedules = dueScheduleRepository
                .findBySubscriptionIdAndStatus(subscriptionId, DueStatus.ACTIVE);

        for (DueSchedule schedule : activeDueSchedules) {
            if (subscriptionStatus == SubscriptionStatus.ACTIVE) {
                schedule.setStatus(DueStatus.ACTIVE);
            } else if (subscriptionStatus == SubscriptionStatus.PAUSED) {
                schedule.setStatus(DueStatus.PAUSED);
            } else if (subscriptionStatus == SubscriptionStatus.CANCELLED) {
                // Stornierte Abos: Fälligkeiten pausieren (nicht löschen)
                schedule.setStatus(DueStatus.SUSPENDED);
            }
        }

        dueScheduleRepository.saveAll(activeDueSchedules);
        logger.info("Updated {} due schedules to match subscription status {}",
                activeDueSchedules.size(), subscriptionStatus);
    }

    private Subscription saveFromDto(Subscription subscription, SubscriptionDto dto) {
        subscription.setSubscriptionNumber(dto.getSubscriptionNumber());
        subscription.setProductId(dto.getProductId());
        subscription.setProductName(dto.getProductName());
        subscription.setMonthlyPrice(dto.getMonthlyPrice());
        subscription.setStartDate(dto.getStartDate());
        subscription.setEndDate(dto.getEndDate());
        subscription.setBillingCycle(dto.getBillingCycle());
        subscription.setSubscriptionStatus(dto.getSubscriptionStatus());
        subscription.setAutoRenewal(dto.isAutoRenewal());

        if (dto.getContractId() != null) {
            Contract contract = contractRepository.findById(dto.getContractId())
                    .orElseThrow(() -> new IllegalArgumentException("Contract not found"));
            subscription.setContract(contract);
        } else {
            subscription.setContract(null);
        }

        return subscriptionRepository.save(subscription);
    }

    // ================= PATCH Actions =================
    @Transactional
    public Subscription activateSubscription(UUID id) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        s.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        Subscription saved = subscriptionRepository.save(s);

        // Fälligkeiten reaktivieren
        updateDueScheduleStatusForSubscription(id, SubscriptionStatus.ACTIVE);

        return saved;
    }

    @Transactional
    public Subscription cancelSubscription(UUID id, LocalDate cancellationDate) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        s.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
        if (cancellationDate != null) s.setEndDate(cancellationDate);
        Subscription saved = subscriptionRepository.save(s);

        // Fälligkeiten suspendieren
        updateDueScheduleStatusForSubscription(id, SubscriptionStatus.CANCELLED);

        return saved;
    }

    @Transactional
    public Subscription pauseSubscription(UUID id) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        s.setSubscriptionStatus(SubscriptionStatus.PAUSED);
        Subscription saved = subscriptionRepository.save(s);

        // Fälligkeiten pausieren
        updateDueScheduleStatusForSubscription(id, SubscriptionStatus.PAUSED);

        return saved;
    }

    @Transactional
    public Subscription renewSubscription(UUID id, LocalDate newEndDate) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        s.setEndDate(newEndDate);
        s.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        Subscription saved = subscriptionRepository.save(s);

        // Fälligkeiten reaktivieren
        updateDueScheduleStatusForSubscription(id, SubscriptionStatus.ACTIVE);

        return saved;
    }

    // ================= DELETE =================
    @Transactional
    public void deleteSubscription(UUID id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subscription not found with ID: " + id
                ));

        // Prüfen, ob bereits abgerechnete Fälligkeiten existieren (COMPLETED)
        long completedSchedules = dueScheduleRepository
                .countBySubscriptionIdAndStatus(id, DueStatus.COMPLETED);

        if (completedSchedules > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete subscription with completed billing schedules (id=" + id + ")"
            );
        }

        // Alle offenen Fälligkeiten löschen
        List<DueSchedule> openSchedules = dueScheduleRepository.findBySubscriptionId(id);
        dueScheduleRepository.deleteAll(openSchedules);

        logger.info("Deleted {} due schedules for subscription {}",
                openSchedules.size(), subscription.getSubscriptionNumber());

        subscriptionRepository.delete(subscription);
    }

    // ================= SubscriptionNumber Generator =================
    private String generateSubscriptionNumber() {
        String prefix = "SUB";
        String year = String.valueOf(LocalDate.now().getYear());
        String subscriptionNumber;

        do {
            int number = (int) (Math.random() * 999999) + 1; // 1–999999
            subscriptionNumber = prefix + year + String.format("%06d", number);
        } while (subscriptionRepository.findBySubscriptionNumber(subscriptionNumber).isPresent());

        return subscriptionNumber;
    }
}