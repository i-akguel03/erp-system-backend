package com.erp.backend.event;

import com.erp.backend.domain.Customer;
import org.springframework.context.ApplicationEvent;

public class CustomerCreatedEvent extends ApplicationEvent {
    private final Customer customer;

    public CustomerCreatedEvent(Object source, Customer customer) {
        super(source);
        this.customer = customer;
    }

    public Customer getCustomer() { return customer; }
}
