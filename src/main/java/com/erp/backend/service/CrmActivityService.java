package com.erp.backend.service;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.CrmActivity;
import com.erp.backend.domain.Customer;
import com.erp.backend.dto.CrmActivityDto;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.CrmActivityRepository;
import com.erp.backend.repository.ContractRepository;
import com.erp.backend.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CrmActivityService {

    private static final Logger logger = LoggerFactory.getLogger(CrmActivityService.class);

    private final CrmActivityRepository activityRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;

    public CrmActivityService(CrmActivityRepository activityRepository,
                               CustomerRepository customerRepository,
                               ContractRepository contractRepository) {
        this.activityRepository = activityRepository;
        this.customerRepository = customerRepository;
        this.contractRepository = contractRepository;
    }

    public CrmActivity createActivity(CrmActivity activity, UUID customerId, UUID contractId) {
        if (customerId == null && contractId == null) {
            throw new BusinessLogicException("Aktivität muss einem Kunden oder Vertrag zugeordnet sein");
        }

        activity.setId(null);
        activity.setCreatedBy(getCurrentUsername());
        if (activity.getStatus() == null) activity.setStatus(CrmActivity.ActivityStatus.OFFEN);

        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Kunde nicht gefunden: " + customerId));
            activity.setCustomer(customer);
        }
        if (contractId != null) {
            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vertrag nicht gefunden: " + contractId));
            activity.setContract(contract);
        }

        CrmActivity saved = activityRepository.save(activity);
        logger.info("Aktivität erstellt: id={}, typ={}, status={}", saved.getId(), saved.getActivityType(), saved.getStatus());
        return saved;
    }

    public CrmActivity updateActivity(UUID id, CrmActivity updated) {
        CrmActivity existing = findById(id);
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        if (updated.getActivityType() != null) existing.setActivityType(updated.getActivityType());
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        if (updated.getActivityDate() != null) existing.setActivityDate(updated.getActivityDate());
        if (updated.getDueDate() != null) existing.setDueDate(updated.getDueDate());
        if (updated.getContactPerson() != null) existing.setContactPerson(updated.getContactPerson());
        if (updated.getResult() != null) existing.setResult(updated.getResult());
        CrmActivity saved = activityRepository.save(existing);
        logger.info("Aktivität aktualisiert: id={}", id);
        return saved;
    }

    public CrmActivity completeActivity(UUID id, String result) {
        CrmActivity activity = findById(id);
        activity.setStatus(CrmActivity.ActivityStatus.ABGESCHLOSSEN);
        activity.setResult(result);
        CrmActivity saved = activityRepository.save(activity);
        logger.info("Aktivität abgeschlossen: id={}", id);
        return saved;
    }

    @Transactional(readOnly = true)
    public CrmActivity findById(UUID id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aktivität nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public List<CrmActivityDto> getActivitiesByCustomer(UUID customerId) {
        return activityRepository.findByCustomerIdOrderByActivityDateDesc(customerId).stream()
                .map(CrmActivityDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<CrmActivityDto> getActivitiesByCustomer(UUID customerId, Pageable pageable) {
        return activityRepository.findByCustomerId(customerId, pageable).map(CrmActivityDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CrmActivityDto> getActivitiesByContract(UUID contractId) {
        return activityRepository.findByContractIdOrderByActivityDateDesc(contractId).stream()
                .map(CrmActivityDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<CrmActivityDto> getActivitiesByContract(UUID contractId, Pageable pageable) {
        return activityRepository.findByContractId(contractId, pageable).map(CrmActivityDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CrmActivityDto> getOverdueActivities() {
        return activityRepository.findByDueDateBeforeAndStatus(LocalDateTime.now(), CrmActivity.ActivityStatus.OFFEN)
                .stream().map(CrmActivityDto::fromEntity).toList();
    }

    public void deleteActivity(UUID id) {
        CrmActivity activity = findById(id);
        activityRepository.delete(activity);
        logger.info("Aktivität gelöscht (soft): id={}", id);
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}
