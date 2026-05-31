package com.erp.backend.repository;

import com.erp.backend.domain.CrmActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CrmActivityRepository extends JpaRepository<CrmActivity, UUID> {

    List<CrmActivity> findByCustomerIdOrderByActivityDateDesc(UUID customerId);

    List<CrmActivity> findByContractIdOrderByActivityDateDesc(UUID contractId);

    Page<CrmActivity> findByCustomerId(UUID customerId, Pageable pageable);

    Page<CrmActivity> findByContractId(UUID contractId, Pageable pageable);

    List<CrmActivity> findByCustomerIdAndStatus(UUID customerId, CrmActivity.ActivityStatus status);

    List<CrmActivity> findByContractIdAndStatus(UUID contractId, CrmActivity.ActivityStatus status);

    List<CrmActivity> findByCustomerIdAndActivityType(UUID customerId, CrmActivity.ActivityType activityType);

    List<CrmActivity> findByDueDateBeforeAndStatus(LocalDateTime dateTime, CrmActivity.ActivityStatus status);

    long countByCustomerId(UUID customerId);

    long countByContractId(UUID contractId);
}
