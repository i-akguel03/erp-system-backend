package com.erp.backend.service;

import com.erp.backend.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NumberGeneratorService {

    private final AtomicLong dueCounter = new AtomicLong(System.currentTimeMillis() % 900_000 + 100_000);
    private final AtomicLong subscriptionCounter = new AtomicLong(1000);

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final OrderRepository orderRepository;
    private final AngebotRepository angebotRepository;
    private final LieferscheinRepository lieferscheinRepository;

    public NumberGeneratorService(InvoiceRepository invoiceRepository,
                                   CustomerRepository customerRepository,
                                   ContractRepository contractRepository,
                                   OrderRepository orderRepository,
                                   AngebotRepository angebotRepository,
                                   LieferscheinRepository lieferscheinRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.contractRepository = contractRepository;
        this.orderRepository = orderRepository;
        this.angebotRepository = angebotRepository;
        this.lieferscheinRepository = lieferscheinRepository;
    }

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

    public String generateInvoiceNumber() {
        String prefix = "INV";
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String invoiceNumber;
        do {
            invoiceNumber = prefix + "-" + yearMonth + "-" + randomPart(6);
        } while (invoiceRepository.existsByInvoiceNumber(invoiceNumber));
        return invoiceNumber;
    }

    public String generateVorgangsnummer() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequencePart = String.format("%05d", System.currentTimeMillis() % 100000);
        return "VG-" + datePart + "-" + sequencePart;
    }

    public String generatePaymentReference() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "PAY-" + date + "-" + randomPart(6);
    }

    public String generateCustomerNumber() {
        String customerNumber;
        do {
            customerNumber = "CUST-" + randomPart(8);
        } while (customerRepository.existsByCustomerNumber(customerNumber));
        return customerNumber;
    }

    public String generateOrderNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String orderNumber;
        do {
            orderNumber = "ORD-" + year + "-" + randomPart(6);
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    public String generateAngebotNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String angebotNumber;
        do {
            angebotNumber = "ANG-" + year + "-" + randomPart(6);
        } while (angebotRepository.existsByAngebotsnummer(angebotNumber));
        return angebotNumber;
    }

    public String generateLieferscheinNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String lieferscheinNumber;
        do {
            lieferscheinNumber = "LS-" + date + "-" + randomPart(6);
        } while (lieferscheinRepository.existsByLieferscheinnummer(lieferscheinNumber));
        return lieferscheinNumber;
    }

    public String generateContractNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String contractNumber;
        do {
            contractNumber = "CONT-" + year + "-" + randomPart(6);
        } while (contractRepository.existsByContractNumber(contractNumber));
        return contractNumber;
    }

    private String randomPart(int digits) {
        int max = (int) Math.pow(10, digits) - 1;
        return String.format("%0" + digits + "d", (int) (Math.random() * max) + 1);
    }
}
