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
     * Erstellt eine neue Fälligkeit (DueSchedule) für ein Abo.
     * Nur Status/Terminverwaltung – kein Preis, keine Zahlung, kein Mahnwesen.
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

        DueSchedule saved = dueScheduleRepository.save(entity);
        logger.info("Created due schedule: id={}, dueNumber={}, dueDate={}, subscription={}",
                saved.getId(), saved.getDueNumber(), saved.getDueDate(), saved.getSubscription().getId());

        return dueScheduleMapper.toDto(saved);
    }

    /**
     * Aktualisiert eine bestehende Fälligkeit.
     */
    public DueScheduleDto updateDueSchedule(UUID id, DueScheduleDto dto) {
        DueSchedule existing = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeit " + id + " nicht gefunden"));

        // Validierung: Keine Überschneidung der Perioden (außer mit sich selbst)
        validateNoPeriodOverlap(existing.getSubscription().getId(), dto.getPeriodStart(), dto.getPeriodEnd(), id);

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
     * Holt überfällige Fälligkeiten (fällig und noch aktiv)
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

        dueSchedule.setStatus(newStatus);
        DueSchedule updated = dueScheduleRepository.save(dueSchedule);

        logger.info("Changed due schedule status: id={}, newStatus={}", updated.getId(), newStatus);

        return dueScheduleMapper.toDto(updated);
    }

    /**
     * Löscht eine Fälligkeit.
     */
    public void deleteDueSchedule(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeit " + id + " nicht gefunden"));

        dueScheduleRepository.delete(dueSchedule);
        logger.info("Deleted due schedule: id={}", id);
    }

    /**
     * Statistiken für Dashboard (Anzahl nach Status)
     */
    @Transactional(readOnly = true)
    public DueScheduleStatisticsDto getDueScheduleStatistics() {
        long active = dueScheduleRepository.countByStatus(DueStatus.ACTIVE);
        long paused = dueScheduleRepository.countByStatus(DueStatus.PAUSED);
        long suspended = dueScheduleRepository.countByStatus(DueStatus.SUSPENDED);
        long completed = dueScheduleRepository.countByStatus(DueStatus.COMPLETED);

        return new DueScheduleStatisticsDto(active, paused, suspended, completed);
    }

    /**
     * Fälligkeitspläne für ein Abonnement automatisch generieren
     */
    public List<DueScheduleDto> generateDueSchedulesForSubscription(UUID subscriptionId, int months) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abo " + subscriptionId + " nicht gefunden"));

        // Generierung: monatlich fortlaufend
        LocalDate start = LocalDate.now();
        List<DueScheduleDto> generated = new java.util.ArrayList<>();
        for (int i = 0; i < months; i++) {
            DueScheduleDto dto = new DueScheduleDto();
            dto.setSubscriptionId(subscriptionId);
            dto.setPeriodStart(start.plusMonths(i));
            dto.setPeriodEnd(start.plusMonths(i + 1).minusDays(1));
            dto.setDueDate(start.plusMonths(i + 1));
            dto.setStatus(DueStatus.ACTIVE);
            generated.add(createDueSchedule(dto));
        }
        return generated;
    }

    /**
     * Nächste fällige Fälligkeit für ein Abonnement
     */
    public DueScheduleDto getNextDueScheduleBySubscription(UUID subscriptionId) {
        return dueScheduleRepository.findBySubscriptionIdAndStatusOrderByDueDateAsc(subscriptionId, DueStatus.ACTIVE)
                .stream()
                .findFirst()
                .map(dueScheduleMapper::toDto)
                .orElse(null);
    }

    // ---------------------------------------------------------
    // Hilfsmethoden (nur noch für Perioden-Validierung)
    // ---------------------------------------------------------
    private void validateDueSchedule(DueScheduleDto dto) {
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
            if (excludeId != null && schedule.getId().equals(excludeId)) continue;
            boolean overlaps = !(end.isBefore(schedule.getPeriodStart()) || start.isAfter(schedule.getPeriodEnd()));
            if (overlaps) {
                throw new IllegalArgumentException("Fälligkeit überschneidet sich mit einer bestehenden Fälligkeit");
            }
        }
    }
}
