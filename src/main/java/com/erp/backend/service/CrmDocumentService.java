package com.erp.backend.service;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.CrmDocument;
import com.erp.backend.domain.Customer;
import com.erp.backend.dto.CrmDocumentDto;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.CrmDocumentRepository;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CrmDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(CrmDocumentService.class);

    private final CrmDocumentRepository documentRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final FileStorageService fileStorageService;

    public CrmDocumentService(CrmDocumentRepository documentRepository,
                               CustomerRepository customerRepository,
                               ContractRepository contractRepository,
                               FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.customerRepository = customerRepository;
        this.contractRepository = contractRepository;
        this.fileStorageService = fileStorageService;
    }

    public CrmDocument uploadDocument(MultipartFile file,
                                      UUID customerId,
                                      UUID contractId,
                                      CrmDocument.DocumentType documentType,
                                      String description) {
        if (customerId == null && contractId == null) {
            throw new BusinessLogicException("Dokument muss einem Kunden oder Vertrag zugeordnet sein");
        }

        CrmDocument doc = new CrmDocument();
        doc.setOriginalFileName(file.getOriginalFilename());
        doc.setMimeType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setDocumentType(documentType != null ? documentType : CrmDocument.DocumentType.SONSTIGES);
        doc.setDescription(description);
        doc.setUploadedBy(getCurrentUsername());

        String subDir = resolveSubDirectory(customerId, contractId);
        String relativePath = fileStorageService.store(file, subDir);
        doc.setFilePath(relativePath);
        doc.setStoredFileName(relativePath.substring(relativePath.lastIndexOf('/') + 1));

        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Kunde nicht gefunden: " + customerId));
            doc.setCustomer(customer);
        }
        if (contractId != null) {
            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vertrag nicht gefunden: " + contractId));
            doc.setContract(contract);
        }

        CrmDocument saved = documentRepository.save(doc);
        logger.info("Dokument hochgeladen: id={}, datei={}, kunde={}, vertrag={}",
                saved.getId(), saved.getOriginalFileName(), customerId, contractId);
        return saved;
    }

    @Transactional(readOnly = true)
    public byte[] downloadDocument(UUID id) {
        CrmDocument doc = findById(id);
        return fileStorageService.load(doc.getFilePath());
    }

    @Transactional(readOnly = true)
    public CrmDocument findById(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dokument nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public List<CrmDocumentDto> getDocumentsByCustomer(UUID customerId) {
        return documentRepository.findByCustomerId(customerId).stream()
                .map(CrmDocumentDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<CrmDocumentDto> getDocumentsByCustomer(UUID customerId, Pageable pageable) {
        return documentRepository.findByCustomerId(customerId, pageable).map(CrmDocumentDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CrmDocumentDto> getDocumentsByContract(UUID contractId) {
        return documentRepository.findByContractId(contractId).stream()
                .map(CrmDocumentDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<CrmDocumentDto> getDocumentsByContract(UUID contractId, Pageable pageable) {
        return documentRepository.findByContractId(contractId, pageable).map(CrmDocumentDto::fromEntity);
    }

    public CrmDocument updateDescription(UUID id, String description, CrmDocument.DocumentType documentType) {
        CrmDocument doc = findById(id);
        if (description != null) doc.setDescription(description);
        if (documentType != null) doc.setDocumentType(documentType);
        CrmDocument saved = documentRepository.save(doc);
        logger.info("Dokument aktualisiert: id={}", id);
        return saved;
    }

    public void deleteDocument(UUID id) {
        CrmDocument doc = findById(id);
        String filePath = doc.getFilePath();
        documentRepository.delete(doc);
        fileStorageService.delete(filePath);
        logger.info("Dokument gelöscht (soft): id={}", id);
    }

    private String resolveSubDirectory(UUID customerId, UUID contractId) {
        if (customerId != null) return "customers/" + customerId;
        return "contracts/" + contractId;
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}
