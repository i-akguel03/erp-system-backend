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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: DashboardService")
class DashboardServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private OpenItemRepository openItemRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {}

    // ===== KPI Tests =====

    @Test
    @DisplayName("getKpi gibt korrekte Kundenzahl zurück")
    void getKpi_returnsCorrectCustomerCount() {
        setupKpiMocks(42L, 20L, new BigDecimal("5000.00"),
                10L, new BigDecimal("12000.00"), new BigDecimal("3000.00"), 5L, List.of());

        DashboardKpiDto result = dashboardService.getKpi();

        assertThat(result.getTotalCustomers()).isEqualTo(42L);
    }

    @Test
    @DisplayName("getKpi gibt korrekte MRR zurück")
    void getKpi_returnsCorrectMrr() {
        setupKpiMocks(10L, 15L, new BigDecimal("7500.00"),
                5L, new BigDecimal("8000.00"), new BigDecimal("1500.00"), 2L, List.of());

        DashboardKpiDto result = dashboardService.getKpi();

        assertThat(result.getMonthlyRecurringRevenue()).isEqualByComparingTo("7500.00");
        assertThat(result.getActiveSubscriptions()).isEqualTo(15L);
    }

    @Test
    @DisplayName("getKpi behandelt null-Werte aus Repository (gibt 0 zurück)")
    void getKpi_handlesNullRepositoryValues() {
        when(customerRepository.count()).thenReturn(0L);
        when(subscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.ACTIVE)).thenReturn(0L);
        when(subscriptionRepository.calculateTotalActiveRevenue()).thenReturn(null);
        when(invoiceRepository.countByStatusIn(any())).thenReturn(0L);
        when(invoiceRepository.sumTotalAmountByStatus(Invoice.InvoiceStatus.ACTIVE)).thenReturn(null);
        when(invoiceRepository.sumTotalAmountByStatus(Invoice.InvoiceStatus.SENT)).thenReturn(null);
        when(openItemRepository.getTotalOutstandingAmount()).thenReturn(null);
        when(openItemRepository.countOverdueItems(any())).thenReturn(0L);
        when(openItemRepository.findOverdueItems(any())).thenReturn(List.of());

        DashboardKpiDto result = dashboardService.getKpi();

        assertThat(result.getMonthlyRecurringRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getOverdueItemsAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ===== Monthly Revenue Tests =====

    @Test
    @DisplayName("getMonthlyRevenue gibt immer 12 Monate zurück")
    void getMonthlyRevenue_alwaysReturns12Months() {
        when(invoiceRepository.findMonthlyRevenue(2026)).thenReturn(List.of());

        List<MonthlyRevenueDto> result = dashboardService.getMonthlyRevenue(2026);

        assertThat(result).hasSize(12);
    }

    @Test
    @DisplayName("getMonthlyRevenue füllt fehlende Monate mit 0")
    void getMonthlyRevenue_fillsMissingMonthsWithZero() {
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{1, 2026, new BigDecimal("1500.00"), 3L});
        when(invoiceRepository.findMonthlyRevenue(2026)).thenReturn(rows);

        List<MonthlyRevenueDto> result = dashboardService.getMonthlyRevenue(2026);

        assertThat(result.get(0).getTotalAmount()).isEqualByComparingTo("1500.00");
        assertThat(result.get(0).getInvoiceCount()).isEqualTo(3L);
        assertThat(result.get(1).getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.get(1).getInvoiceCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getMonthlyRevenue setzt monthLabel korrekt")
    void getMonthlyRevenue_setsMonthLabel() {
        when(invoiceRepository.findMonthlyRevenue(2026)).thenReturn(List.of());

        List<MonthlyRevenueDto> result = dashboardService.getMonthlyRevenue(2026);

        assertThat(result.get(0).getMonthLabel()).contains("2026");
        assertThat(result.get(0).getMonth()).isEqualTo(1);
        assertThat(result.get(11).getMonth()).isEqualTo(12);
    }

    // ===== Open Items Overview Tests =====

    @Test
    @DisplayName("getOpenItemsOverview berechnet Gesamtbetrag korrekt")
    void getOpenItemsOverview_calculatesTotal() {
        when(openItemRepository.getTotalOutstandingAmount()).thenReturn(new BigDecimal("5000.00"));
        when(openItemRepository.countByStatus(OpenItem.OpenItemStatus.OPEN)).thenReturn(10L);
        when(openItemRepository.countByStatus(OpenItem.OpenItemStatus.PARTIALLY_PAID)).thenReturn(3L);
        when(openItemRepository.countByStatus(OpenItem.OpenItemStatus.OVERDUE)).thenReturn(5L);
        when(openItemRepository.sumAmountByStatus(OpenItem.OpenItemStatus.OPEN)).thenReturn(new BigDecimal("3000.00"));
        when(openItemRepository.sumAmountByStatus(OpenItem.OpenItemStatus.PARTIALLY_PAID)).thenReturn(new BigDecimal("500.00"));
        when(openItemRepository.sumAmountByStatus(OpenItem.OpenItemStatus.OVERDUE)).thenReturn(new BigDecimal("1500.00"));
        when(openItemRepository.findAllOpenItems()).thenReturn(List.of());

        OpenItemsOverviewDto result = dashboardService.getOpenItemsOverview();

        assertThat(result.getTotalOutstandingAmount()).isEqualByComparingTo("5000.00");
        assertThat(result.getTotalOpenCount()).isEqualTo(18L);
        assertThat(result.getOpen().getCount()).isEqualTo(10L);
        assertThat(result.getOverdue().getAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("getOpenItemsOverview berechnet Aging-Buckets korrekt")
    void getOpenItemsOverview_calculatesAgingBuckets() {
        OpenItem currentItem = createOpenItemWithDue(LocalDate.now().plusDays(10), new BigDecimal("500.00"));
        OpenItem overdueItem = createOpenItemWithDue(LocalDate.now().minusDays(15), new BigDecimal("300.00"));
        OpenItem oldOverdueItem = createOpenItemWithDue(LocalDate.now().minusDays(100), new BigDecimal("200.00"));

        when(openItemRepository.getTotalOutstandingAmount()).thenReturn(new BigDecimal("1000.00"));
        when(openItemRepository.countByStatus(any())).thenReturn(0L);
        when(openItemRepository.sumAmountByStatus(any())).thenReturn(BigDecimal.ZERO);
        when(openItemRepository.findAllOpenItems()).thenReturn(List.of(currentItem, overdueItem, oldOverdueItem));

        OpenItemsOverviewDto result = dashboardService.getOpenItemsOverview();

        assertThat(result.getAging().getCurrent()).isEqualByComparingTo("500.00");
        assertThat(result.getAging().getDays1to30()).isEqualByComparingTo("300.00");
        assertThat(result.getAging().getOver90days()).isEqualByComparingTo("200.00");
    }

    // ===== Outstanding Payments Tests =====

    @Test
    @DisplayName("getOutstandingPayments gibt korrekte Gesamtsummen zurück")
    void getOutstandingPayments_returnsCorrectTotals() {
        when(openItemRepository.getTotalOutstandingAmount()).thenReturn(new BigDecimal("8000.00"));
        when(openItemRepository.countByStatus(OpenItem.OpenItemStatus.OPEN)).thenReturn(20L);
        when(openItemRepository.countByStatus(OpenItem.OpenItemStatus.OVERDUE)).thenReturn(8L);
        when(openItemRepository.countByStatus(OpenItem.OpenItemStatus.PARTIALLY_PAID)).thenReturn(4L);
        when(openItemRepository.findOverdueItems(any())).thenReturn(List.of());
        when(openItemRepository.sumAmountByStatus(OpenItem.OpenItemStatus.PARTIALLY_PAID)).thenReturn(new BigDecimal("600.00"));
        when(openItemRepository.getTotalPaidAmount()).thenReturn(new BigDecimal("15000.00"));

        OutstandingPaymentsDto result = dashboardService.getOutstandingPayments();

        assertThat(result.getTotalOutstandingAmount()).isEqualByComparingTo("8000.00");
        assertThat(result.getTotalOutstandingCount()).isEqualTo(32L);
        assertThat(result.getTotalCollectedAmount()).isEqualByComparingTo("15000.00");
        assertThat(result.getPartiallyPaidAmount()).isEqualByComparingTo("600.00");
    }

    // ===== Helper Methods =====

    private void setupKpiMocks(long customers, long activeSubs, BigDecimal mrr,
                               long openInvoices, BigDecimal outstanding,
                               BigDecimal overdueTotal, long overdueCount,
                               List<OpenItem> overdueItems) {
        when(customerRepository.count()).thenReturn(customers);
        when(subscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.ACTIVE)).thenReturn(activeSubs);
        when(subscriptionRepository.calculateTotalActiveRevenue()).thenReturn(mrr);
        when(invoiceRepository.countByStatusIn(any())).thenReturn(openInvoices);
        when(invoiceRepository.sumTotalAmountByStatus(Invoice.InvoiceStatus.ACTIVE)).thenReturn(outstanding.divide(BigDecimal.TWO));
        when(invoiceRepository.sumTotalAmountByStatus(Invoice.InvoiceStatus.SENT)).thenReturn(outstanding.divide(BigDecimal.TWO));
        when(openItemRepository.getTotalOutstandingAmount()).thenReturn(outstanding);
        when(openItemRepository.countOverdueItems(any())).thenReturn(overdueCount);
        when(openItemRepository.findOverdueItems(any())).thenReturn(overdueItems);
    }

    private OpenItem createOpenItemWithDue(LocalDate dueDate, BigDecimal amount) {
        OpenItem item = new OpenItem();
        item.setDueDate(dueDate);
        item.setAmount(amount);
        item.setPaidAmount(BigDecimal.ZERO);
        item.setStatus(OpenItem.OpenItemStatus.OPEN);
        return item;
    }
}
