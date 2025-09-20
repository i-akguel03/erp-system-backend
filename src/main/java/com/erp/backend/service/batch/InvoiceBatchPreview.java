// ===============================================================================================
// 8. INVOICE BATCH PREVIEW (Enhanced)
// ===============================================================================================

package com.erp.backend.service.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import com.erp.backend.domain.DueSchedule;

/**
 * Immutable Preview Class für geplante Rechnungsläufe.
 * Zeigt was passieren würde ohne tatsächliche Ausführung.
 */
public class InvoiceBatchPreview {

    private final InvoiceBatchAnalysis analysis;
    private final BigDecimal estimatedAmount;

    public InvoiceBatchPreview(InvoiceBatchAnalysis analysis, BigDecimal estimatedAmount) {
        this.analysis = analysis;
        this.estimatedAmount = estimatedAmount != null ? estimatedAmount : BigDecimal.ZERO;
    }

    // Delegated getters from analysis
    public int getTotalDueSchedules() { return analysis.getTotalCount(); }
    public long getOverdueCount() { return analysis.getOverdueCount(); }
    public long getCurrentCount() { return analysis.getCurrentCount(); }
    public LocalDate getBillingDate() { return analysis.getBillingDate(); }
    public boolean isIncludeAllPreviousMonths() { return analysis.isIncludeAllPreviousMonths(); }
    public Map<String, List<DueSchedule>> getMonthGroups() { return analysis.getMonthGroups(); }
    public int getMonthCount() { return analysis.getMonthCount(); }

    // Own properties
    public BigDecimal getEstimatedAmount() { return estimatedAmount; }

    @Override
    public String toString() {
        String mode = isIncludeAllPreviousMonths() ? "alle offenen Monate" : "nur exakter Stichtag";
        return String.format(
                "Rechnungslauf-Vorschau für %s (%s): %d Monate, %d Fälligkeiten (%d überfällig, %d aktuell), geschätzt %.2f EUR",
                getBillingDate(), mode, getMonthCount(), getTotalDueSchedules(),
                getOverdueCount(), getCurrentCount(), estimatedAmount);
    }
}