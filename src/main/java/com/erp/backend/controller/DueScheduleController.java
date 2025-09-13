package com.erp.backend.controller;

import com.erp.backend.domain.DueStatus;
import com.erp.backend.dto.DueScheduleDto;
import com.erp.backend.dto.DueScheduleStatisticsDto;
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
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<DueScheduleDto> dueSchedules = dueScheduleService.getAllDueSchedules(pageable);

        return ResponseEntity.ok(dueSchedules);
    }

    // Einzelnen Fälligkeitsplan abrufen
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
    public ResponseEntity<DueScheduleDto> createDueSchedule(@RequestBody DueScheduleDto dueScheduleDto) {
        DueScheduleDto createdDueSchedule = dueScheduleService.createDueSchedule(dueScheduleDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDueSchedule);
    }

    // Fälligkeitsplan aktualisieren
    @PutMapping("/{id}")
    public ResponseEntity<DueScheduleDto> updateDueSchedule(
            @PathVariable UUID id,
            @RequestBody DueScheduleDto dueScheduleDto) {
        DueScheduleDto updatedDueSchedule = dueScheduleService.updateDueSchedule(id, dueScheduleDto);
        return ResponseEntity.ok(updatedDueSchedule);
    }

    // Fälligkeitsplan löschen
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDueSchedule(@PathVariable UUID id) {
        dueScheduleService.deleteDueSchedule(id);
        return ResponseEntity.noContent().build();
    }

    // Fälligkeitspläne nach Abonnement
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesBySubscription(@PathVariable UUID subscriptionId) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesBySubscription(subscriptionId);
        return ResponseEntity.ok(dueSchedules);
    }

    // Fälligkeitspläne nach Status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesByStatus(@PathVariable DueStatus status) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesByStatus(status);
        return ResponseEntity.ok(dueSchedules);
    }

    // Überfällige Fälligkeitspläne
    @GetMapping("/overdue")
    public ResponseEntity<List<DueScheduleDto>> getOverdueDueSchedules() {
        List<DueScheduleDto> overdueDueSchedules = dueScheduleService.getOverdueDueSchedules();
        return ResponseEntity.ok(overdueDueSchedules);
    }

    // Fälligkeitspläne heute fällig
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

    // Generierung von Fälligkeitsplänen für ein Abonnement
    @PostMapping("/subscription/{subscriptionId}/generate")
    public ResponseEntity<List<DueScheduleDto>> generateDueSchedulesForSubscription(
            @PathVariable UUID subscriptionId,
            @RequestParam int months) {
        List<DueScheduleDto> generatedSchedules = dueScheduleService.generateDueSchedulesForSubscription(subscriptionId, months);
        return ResponseEntity.status(HttpStatus.CREATED).body(generatedSchedules);
    }

    // Nächste fällige Fälligkeit für Abonnement
    @GetMapping("/subscription/{subscriptionId}/next-due")
    public ResponseEntity<DueScheduleDto> getNextDueScheduleBySubscription(@PathVariable UUID subscriptionId) {
        DueScheduleDto nextDueSchedule = dueScheduleService.getNextDueScheduleBySubscription(subscriptionId);
        return ResponseEntity.ok(nextDueSchedule);
    }
}
