package com.erp.backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NumberGeneratorService {

    // Seeded from current time so restarts start at a different position
    private final AtomicLong dueCounter = new AtomicLong(System.currentTimeMillis() % 900_000 + 100_000);
    private final AtomicLong subscriptionCounter = new AtomicLong(1000);

    /**
     * Generiert eine eindeutige Fälligkeitsnummer
     * Format: DUE-YYYY-NNNNNN
     */
    public String generateDueNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        long seq = dueCounter.getAndIncrement() % 1_000_000;
        return String.format("DUE-%s-%06d", year, seq);
    }

    public String generateSubscriptionNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long counter = subscriptionCounter.getAndIncrement();
        return String.format("SUB-%s-%04d", dateStr, counter % 10000);
    }

    /**
     * Generiert eine Rechnungsnummer
     * Format: INV-YYYY-MM-NNNNNN
     */
    public String generateInvoiceNumber() {
        String prefix = "INV";
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String randomPart;
        String invoiceNumber;

        do {
            int number = (int) (Math.random() * 999999) + 1;
            randomPart = String.format("%06d", number);
            invoiceNumber = prefix + "-" + yearMonth + "-" + randomPart;
        } while (invoiceNumberExists(invoiceNumber));

        return invoiceNumber;
    }

    /**
     * Generiert eine eindeutige Vorgangsnummer
     * Format: VG-YYYYMMDD-XXXXX
     */
    public String generateVorgangsnummer() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequencePart = String.format("%05d", System.currentTimeMillis() % 100000);
        return "VG-" + datePart + "-" + sequencePart;
    }

    /**
     * Generiert eine Referenznummer für Zahlungen
     * Format: PAY-YYYYMMDD-NNNNNN
     */
    public String generatePaymentReference() {
        String prefix = "PAY";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart;
        String paymentReference;

        do {
            int number = (int) (Math.random() * 999999) + 1;
            randomPart = String.format("%06d", number);
            paymentReference = prefix + "-" + date + "-" + randomPart;
        } while (paymentReferenceExists(paymentReference));

        return paymentReference;
    }

    /**
     * Generiert eine Kundennummer
     * Format: CUST-NNNNNNNN
     */
    public String generateCustomerNumber() {
        String prefix = "CUST";
        String randomPart;
        String customerNumber;

        do {
            int number = (int) (Math.random() * 99999999) + 1;
            randomPart = String.format("%08d", number);
            customerNumber = prefix + "-" + randomPart;
        } while (customerNumberExists(customerNumber));

        return customerNumber;
    }

    /**
     * Generiert eine Auftragsnummer
     * Format: ORD-YYYY-NNNNNN
     */
    public String generateOrderNumber() {
        String prefix = "ORD";
        String year = String.valueOf(LocalDate.now().getYear());
        String randomPart;
        String orderNumber;
        do {
            int number = (int) (Math.random() * 999999) + 1;
            randomPart = String.format("%06d", number);
            orderNumber = prefix + "-" + year + "-" + randomPart;
        } while (orderNumberExists(orderNumber));
        return orderNumber;
    }

    /**
     * Generiert eine Angebotsnummer
     * Format: ANG-YYYY-NNNNNN
     */
    public String generateAngebotNumber() {
        String prefix = "ANG";
        String year = String.valueOf(LocalDate.now().getYear());
        String randomPart;
        String angebotNumber;
        do {
            int number = (int) (Math.random() * 999999) + 1;
            randomPart = String.format("%06d", number);
            angebotNumber = prefix + "-" + year + "-" + randomPart;
        } while (angebotNumberExists(angebotNumber));
        return angebotNumber;
    }

    /**
     * Generiert eine Lieferscheinnummer
     * Format: LS-YYYYMMDD-NNNNNN
     */
    public String generateLieferscheinNumber() {
        String prefix = "LS";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart;
        String lieferscheinNumber;
        do {
            int number = (int) (Math.random() * 999999) + 1;
            randomPart = String.format("%06d", number);
            lieferscheinNumber = prefix + "-" + date + "-" + randomPart;
        } while (lieferscheinNumberExists(lieferscheinNumber));
        return lieferscheinNumber;
    }

    /**
     * Generiert eine Vertragsnummer
     * Format: CONT-YYYY-NNNNNN
     */
    public String generateContractNumber() {
        String prefix = "CONT";
        String year = String.valueOf(LocalDate.now().getYear());
        String randomPart;
        String contractNumber;

        do {
            int number = (int) (Math.random() * 999999) + 1;
            randomPart = String.format("%06d", number);
            contractNumber = prefix + "-" + year + "-" + randomPart;
        } while (contractNumberExists(contractNumber));

        return contractNumber;
    }

    // Hilfsmethoden zur Existenz-Prüfung
    // Diese müssten entsprechend implementiert werden, je nach verfügbaren Repositories

    private boolean invoiceNumberExists(String invoiceNumber) {
        // TODO: Implementation mit InvoiceRepository
        // return invoiceRepository.existsByInvoiceNumber(invoiceNumber);
        return false; // Placeholder
    }

    private boolean paymentReferenceExists(String paymentReference) {
        // TODO: Implementation mit PaymentRepository
        // return paymentRepository.existsByPaymentReference(paymentReference);
        return false; // Placeholder
    }

    private boolean customerNumberExists(String customerNumber) {
        // TODO: Implementation mit CustomerRepository
        // return customerRepository.existsByCustomerNumber(customerNumber);
        return false; // Placeholder
    }

    private boolean contractNumberExists(String contractNumber) {
        return false;
    }

    private boolean orderNumberExists(String orderNumber) {
        return false;
    }

    private boolean angebotNumberExists(String angebotNumber) {
        return false;
    }

    private boolean lieferscheinNumberExists(String lieferscheinNumber) {
        return false;
    }
}