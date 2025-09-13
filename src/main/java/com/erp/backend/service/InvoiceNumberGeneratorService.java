package com.erp.backend.service;

import com.erp.backend.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceNumberGeneratorService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    /**
     * Generiert eine neue Rechnungsnummer im Format: RE-YYYY-NNNNNN
     */
    public String generateInvoiceNumber() {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        String prefix = "RE-" + currentYear + "-";

        String highestNumber = invoiceRepository.findHighestInvoiceNumberWithPrefix(prefix)
                .orElse(prefix + "000000");

        // Extrahiere die laufende Nummer
        String numberPart = highestNumber.substring(prefix.length());
        int nextNumber = Integer.parseInt(numberPart) + 1;

        // Formatiere mit führenden Nullen (6 Stellen)
        return prefix + String.format("%06d", nextNumber);
    }
}