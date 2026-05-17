package com.erp.backend.event;

import com.erp.backend.domain.Contract;
import org.springframework.context.ApplicationEvent;

public class ContractCreatedEvent extends ApplicationEvent {
    private final Contract contract;

    public ContractCreatedEvent(Object source, Contract contract) {
        super(source);
        this.contract = contract;
    }

    public Contract getContract() { return contract; }
}
