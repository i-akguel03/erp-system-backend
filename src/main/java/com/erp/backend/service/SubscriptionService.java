package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.dto.SubscriptionDto;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.DueScheduleRepository;
import com.erp.backend.repository.SubscriptionRepository;
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

    private final SubscriptionRepository subscriptionRepository;
    private final ContractRepository contractRepository;
    private final DueScheduleRepository dueScheduleRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               ContractRepository contractRepository, DueScheduleRepository dueScheduleRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.contractRepository = contractRepository;
        this.dueScheduleRepository = dueScheduleRepository;
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

        // ===== Fälligkeitspläne erstellen =====
        createDueSchedules(saved);

        return saved;
    }

    /**
     * Erzeugt automatisch DueSchedules für die Subscription entsprechend BillingCycle.
     *
     * Hinweis:
     * - DueSchedules enthalten **keine Beträge**.
     * - Preise werden erst beim Invoice-Generieren aus der Subscription gezogen.
     */
    private void createDueSchedules(Subscription subscription) {
        LocalDate periodStart = subscription.getStartDate();
        LocalDate subscriptionEnd = subscription.getEndDate();

        List<DueSchedule> schedules = new ArrayList<>();

        while (!periodStart.isAfter(subscriptionEnd)) {
            // Periode Ende berechnen basierend auf BillingCycle
            LocalDate periodEnd = calculatePeriodEnd(periodStart, subscription.getBillingCycle());

            // Sicherstellen, dass die Periode nicht über das Subscription-Ende hinausgeht
            if (periodEnd.isAfter(subscriptionEnd)) {
                periodEnd = subscriptionEnd;
            }

            // Fälligkeitsdatum berechnen (normalerweise am Ende der Periode oder mit Zahlungsfrist)
            LocalDate dueDate = calculateDueDate(periodEnd, subscription.getBillingCycle());

            DueSchedule schedule = new DueSchedule();
            schedule.setSubscription(subscription);
            schedule.setPeriodStart(periodStart);   // Periode Start
            schedule.setPeriodEnd(periodEnd);       // Periode Ende
            schedule.setDueDate(dueDate);           // Fälligkeit
            schedule.setStatus(DueStatus.ACTIVE);   // Status explizit setzen

            schedules.add(schedule);

            // Nächste Periode starten
            periodStart = periodEnd.plusDays(1);

            // Abbruch, falls Subscription-Ende erreicht
            if (periodStart.isAfter(subscriptionEnd)) {
                break;
            }
        }

        dueScheduleRepository.saveAll(schedules);
    }

    /**
     * Berechnet das Perioden-Ende basierend auf Billing-Zyklus
     */
    private LocalDate calculatePeriodEnd(LocalDate periodStart, BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> periodStart.plusMonths(1).minusDays(1);
            case QUARTERLY -> periodStart.plusMonths(3).minusDays(1);
            case ANNUALLY -> periodStart.plusYears(1).minusDays(1);
            case SEMI_ANNUALLY -> periodStart.plusYears(1).minusDays(1);
        };
    }

    /**
     * Berechnet das Fälligkeitsdatum (meist Ende der Periode + optional Zahlungsfrist)
     */
    private LocalDate calculateDueDate(LocalDate periodEnd, BillingCycle billingCycle) {
        // Standard: Fällig am Ende der Periode
        // Optional: + Zahlungsfrist (z.B. 14 Tage)
        return periodEnd; // oder periodEnd.plusDays(14) für Zahlungsfrist
    }


    @Transactional
    public Subscription updateSubscriptionFromDto(SubscriptionDto dto) {
        Subscription subscription = subscriptionRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        return saveFromDto(subscription, dto);
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
        return subscriptionRepository.save(s);
    }

    @Transactional
    public Subscription cancelSubscription(UUID id, LocalDate cancellationDate) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        s.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
        if (cancellationDate != null) s.setEndDate(cancellationDate);
        return subscriptionRepository.save(s);
    }

    @Transactional
    public Subscription pauseSubscription(UUID id) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        s.setSubscriptionStatus(SubscriptionStatus.PAUSED);
        return subscriptionRepository.save(s);
    }

    @Transactional
    public Subscription renewSubscription(UUID id, LocalDate newEndDate) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        s.setEndDate(newEndDate);
        s.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        return subscriptionRepository.save(s);
    }

    // ================= DELETE =================
    @Transactional
    public void deleteSubscription(UUID id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subscription not found with ID: " + id
                ));

        // Prüfen, ob offene Fälligkeiten existieren
        boolean hasOpenPaymentSchedules = subscription.getPaymentSchedules().stream()
                .anyMatch(ps -> ps.getStatus() == DueStatus.ACTIVE);

        System.out.println(hasOpenPaymentSchedules);

        if (hasOpenPaymentSchedules) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete subscription with open payment schedules (id=" + id + ")"
            );
        }

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
