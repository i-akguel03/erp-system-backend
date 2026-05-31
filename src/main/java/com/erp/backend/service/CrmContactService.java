package com.erp.backend.service;

import com.erp.backend.domain.CrmContact;
import com.erp.backend.domain.Customer;
import com.erp.backend.dto.CrmContactDto;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.CrmContactRepository;
import com.erp.backend.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CrmContactService {

    private static final Logger logger = LoggerFactory.getLogger(CrmContactService.class);

    private final CrmContactRepository contactRepository;
    private final CustomerRepository customerRepository;

    public CrmContactService(CrmContactRepository contactRepository,
                              CustomerRepository customerRepository) {
        this.contactRepository = contactRepository;
        this.customerRepository = customerRepository;
    }

    public CrmContact createContact(CrmContact contact, UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Kunde nicht gefunden: " + customerId));

        contact.setId(null);
        contact.setCustomer(customer);

        if (contact.isPrimaryContact()) {
            clearPrimaryContact(customerId);
        }

        CrmContact saved = contactRepository.save(contact);
        logger.info("Ansprechpartner erstellt: id={}, name={}, kunde={}", saved.getId(), saved.getFullName(), customerId);
        return saved;
    }

    public CrmContact updateContact(UUID id, CrmContact updated) {
        CrmContact existing = findById(id);

        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail());
        if (updated.getPhone() != null) existing.setPhone(updated.getPhone());
        if (updated.getMobile() != null) existing.setMobile(updated.getMobile());
        if (updated.getPosition() != null) existing.setPosition(updated.getPosition());
        if (updated.getDepartment() != null) existing.setDepartment(updated.getDepartment());
        if (updated.getNotes() != null) existing.setNotes(updated.getNotes());

        if (updated.isPrimaryContact() && !existing.isPrimaryContact()) {
            clearPrimaryContact(existing.getCustomer().getId());
            existing.setPrimaryContact(true);
        } else if (!updated.isPrimaryContact()) {
            existing.setPrimaryContact(false);
        }

        CrmContact saved = contactRepository.save(existing);
        logger.info("Ansprechpartner aktualisiert: id={}", id);
        return saved;
    }

    public CrmContact setPrimaryContact(UUID id) {
        CrmContact contact = findById(id);
        clearPrimaryContact(contact.getCustomer().getId());
        contact.setPrimaryContact(true);
        CrmContact saved = contactRepository.save(contact);
        logger.info("Hauptansprechpartner gesetzt: id={}", id);
        return saved;
    }

    @Transactional(readOnly = true)
    public CrmContact findById(UUID id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ansprechpartner nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public List<CrmContactDto> getContactsByCustomer(UUID customerId) {
        return contactRepository.findByCustomerIdOrderByPrimaryContactDescLastNameAsc(customerId).stream()
                .map(CrmContactDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<CrmContactDto> getContactsByCustomer(UUID customerId, Pageable pageable) {
        return contactRepository.findByCustomerId(customerId, pageable).map(CrmContactDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CrmContactDto> searchContacts(String query) {
        return contactRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query)
                .stream().map(CrmContactDto::fromEntity).toList();
    }

    public void deleteContact(UUID id) {
        CrmContact contact = findById(id);
        contactRepository.delete(contact);
        logger.info("Ansprechpartner gelöscht (soft): id={}", id);
    }

    private void clearPrimaryContact(UUID customerId) {
        contactRepository.findByCustomerIdAndPrimaryContactTrue(customerId)
                .ifPresent(existing -> {
                    existing.setPrimaryContact(false);
                    contactRepository.save(existing);
                });
    }
}
