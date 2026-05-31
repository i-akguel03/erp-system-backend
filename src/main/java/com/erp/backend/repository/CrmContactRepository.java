package com.erp.backend.repository;

import com.erp.backend.domain.CrmContact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmContactRepository extends JpaRepository<CrmContact, UUID> {

    List<CrmContact> findByCustomerIdOrderByPrimaryContactDescLastNameAsc(UUID customerId);

    Page<CrmContact> findByCustomerId(UUID customerId, Pageable pageable);

    Optional<CrmContact> findByCustomerIdAndPrimaryContactTrue(UUID customerId);

    List<CrmContact> findByCustomerIdAndDepartment(UUID customerId, String department);

    List<CrmContact> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName);

    long countByCustomerId(UUID customerId);
}
