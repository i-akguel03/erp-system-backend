package com.erp.backend.controller;

import com.erp.backend.domain.DueStatus;
import com.erp.backend.dto.DueScheduleDto;
import com.erp.backend.dto.DueScheduleStatisticsDto;
import com.erp.backend.service.DueScheduleService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST-Controller für die Verwaltung von Fälligkeitsplänen (DueSchedules).
 *
 * Stellt Endpunkte bereit für:
 * - CRUD-Operationen auf Fälligkeiten
 * - Abfragen nach Status, Kunde, Abonnement
 * - Spezialfunktionen wie Generierung oder Statistiken
 */
@RestController
@RequestMapping("/api/due-schedules")
@CrossOrigin(origins = "*")
public class DueScheduleController {

    @Autowired
    private DueScheduleService dueScheduleService;

    /**
     * Einzelnen Fälligkeitsplan abrufen.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DueScheduleDto> getDueScheduleById(@PathVariable UUID id) {
        return dueScheduleService.getDueSchedulesBySubscription(id).stream()
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Alle Fälligkeitspläne abrufen.
     */
    @GetMapping
    public ResponseEntity<List<DueScheduleDto>> getAllDueSchedules() {
        List<DueScheduleDto> allSchedules = dueScheduleService.getAllDueSchedules();
        if (allSchedules.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(allSchedules);
    }

    /**
     * Neuen Fälligkeitsplan erstellen.
     */
    @PostMapping
    public ResponseEntity<DueScheduleDto> createDueSchedule(@RequestBody DueScheduleDto dueScheduleDto) {
        DueScheduleDto createdDueSchedule = dueScheduleService.createDueSchedule(dueScheduleDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDueSchedule);
    }

    /**
     * Fälligkeitsplan aktualisieren.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DueScheduleDto> updateDueSchedule(
            @PathVariable UUID id,
            @RequestBody DueScheduleDto dueScheduleDto) {
        DueScheduleDto updatedDueSchedule = dueScheduleService.updateDueSchedule(id, dueScheduleDto);
        return ResponseEntity.ok(updatedDueSchedule);
    }

    /**
     * Fälligkeitsplan löschen.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDueSchedule(@PathVariable UUID id) {
        dueScheduleService.deleteDueSchedule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Fälligkeitspläne nach Abonnement abrufen.
     */
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesBySubscription(@PathVariable UUID subscriptionId) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesBySubscription(subscriptionId);
        return ResponseEntity.ok(dueSchedules);
    }

    /**
     * Fälligkeitspläne nach Status abrufen.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesByStatus(@PathVariable DueStatus status) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesByStatus(status);
        return ResponseEntity.ok(dueSchedules);
    }

    /**
     * Überfällige Fälligkeitspläne abrufen.
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<DueScheduleDto>> getOverdueDueSchedules() {
        List<DueScheduleDto> overdueDueSchedules = dueScheduleService.getOverdueDueSchedules();
        return ResponseEntity.ok(overdueDueSchedules);
    }

    /**
     * Fälligkeitspläne abrufen, die heute fällig sind.
     */
    @GetMapping("/due-today")
    public ResponseEntity<List<DueScheduleDto>> getDueTodaySchedules() {
        List<DueScheduleDto> dueTodaySchedules = dueScheduleService.getDueTodaySchedules();
        return ResponseEntity.ok(dueTodaySchedules);
    }

    /**
     * Kommende Fälligkeitspläne (innerhalb der nächsten X Tage).
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<DueScheduleDto>> getUpcomingDueSchedules(
            @RequestParam(defaultValue = "7") int days) {
        List<DueScheduleDto> upcomingSchedules = dueScheduleService.getUpcomingDueSchedules(days);
        return ResponseEntity.ok(upcomingSchedules);
    }

    /**
     * Fälligkeitspläne in einem bestimmten Zeitraum abrufen.
     */
    @GetMapping("/period")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesByPeriod(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesByPeriod(startDate, endDate);
        return ResponseEntity.ok(dueSchedules);
    }

    /**
     * Fälligkeitspläne eines Kunden über alle Abonnements.
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesByCustomer(@PathVariable UUID customerId) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesByCustomer(customerId);
        return ResponseEntity.ok(dueSchedules);
    }

    /**
     * Statistiken für Dashboard (z. B. Anzahl nach Status).
     */
    @GetMapping("/statistics")
    public ResponseEntity<DueScheduleStatisticsDto> getDueScheduleStatistics() {
        DueScheduleStatisticsDto statistics = dueScheduleService.getDueScheduleStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Automatische Generierung von Fälligkeitsplänen für ein Abonnement.
     */
    @PostMapping("/subscription/{subscriptionId}/generate")
    public ResponseEntity<List<DueScheduleDto>> generateDueSchedulesForSubscription(
            @PathVariable UUID subscriptionId,
            @RequestParam int months) {
//        List<DueScheduleDto> generatedSchedules = dueScheduleService.generateDueSchedulesForSubscription(subscriptionId, months);
//        return ResponseEntity.status(HttpStatus.CREATED).body(generatedSchedules);
        return  ResponseEntity.ok(null);
    }

    /**
     * Nächste fällige Fälligkeit für ein Abonnement.
     */
    @GetMapping("/subscription/{subscriptionId}/next-due")
    public ResponseEntity<DueScheduleDto> getNextDueScheduleBySubscription(@PathVariable UUID subscriptionId) {
        DueScheduleDto nextDueSchedule = dueScheduleService.getNextDueScheduleBySubscription(subscriptionId);
        return ResponseEntity.ok(nextDueSchedule);
    }
}
