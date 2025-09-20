// ===============================================================================================
// 2. INVOICE BATCH ANALYZER (Analyse und Scoping)
// ===============================================================================================

package com.erp.backend.service.batch;

import com.erp.backend.domain.DueSchedule;
import com.erp.backend.domain.DueStatus;
import com.erp.backend.repository.DueScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analysiert und bestimmt den Umfang eines Rechnungslaufs.
 * Verantwortlich für: Fälligkeiten-Auswahl, Analyse, Statistiken
 */
@Service
@Transactional(readOnly = true)
public class InvoiceBatchAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceBatchAnalyzer.class);

    private final DueScheduleRepository dueScheduleRepository;

    public InvoiceBatchAnalyzer(DueScheduleRepository dueScheduleRepository) {
        this.dueScheduleRepository = dueScheduleRepository;
    }

    public InvoiceBatchAnalysis analyzeBillingScope(LocalDate billingDate, boolean includeAllPreviousMonths) {
        // Fälligkeiten ermitteln
        List<DueSchedule> schedules = includeAllPreviousMonths ?
                dueScheduleRepository.findByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, billingDate) :
                dueScheduleRepository.findByStatusAndDueDate(DueStatus.ACTIVE, billingDate);

        // Analyse durchführen
        return createAnalysis(schedules, billingDate, includeAllPreviousMonths);
    }

    private InvoiceBatchAnalysis createAnalysis(List<DueSchedule> schedules, LocalDate billingDate, boolean includeAllPreviousMonths) {
        long overdueCount = schedules.stream()
                .filter(ds -> ds.getDueDate().isBefore(billingDate))
                .count();

        long currentCount = schedules.stream()
                .filter(ds -> ds.getDueDate().equals(billingDate))
                .count();

        Map<String, List<DueSchedule>> monthGroups = schedules.stream()
                .collect(Collectors.groupingBy(ds ->
                        ds.getDueDate().getYear() + "-" + String.format("%02d", ds.getDueDate().getMonthValue())));

        InvoiceBatchAnalysis analysis = new InvoiceBatchAnalysis(
                schedules, billingDate, includeAllPreviousMonths,
                overdueCount, currentCount, monthGroups
        );

        logAnalysis(analysis);
        return analysis;
    }

    private void logAnalysis(InvoiceBatchAnalysis analysis) {
        logger.info("RECHNUNGSLAUF-ANALYSE:");
        logger.info("- Modus: {}", analysis.isIncludeAllPreviousMonths() ? "alle offenen Monate" : "nur exakter Stichtag");
        logger.info("- Gesamt: {} Fälligkeiten", analysis.getTotalCount());
        logger.info("- Überfällig: {} Fälligkeiten", analysis.getOverdueCount());
        logger.info("- Aktueller Stichtag: {} Fälligkeiten", analysis.getCurrentCount());
        logger.info("- Betroffene Monate: {}", analysis.getMonthCount());

        logger.info("AUFSCHLÜSSELUNG NACH MONATEN:");
        analysis.getMonthGroups().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry ->
                        logger.info("- {}: {} Fälligkeiten", entry.getKey(), entry.getValue().size()));
    }

    public boolean canRunBillingBatch(LocalDate billingDate, boolean includeAllPreviousMonths) {
        long count = includeAllPreviousMonths ?
                dueScheduleRepository.countByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, billingDate) :
                dueScheduleRepository.countByStatusAndDueDateLessThanEqual(DueStatus.ACTIVE, billingDate);
        return count > 0;
    }
}