package com.erp.backend.controller;

import com.erp.backend.domain.DueStatus;
import com.erp.backend.dto.DueScheduleDto;
import com.erp.backend.dto.DueScheduleStatisticsDto;
import com.erp.backend.service.DueScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'DUE_SCHEDULES_READ')")
@Tag(name = "Fälligkeitspläne")
public class DueScheduleController {

    @Autowired
    private DueScheduleService dueScheduleService;

    @Operation(summary = "Einzelnen Fälligkeitsplan abrufen")
    @GetMapping("/{id}")
    public ResponseEntity<DueScheduleDto> getDueScheduleById(@PathVariable UUID id) {
        return dueScheduleService.getDueSchedulesBySubscription(id).stream()
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Alle Fälligkeitspläne abrufen — optional paginiert")
    @GetMapping
    public ResponseEntity<List<DueScheduleDto>> getAllDueSchedules(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<DueScheduleDto> schedulePage = dueScheduleService.getAllDueSchedules(pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(schedulePage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(schedulePage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(schedulePage.getContent());
        }
        List<DueScheduleDto> allSchedules = dueScheduleService.getAllDueSchedules();
        if (allSchedules.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(allSchedules);
    }

    @Operation(summary = "Neuen Fälligkeitsplan erstellen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DueScheduleDto> createDueSchedule(@Valid @RequestBody DueScheduleDto dueScheduleDto) {
        DueScheduleDto createdDueSchedule = dueScheduleService.createDueSchedule(dueScheduleDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDueSchedule);
    }

    @Operation(summary = "Fälligkeitsplan aktualisieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DueScheduleDto> updateDueSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody DueScheduleDto dueScheduleDto) {
        DueScheduleDto updatedDueSchedule = dueScheduleService.updateDueSchedule(id, dueScheduleDto);
        return ResponseEntity.ok(updatedDueSchedule);
    }

    @Operation(summary = "Fälligkeitsplan löschen")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDueSchedule(@PathVariable UUID id) {
        dueScheduleService.deleteDueSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Fälligkeitspläne eines Abonnements abrufen")
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesBySubscription(@PathVariable UUID subscriptionId) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesBySubscription(subscriptionId);
        return ResponseEntity.ok(dueSchedules);
    }

    @Operation(summary = "Fälligkeitspläne nach Status filtern")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesByStatus(@PathVariable DueStatus status) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesByStatus(status);
        return ResponseEntity.ok(dueSchedules);
    }

    @Operation(summary = "Überfällige Fälligkeitspläne abrufen")
    @GetMapping("/overdue")
    public ResponseEntity<List<DueScheduleDto>> getOverdueDueSchedules() {
        List<DueScheduleDto> overdueDueSchedules = dueScheduleService.getOverdueDueSchedules();
        return ResponseEntity.ok(overdueDueSchedules);
    }

    @Operation(summary = "Heute fällige Fälligkeitspläne abrufen")
    @GetMapping("/due-today")
    public ResponseEntity<List<DueScheduleDto>> getDueTodaySchedules() {
        List<DueScheduleDto> dueTodaySchedules = dueScheduleService.getDueTodaySchedules();
        return ResponseEntity.ok(dueTodaySchedules);
    }

    @Operation(summary = "Kommende Fälligkeiten abrufen (nächste X Tage)")
    @GetMapping("/upcoming")
    public ResponseEntity<List<DueScheduleDto>> getUpcomingDueSchedules(
            @RequestParam(defaultValue = "7") int days) {
        List<DueScheduleDto> upcomingSchedules = dueScheduleService.getUpcomingDueSchedules(days);
        return ResponseEntity.ok(upcomingSchedules);
    }

    @Operation(summary = "Fälligkeitspläne in Zeitraum abrufen")
    @GetMapping("/period")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesByPeriod(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesByPeriod(startDate, endDate);
        return ResponseEntity.ok(dueSchedules);
    }

    @Operation(summary = "Fälligkeitspläne eines Kunden abrufen")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<DueScheduleDto>> getDueSchedulesByCustomer(@PathVariable UUID customerId) {
        List<DueScheduleDto> dueSchedules = dueScheduleService.getDueSchedulesByCustomer(customerId);
        return ResponseEntity.ok(dueSchedules);
    }

    @Operation(summary = "Fälligkeitsplan-Statistiken abrufen")
    @GetMapping("/statistics")
    public ResponseEntity<DueScheduleStatisticsDto> getDueScheduleStatistics() {
        DueScheduleStatisticsDto statistics = dueScheduleService.getDueScheduleStatistics();
        return ResponseEntity.ok(statistics);
    }

    @Operation(summary = "Fälligkeitspläne für Abonnement generieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/subscription/{subscriptionId}/generate")
    public ResponseEntity<List<DueScheduleDto>> generateDueSchedulesForSubscription(
            @PathVariable UUID subscriptionId,
            @RequestParam int months) {
//        List<DueScheduleDto> generatedSchedules = dueScheduleService.generateDueSchedulesForSubscription(subscriptionId, months);
//        return ResponseEntity.status(HttpStatus.CREATED).body(generatedSchedules);
        return  ResponseEntity.ok(null);
    }

    @Operation(summary = "Nächste Fälligkeit eines Abonnements abrufen")
    @GetMapping("/subscription/{subscriptionId}/next-due")
    public ResponseEntity<DueScheduleDto> getNextDueScheduleBySubscription(@PathVariable UUID subscriptionId) {
        DueScheduleDto nextDueSchedule = dueScheduleService.getNextDueScheduleBySubscription(subscriptionId);
        return ResponseEntity.ok(nextDueSchedule);
    }
}
