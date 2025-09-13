package com.erp.backend.service;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.domain.DueStatus;
import com.erp.backend.domain.Subscription;
import com.erp.backend.dto.DueScheduleDto;
import com.erp.backend.dto.DueScheduleStatisticsDto;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.mapper.DueScheduleMapper;
import com.erp.backend.repository.DueScheduleRepository;
import com.erp.backend.repository.SubscriptionRepository;
import com.erp.backend.service.NumberGeneratorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service für DueSchedule-Verwaltung.
 *
 * Korrigierte Architektur:
 * - DueSchedule = nur Terminplanung (Datum, Periode, Status)
 * - Keine Beträge oder Zahlungsinformationen
 * - Preise kommen aus der zugehörigen Subscription
 * - Zahlungen werden über Invoice/OpenItem abgewickelt
 */
@Service
@Transactional
public class DueScheduleService {

    private final Logger logger = LoggerFactory.getLogger(DueScheduleService.class);

    private final DueScheduleRepository dueScheduleRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DueScheduleMapper dueScheduleMapper;
    private final NumberGeneratorService numberGeneratorService;

    public DueScheduleService(DueScheduleRepository dueScheduleRepository,
                              SubscriptionRepository subscriptionRepository,
                              DueScheduleMapper dueScheduleMapper,
                              NumberGeneratorService numberGeneratorService) {
        this.dueScheduleRepository = dueScheduleRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.dueScheduleMapper = dueScheduleMapper;
        this.numberGeneratorService = numberGeneratorService;
    }

    /**
     * Erstellt einen neuen Fälligkeitsplan (nur Termine, keine Beträge).
     */
    public DueScheduleDto createDueSchedule(DueScheduleDto dto) {
        validateDueSchedule(dto);

        Subscription subscription = subscriptionRepository.findById(dto.getSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Abo " + dto.getSubscriptionId() + " nicht gefunden"));

        // Prüfen, ob sich die Perioden überschneiden
        validateNoPeriodOverlap(subscription.getId(), dto.getPeriodStart(), dto.getPeriodEnd(), null);

        DueSchedule entity = dueScheduleMapper.toEntity(dto);
        entity.setSubscription(subscription);

        // Fälligkeitsnummer generieren, falls nicht gesetzt
        if (entity.getDueNumber() == null || entity.getDueNumber().isEmpty()) {
            entity.setDueNumber(numberGeneratorService.generateDueNumber());
        }

        // Standardstatus setzen
        if (entity.getStatus() == null) {
            entity.setStatus(DueStatus.ACTIVE);
        }

        // Keine Beträge setzen - die kommen beim Rechnungslauf aus der Subscription!

        DueSchedule saved = dueScheduleRepository.save(entity);
        logger.info("Created due schedule: id={}, dueNumber={}, dueDate={}, subscription={}",
                saved.getId(), saved.getDueNumber(), saved.getDueDate(), saved.getSubscription().getId());

        return dueScheduleMapper.toDto(saved);
    }

    /**
     * Aktualisiert einen bestehenden Fälligkeitsplan.
     */
    public DueScheduleDto updateDueSchedule(UUID id, DueScheduleDto dto) {
        DueSchedule existing = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeit " + id + " nicht gefunden"));

        // Validierung: Keine Überschneidung der Perioden (außer mit sich selbst)
        validateNoPeriodOverlap(existing.getSubscription().getId(), dto.getPeriodStart(), dto.getPeriodEnd(), id);

        // Nur Termine und Status aktualisieren - keine Beträge!
        existing.setDueDate(dto.getDueDate());
        existing.setPeriodStart(dto.getPeriodStart());
        existing.setPeriodEnd(dto.getPeriodEnd());
        existing.setStatus(dto.getStatus());

        DueSchedule updated = dueScheduleRepository.save(existing);
        logger.info("Updated due schedule: id={}, status={}", updated.getId(), updated.getStatus());

        return dueScheduleMapper.toDto(updated);
    }

    /**
     * Holt alle Fälligkeiten eines Abos.
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueSchedulesBySubscription(UUID subscriptionId) {
        return dueScheduleRepository.findBySubscriptionId(subscriptionId)
                .stream()
                .map(dueScheduleMapper::toDto)
                .toList();
    }

    /**
     * Holt Fälligkeiten nach Status.
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueSchedulesByStatus(DueStatus status) {
        return dueScheduleRepository.findByStatus(status)
                .stream()
                .map(dueScheduleMapper::toDto)
                .toList();
    }

    /**
     * Holt überfällige Fälligkeiten (fällig und noch aktiv).
     * Diese müssen noch abgerechnet werden.
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getOverdueDueSchedules() {
        LocalDate today = LocalDate.now();
        return dueScheduleRepository.findByStatusAndDueDateBefore(DueStatus.ACTIVE, today)
                .stream()
                .map(dueScheduleMapper::toDto)
                .toList();
    }

    /**
     * Holt Fälligkeiten für heute.
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueTodaySchedules() {
        LocalDate today = LocalDate.now();
        return dueScheduleRepository.findByDueDate(today)
                .stream()
                .map(dueScheduleMapper::toDto)
                .toList();
    }

    /**
     * Holt kommende Fälligkeiten innerhalb der nächsten X Tage.
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getUpcomingDueSchedules(int days) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);
        return dueScheduleRepository.findByDueDateBetween(today, endDate)
                .stream()
                .map(dueScheduleMapper::toDto)
                .toList();
    }

    /**
     * Holt Fälligkeiten in einem Zeitraum.
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueSchedulesByPeriod(LocalDate start, LocalDate end) {
        return dueScheduleRepository.findByDueDateBetween(start, end)
                .stream()
                .map(dueScheduleMapper::toDto)
                .toList();
    }

    /**
     * Holt Fälligkeiten eines Kunden über alle Abonnements.
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueSchedulesByCustomer(UUID customerId) {
        return dueScheduleRepository.findBySubscriptionCustomerId(customerId)
                .stream()
                .map(dueScheduleMapper::toDto)
                .toList();
    }

    /**
     * Ändert den Status einer Fälligkeit.
     */
    public DueScheduleDto changeStatus(UUID id, DueStatus newStatus) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeit " + id + " nicht gefunden"));

        DueStatus oldStatus = dueSchedule.getStatus();
        dueSchedule.setStatus(newStatus);

        DueSchedule updated = dueScheduleRepository.save(dueSchedule);

        logger.info("Changed due schedule status: id={}, oldStatus={}, newStatus={}",
                updated.getId(), oldStatus, newStatus);

        return dueScheduleMapper.toDto(updated);
    }

    /**
     * Markiert eine Fälligkeit als abgerechnet (COMPLETED).
     * Wird vom Rechnungslauf aufgerufen.
     */
    public DueScheduleDto markAsCompleted(UUID id) {
        return changeStatus(id, DueStatus.COMPLETED);
    }

    /**
     * Pausiert eine Fälligkeit.
     */
    public DueScheduleDto pauseDueSchedule(UUID id) {
        return changeStatus(id, DueStatus.PAUSED);
    }

    /**
     * Reaktiviert eine pausierte Fälligkeit.
     */
    public DueScheduleDto resumeDueSchedule(UUID id) {
        return changeStatus(id, DueStatus.ACTIVE);
    }

    /**
     * Löscht eine Fälligkeit.
     */
    public void deleteDueSchedule(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeit " + id + " nicht gefunden"));

        // Prüfen ob bereits abgerechnet
        if (dueSchedule.getStatus() == DueStatus.COMPLETED && dueSchedule.getInvoice() != null) {
            throw new IllegalStateException("Abgerechnete Fälligkeiten können nicht gelöscht werden");
        }

        dueScheduleRepository.delete(dueSchedule);
        logger.info("Deleted due schedule: id={}", id);
    }

    /**
     * Statistiken für Dashboard (Anzahl nach Status).
     */
    @Transactional(readOnly = true)
    public DueScheduleStatisticsDto getDueScheduleStatistics() {
        long active = dueScheduleRepository.countByStatus(DueStatus.ACTIVE);
        long paused = dueScheduleRepository.countByStatus(DueStatus.PAUSED);
        long suspended = dueScheduleRepository.countByStatus(DueStatus.SUSPENDED);
        long completed = dueScheduleRepository.countByStatus(DueStatus.COMPLETED);

        //return new DueScheduleStatisticsDto(active, paused, suspended, completed);
        return null;
    }

    /**
     * Fälligkeitspläne für ein Abonnement automatisch generieren (nur Termine).
     */
    public List<DueScheduleDto> generateDueSchedulesForSubscription(UUID subscriptionId, int months) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abo " + subscriptionId + " nicht gefunden"));

        // Startdatum: letzter Plan oder Abo-Start
        LocalDate startDate = getLastDueDateForSubscription(subscriptionId);
        if (startDate == null) {
            startDate = subscription.getStartDate();
        } else {
            startDate = startDate.plusMonths(1); // Nach letzter Fälligkeit
        }

        List<DueScheduleDto> generated = new java.util.ArrayList<>();

        for (int i = 0; i < months; i++) {
            LocalDate periodStart = startDate.plusMonths(i);
            LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
            LocalDate dueDate = periodEnd.plusDays(1); // Fällig am Ende der Periode

            DueScheduleDto dto = new DueScheduleDto();
            dto.setSubscriptionId(subscriptionId);
            dto.setPeriodStart(periodStart);
            dto.setPeriodEnd(periodEnd);
            dto.setDueDate(dueDate);
            dto.setStatus(DueStatus.ACTIVE);

            generated.add(createDueSchedule(dto));
        }

        logger.info("Generated {} due schedules for subscription {}", months, subscriptionId);
        return generated;
    }

    /**
     * Holt alle Fälligkeiten die zur Abrechnung bereit sind.
     * (Status ACTIVE und Datum in der Vergangenheit oder heute)
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueSchedulesReadyForBilling() {
        LocalDate today = LocalDate.now();
        return dueScheduleRepository.findByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, today)
                .stream()
                .map(dueScheduleMapper::toDto)
                .toList();
    }

    /**
     * Holt nächste fällige Fälligkeit für ein Abonnement.
     */
    @Transactional(readOnly = true)
    public DueScheduleDto getNextDueScheduleBySubscription(UUID subscriptionId) {
        return dueScheduleRepository.findBySubscriptionIdAndStatusOrderByDueDateAsc(subscriptionId, DueStatus.ACTIVE)
                .stream()
                .findFirst()
                .map(dueScheduleMapper::toDto)
                .orElse(null);
    }

    /**
     * Prüft ob ein Abonnement offene Fälligkeiten hat.
     */
    @Transactional(readOnly = true)
    public boolean hasOpenDueSchedules(UUID subscriptionId) {
        return dueScheduleRepository.countBySubscriptionIdAndStatus(subscriptionId, DueStatus.ACTIVE) > 0;
    }

    /**
     * Holt das letzte Fälligkeitsdatum für ein Abonnement.
     */
    private LocalDate getLastDueDateForSubscription(UUID subscriptionId) {
        return dueScheduleRepository.findBySubscriptionIdOrderByDueDateDesc(subscriptionId)
                .stream()
                .findFirst()
                .map(DueSchedule::getDueDate)
                .orElse(null);
    }

    // ---------------------------------------------------------
    // Validierungsmethoden
    // ---------------------------------------------------------

    private void validateDueSchedule(DueScheduleDto dto) {
        if (dto.getSubscriptionId() == null) {
            throw new IllegalArgumentException("Fälligkeit muss einem Abonnement zugeordnet sein");
        }
        if (dto.getPeriodStart() == null || dto.getPeriodEnd() == null) {
            throw new IllegalArgumentException("Fälligkeit muss einen Start- und Endzeitraum haben");
        }
        if (dto.getPeriodEnd().isBefore(dto.getPeriodStart())) {
            throw new IllegalArgumentException("Periodenende darf nicht vor Periodenstart liegen");
        }
        if (dto.getDueDate() == null) {
            throw new IllegalArgumentException("Fälligkeit muss ein Fälligkeitsdatum haben");
        }
    }

    private void validateNoPeriodOverlap(UUID subscriptionId, LocalDate start, LocalDate end, UUID excludeId) {
        List<DueSchedule> existingSchedules = dueScheduleRepository.findBySubscriptionId(subscriptionId);

        for (DueSchedule schedule : existingSchedules) {
            // Sich selbst ausschließen bei Updates
            if (excludeId != null && schedule.getId().equals(excludeId)) {
                continue;
            }

            // Überschneidung prüfen
            boolean overlaps = !(end.isBefore(schedule.getPeriodStart()) ||
                    start.isAfter(schedule.getPeriodEnd()));

            if (overlaps) {
                throw new IllegalArgumentException(
                        String.format("Fälligkeit überschneidet sich mit bestehender Fälligkeit %s (%s bis %s)",
                                schedule.getDueNumber(),
                                schedule.getPeriodStart(),
                                schedule.getPeriodEnd())
                );
            }
        }
    }
}