package com.erp.backend.service;

import com.erp.backend.domain.Customer;
import com.erp.backend.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    private final CustomerRepository repo = Mockito.mock(CustomerRepository.class);
    private final CustomerService service = new CustomerService(repo);

    @Test
    void shouldReturnAllCustomers() {
        Customer c1 = new Customer("1", "Max", "Muster", "max@example.com", "123");
        Customer c2 = new Customer("2", "Erika", "Muster", "erika@example.com", "456");

        when(repo.findAll()).thenReturn(Arrays.asList(c1, c2));

        List<Customer> result = service.getAllCustomers();

        assertEquals(2, result.size());
        verify(repo, times(1)).findAll();
    }

    @Test
    void shouldReturnCustomerById() {
        Customer customer = new Customer("1", "Max", "Muster", "max@example.com", "123");
        when(repo.findById("1")).thenReturn(Optional.of(customer));

        Optional<Customer> result = service.getCustomerById("1");

        assertTrue(result.isPresent());
        assertEquals("Max", result.get().getFirstName());
    }

    @Test
    void shouldSaveCustomer() {
        Customer input = new Customer(null, "Max", "Muster", "max@example.com", "123");
        Customer saved = new Customer("1", "Max", "Muster", "max@example.com", "123");

        when(repo.save(input)).thenReturn(saved);

        Customer result = service.createOrUpdateCustomer(input);

        assertEquals("1", result.getId());
        verify(repo).save(input);
    }

    @Test
    void shouldDeleteCustomer() {
        service.deleteCustomerById("1");
        verify(repo, times(1)).deleteById("1");
    }
}
