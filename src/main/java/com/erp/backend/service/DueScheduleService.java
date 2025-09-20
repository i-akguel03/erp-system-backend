package com.erp.backend.service;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.domain.DueStatus;
import com.erp.backend.domain.Subscription;
import com.erp.backend.domain.SubscriptionStatus;
import com.erp.backend.dto.DueScheduleDto;
import com.erp.backend.dto.DueScheduleStatisticsDto;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.mapper.DueScheduleMapper;
import com.erp.backend.repository.DueScheduleRepository;
import com.erp.backend.repository.SubscriptionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service für DueSchedule-Verwaltung mit strikten Workflow-Regeln.
 *
 * WICHTIGE WORKFLOW-REGELN:
 * 1. DueSchedules werden NUR beim Abo-Erstellen automatisch generiert
 * 2. DueSchedules enthalten NUR Termine und Status - KEINE Beträge
 * 3. Status COMPLETED darf NUR vom InvoiceBatchService gesetzt werden
 * 4. Wenn Status auf COMPLETED gesetzt wird, MÜSSEN Rechnung und OpenItem existieren
 * 5. Benutzer können nur zwischen ACTIVE/PAUSED wechseln
 * 6. Manuelle Erstellung nur für Nachkorrekturen
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

    // ===============================================================================================
    // ABFRAGE-METHODEN (READ-ONLY)
    // ===============================================================================================

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
     * Holt überfällige UND noch nicht abgerechnete Fälligkeiten.
     * Diese sind für den Rechnungslauf relevant.
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
     * Holt Fälligkeiten für heute (noch nicht abgerechnet).
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueTodaySchedules() {
        LocalDate today = LocalDate.now();
        return dueScheduleRepository.findByStatusAndDueDate(DueStatus.ACTIVE, today)
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
        return dueScheduleRepository.findByStatusAndDueDateBetween(DueStatus.ACTIVE, today, endDate)
                .stream()
                .map(dueScheduleMapper::toDto)
                .toList();
    }

    /**
     * Holt ALLE Fälligkeiten die zur Abrechnung bereit sind.
     * Das sind alle AKTIVEN Fälligkeiten bis zum Stichtag (inkl. überfällige).
     *
     * DIESE METHODE WIRD VOM RECHNUNGSLAUF VERWENDET!
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueSchedulesReadyForBilling(LocalDate billingDate) {
        if (billingDate == null) {
            billingDate = LocalDate.now();
        }

        return dueScheduleRepository.findByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, billingDate)
                .stream()
                .map(dueScheduleMapper::toDto)
                .toList();
    }

    /**
     * Holt alle Fälligkeiten die zur Abrechnung bereit sind (bis heute).
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getDueSchedulesReadyForBilling() {
        return getDueSchedulesReadyForBilling(LocalDate.now());
    }

    /**
     * Holt Fälligkeiten in einem Zeitraum (alle Status).
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
     * Holt alle Fälligkeiten (für Admin-Übersicht).
     */
    @Transactional(readOnly = true)
    public List<DueScheduleDto> getAllDueSchedules() {
        return dueScheduleRepository.findAll()
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
     * Statistiken für Dashboard.
     */
    @Transactional(readOnly = true)
    public DueScheduleStatisticsDto getDueScheduleStatistics() {
        long active = dueScheduleRepository.countByStatus(DueStatus.ACTIVE);
        long paused = dueScheduleRepository.countByStatus(DueStatus.PAUSED);
        long suspended = dueScheduleRepository.countByStatus(DueStatus.SUSPENDED);
        long completed = dueScheduleRepository.countByStatus(DueStatus.COMPLETED);

        // Überfällige aktive Fälligkeiten
        LocalDate today = LocalDate.now();
        long overdue = dueScheduleRepository.countByStatusAndDueDateBefore(DueStatus.ACTIVE, today);

        return null;//new DueScheduleStatisticsDto(active, paused, suspended, completed, overdue);
    }

    // ===============================================================================================
    // BENUTZER-ACTIONS (Beschränkt auf ACTIVE/PAUSED)
    // ===============================================================================================

    /**
     * Pausiert eine Fälligkeit (nur ACTIVE -> PAUSED erlaubt).
     * COMPLETED Fälligkeiten können NICHT mehr geändert werden!
     */
    public DueScheduleDto pauseDueSchedule(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeit " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() == DueStatus.COMPLETED) {
            throw new IllegalStateException("Abgerechnete Fälligkeiten können nicht pausiert werden (ID: " + id + ")");
        }

        if (dueSchedule.getStatus() != DueStatus.ACTIVE) {
            throw new IllegalStateException("Nur aktive Fälligkeiten können pausiert werden (Status: " + dueSchedule.getStatus() + ")");
        }

        dueSchedule.setStatus(DueStatus.PAUSED);
        DueSchedule updated = dueScheduleRepository.save(dueSchedule);

        logger.info("Paused due schedule: id={}, dueNumber={}", updated.getId(), updated.getDueNumber());
        return dueScheduleMapper.toDto(updated);
    }

    /**
     * Reaktiviert eine pausierte Fälligkeit (nur PAUSED -> ACTIVE erlaubt).
     */
    public DueScheduleDto resumeDueSchedule(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeit " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() == DueStatus.COMPLETED) {
            throw new IllegalStateException("Abgerechnete Fälligkeiten können nicht reaktiviert werden (ID: " + id + ")");
        }

        if (dueSchedule.getStatus() != DueStatus.PAUSED) {
            throw new IllegalStateException("Nur pausierte Fälligkeiten können reaktiviert werden (Status: " + dueSchedule.getStatus() + ")");
        }

        dueSchedule.setStatus(DueStatus.ACTIVE);
        DueSchedule updated = dueScheduleRepository.save(dueSchedule);

        logger.info("Resumed due schedule: id={}, dueNumber={}", updated.getId(), updated.getDueNumber());
        return dueScheduleMapper.toDto(updated);
    }

    // ===============================================================================================
    // SYSTEM-METHODEN (Nur für InvoiceBatchService und Admin)
    // ===============================================================================================

    /**
     * Markiert eine Fälligkeit als abgerechnet (COMPLETED).
     *
     * KRITISCH: Diese Methode darf NUR vom InvoiceBatchService aufgerufen werden!
     * Sie setzt automatisch auch die invoiceId und invoiceBatchId.
     *
     * @param id Fälligkeits-ID
     * @param invoiceId Rechnung die für diese Fälligkeit erstellt wurde
     * @param invoiceBatchId Batch-ID des Rechnungslaufs
     * @return Aktualisierte Fälligkeit
     */
    @Transactional
    public DueScheduleDto markAsCompleted(UUID id, UUID invoiceId, String invoiceBatchId) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeit " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() == DueStatus.COMPLETED) {
            logger.warn("Due schedule {} is already completed - skipping", id);
            return dueScheduleMapper.toDto(dueSchedule);
        }

        if (dueSchedule.getStatus() != DueStatus.ACTIVE) {
            throw new IllegalStateException("Nur aktive Fälligkeiten können als abgerechnet markiert werden (Status: " + dueSchedule.getStatus() + ")");
        }

        // Status und Verknüpfungen setzen
        dueSchedule.setStatus(DueStatus.COMPLETED);
        dueSchedule.setInvoiceId(invoiceId);
        dueSchedule.setInvoiceBatchId(invoiceBatchId);

        DueSchedule updated = dueScheduleRepository.save(dueSchedule);

        logger.info("Marked due schedule as completed: id={}, dueNumber={}, invoiceId={}, batchId={}",
                updated.getId(), updated.getDueNumber(), invoiceId, invoiceBatchId);

        return dueScheduleMapper.toDto(updated);
    }

    /**
     * Überladene Methode für Rückwärtskompatibilität.
     * DEPRECATED: Verwenden Sie die Variante mit invoiceId und batchId!
     */
    @Deprecated
    public DueScheduleDto markAsCompleted(UUID id) {
        logger.warn("DEPRECATED: markAsCompleted ohne invoiceId aufgerufen für DueSchedule {}", id);
        return markAsCompleted(id, null, null);
    }

    /**
     * Rollback einer abgerechneten Fälligkeit zurück auf ACTIVE.
     *
     * KRITISCH: Nur für Fehlerkorrekturen! Normalerweise sollten abgerechnete
     * Fälligkeiten NIEMALS zurückgesetzt werden.
     *
     * @param id Fälligkeits-ID
     * @param reason Grund für den Rollback (für Audit-Log)
     * @return Zurückgesetzte Fälligkeit
     */
    @Transactional
    public DueScheduleDto rollbackCompleted(UUID id, String reason) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeit " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() != DueStatus.COMPLETED) {
            throw new IllegalStateException("Nur abgerechnete Fälligkeiten können zurückgesetzt werden (Status: " + dueSchedule.getStatus() + ")");
        }

        // WARNUNG loggen
        logger.warn("ROLLBACK: Resetting completed due schedule {} to ACTIVE. Reason: {}", id, reason);

        // Status zurücksetzen
        dueSchedule.setStatus(DueStatus.ACTIVE);
        dueSchedule.setInvoiceId(null);
        dueSchedule.setInvoiceBatchId(null);

        DueSchedule updated = dueScheduleRepository.save(dueSchedule);
        return dueScheduleMapper.toDto(updated);
    }

    // ===============================================================================================
    // MANUELLE ERSTELLUNG (Nur für Nachkorrekturen)
    // ===============================================================================================

    /**
     * Erstellt eine neue Fälligkeit manuell.
     *
     * HINWEIS: Normalerweise werden Fälligkeiten automatisch beim Abo-Erstellen generiert.
     * Diese Methode ist nur für Nachkorrekturen oder spezielle Fälle gedacht.
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

        // Standardstatus setzen (niemals COMPLETED bei manueller Erstellung!)
        if (entity.getStatus() == null || entity.getStatus() == DueStatus.COMPLETED) {
            entity.setStatus(DueStatus.ACTIVE);
        }

        // Keine Beträge, invoiceId oder batchId bei manueller Erstellung!
        entity.setInvoiceId(null);
        entity.setInvoiceBatchId(null);

        DueSchedule saved = dueScheduleRepository.save(entity);

        logger.info("Manually created due schedule: id={}, dueNumber={}, dueDate={}, subscription={}",
                saved.getId(), saved.getDueNumber(), saved.getDueDate(), saved.getSubscription().getId());

        return dueScheduleMapper.toDto(saved);
    }

    /**
     * Aktualisiert eine bestehende Fälligkeit.
     *
     * EINSCHRÄNKUNGEN:
     * - COMPLETED Fälligkeiten können NICHT geändert werden
     * - invoiceId und batchId können nicht manuell gesetzt werden
     */
    public DueScheduleDto updateDueSchedule(UUID id, DueScheduleDto dto) {
        DueSchedule existing = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeit " + id + " nicht gefunden"));

        if (existing.getStatus() == DueStatus.COMPLETED) {
            throw new IllegalStateException("Abgerechnete Fälligkeiten können nicht bearbeitet werden (ID: " + id + ")");
        }

        // Validierung: Keine Überschneidung der Perioden (außer mit sich selbst)
        validateNoPeriodOverlap(existing.getSubscription().getId(), dto.getPeriodStart(), dto.getPeriodEnd(), id);

        // Nur Termine und Status aktualisieren - keine Beträge oder Verknüpfungen!
        existing.setDueDate(dto.getDueDate());
        existing.setPeriodStart(dto.getPeriodStart());
        existing.setPeriodEnd(dto.getPeriodEnd());

        // Status nur zwischen ACTIVE/PAUSED wechseln lassen
        if (dto.getStatus() == DueStatus.COMPLETED) {
            throw new IllegalArgumentException("Status COMPLETED kann nicht manuell gesetzt werden");
        }
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }

        DueSchedule updated = dueScheduleRepository.save(existing);

        logger.info("Updated due schedule: id={}, status={}, dueDate={}",
                updated.getId(), updated.getStatus(), updated.getDueDate());

        return dueScheduleMapper.toDto(updated);
    }

    /**
     * Löscht eine Fälligkeit.
     *
     * EINSCHRÄNKUNGEN:
     * - COMPLETED Fälligkeiten können NICHT gelöscht werden
     * - Nur leere Fälligkeiten ohne Rechnungsverknüpfung
     */
    public void deleteDueSchedule(UUID id) {
        DueSchedule dueSchedule = dueScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fälligkeit " + id + " nicht gefunden"));

        if (dueSchedule.getStatus() == DueStatus.COMPLETED) {
            throw new IllegalStateException("Abgerechnete Fälligkeiten können nicht gelöscht werden (ID: " + id + ")");
        }

        if (dueSchedule.getInvoiceId() != null) {
            throw new IllegalStateException("Fälligkeiten mit Rechnungsverknüpfung können nicht gelöscht werden (ID: " + id + ")");
        }

        dueScheduleRepository.delete(dueSchedule);
        logger.info("Deleted due schedule: id={}, dueNumber={}", id, dueSchedule.getDueNumber());
    }

    // ===============================================================================================
    // BULK-OPERATIONEN
    // ===============================================================================================

    /**
     * Generiert zusätzliche Fälligkeitspläne für ein bestehendes Abonnement.
     *
     * HINWEIS: Normalerweise sollte dies nicht nötig sein, da Fälligkeiten
     * automatisch beim Abo-Erstellen für 12 Monate generiert werden.
     */
    public List<DueScheduleDto> generateAdditionalDueSchedules(UUID subscriptionId, int additionalMonths) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abo " + subscriptionId + " nicht gefunden"));

        if (subscription.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Zusätzliche Fälligkeiten können nur für aktive Abonnements erstellt werden");
        }

        // Finde die letzte bestehende Fälligkeit
        Optional<DueSchedule> lastSchedule = dueScheduleRepository
                .findBySubscriptionIdOrderByPeriodEndDesc(subscriptionId)
                .stream()
                .findFirst();

        LocalDate nextPeriodStart;
        if (lastSchedule.isPresent()) {
            nextPeriodStart = lastSchedule.get().getPeriodEnd().plusDays(1);
        } else {
            nextPeriodStart = subscription.getStartDate();
        }

        List<DueScheduleDto> generated = new java.util.ArrayList<>();
        LocalDate subscriptionEnd = subscription.getEndDate();

        for (int i = 0; i < additionalMonths; i++) {
            if (nextPeriodStart.isAfter(subscriptionEnd)) {
                break;
            }

            LocalDate periodEnd = nextPeriodStart.plusMonths(1).minusDays(1);
            if (periodEnd.isAfter(subscriptionEnd)) {
                periodEnd = subscriptionEnd;
            }

            DueScheduleDto dto = new DueScheduleDto();
            dto.setSubscriptionId(subscriptionId);
            dto.setPeriodStart(nextPeriodStart);
            dto.setPeriodEnd(periodEnd);
            dto.setDueDate(nextPeriodStart);
            dto.setStatus(DueStatus.ACTIVE);

            generated.add(createDueSchedule(dto));
            nextPeriodStart = periodEnd.plusDays(1);
        }

        logger.info("Generated {} additional due schedules for subscription {}",
                generated.size(), subscriptionId);

        return generated;
    }

    // ===============================================================================================
    // VALIDIERUNGS-METHODEN
    // ===============================================================================================

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
        if (dto.getStatus() == DueStatus.COMPLETED) {
            throw new IllegalArgumentException("Status COMPLETED kann nicht manuell gesetzt werden");
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