package com.erp.backend.controller;

import com.erp.backend.dto.DashboardKpiDto;
import com.erp.backend.dto.MonthlyRevenueDto;
import com.erp.backend.dto.OpenItemsOverviewDto;
import com.erp.backend.dto.OutstandingPaymentsDto;
import com.erp.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "KPI-Kennzahlen abrufen — Kunden, Abos, MRR, offene Posten")
    @GetMapping("/kpi")
    public ResponseEntity<DashboardKpiDto> getKpi() {
        return ResponseEntity.ok(dashboardService.getKpi());
    }

    @Operation(summary = "Monatsumsatz für ein Jahr abrufen (alle 12 Monate)")
    @GetMapping("/revenue/monthly")
    public ResponseEntity<List<MonthlyRevenueDto>> getMonthlyRevenue(
            @RequestParam(defaultValue = "0") int year) {
        if (year <= 0) {
            year = LocalDate.now().getYear();
        }
        return ResponseEntity.ok(dashboardService.getMonthlyRevenue(year));
    }

    @Operation(summary = "Offene-Posten-Übersicht abrufen (Summen, Aging)")
    @GetMapping("/open-items")
    public ResponseEntity<OpenItemsOverviewDto> getOpenItemsOverview() {
        return ResponseEntity.ok(dashboardService.getOpenItemsOverview());
    }

    @Operation(summary = "Ausstehende Zahlungen abrufen — Gesamtbetrag, überfällig, Teilzahlungen")
    @GetMapping("/payments/outstanding")
    public ResponseEntity<OutstandingPaymentsDto> getOutstandingPayments() {
        return ResponseEntity.ok(dashboardService.getOutstandingPayments());
    }
}
