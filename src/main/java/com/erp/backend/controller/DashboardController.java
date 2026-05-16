package com.erp.backend.controller;

import com.erp.backend.dto.DashboardKpiDto;
import com.erp.backend.dto.MonthlyRevenueDto;
import com.erp.backend.dto.OpenItemsOverviewDto;
import com.erp.backend.dto.OutstandingPaymentsDto;
import com.erp.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * KPI-Kacheln: Kunden, Abos, MRR, offene Rechnungen, ausstehende Beträge, Überfälligkeiten.
     */
    @GetMapping("/kpi")
    public ResponseEntity<DashboardKpiDto> getKpi() {
        return ResponseEntity.ok(dashboardService.getKpi());
    }

    /**
     * Monatsumsatz für ein Jahr (alle 12 Monate, fehlende Monate mit 0).
     * Basiert auf nicht-stornierten, nicht-Draft Rechnungen.
     */
    @GetMapping("/revenue/monthly")
    public ResponseEntity<List<MonthlyRevenueDto>> getMonthlyRevenue(
            @RequestParam(defaultValue = "0") int year) {
        if (year <= 0) {
            year = LocalDate.now().getYear();
        }
        return ResponseEntity.ok(dashboardService.getMonthlyRevenue(year));
    }

    /**
     * Offene Posten Übersicht: Summen nach Status und Altersstrukturanalyse (Aging).
     */
    @GetMapping("/open-items")
    public ResponseEntity<OpenItemsOverviewDto> getOpenItemsOverview() {
        return ResponseEntity.ok(dashboardService.getOpenItemsOverview());
    }

    /**
     * Ausstehende Zahlungen: Gesamtbetrag, überfällige und Teilzahlungen, bereits eingezahlter Betrag.
     */
    @GetMapping("/payments/outstanding")
    public ResponseEntity<OutstandingPaymentsDto> getOutstandingPayments() {
        return ResponseEntity.ok(dashboardService.getOutstandingPayments());
    }
}
