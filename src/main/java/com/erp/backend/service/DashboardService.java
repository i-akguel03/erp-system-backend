package com.erp.backend.service;

import com.erp.backend.domain.Invoice;
import com.erp.backend.domain.OpenItem;
import com.erp.backend.domain.SubscriptionStatus;
import com.erp.backend.dto.DashboardKpiDto;
import com.erp.backend.dto.MonthlyRevenueDto;
import com.erp.backend.dto.OpenItemsOverviewDto;
import com.erp.backend.dto.OutstandingPaymentsDto;
import com.erp.backend.repository.CustomerRepository;
import com.erp.backend.repository.InvoiceRepository;
import com.erp.backend.repository.OpenItemRepository;
import com.erp.backend.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final OpenItemRepository openItemRepository;

    public DashboardService(CustomerRepository customerRepository,
                            SubscriptionRepository subscriptionRepository,
                            InvoiceRepository invoiceRepository,
                            OpenItemRepository openItemRepository) {
        this.customerRepository = customerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.openItemRepository = openItemRepository;
    }

    public DashboardKpiDto getKpi() {
        DashboardKpiDto dto = new DashboardKpiDto();

        dto.setTotalCustomers(customerRepository.count());

        long activeSubs = subscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.ACTIVE);
        dto.setActiveSubscriptions(activeSubs);

        BigDecimal mrr = subscriptionRepository.calculateTotalActiveRevenue();
        dto.setMonthlyRecurringRevenue(orZero(mrr));

        long openInvoicesCount = invoiceRepository.countByStatusIn(
                List.of(Invoice.InvoiceStatus.ACTIVE, Invoice.InvoiceStatus.SENT));
        dto.setOpenInvoicesCount(openInvoicesCount);

        BigDecimal openInvoicesTotal = orZero(invoiceRepository.sumTotalAmountByStatus(Invoice.InvoiceStatus.ACTIVE))
                .add(orZero(invoiceRepository.sumTotalAmountByStatus(Invoice.InvoiceStatus.SENT)));
        dto.setOpenInvoicesTotalAmount(openInvoicesTotal);

        BigDecimal outstanding = openItemRepository.getTotalOutstandingAmount();
        dto.setTotalOutstandingAmount(orZero(outstanding));

        LocalDate today = LocalDate.now();
        long overdueCount = openItemRepository.countOverdueItems(today);
        dto.setOverdueItemsCount(overdueCount);

        BigDecimal overdueAmount = calculateOverdueAmount(today);
        dto.setOverdueItemsAmount(orZero(overdueAmount));

        return dto;
    }

    public List<MonthlyRevenueDto> getMonthlyRevenue(int year) {
        List<Object[]> rows = invoiceRepository.findMonthlyRevenue(year);
        Map<Integer, MonthlyRevenueDto> byMonth = rows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> new MonthlyRevenueDto(
                                ((Number) row[0]).intValue(),
                                ((Number) row[1]).intValue(),
                                (BigDecimal) row[2],
                                ((Number) row[3]).longValue()
                        )
                ));

        List<MonthlyRevenueDto> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            result.add(byMonth.getOrDefault(m, new MonthlyRevenueDto(m, year, BigDecimal.ZERO, 0L)));
        }
        return result;
    }

    public OpenItemsOverviewDto getOpenItemsOverview() {
        LocalDate today = LocalDate.now();

        BigDecimal totalOutstanding = orZero(openItemRepository.getTotalOutstandingAmount());
        long openCount = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OPEN);
        long partiallyPaidCount = openItemRepository.countByStatus(OpenItem.OpenItemStatus.PARTIALLY_PAID);
        long overdueCount = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OVERDUE);
        long totalOpenCount = openCount + partiallyPaidCount + overdueCount;

        BigDecimal openAmount = orZero(openItemRepository.sumAmountByStatus(OpenItem.OpenItemStatus.OPEN));
        BigDecimal partiallyPaidAmount = orZero(openItemRepository.sumAmountByStatus(OpenItem.OpenItemStatus.PARTIALLY_PAID));
        BigDecimal overdueAmount = orZero(openItemRepository.sumAmountByStatus(OpenItem.OpenItemStatus.OVERDUE));

        OpenItemsOverviewDto dto = new OpenItemsOverviewDto();
        dto.setTotalOutstandingAmount(totalOutstanding);
        dto.setTotalOpenCount(totalOpenCount);
        dto.setOpen(new OpenItemsOverviewDto.StatusBreakdown(openCount, openAmount));
        dto.setPartiallyPaid(new OpenItemsOverviewDto.StatusBreakdown(partiallyPaidCount, partiallyPaidAmount));
        dto.setOverdue(new OpenItemsOverviewDto.StatusBreakdown(overdueCount, overdueAmount));
        dto.setAging(buildAgingBreakdown(today));

        return dto;
    }

    public OutstandingPaymentsDto getOutstandingPayments() {
        LocalDate today = LocalDate.now();

        BigDecimal totalOutstanding = orZero(openItemRepository.getTotalOutstandingAmount());
        long openCount = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OPEN);
        long overdueCount = openItemRepository.countByStatus(OpenItem.OpenItemStatus.OVERDUE);
        long partiallyPaidCount = openItemRepository.countByStatus(OpenItem.OpenItemStatus.PARTIALLY_PAID);

        BigDecimal overdueAmount = calculateOverdueAmount(today);
        BigDecimal partiallyPaidAmount = orZero(openItemRepository.sumAmountByStatus(OpenItem.OpenItemStatus.PARTIALLY_PAID));
        BigDecimal collected = orZero(openItemRepository.getTotalPaidAmount());

        OutstandingPaymentsDto dto = new OutstandingPaymentsDto();
        dto.setTotalOutstandingAmount(totalOutstanding);
        dto.setTotalOutstandingCount(openCount + overdueCount + partiallyPaidCount);
        dto.setOverdueAmount(orZero(overdueAmount));
        dto.setOverdueCount(overdueCount);
        dto.setPartiallyPaidAmount(partiallyPaidAmount);
        dto.setPartiallyPaidCount(partiallyPaidCount);
        dto.setTotalCollectedAmount(collected);

        return dto;
    }

    private OpenItemsOverviewDto.AgingBreakdown buildAgingBreakdown(LocalDate today) {
        List<OpenItem> openItems = openItemRepository.findAllOpenItems();

        BigDecimal current = BigDecimal.ZERO;
        BigDecimal days1to30 = BigDecimal.ZERO;
        BigDecimal days31to60 = BigDecimal.ZERO;
        BigDecimal days61to90 = BigDecimal.ZERO;
        BigDecimal over90 = BigDecimal.ZERO;

        for (OpenItem item : openItems) {
            BigDecimal outstanding = item.getOutstandingAmount();
            if (outstanding == null || outstanding.compareTo(BigDecimal.ZERO) <= 0) continue;

            LocalDate dueDate = item.getDueDate();
            if (dueDate == null) continue;

            long daysOverdue = today.toEpochDay() - dueDate.toEpochDay();

            if (daysOverdue <= 0) {
                current = current.add(outstanding);
            } else if (daysOverdue <= 30) {
                days1to30 = days1to30.add(outstanding);
            } else if (daysOverdue <= 60) {
                days31to60 = days31to60.add(outstanding);
            } else if (daysOverdue <= 90) {
                days61to90 = days61to90.add(outstanding);
            } else {
                over90 = over90.add(outstanding);
            }
        }

        return new OpenItemsOverviewDto.AgingBreakdown(current, days1to30, days31to60, days61to90, over90);
    }

    private BigDecimal calculateOverdueAmount(LocalDate today) {
        return openItemRepository.findOverdueItems(today).stream()
                .map(OpenItem::getOutstandingAmount)
                .filter(a -> a != null && a.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
