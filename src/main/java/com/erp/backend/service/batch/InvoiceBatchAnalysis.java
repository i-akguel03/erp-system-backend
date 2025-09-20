// ===============================================================================================
// 6. INVOICE BATCH ANALYSIS (Data Class)
// ===============================================================================================

package com.erp.backend.service.batch;

import com.erp.backend.domain.DueSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Immutable Data Class für Rechnungslauf-Analyse.
 * Enthält alle Informationen über den Umfang eines geplanten Rechnungslaufs.
 */
public class InvoiceBatchAnalysis {

    private final List<DueSchedule> dueSchedules;
    private final LocalDate billingDate;
    private final boolean includeAllPreviousMonths;
    private final long overdueCount;
    private final long currentCount;
    private final Map<String, List<DueSchedule>> monthGroups;

    public InvoiceBatchAnalysis(List<DueSchedule> dueSchedules,
                                LocalDate billingDate,
                                boolean includeAllPreviousMonths,
                                long overdueCount,
                                long currentCount,
                                Map<String, List<DueSchedule>> monthGroups) {
        this.dueSchedules = List.copyOf(dueSchedules); // Immutable copy
        this.billingDate = billingDate;
        this.includeAllPreviousMonths = includeAllPreviousMonths;
        this.overdueCount = overdueCount;
        this.currentCount = currentCount;
        this.monthGroups = Map.copyOf(monthGroups); // Immutable copy
    }

    // Getters
    public List<DueSchedule> getDueSchedules() { return dueSchedules; }
    public LocalDate getBillingDate() { return billingDate; }
    public boolean isIncludeAllPreviousMonths() { return includeAllPreviousMonths; }
    public long getOverdueCount() { return overdueCount; }
    public long getCurrentCount() { return currentCount; }
    public Map<String, List<DueSchedule>> getMonthGroups() { return monthGroups; }

    // Derived properties
    public int getTotalCount() { return dueSchedules.size(); }
    public int getMonthCount() { return monthGroups.size(); }
    public long getFutureCount() { return getTotalCount() - overdueCount - currentCount; }

    public boolean isEmpty() { return dueSchedules.isEmpty(); }
    public boolean hasOverdueItems() { return overdueCount > 0; }
}