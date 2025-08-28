package com.erp.backend.controller;

import com.erp.backend.domain.DueStatus;
import com.erp.backend.dto.DueScheduleDto;
import com.erp.backend.dto.DueScheduleStatisticsDto;
import com.erp.backend.dto.PaymentDto;
import com.erp.backend.service.DueScheduleService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/due-schedules")
@CrossOrigin(origins = "*")
public class DueScheduleController {

    @Autowired
    private DueScheduleService dueScheduleService;

    // Alle Fälligkeitspläne abrufen (mit Pagination)
    @GetMapping
    public ResponseEntity<Page<DueScheduleDto>> getAllDueSchedules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<DueScheduleDto> dueSchedules = dueScheduleService.getAllDueSchedules(pageable);

        return ResponseEntity.ok(dueSchedules);
    }

    // Fälligkeitsplan nach ID abrufen
    @GetMapping("/{id}")
    public ResponseEntity<DueScheduleDto> getDueScheduleById(@PathVariable UUID id) {
        DueScheduleDto dueSchedule = dueScheduleService.getDueScheduleById(id);
        return ResponseEntity.ok(dueSchedule);
    }

    // Fälligkeitsplan nach Nummer abrufen
    @GetMapping("/number/{dueNumber}")
    public ResponseEntity<DueScheduleDto> getDueScheduleByNumber(@PathVariable String dueNumber) {
        DueScheduleDto dueSchedule = dueScheduleService.getDueScheduleByNumber(dueNumber);
        return ResponseEntity.ok(dueSchedule);
    }

    // Neuen Fälligkeitsplan erstellen
    @PostMapping
    public ResponseEntity<DueScheduleDto> createDueSchedule(@Valid @RequestBody DueScheduleDto dueScheduleDto) {
        DueScheduleDto createdDueSchedule = dueScheduleService.createDueSchedule(dueScheduleDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDueSchedule);
    }

    // Fälligkeitsplan aktualisieren
    @PutMapping("/{id}")
    public ResponseEntity<DueScheduleDto> updateDueSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody DueScheduleDto dueScheduleDto) {
        DueScheduleDto updatedDueSchedule = dueScheduleService.updateDueSchedule(id, dueScheduleDto);
        return ResponseEntity.ok(updatedDueSchedule);
    }

    // Fälligkeitsplan löschen
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDueSchedule(@PathVariable UUID id) {
        dueScheduleService.deleteDueSchedule(id);
        return ResponseEntity.noContent().build();
    }

    // Fälligkeitspläne nach Abonnement abrufen
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesBySubscription(@PathVariable UUID subscriptionId) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesBySubscription(subscriptionId);
        return ResponseEntity.ok(dueSchedules);
    }

    // Fälligkeitspläne nach Status abrufen
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesByStatus(@PathVariable DueStatus status) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesByStatus(status);
        return ResponseEntity.ok(dueSchedules);
    }

    // Überfällige Fälligkeitspläne abrufen
    @GetMapping("/overdue")
    public ResponseEntity<List<DueScheduleDto>> getOverdueDueSchedules() {
        List<DueScheduleDto> overdueDueSchedules = dueScheduleService.getOverdueDueSchedules();
        return ResponseEntity.ok(overdueDueSchedules);
    }

    // Fälligkeitspläne die heute fällig sind
    @GetMapping("/due-today")
    public ResponseEntity<List<DueScheduleDto>> getDueTodaySchedules() {
        List<DueScheduleDto> dueTodaySchedules = dueScheduleService.getDueTodaySchedules();
        return ResponseEntity.ok(dueTodaySchedules);
    }

    // Kommende Fälligkeitspläne (nächste X Tage)
    @GetMapping("/upcoming")
    public ResponseEntity<List<DueScheduleDto>> getUpcomingDueSchedules(
            @RequestParam(defaultValue = "7") int days) {
        List<DueScheduleDto> upcomingSchedules = dueScheduleService.getUpcomingDueSchedules(days);
        return ResponseEntity.ok(upcomingSchedules);
    }

    // Fälligkeitspläne in einem Zeitraum
    @GetMapping("/period")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesByPeriod(startDate, endDate);
        return ResponseEntity.ok(dueSchedules);
    }

    // Zahlung für Fälligkeitsplan verbuchen
    @PostMapping("/{id}/payment")
    public ResponseEntity<DueScheduleDto> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentDto paymentDto) {
        DueScheduleDto updatedDueSchedule = dueScheduleService.recordPayment(id, paymentDto);
        return ResponseEntity.ok(updatedDueSchedule);
    }

    // Fälligkeitsplan als bezahlt markieren
    @PutMapping("/{id}/mark-paid")
    public ResponseEntity<DueScheduleDto> markAsPaid(@PathVariable UUID id) {
        DueScheduleDto updatedDueSchedule = dueScheduleService.markAsPaid(id);
        return ResponseEntity.ok(updatedDueSchedule);
    }

    // Fälligkeitsplan stornieren
    @PutMapping("/{id}/cancel")
    public ResponseEntity<DueScheduleDto> cancelDueSchedule(@PathVariable UUID id) {
        DueScheduleDto updatedDueSchedule = dueScheduleService.cancelDueSchedule(id);
        return ResponseEntity.ok(updatedDueSchedule);
    }

    // Mahnung senden
    @PostMapping("/{id}/send-reminder")
    public ResponseEntity<DueScheduleDto> sendReminder(@PathVariable UUID id) {
        DueScheduleDto updatedDueSchedule = dueScheduleService.sendReminder(id);
        return ResponseEntity.ok(updatedDueSchedule);
    }

    // Alle Fälligkeitspläne die eine Mahnung benötigen
    @GetMapping("/needs-reminder")
    public ResponseEntity<List<DueScheduleDto>> getSchedulesNeedingReminder() {
        List<DueScheduleDto> schedules = dueScheduleService.getSchedulesNeedingReminder();
        return ResponseEntity.ok(schedules);
    }

    // Ausstehenden Betrag für Abonnement abrufen
    @GetMapping("/subscription/{subscriptionId}/pending-amount")
    public ResponseEntity<BigDecimal> getPendingAmountBySubscription(@PathVariable UUID subscriptionId) {
        BigDecimal pendingAmount = dueScheduleService.getPendingAmountBySubscription(subscriptionId);
        return ResponseEntity.ok(pendingAmount);
    }

    // Bezahlten Betrag für Abonnement abrufen
    @GetMapping("/subscription/{subscriptionId}/paid-amount")
    public ResponseEntity<BigDecimal> getPaidAmountBySubscription(@PathVariable UUID subscriptionId) {
        BigDecimal paidAmount = dueScheduleService.getPaidAmountBySubscription(subscriptionId);
        return ResponseEntity.ok(paidAmount);
    }

    // Fälligkeitspläne eines Kunden über alle Abonnements
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesByCustomer(@PathVariable UUID customerId) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesByCustomer(customerId);
        return ResponseEntity.ok(dueSchedules);
    }

    // Statistiken für Dashboard
    @GetMapping("/statistics")
    public ResponseEntity<DueScheduleStatisticsDto> getDueScheduleStatistics() {
        DueScheduleStatisticsDto statistics = dueScheduleService.getDueScheduleStatistics();
        return ResponseEntity.ok(statistics);
    }

    // Summen-Endpunkte (optional fürs Dashboard)
    @GetMapping("/sum/pending")
    public ResponseEntity<BigDecimal> getTotalPendingAmount() {
        return ResponseEntity.ok(dueScheduleService.getTotalPendingAmount());
    }

    @GetMapping("/sum/paid")
    public ResponseEntity<BigDecimal> getTotalPaidAmount() {
        return ResponseEntity.ok(dueScheduleService.getTotalPaidAmount());
    }

    @GetMapping("/sum/overdue")
    public ResponseEntity<BigDecimal> getTotalOverdueAmount() {
        return ResponseEntity.ok(dueScheduleService.getTotalOverdueAmount());
    }

    // Fälligkeitspläne für ein Abonnement automatisch generieren
    @PostMapping("/subscription/{subscriptionId}/generate")
    public ResponseEntity<List<DueScheduleDto>> generateDueSchedulesForSubscription(
            @PathVariable UUID subscriptionId,
            @RequestParam int months) {
        List<DueScheduleDto> generatedSchedules = dueScheduleService.generateDueSchedulesForSubscription(subscriptionId, months);
        return ResponseEntity.status(HttpStatus.CREATED).body(generatedSchedules);
    }

    // Nächste fällige Zahlung für Abonnement
    @GetMapping("/subscription/{subscriptionId}/next-due")
    public ResponseEntity<DueScheduleDto> getNextDueScheduleBySubscription(@PathVariable UUID subscriptionId) {
        DueScheduleDto nextDueSchedule = dueScheduleService.getNextDueScheduleBySubscription(subscriptionId);
        return ResponseEntity.ok(nextDueSchedule);
    }
}
