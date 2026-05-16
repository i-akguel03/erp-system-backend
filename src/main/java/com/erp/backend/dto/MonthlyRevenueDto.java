package com.erp.backend.dto;

import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

public class MonthlyRevenueDto {

    private int month;
    private int year;
    private String monthLabel;
    private BigDecimal totalAmount;
    private long invoiceCount;

    public MonthlyRevenueDto() {}

    public MonthlyRevenueDto(int month, int year, BigDecimal totalAmount, long invoiceCount) {
        this.month = month;
        this.year = year;
        this.monthLabel = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.GERMAN) + " " + year;
        this.totalAmount = totalAmount;
        this.invoiceCount = invoiceCount;
    }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getMonthLabel() { return monthLabel; }
    public void setMonthLabel(String monthLabel) { this.monthLabel = monthLabel; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public long getInvoiceCount() { return invoiceCount; }
    public void setInvoiceCount(long invoiceCount) { this.invoiceCount = invoiceCount; }
}
