package com.erp.backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NumberGeneratorService {

    private final AtomicLong dueCounter = new AtomicLong(1000);
    private final AtomicLong contractCounter = new AtomicLong(1000);
    private final AtomicLong subscriptionCounter = new AtomicLong(1000);
    private final AtomicLong customerCounter = new AtomicLong(1000);

    /**
     * Generiert eine eindeutige Fälligkeitsnummer im Format: DUE-YYYYMMDD-NNNN
     */
    public String generateDueNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long counter = dueCounter.getAndIncrement();
        return String.format("DUE-%s-%04d", dateStr, counter % 10000);
    }

    /**
     * Generiert eine eindeutige Vertragsnummer im Format: CON-YYYYMMDD-NNNN
     */
    public String generateContractNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long counter = contractCounter.getAndIncrement();
        return String.format("CON-%s-%04d", dateStr, counter % 10000);
    }

    /**
     * Generiert eine eindeutige Abonnementnummer im Format: SUB-YYYYMMDD-NNNN
     */
    public String generateSubscriptionNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long counter = subscriptionCounter.getAndIncrement();
        return String.format("SUB-%s-%04d", dateStr, counter % 10000);
    }

    /**
     * Generiert eine eindeutige Kundennummer im Format: CUS-YYYYMMDD-NNNN
     */
    public String generateCustomerNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long counter = customerCounter.getAndIncrement();
        return String.format("CUS-%s-%04d", dateStr, counter % 10000);
    }

    /**
     * Generiert eine Rechnungsnummer im Format: INV-YYYYMMDD-NNNN
     */
    public String generateInvoiceNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long counter = System.currentTimeMillis() % 10000;
        return String.format("INV-%s-%04d", dateStr, counter);
    }
}