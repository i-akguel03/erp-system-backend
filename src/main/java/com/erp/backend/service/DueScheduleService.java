package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.dto.DueScheduleDto;
import com.erp.backend.dto.DueScheduleStatisticsDto;
import com.erp.backend.dto.PaymentDto;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.mapper.DueScheduleMapper;
import com.erp.backend.repository.DueScheduleRepository;
import com.erp.backend.repository.SubscriptionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class DueScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(DueScheduleService.class);

    @Autowired
    private DueScheduleRepository dueScheduleRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private DueScheduleMapper dueScheduleMapper;

    @Autowired
    private NumberGeneratorService numberGeneratorService;

    // ----------------------------------------------------------
    // CRUD Operationen
    // ----------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<DueScheduleDto> getAllDueSchedules(Pageable pageable) {
        return dueScheduleRepository.findAll(pageable)
                .map(dueScheduleMapper::toDto);
    }

    @Transactional(readOnly = true)
    public DueScheduleDto getDueScheduleById(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));
        return dueScheduleMapper.toDto(dueSchedule);
    }

    @Transactional(readOnly = true)
    public DueScheduleDto getDueScheduleByNumber(String dueNumber) {
        DueSchedule dueSchedule = dueScheduleRepository.findByDueNumber(dueNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit Nummer " + dueNumber + " nicht gefunden"));
        return dueScheduleMapper.toDto(dueSchedule);
    }

    public DueScheduleDto createDueSchedule(DueScheduleDto dto) {
        validateDueSchedule(dto);

        Subscription subscription = subscriptionRepository.findById(dto.getSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement mit ID " + dto.getSubscriptionId() + " nicht gefunden"));

        // Prüfung auf Überschneidungen mit bestehenden Fälligkeitsplänen
        validateNoPeriodOverlap(subscription.getId(), dto.getPeriodStart(), dto.getPeriodEnd(), null);

        DueSchedule entity = dueScheduleMapper.toEntity(dto);
        entity.setSubscription(subscription);

        if (entity.getDueNumber() == null || entity.getDueNumber().isEmpty()) {
            entity.setDueNumber(numberGeneratorService.generateDueNumber());
        }

        if (entity.getStatus() == null) {
            entity.setStatus(DueStatus.PENDING);
        }

        // Initial-Werte setzen
        entity.setPaidAmount(BigDecimal.ZERO);
        entity.setReminderSent(false);
        entity.setReminderCount(0);

        DueSchedule saved = dueScheduleRepository.save(entity);
        logger.info("Created due schedule: id={}, dueNumber={}, amount={}, dueDate={}, subscription={}",
                saved.getId(), saved.getDueNumber(), saved.getAmount(), saved.getDueDate(), saved.getSubscription().getId());

        return dueScheduleMapper.toDto(saved);
    }

    public DueScheduleDto updateDueSchedule(UUID id, DueScheduleDto dto) {
        DueSchedule existing = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));

        validateDueSchedule(dto);

        // Keine Änderungen erlaubt, wenn bereits bezahlt
        if (existing.getStatus() == DueStatus.PAID) {
            throw new BusinessLogicException("Bereits bezahlte Fälligkeitspläne können nicht geändert werden");
        }

        // Bei Teilzahlung darf Betrag nicht reduziert werden
        if (existing.getStatus() == DueStatus.PARTIAL_PAID &&
                dto.getAmount().compareTo(existing.getAmount()) < 0) {
            throw new BusinessLogicException("Betrag darf nach Teilzahlung nicht reduziert werden");
        }

        // Prüfung auf Überschneidungen bei Periode-Änderung
        if (!existing.getPeriodStart().equals(dto.getPeriodStart()) ||
                !existing.getPeriodEnd().equals(dto.getPeriodEnd())) {
            validateNoPeriodOverlap(existing.getSubscription().getId(), dto.getPeriodStart(), dto.getPeriodEnd(), id);
        }

        existing.setDueDate(dto.getDueDate());
        existing.setAmount(dto.getAmount());
        existing.setPeriodStart(dto.getPeriodStart());
        existing.setPeriodEnd(dto.getPeriodEnd());
        existing.setStatus(dto.getStatus());
        existing.setNotes(dto.getNotes());

        DueSchedule saved = dueScheduleRepository.save(existing);
        logger.info("Updated due schedule: id={}, dueNumber={}", saved.getId(), saved.getDueNumber());

        return dueScheduleMapper.toDto(saved);
    }

    public void deleteDueSchedule(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() == DueStatus.PAID) {
            throw new BusinessLogicException("Bezahlte Fälligkeitspläne können nicht gelöscht werden");
        }

        if (dueSchedule.getPaidAmount() != null && dueSchedule.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessLogicException("Fälligkeitspläne mit Teilzahlungen können nicht gelöscht werden");
        }

        dueScheduleRepository.delete(dueSchedule);
        logger.info("Deleted due schedule: id={}, dueNumber={}", dueSchedule.getId(), dueSchedule.getDueNumber());
    }

    // ----------------------------------------------------------
    // Abfrage-Methoden
    // ----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueSchedulesBySubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement mit ID " + subscriptionId + " nicht gefunden"));

        return dueScheduleRepository.findBySubscriptionOrderByDueDateAsc(subscription)
                .stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueSchedulesByStatus(DueStatus status) {
        return dueScheduleRepository.findByStatus(status)
                .stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DueScheduleDto> getOverdueDueSchedules() {
        return dueScheduleRepository.findOverdueSchedules(LocalDate.now())
                .stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueTodaySchedules() {
        return dueScheduleRepository.findDueTodaySchedules(LocalDate.now())
                .stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DueScheduleDto> getUpcomingDueSchedules(int days) {
        LocalDate now = LocalDate.now();
        return dueScheduleRepository.findUpcomingDueSchedules(now, now.plusDays(days))
                .stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueSchedulesByPeriod(LocalDate startDate, LocalDate endDate) {
        return dueScheduleRepository.findByDueDateBetween(startDate, endDate)
                .stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DueScheduleDto> getPendingDueSchedulesBySubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement mit ID " + subscriptionId + " nicht gefunden"));

        return dueScheduleRepository.findBySubscriptionAndStatusOrderByDueDateAsc(subscription, DueStatus.PENDING)
                .stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
    }

    // ----------------------------------------------------------
    // Zahlungs-Management
    // ----------------------------------------------------------

    public DueScheduleDto recordPayment(UUID id, PaymentDto paymentDto) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() == DueStatus.PAID) {
            throw new BusinessLogicException("Fälligkeitsplan ist bereits als bezahlt markiert");
        }

        if (dueSchedule.getStatus() == DueStatus.CANCELLED) {
            throw new BusinessLogicException("Stornierte Fälligkeitspläne können nicht bezahlt werden");
        }

        if (paymentDto.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessLogicException("Zahlungsbetrag muss größer als 0 sein");
        }

        BigDecimal existingPaid = dueSchedule.getPaidAmount() != null ? dueSchedule.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newTotalPaid = existingPaid.add(paymentDto.getPaidAmount());

        // Überzahlung prüfen
        if (newTotalPaid.compareTo(dueSchedule.getAmount()) > 0) {
            throw new BusinessLogicException("Zahlungsbetrag überschreitet offenen Betrag");
        }

        dueSchedule.setPaidAmount(newTotalPaid);
        dueSchedule.setPaidDate(paymentDto.getPaidDate() != null ? paymentDto.getPaidDate() : LocalDate.now());
        dueSchedule.setPaymentMethod(paymentDto.getPaymentMethod());
        dueSchedule.setPaymentReference(paymentDto.getPaymentReference());

        // Status aktualisieren
        if (newTotalPaid.compareTo(dueSchedule.getAmount()) == 0) {
            dueSchedule.setStatus(DueStatus.PAID);
        } else {
            dueSchedule.setStatus(DueStatus.PARTIAL_PAID);
        }

        // Notizen anhängen
        if (paymentDto.getNotes() != null && !paymentDto.getNotes().isEmpty()) {
            dueSchedule.setNotes(
                    (dueSchedule.getNotes() != null ? dueSchedule.getNotes() + "\n" : "") +
                            "[" + LocalDate.now() + "] Zahlung: " + paymentDto.getNotes()
            );
        }

        DueSchedule saved = dueScheduleRepository.save(dueSchedule);
        logger.info("Recorded payment for due schedule: id={}, paidAmount={}, totalPaid={}, status={}",
                saved.getId(), paymentDto.getPaidAmount(), newTotalPaid, saved.getStatus());

        return dueScheduleMapper.toDto(saved);
    }

    public DueScheduleDto markAsPaid(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() == DueStatus.PAID) {
            throw new BusinessLogicException("Fälligkeitsplan ist bereits als bezahlt markiert");
        }

        if (dueSchedule.getStatus() == DueStatus.CANCELLED) {
            throw new BusinessLogicException("Stornierte Fälligkeitspläne können nicht als bezahlt markiert werden");
        }

        dueSchedule.setPaidAmount(dueSchedule.getAmount());
        dueSchedule.setPaidDate(LocalDate.now());
        dueSchedule.setStatus(DueStatus.PAID);

        DueSchedule saved = dueScheduleRepository.save(dueSchedule);
        logger.info("Marked due schedule as paid: id={}, amount={}", saved.getId(), saved.getAmount());

        return dueScheduleMapper.toDto(saved);
    }

    public DueScheduleDto cancelDueSchedule(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() == DueStatus.PAID) {
            throw new BusinessLogicException("Bezahlte Fälligkeitspläne können nicht storniert werden");
        }

        if (dueSchedule.getPaidAmount() != null && dueSchedule.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessLogicException("Fälligkeitspläne mit Teilzahlungen können nicht storniert werden");
        }

        dueSchedule.setStatus(DueStatus.CANCELLED);
        dueSchedule.setNotes(
                (dueSchedule.getNotes() != null ? dueSchedule.getNotes() + "\n" : "") +
                        "[" + LocalDate.now() + "] Fälligkeitsplan storniert"
        );

        DueSchedule saved = dueScheduleRepository.save(dueSchedule);
        logger.info("Cancelled due schedule: id={}, dueNumber={}", saved.getId(), saved.getDueNumber());

        return dueScheduleMapper.toDto(saved);
    }

    // ----------------------------------------------------------
    // Mahnwesen
    // ----------------------------------------------------------

    public DueScheduleDto sendReminder(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));

        if (!(dueSchedule.getStatus() == DueStatus.PENDING || dueSchedule.getStatus() == DueStatus.PARTIAL_PAID)) {
            throw new BusinessLogicException("Mahnungen können nur für offene Fälligkeitspläne gesendet werden");
        }

        if (dueSchedule.getDueDate().isAfter(LocalDate.now())) {
            throw new BusinessLogicException("Mahnungen können nur für fällige oder überfällige Rechnungen gesendet werden");
        }

        dueSchedule.setReminderSent(true);
        dueSchedule.setReminderCount(dueSchedule.getReminderCount() + 1);
        dueSchedule.setLastReminderDate(LocalDate.now());

        // Notiz über Mahnung hinzufügen
        String reminderNote = "[" + LocalDate.now() + "] " + dueSchedule.getReminderCount() + ". Mahnung gesendet";
        dueSchedule.setNotes(
                (dueSchedule.getNotes() != null ? dueSchedule.getNotes() + "\n" : "") + reminderNote
        );

        // TODO: Integration mit E-Mail/SMS Service
        // emailService.sendPaymentReminder(dueSchedule);

        DueSchedule saved = dueScheduleRepository.save(dueSchedule);
        logger.info("Sent reminder for due schedule: id={}, reminderCount={}", saved.getId(), saved.getReminderCount());

        return dueScheduleMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DueScheduleDto> getSchedulesNeedingReminder() {
        return dueScheduleRepository.findSchedulesNeedingReminder(LocalDate.now())
                .stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
    }

    /**
     * Automatischer Batch-Job für Mahnungen
     */
    public int processPendingReminders() {
        List<DueSchedule> schedulesNeedingReminder = dueScheduleRepository.findSchedulesNeedingReminder(LocalDate.now());
        int processedCount = 0;

        for (DueSchedule schedule : schedulesNeedingReminder) {
            try {
                sendReminder(schedule.getId());
                processedCount++;
            } catch (Exception e) {
                logger.error("Fehler beim Senden der Mahnung für DueSchedule {}: {}", schedule.getId(), e.getMessage());
            }
        }

        logger.info("Processed {} payment reminders", processedCount);
        return processedCount;
    }

    // ----------------------------------------------------------
    // Statistiken und Berichte
    // ----------------------------------------------------------

    @Transactional(readOnly = true)
    public DueScheduleStatisticsDto getDueScheduleStatistics() {
        long total = dueScheduleRepository.count();
        long pending = dueScheduleRepository.countByStatus(DueStatus.PENDING);
        long overdue = dueScheduleRepository.countOverdue(LocalDate.now());
        long paid = dueScheduleRepository.countByStatus(DueStatus.PAID);
        long partialPaid = dueScheduleRepository.countByStatus(DueStatus.PARTIAL_PAID);
        long cancelled = dueScheduleRepository.countByStatus(DueStatus.CANCELLED);
        long needingReminder = dueScheduleRepository.countSchedulesNeedingReminder(LocalDate.now());

        BigDecimal totalPendingAmount = dueScheduleRepository.sumAmountByStatus(DueStatus.PENDING).orElse(BigDecimal.ZERO);
        BigDecimal totalPaidAmount = dueScheduleRepository.sumPaidAmount().orElse(BigDecimal.ZERO);
        BigDecimal totalOverdueAmount = dueScheduleRepository.sumOverdueAmount(LocalDate.now()).orElse(BigDecimal.ZERO);

        return new DueScheduleStatisticsDto(
                total, pending, overdue, paid, partialPaid, cancelled,
                totalPendingAmount, totalOverdueAmount, totalPaidAmount, needingReminder
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal getPendingAmountBySubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));
        return dueScheduleRepository.sumPendingAmountBySubscription(subscription);
    }

    @Transactional(readOnly = true)
    public BigDecimal getPaidAmountBySubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));
        return dueScheduleRepository.sumPaidAmountBySubscription(subscription);
    }

    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueSchedulesByCustomer(UUID customerId) {
        List<DueSchedule> schedules = dueScheduleRepository.findByCustomerId(customerId);
        return schedules.stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPendingAmount() {
        return dueScheduleRepository.sumAmountByStatus(DueStatus.PENDING).orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPaidAmount() {
        return dueScheduleRepository.sumPaidAmount().orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalOverdueAmount() {
        return dueScheduleRepository.sumOverdueAmount(LocalDate.now()).orElse(BigDecimal.ZERO);
    }

    // ----------------------------------------------------------
    // Automatische Generierung von Fälligkeitsplänen
    // ----------------------------------------------------------

    /**
     * Generiert Fälligkeitspläne für ein Abonnement basierend auf dessen Billing-Zyklus
     */
    public List<DueScheduleDto> generateDueSchedulesForSubscription(UUID subscriptionId, int periods) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement mit ID " + subscriptionId + " nicht gefunden"));

        // Bestehende Fälligkeitspläne prüfen
        List<DueSchedule> existing = dueScheduleRepository.findBySubscriptionOrderByDueDateAsc(subscription);

        // Startdatum für neue Pläne bestimmen
        LocalDate lastPeriodEnd = existing.stream()
                .map(DueSchedule::getPeriodEnd)
                .max(LocalDate::compareTo)
                .orElse(subscription.getStartDate().minusDays(1));

        // Subscription sollte noch nicht abgelaufen sein
        if (subscription.getEndDate() != null && lastPeriodEnd.isAfter(subscription.getEndDate())) {
            throw new BusinessLogicException("Abonnement ist bereits abgelaufen");
        }

        List<DueSchedule> newSchedules = new ArrayList<>();
        LocalDate currentPeriodStart = lastPeriodEnd.plusDays(1);

        for (int i = 0; i < periods; i++) {
            LocalDate periodEnd = calculatePeriodEnd(currentPeriodStart, subscription.getBillingCycle());
            LocalDate dueDate = calculateDueDate(periodEnd, subscription.getBillingCycle());

            // Nicht über Abonnement-Enddatum hinaus
            if (subscription.getEndDate() != null && periodEnd.isAfter(subscription.getEndDate())) {
                periodEnd = subscription.getEndDate();
                if (currentPeriodStart.isAfter(periodEnd)) {
                    break; // Keine weitere Periode möglich
                }
            }

            DueSchedule dueSchedule = new DueSchedule();
            dueSchedule.setDueNumber(numberGeneratorService.generateDueNumber());
            dueSchedule.setDueDate(dueDate);
            dueSchedule.setAmount(subscription.getMonthlyPrice());
            dueSchedule.setPeriodStart(currentPeriodStart);
            dueSchedule.setPeriodEnd(periodEnd);
            dueSchedule.setStatus(DueStatus.PENDING);
            dueSchedule.setSubscription(subscription);
            dueSchedule.setPaidAmount(BigDecimal.ZERO);
            dueSchedule.setReminderSent(false);
            dueSchedule.setReminderCount(0);

            newSchedules.add(dueSchedule);
            currentPeriodStart = periodEnd.plusDays(1);

            // Stoppen wenn Abonnement-Ende erreicht
            if (subscription.getEndDate() != null && currentPeriodStart.isAfter(subscription.getEndDate())) {
                break;
            }
        }

        if (newSchedules.isEmpty()) {
            logger.warn("Keine neuen Fälligkeitspläne für Subscription {} generiert", subscriptionId);
            return Collections.emptyList();
        }

        List<DueSchedule> saved = dueScheduleRepository.saveAll(newSchedules);
        logger.info("Generated {} new due schedules for subscription {}", saved.size(), subscriptionId);

        return saved.stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
    }

    /**
     * Generiert automatisch fehlende Fälligkeitspläne für alle aktiven Abonnements
     */
    public int generateMissingDueSchedules() {
        // Alle aktiven Subscriptions ohne ausreichende zukünftige Fälligkeitspläne finden
        List<Subscription> activeSubscriptions = subscriptionRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE);
        int generatedCount = 0;

        for (Subscription subscription : activeSubscriptions) {
            try {
                // Prüfen ob zukünftige Fälligkeitspläne existieren
                List<DueSchedule> futureSchedules = dueScheduleRepository
                        .findBySubscriptionAndDueDateAfter(subscription, LocalDate.now());

                // Wenn weniger als 2 zukünftige Pläne vorhanden, neue generieren
                if (futureSchedules.size() < 2) {
                    int periodsToGenerate = 6; // 6 Monate voraus
                    generateDueSchedulesForSubscription(subscription.getId(), periodsToGenerate);
                    generatedCount++;
                }
            } catch (Exception e) {
                logger.error("Fehler beim Generieren von Fälligkeitsplänen für Subscription {}: {}",
                        subscription.getId(), e.getMessage());
            }
        }

        logger.info("Generated missing due schedules for {} subscriptions", generatedCount);
        return generatedCount;
    }

    @Transactional(readOnly = true)
    public DueScheduleDto getNextDueScheduleBySubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement mit ID " + subscriptionId + " nicht gefunden"));

        return dueScheduleRepository.findNextDueScheduleBySubscription(subscription)
                .stream().findFirst()
                .map(dueScheduleMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Keine offenen Fälligkeitspläne für Abo " + subscriptionId));
    }

    // ----------------------------------------------------------
    // Hilfsmethoden für Validierung und Berechnung
    // ----------------------------------------------------------

    private void validateDueSchedule(DueScheduleDto dto) {
        if (dto.getPeriodStart() == null || dto.getPeriodEnd() == null || dto.getDueDate() == null) {
            throw new BusinessLogicException("Periode und Fälligkeitsdatum dürfen nicht null sein");
        }
        if (dto.getPeriodStart().isAfter(dto.getPeriodEnd())) {
            throw new BusinessLogicException("Periode Start darf nicht nach Periode Ende liegen");
        }
        if (dto.getDueDate().isBefore(dto.getPeriodEnd())) {
            throw new BusinessLogicException("Fälligkeitsdatum sollte nicht vor Ende der Periode liegen");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessLogicException("Betrag muss größer als 0 sein");
        }
        if (dto.getSubscriptionId() == null) {
            throw new BusinessLogicException("Abonnement-ID ist erforderlich");
        }
    }

    /**
     * Prüft auf Überschneidungen von Perioden bei einem Abonnement
     */
    private void validateNoPeriodOverlap(UUID subscriptionId, LocalDate periodStart, LocalDate periodEnd, UUID excludeId) {
        List<DueSchedule> existingSchedules = dueScheduleRepository
                .findBySubscriptionId(subscriptionId);

        for (DueSchedule existing : existingSchedules) {
            // Aktuelle Bearbeitung ausschließen
            if (excludeId != null && existing.getId().equals(excludeId)) {
                continue;
            }

            // Überschneidung prüfen
            if (!(periodEnd.isBefore(existing.getPeriodStart()) || periodStart.isAfter(existing.getPeriodEnd()))) {
                throw new BusinessLogicException("Periode überschneidet sich mit existierendem Fälligkeitsplan: "
                        + existing.getDueNumber());
            }
        }
    }

    /**
     * Berechnet das Perioden-Ende basierend auf Billing-Zyklus
     */
    private LocalDate calculatePeriodEnd(LocalDate periodStart, BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> periodStart.plusMonths(1).minusDays(1);
            case QUARTERLY -> periodStart.plusMonths(3).minusDays(1);
            case SEMI_ANNUALLY -> periodStart.plusMonths(6).minusDays(1);
            case ANNUALLY -> periodStart.plusYears(1).minusDays(1);
        };
    }

    /**
     * Berechnet das Fälligkeitsdatum (meist Ende der Periode + Zahlungsfrist)
     */
    private LocalDate calculateDueDate(LocalDate periodEnd, BillingCycle billingCycle) {
        // Standard: 14 Tage Zahlungsfrist nach Perioden-Ende
        return periodEnd.plusDays(14);
    }

    /**
     * Berechnet das nächste Fälligkeitsdatum basierend auf Billing-Zyklus
     */
    private LocalDate calculateNextDueDate(LocalDate currentDate, BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> currentDate.plusMonths(1);
            case QUARTERLY -> currentDate.plusMonths(3);
            case SEMI_ANNUALLY -> currentDate.plusMonths(6);
            case ANNUALLY -> currentDate.plusYears(1);
        };
    }

    /**
     * Prüft die Konsistenz der Fälligkeitspläne eines Abonnements
     */
    public void validateSubscriptionDueScheduleConsistency(UUID subscriptionId) {
        List<DueSchedule> schedules = dueScheduleRepository.findBySubscriptionIdOrderByPeriodStart(subscriptionId);

        if (schedules.isEmpty()) {
            return;
        }

        for (int i = 0; i < schedules.size() - 1; i++) {
            DueSchedule current = schedules.get(i);
            DueSchedule next = schedules.get(i + 1);

            // Prüfung auf Lücken
            if (!current.getPeriodEnd().plusDays(1).equals(next.getPeriodStart())) {
                logger.warn("Lücke zwischen Fälligkeitsplänen gefunden: {} -> {}",
                        current.getDueNumber(), next.getDueNumber());
            }

            // Prüfung auf Überschneidungen
            if (current.getPeriodEnd().isAfter(next.getPeriodStart()) ||
                    current.getPeriodEnd().equals(next.getPeriodStart())) {
                logger.warn("Überschneidung zwischen Fälligkeitsplänen gefunden: {} -> {}",
                        current.getDueNumber(), next.getDueNumber());
            }
        }
    }

    /**
     * Repariert Lücken in den Fälligkeitsplänen eines Abonnements
     */
    public List<DueScheduleDto> repairScheduleGaps(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement mit ID " + subscriptionId + " nicht gefunden"));

        List<DueSchedule> schedules = dueScheduleRepository.findBySubscriptionIdOrderByPeriodStart(subscriptionId);
        List<DueSchedule> newSchedules = new ArrayList<>();

        for (int i = 0; i < schedules.size() - 1; i++) {
            DueSchedule current = schedules.get(i);
            DueSchedule next = schedules.get(i + 1);

            // Lücke gefunden?
            LocalDate expectedNextStart = current.getPeriodEnd().plusDays(1);
            if (!expectedNextStart.equals(next.getPeriodStart())) {

                // Lücke füllen
                LocalDate gapStart = expectedNextStart;
                LocalDate gapEnd = next.getPeriodStart().minusDays(1);

                DueSchedule gapSchedule = new DueSchedule();
                gapSchedule.setDueNumber(numberGeneratorService.generateDueNumber());
                gapSchedule.setPeriodStart(gapStart);
                gapSchedule.setPeriodEnd(gapEnd);
                gapSchedule.setDueDate(calculateDueDate(gapEnd, subscription.getBillingCycle()));
                gapSchedule.setAmount(subscription.getMonthlyPrice());
                gapSchedule.setStatus(DueStatus.PENDING);
                gapSchedule.setSubscription(subscription);
                gapSchedule.setPaidAmount(BigDecimal.ZERO);
                gapSchedule.setReminderSent(false);
                gapSchedule.setReminderCount(0);

                newSchedules.add(gapSchedule);

                logger.info("Created gap-filling due schedule for subscription {}: {} to {}",
                        subscriptionId, gapStart, gapEnd);
            }
        }

        if (!newSchedules.isEmpty()) {
            List<DueSchedule> saved = dueScheduleRepository.saveAll(newSchedules);
            return saved.stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * Batch-Verarbeitung für überfällige Fälligkeitspläne
     */
    public int processOverdueDueSchedules() {
        List<DueSchedule> overdueSchedules = dueScheduleRepository.findOverdueSchedules(LocalDate.now());
        int processedCount = 0;

        for (DueSchedule schedule : overdueSchedules) {
            try {
                // Automatische Mahnung senden wenn noch nicht gesendet
                if (!schedule.isReminderSent() ||
                        (schedule.getLastReminderDate() != null &&
                                schedule.getLastReminderDate().isBefore(LocalDate.now().minusDays(7)))) {

                    sendReminder(schedule.getId());
                    processedCount++;
                }
            } catch (Exception e) {
                logger.error("Fehler bei der Verarbeitung des überfälligen Fälligkeitsplans {}: {}",
                        schedule.getId(), e.getMessage());
            }
        }

        logger.info("Processed {} overdue due schedules", processedCount);
        return processedCount;
    }

    /**
     * Erstellt einen Zahlungsplan-Bericht für ein Abonnement
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getPaymentScheduleReport(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement mit ID " + subscriptionId + " nicht gefunden"));

        List<DueSchedule> schedules = dueScheduleRepository.findBySubscriptionOrderByDueDateAsc(subscription);
        return schedules.stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
    }

    /**
     * Berechnet die nächste Fälligkeit für ein Abonnement
     */
    @Transactional(readOnly = true)
    public Optional<DueScheduleDto> getNextDueDate(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement mit ID " + subscriptionId + " nicht gefunden"));

        Optional<DueSchedule> nextDue = dueScheduleRepository
                .findBySubscriptionAndStatusAndDueDateAfterOrderByDueDateAsc(
                        subscription, DueStatus.PENDING, LocalDate.now())
                .stream().findFirst();

        return nextDue.map(dueScheduleMapper::toDto);
    }

    /**
     * Aktualisiert den Status überfälliger Fälligkeitspläne
     */
    public int updateOverdueStatus() {
        List<DueSchedule> overdueSchedules = dueScheduleRepository
                .findByStatusAndDueDateBefore(DueStatus.PENDING, LocalDate.now());

        int updatedCount = 0;

        for (DueSchedule schedule : overdueSchedules) {
            // Status könnte als OVERDUE markiert werden (falls solcher Status existiert)
            // Hier setzen wir eine Notiz über den überfälligen Status
            String overdueNote = "[" + LocalDate.now() + "] Status: Überfällig seit " + schedule.getDueDate();
            schedule.setNotes(
                    (schedule.getNotes() != null ? schedule.getNotes() + "\n" : "") + overdueNote
            );

            dueScheduleRepository.save(schedule);
            updatedCount++;
        }

        logger.info("Updated overdue status for {} due schedules", updatedCount);
        return updatedCount;
    }
}