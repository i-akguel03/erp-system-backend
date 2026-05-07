package com.erp.backend.service;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.domain.DueStatus;
import com.erp.backend.domain.Subscription;
import com.erp.backend.domain.SubscriptionStatus;
import com.erp.backend.dto.DueScheduleDto;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.InvalidStatusTransitionException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.mapper.DueScheduleMapper;
import com.erp.backend.repository.DueScheduleRepository;
import com.erp.backend.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: DueScheduleService - Status-Übergänge")
class DueScheduleServiceTest {

    @Mock private DueScheduleRepository dueScheduleRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Spy  private DueScheduleMapper dueScheduleMapper;

    @InjectMocks
    private DueScheduleService dueScheduleService;

    private UUID dueId;
    private DueSchedule activeDue;
    private DueSchedule pausedDue;
    private DueSchedule completedDue;

    @BeforeEach
    void setUp() {
        dueId = UUID.randomUUID();

        Subscription sub = new Subscription();
        sub.setId(UUID.randomUUID());
        sub.setSubscriptionNumber("ABO-001");

        activeDue = buildDueSchedule(dueId, DueStatus.ACTIVE, sub);
        pausedDue = buildDueSchedule(dueId, DueStatus.PAUSED, sub);
        completedDue = buildDueSchedule(dueId, DueStatus.COMPLETED, sub);
    }

    // ========== pauseDueSchedule ==========

    @Test
    @DisplayName("Aktive Fälligkeit kann pausiert werden")
    void pause_activeDue_success() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(activeDue));
        when(dueScheduleRepository.save(any())).thenReturn(activeDue);

        DueScheduleDto result = dueScheduleService.pauseDueSchedule(dueId);

        assertThat(activeDue.getStatus()).isEqualTo(DueStatus.PAUSED);
        verify(dueScheduleRepository, times(1)).save(activeDue);
    }

    @Test
    @DisplayName("Abgerechnete Fälligkeit kann nicht pausiert werden")
    void pause_completedDue_throwsInvalidStatusTransitionException() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(completedDue));

        assertThatThrownBy(() -> dueScheduleService.pauseDueSchedule(dueId))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("pausiert");

        verify(dueScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bereits pausierte Fälligkeit kann nicht nochmal pausiert werden")
    void pause_alreadyPaused_throwsInvalidStatusTransitionException() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(pausedDue));

        assertThatThrownBy(() -> dueScheduleService.pauseDueSchedule(dueId))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("aktive");
    }

    @Test
    @DisplayName("Pausieren einer nicht vorhandenen Fälligkeit wirft ResourceNotFoundException")
    void pause_notFound_throwsResourceNotFoundException() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dueScheduleService.pauseDueSchedule(dueId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ========== resumeDueSchedule ==========

    @Test
    @DisplayName("Pausierte Fälligkeit kann reaktiviert werden")
    void resume_pausedDue_success() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(pausedDue));
        when(dueScheduleRepository.save(any())).thenReturn(pausedDue);

        dueScheduleService.resumeDueSchedule(dueId);

        assertThat(pausedDue.getStatus()).isEqualTo(DueStatus.ACTIVE);
        verify(dueScheduleRepository, times(1)).save(pausedDue);
    }

    @Test
    @DisplayName("Abgerechnete Fälligkeit kann nicht reaktiviert werden")
    void resume_completedDue_throwsInvalidStatusTransitionException() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(completedDue));

        assertThatThrownBy(() -> dueScheduleService.resumeDueSchedule(dueId))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("reaktiviert");
    }

    @Test
    @DisplayName("Aktive Fälligkeit kann nicht reaktiviert werden (nicht pausiert)")
    void resume_activeDue_throwsInvalidStatusTransitionException() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(activeDue));

        assertThatThrownBy(() -> dueScheduleService.resumeDueSchedule(dueId))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("pausierte");
    }

    // ========== markAsCompleted ==========

    @Test
    @DisplayName("Aktive Fälligkeit kann als abgerechnet markiert werden")
    void markAsCompleted_activeDue_success() {
        UUID invoiceId = UUID.randomUUID();
        String batchId = "BATCH-001";

        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(activeDue));
        when(dueScheduleRepository.save(any())).thenReturn(activeDue);

        dueScheduleService.markAsCompleted(dueId, invoiceId, batchId);

        assertThat(activeDue.getStatus()).isEqualTo(DueStatus.COMPLETED);
        assertThat(activeDue.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(activeDue.getInvoiceBatchId()).isEqualTo(batchId);
    }

    @Test
    @DisplayName("Bereits abgerechnete Fälligkeit wird bei markAsCompleted übersprungen (idempotent)")
    void markAsCompleted_alreadyCompleted_isIdempotent() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(completedDue));

        dueScheduleService.markAsCompleted(dueId, UUID.randomUUID(), "BATCH");

        verify(dueScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pausierte Fälligkeit kann nicht als abgerechnet markiert werden")
    void markAsCompleted_pausedDue_throwsInvalidStatusTransitionException() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(pausedDue));

        assertThatThrownBy(() -> dueScheduleService.markAsCompleted(dueId, UUID.randomUUID(), "BATCH"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ========== rollbackCompleted ==========

    @Test
    @DisplayName("Abgerechnete Fälligkeit kann zurückgesetzt werden")
    void rollback_completedDue_success() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(completedDue));
        when(dueScheduleRepository.save(any())).thenReturn(completedDue);

        dueScheduleService.rollbackCompleted(dueId, "Korrektur");

        assertThat(completedDue.getStatus()).isEqualTo(DueStatus.ACTIVE);
        assertThat(completedDue.getInvoiceId()).isNull();
        assertThat(completedDue.getInvoiceBatchId()).isNull();
    }

    @Test
    @DisplayName("Aktive Fälligkeit kann nicht zurückgesetzt werden")
    void rollback_activeDue_throwsInvalidStatusTransitionException() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(activeDue));

        assertThatThrownBy(() -> dueScheduleService.rollbackCompleted(dueId, "Test"))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("zurückgesetzt");
    }

    // ========== deleteDueSchedule ==========

    @Test
    @DisplayName("Aktive Fälligkeit ohne Rechnungsverknüpfung kann gelöscht werden")
    void delete_activeDueWithoutInvoice_success() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(activeDue));

        dueScheduleService.deleteDueSchedule(dueId);

        verify(dueScheduleRepository, times(1)).delete(activeDue);
    }

    @Test
    @DisplayName("Abgerechnete Fälligkeit kann nicht gelöscht werden")
    void delete_completedDue_throwsBusinessLogicException() {
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(completedDue));

        assertThatThrownBy(() -> dueScheduleService.deleteDueSchedule(dueId))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("gelöscht");

        verify(dueScheduleRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Fälligkeit mit Rechnungsverknüpfung kann nicht gelöscht werden")
    void delete_dueWithInvoice_throwsBusinessLogicException() {
        activeDue.setInvoiceId(UUID.randomUUID());
        when(dueScheduleRepository.findById(dueId)).thenReturn(Optional.of(activeDue));

        assertThatThrownBy(() -> dueScheduleService.deleteDueSchedule(dueId))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("Rechnungsverknüpfung");
    }

    // ========== generateAdditionalDueSchedules ==========

    @Test
    @DisplayName("Fälligkeiten können nur für aktive Abonnements generiert werden")
    void generateAdditional_inactiveSubscription_throwsBusinessLogicException() {
        Subscription cancelled = new Subscription();
        cancelled.setId(UUID.randomUUID());
        cancelled.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
        cancelled.setStartDate(LocalDate.now());
        cancelled.setEndDate(LocalDate.now().plusMonths(6));

        when(subscriptionRepository.findById(cancelled.getId())).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> dueScheduleService.generateAdditionalDueSchedules(cancelled.getId(), 3))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("aktive Abonnements");
    }

    // ========== validateDueSchedule ==========

    @Test
    @DisplayName("Fälligkeit ohne SubscriptionId wirft BusinessLogicException bei Erstellung")
    void create_missingSubscriptionId_throwsBusinessLogicException() {
        DueScheduleDto dto = new DueScheduleDto();
        dto.setDueDate(LocalDate.now().plusDays(10));
        dto.setPeriodStart(LocalDate.now());
        dto.setPeriodEnd(LocalDate.now().plusMonths(1));
        // subscriptionId absichtlich nicht gesetzt

        assertThatThrownBy(() -> dueScheduleService.createDueSchedule(dto))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("Abonnement");
    }

    @Test
    @DisplayName("Periodenende vor Periodenstart wirft BusinessLogicException")
    void create_periodEndBeforeStart_throwsBusinessLogicException() {
        DueScheduleDto dto = new DueScheduleDto();
        dto.setSubscriptionId(UUID.randomUUID());
        dto.setDueDate(LocalDate.now());
        dto.setPeriodStart(LocalDate.now().plusMonths(1));
        dto.setPeriodEnd(LocalDate.now()); // Ende vor Start!

        assertThatThrownBy(() -> dueScheduleService.createDueSchedule(dto))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("Periodenende");
    }

    // ========== Hilfsmethode ==========

    private DueSchedule buildDueSchedule(UUID id, DueStatus status, Subscription sub) {
        DueSchedule ds = new DueSchedule();
        ds.setId(id);
        ds.setDueNumber("DUE-" + id.toString().substring(0, 8));
        ds.setStatus(status);
        ds.setDueDate(LocalDate.now().plusDays(5));
        ds.setPeriodStart(LocalDate.now());
        ds.setPeriodEnd(LocalDate.now().plusMonths(1));
        ds.setSubscription(sub);
        return ds;
    }
}
