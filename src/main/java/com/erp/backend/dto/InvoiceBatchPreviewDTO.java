package com.erp.backend.dto;// InvoiceBatchPreviewDTO.java

import com.erp.backend.service.batch.InvoiceBatchAnalysis;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InvoiceBatchPreviewDTO {
    private Map<String, List<DueScheduleDto>> monthGroups;
    private int totalCount;
    private BigDecimal estimatedTotal;

    public InvoiceBatchPreviewDTO(InvoiceBatchAnalysis analysis, BigDecimal estimatedTotal) {
        this.totalCount = analysis.getDueSchedules().size();
        this.estimatedTotal = estimatedTotal;

        // Konvertiere zu DTOs
//        this.monthGroups = analysis.getMonthGroups().entrySet().stream()
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        entry -> entry.getValue().stream()
//                                .map(DueScheduleDto::new)
//                                .collect(Collectors.toList())
//                ));
    }

    // Getters
    public Map<String, List<DueScheduleDto>> getMonthGroups() { return monthGroups; }
    public int getTotalCount() { return totalCount; }
    public BigDecimal getEstimatedTotal() { return estimatedTotal; }
}