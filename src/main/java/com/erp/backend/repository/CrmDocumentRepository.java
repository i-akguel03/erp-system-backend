package com.erp.backend.repository;

import com.erp.backend.domain.CrmDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CrmDocumentRepository extends JpaRepository<CrmDocument, UUID> {

    List<CrmDocument> findByCustomerId(UUID customerId);

    List<CrmDocument> findByContractId(UUID contractId);

    Page<CrmDocument> findByCustomerId(UUID customerId, Pageable pageable);

    Page<CrmDocument> findByContractId(UUID contractId, Pageable pageable);

    List<CrmDocument> findByCustomerIdAndDocumentType(UUID customerId, CrmDocument.DocumentType documentType);

    List<CrmDocument> findByContractIdAndDocumentType(UUID contractId, CrmDocument.DocumentType documentType);

    long countByCustomerId(UUID customerId);

    long countByContractId(UUID contractId);
}
