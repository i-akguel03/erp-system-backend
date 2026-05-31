package com.erp.backend.repository;

import com.erp.backend.domain.CrmNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CrmNoteRepository extends JpaRepository<CrmNote, UUID> {

    List<CrmNote> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<CrmNote> findByContractIdOrderByCreatedAtDesc(UUID contractId);

    Page<CrmNote> findByCustomerId(UUID customerId, Pageable pageable);

    Page<CrmNote> findByContractId(UUID contractId, Pageable pageable);

    List<CrmNote> findByCustomerIdAndPriority(UUID customerId, CrmNote.NotePriority priority);

    List<CrmNote> findByContractIdAndPriority(UUID contractId, CrmNote.NotePriority priority);

    long countByCustomerId(UUID customerId);

    long countByContractId(UUID contractId);
}
