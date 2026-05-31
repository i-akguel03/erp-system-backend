package com.erp.backend.service;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.CrmNote;
import com.erp.backend.domain.Customer;
import com.erp.backend.dto.CrmNoteDto;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.CrmNoteRepository;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CrmNoteService {

    private static final Logger logger = LoggerFactory.getLogger(CrmNoteService.class);

    private final CrmNoteRepository noteRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;

    public CrmNoteService(CrmNoteRepository noteRepository,
                          CustomerRepository customerRepository,
                          ContractRepository contractRepository) {
        this.noteRepository = noteRepository;
        this.customerRepository = customerRepository;
        this.contractRepository = contractRepository;
    }

    public CrmNote createNote(CrmNote note, UUID customerId, UUID contractId) {
        if (customerId == null && contractId == null) {
            throw new BusinessLogicException("Notiz muss einem Kunden oder Vertrag zugeordnet sein");
        }

        note.setId(null);
        note.setCreatedBy(getCurrentUsername());

        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Kunde nicht gefunden: " + customerId));
            note.setCustomer(customer);
        }
        if (contractId != null) {
            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vertrag nicht gefunden: " + contractId));
            note.setContract(contract);
        }

        CrmNote saved = noteRepository.save(note);
        logger.info("Notiz erstellt: id={}, titel={}", saved.getId(), saved.getTitle());
        return saved;
    }

    public CrmNote updateNote(UUID id, CrmNote updated) {
        CrmNote existing = findById(id);
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        if (updated.getPriority() != null) existing.setPriority(updated.getPriority());
        CrmNote saved = noteRepository.save(existing);
        logger.info("Notiz aktualisiert: id={}", id);
        return saved;
    }

    @Transactional(readOnly = true)
    public CrmNote findById(UUID id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notiz nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public List<CrmNoteDto> getNotesByCustomer(UUID customerId) {
        return noteRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(CrmNoteDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<CrmNoteDto> getNotesByCustomer(UUID customerId, Pageable pageable) {
        return noteRepository.findByCustomerId(customerId, pageable).map(CrmNoteDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CrmNoteDto> getNotesByContract(UUID contractId) {
        return noteRepository.findByContractIdOrderByCreatedAtDesc(contractId).stream()
                .map(CrmNoteDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<CrmNoteDto> getNotesByContract(UUID contractId, Pageable pageable) {
        return noteRepository.findByContractId(contractId, pageable).map(CrmNoteDto::fromEntity);
    }

    public void deleteNote(UUID id) {
        CrmNote note = findById(id);
        noteRepository.delete(note);
        logger.info("Notiz gelöscht (soft): id={}", id);
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}
