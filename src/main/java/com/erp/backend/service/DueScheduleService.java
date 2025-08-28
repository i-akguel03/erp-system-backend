package com.erp.backend.service;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.domain.DueStatus;
import com.erp.backend.domain.Subscription;
import com.erp.backend.domain.BillingCycle;
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

@Service
@Transactional
public class DueScheduleService {

    @Autowired
    private DueScheduleRepository dueScheduleRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private DueScheduleMapper dueScheduleMapper;

    @Autowired
    private NumberGeneratorService numberGeneratorService;

    // ----------------------------------------------------------
    // CRUD
    // ----------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<DueScheduleDto> getAllDueSchedules(Pageable pageable) {
        return dueScheduleRepository.findAll(pageable)
                .map(dueScheduleMapper::toDto);
    }

    @Transactional(readOnly = true)
    public DueScheduleDto getDueScheduleById(UUID id) {
        return dueScheduleMapper.toDto(
                dueScheduleRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"))
        );
    }

    @Transactional(readOnly = true)
    public DueScheduleDto getDueScheduleByNumber(String dueNumber) {
        return dueScheduleMapper.toDto(
                dueScheduleRepository.findByDueNumber(dueNumber)
                        .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit Nummer " + dueNumber + " nicht gefunden"))
        );
    }

    public DueScheduleDto createDueSchedule(DueScheduleDto dto) {
        validateDueSchedule(dto);

        Subscription subscription = subscriptionRepository.findById(dto.getSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement mit ID " + dto.getSubscriptionId() + " nicht gefunden"));

        DueSchedule entity = dueScheduleMapper.toEntity(dto);
        entity.setSubscription(subscription);

        if (entity.getDueNumber() == null || entity.getDueNumber().isEmpty()) {
            entity.setDueNumber(numberGeneratorService.generateDueNumber());
        }

        entity.setStatus(DueStatus.PENDING);

        return dueScheduleMapper.toDto(dueScheduleRepository.save(entity));
    }

    public DueScheduleDto updateDueSchedule(UUID id, DueScheduleDto dto) {
        DueSchedule existing = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));

        validateDueSchedule(dto);

        // keine Änderungen erlaubt, wenn bezahlt
        if (existing.getStatus() == DueStatus.PAID) {
            throw new BusinessLogicException("Bereits bezahlte Fälligkeitspläne können nicht geändert werden");
        }

        // bei Teilzahlung -> Betrag darf nicht reduziert werden
        if (existing.getStatus() == DueStatus.PARTIAL_PAID &&
                dto.getAmount().compareTo(existing.getAmount()) < 0) {
            throw new BusinessLogicException("Betrag darf nach Teilzahlung nicht reduziert werden");
        }

        existing.setDueDate(dto.getDueDate());
        existing.setAmount(dto.getAmount());
        existing.setPeriodStart(dto.getPeriodStart());
        existing.setPeriodEnd(dto.getPeriodEnd());
        existing.setStatus(dto.getStatus());
        existing.setNotes(dto.getNotes());

        return dueScheduleMapper.toDto(dueScheduleRepository.save(existing));
    }

    public void deleteDueSchedule(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() == DueStatus.PAID) {
            throw new BusinessLogicException("Bezahlte Fälligkeitspläne können nicht gelöscht werden");
        }

        dueScheduleRepository.delete(dueSchedule);
    }

    // ----------------------------------------------------------
    // Abfragen
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

    // ----------------------------------------------------------
    // Zahlungen
    // ----------------------------------------------------------

    public DueScheduleDto recordPayment(UUID id, PaymentDto paymentDto) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() == DueStatus.PAID) {
            throw new BusinessLogicException("Fälligkeitsplan ist bereits als bezahlt markiert");
        }

        BigDecimal existingPaid = dueSchedule.getPaidAmount() != null ? dueSchedule.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newTotalPaid = existingPaid.add(paymentDto.getPaidAmount());

        dueSchedule.setPaidAmount(newTotalPaid);
        dueSchedule.setPaidDate(paymentDto.getPaidDate() != null ? paymentDto.getPaidDate() : LocalDate.now());
        dueSchedule.setPaymentMethod(paymentDto.getPaymentMethod());
        dueSchedule.setPaymentReference(paymentDto.getPaymentReference());

        if (newTotalPaid.compareTo(dueSchedule.getAmount()) >= 0) {
            dueSchedule.setStatus(DueStatus.PAID);
        } else {
            dueSchedule.setStatus(DueStatus.PARTIAL_PAID);
        }

        if (paymentDto.getNotes() != null && !paymentDto.getNotes().isEmpty()) {
            dueSchedule.setNotes(
                    (dueSchedule.getNotes() != null ? dueSchedule.getNotes() + "\n" : "") + paymentDto.getNotes()
            );
        }

        return dueScheduleMapper.toDto(dueScheduleRepository.save(dueSchedule));
    }

    public DueScheduleDto markAsPaid(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() == DueStatus.PAID) {
            throw new BusinessLogicException("Fälligkeitsplan ist bereits als bezahlt markiert");
        }

        dueSchedule.setPaidAmount(dueSchedule.getAmount());
        dueSchedule.setPaidDate(LocalDate.now());
        dueSchedule.setStatus(DueStatus.PAID);

        return dueScheduleMapper.toDto(dueScheduleRepository.save(dueSchedule));
    }

    public DueScheduleDto cancelDueSchedule(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() == DueStatus.PAID) {
            throw new BusinessLogicException("Bezahlte Fälligkeitspläne können nicht storniert werden");
        }

        dueSchedule.setStatus(DueStatus.CANCELLED);
        return dueScheduleMapper.toDto(dueScheduleRepository.save(dueSchedule));
    }

    // ----------------------------------------------------------
    // Mahnungen
    // ----------------------------------------------------------

    public DueScheduleDto sendReminder(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeitsplan mit ID " + id + " nicht gefunden"));

        if (!(dueSchedule.getStatus() == DueStatus.PENDING || dueSchedule.getStatus() == DueStatus.PARTIAL_PAID)) {
            throw new BusinessLogicException("Mahnungen können nur für offene Fälligkeitspläne gesendet werden");
        }

        dueSchedule.setReminderSent(true);
        dueSchedule.setReminderCount(dueSchedule.getReminderCount() + 1);
        dueSchedule.setLastReminderDate(LocalDate.now());

        // TODO: Email/SMS Service hier einbinden

        return dueScheduleMapper.toDto(dueScheduleRepository.save(dueSchedule));
    }

    @Transactional(readOnly = true)
    public List<DueScheduleDto> getSchedulesNeedingReminder() {
        return dueScheduleRepository.findSchedulesNeedingReminder(LocalDate.now())
                .stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
    }

    // ----------------------------------------------------------
    // Statistiken
    // ----------------------------------------------------------

    @Transactional(readOnly = true)
    public DueScheduleStatisticsDto getDueScheduleStatistics() {
        long total = dueScheduleRepository.count();
        long pending = dueScheduleRepository.countByStatus(DueStatus.PENDING);
        long overdue = dueScheduleRepository.countOverdue(LocalDate.now());
        long paid = dueScheduleRepository.countByStatus(DueStatus.PAID);
        long needingReminder = dueScheduleRepository.countSchedulesNeedingReminder(LocalDate.now());

        BigDecimal totalPendingAmount = dueScheduleRepository.sumAmountByStatus(DueStatus.PENDING).orElse(BigDecimal.ZERO);
        BigDecimal totalPaidAmount = dueScheduleRepository.sumPaidAmount().orElse(BigDecimal.ZERO);
        BigDecimal totalOverdueAmount = dueScheduleRepository.sumOverdueAmount(LocalDate.now()).orElse(BigDecimal.ZERO);

        return new DueScheduleStatisticsDto(
                total, pending, overdue, paid,
                totalPendingAmount, totalOverdueAmount, totalPaidAmount, needingReminder
        );
    }

    // ----------------------------------------------------------
    // Automatische Generierung
    // ----------------------------------------------------------

    public List<DueScheduleDto> generateDueSchedulesForSubscription(UUID subscriptionId, int months) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement mit ID " + subscriptionId + " nicht gefunden"));

        List<DueSchedule> existing = dueScheduleRepository.findBySubscription(subscription);

        LocalDate lastDueDate = existing.stream()
                .map(DueSchedule::getDueDate)
                .max(LocalDate::compareTo)
                .orElse(subscription.getStartDate().minusDays(1));

        List<DueSchedule> newSchedules = new ArrayList<>();
        LocalDate currentDate = lastDueDate;

        for (int i = 0; i < months; i++) {
            LocalDate nextDueDate = calculateNextDueDate(currentDate, subscription.getBillingCycle());

            DueSchedule dueSchedule = new DueSchedule();
            dueSchedule.setDueNumber(numberGeneratorService.generateDueNumber());
            dueSchedule.setDueDate(nextDueDate);
            dueSchedule.setAmount(subscription.getMonthlyPrice());
            dueSchedule.setPeriodStart(currentDate.plusDays(1));
            dueSchedule.setPeriodEnd(nextDueDate);
            dueSchedule.setStatus(DueStatus.PENDING);
            dueSchedule.setSubscription(subscription);
            dueSchedule.setReminderSent(false);
            dueSchedule.setReminderCount(0);

            newSchedules.add(dueSchedule);
            currentDate = nextDueDate;
        }

        return dueScheduleRepository.saveAll(newSchedules)
                .stream().map(dueScheduleMapper::toDto).collect(Collectors.toList());
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
    // Hilfsmethoden
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
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessLogicException("Betrag darf nicht negativ sein");
        }
    }


    public BigDecimal getPendingAmountBySubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));
        return dueScheduleRepository.sumPendingAmountBySubscription(subscription);
    }

    public BigDecimal getPaidAmountBySubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));
        return dueScheduleRepository.sumPaidAmountBySubscription(subscription);
    }


    // Alle Fälligkeiten eines Kunden über alle Abos
    public List<DueScheduleDto> getDueSchedulesByCustomer(UUID customerId) {
        List<DueSchedule> schedules = dueScheduleRepository.findByCustomerId(customerId);
        return schedules.stream().map(this::mapToDto).toList();
    }

    // Gesamt offener Betrag (alle Pending)
    public BigDecimal getTotalPendingAmount() {
        return dueScheduleRepository.sumAmountByStatus(DueStatus.PENDING).orElse(BigDecimal.ZERO);
    }

    // Gesamt bezahlter Betrag
    public BigDecimal getTotalPaidAmount() {
        return dueScheduleRepository.sumPaidAmount().orElse(BigDecimal.ZERO);
    }

    // Gesamt überfälliger Betrag
    public BigDecimal getTotalOverdueAmount() {
        return dueScheduleRepository.sumOverdueAmount(LocalDate.now()).orElse(BigDecimal.ZERO);
    }

    // Mapping Hilfsmethode (falls noch nicht vorhanden)
    private DueScheduleDto mapToDto(DueSchedule entity) {
        DueScheduleDto dto = new DueScheduleDto();
        dto.setId(entity.getId());
        dto.setDueNumber(entity.getDueNumber());
        dto.setAmount(entity.getAmount());
        dto.setPaidAmount(entity.getPaidAmount());
        dto.setDueDate(entity.getDueDate());
        dto.setStatus(entity.getStatus());
        // weitere Felder je nach Bedarf
        return dto;
    }

    private LocalDate calculateNextDueDate(LocalDate currentDate, BillingCycle billingCycle) {
        switch (billingCycle) {
            case MONTHLY: return currentDate.plusMonths(1);
            case QUARTERLY: return currentDate.plusMonths(3);
            case SEMI_ANNUALLY: return currentDate.plusMonths(6);
            case ANNUALLY: return currentDate.plusYears(1);
            default: return currentDate.plusMonths(1);
        }
    }
}
