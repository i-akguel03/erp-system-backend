package com.erp.backend.mapper;

import com.erp.backend.domain.Contract;
import com.erp.backend.domain.Customer;
import com.erp.backend.domain.Subscription;
import com.erp.backend.dto.ContractDTO;

import java.util.stream.Collectors;

public class ContractMapper {

    public static ContractDTO toDTO(Contract contract) {
        ContractDTO dto = new ContractDTO();
        dto.setId(contract.getId());
        dto.setContractNumber(contract.getContractNumber());
        dto.setContractTitle(contract.getContractTitle());
        dto.setStartDate(contract.getStartDate());
        dto.setEndDate(contract.getEndDate());
        dto.setContractStatus(contract.getContractStatus());
        dto.setNotes(contract.getNotes());

        if (contract.getCustomer() != null) {
            dto.setCustomerId(contract.getCustomer().getId());
        }

        if (contract.getSubscriptions() != null) {
            dto.setSubscriptionIds(
                    contract.getSubscriptions().stream()
                            .map(Subscription::getId)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    public static Contract toEntity(ContractDTO dto, Customer customer) {
        Contract contract = new Contract();
        contract.setId(dto.getId());
        contract.setContractNumber(dto.getContractNumber());
        contract.setContractTitle(dto.getContractTitle());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setContractStatus(dto.getContractStatus());
        contract.setNotes(dto.getNotes());
        contract.setCustomer(customer); // Customer musst du separat laden

        return contract;
    }

    public static Contract toEntity(ContractDTO dto) {
        Contract contract = new Contract();
        contract.setId(dto.getId());
        contract.setContractNumber(dto.getContractNumber());
        contract.setContractTitle(dto.getContractTitle());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setContractStatus(dto.getContractStatus());
        contract.setNotes(dto.getNotes());
        // Customer wird hier NICHT gesetzt!
        return contract;
    }

}
