package com.erp.backend.service;

import com.erp.backend.domain.Address;
import com.erp.backend.domain.Contract;
import com.erp.backend.domain.ContractStatus;
import com.erp.backend.domain.Customer;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.DuplicateResourceException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.AddressRepository;
import com.erp.backend.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: CustomerService")
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private AddressRepository addressRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer validCustomer;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        validCustomer = new Customer("Max", "Mustermann", "max@test.de", "+49123456");
        validCustomer.setId(customerId);
    }

    // ========== createCustomer ==========

    @Test
    @DisplayName("Kunde wird erfolgreich erstellt")
    void createCustomer_success() {
        when(customerRepository.existsByEmail("max@test.de")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(validCustomer);

        Customer result = customerService.createCustomer(validCustomer);

        assertThat(result.getFirstName()).isEqualTo("Max");
        assertThat(result.getEmail()).isEqualTo("max@test.de");
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Erstellen mit bereits vorhandener E-Mail wirft DuplicateResourceException")
    void createCustomer_duplicateEmail_throwsDuplicateResourceException() {
        when(customerRepository.existsByEmail("max@test.de")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(validCustomer))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("max@test.de");

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Erstellen ohne Vorname wirft BusinessLogicException")
    void createCustomer_missingFirstName_throwsBusinessLogicException() {
        Customer noName = new Customer(null, "Mustermann", "test@test.de", "+49123");

        assertThatThrownBy(() -> customerService.createCustomer(noName))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("Vorname");

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Erstellen ohne Nachname wirft BusinessLogicException")
    void createCustomer_missingLastName_throwsBusinessLogicException() {
        Customer noLastName = new Customer("Max", null, "test@test.de", "+49123");

        assertThatThrownBy(() -> customerService.createCustomer(noLastName))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("Nachname");
    }

    @Test
    @DisplayName("Erstellen ohne E-Mail wirft BusinessLogicException")
    void createCustomer_missingEmail_throwsBusinessLogicException() {
        Customer noEmail = new Customer("Max", "Mustermann", null, "+49123");

        assertThatThrownBy(() -> customerService.createCustomer(noEmail))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("E-Mail");
    }

    // ========== updateCustomer ==========

    @Test
    @DisplayName("Kunde wird erfolgreich aktualisiert")
    void updateCustomer_success() {
        validCustomer.setFirstName("Anna");
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(validCustomer);

        Customer result = customerService.updateCustomer(validCustomer);

        assertThat(result.getFirstName()).isEqualTo("Anna");
        verify(customerRepository, times(1)).save(validCustomer);
    }

    @Test
    @DisplayName("Update ohne ID wirft BusinessLogicException")
    void updateCustomer_nullId_throwsBusinessLogicException() {
        validCustomer.setId(null);

        assertThatThrownBy(() -> customerService.updateCustomer(validCustomer))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    @DisplayName("Update mit unbekannter ID wirft ResourceNotFoundException")
    void updateCustomer_notFound_throwsResourceNotFoundException() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer(validCustomer))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(customerId.toString());
    }

    // ========== deleteCustomerById ==========

    @Test
    @DisplayName("Kunde ohne aktive Verträge wird gelöscht")
    void deleteCustomer_noActiveContracts_success() {
        Contract inactiveContract = new Contract("Vertrag", LocalDate.now(), validCustomer);
        inactiveContract.setContractStatus(ContractStatus.TERMINATED);
        validCustomer.getContracts().add(inactiveContract);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));

        customerService.deleteCustomerById(customerId);

        verify(customerRepository, times(1)).delete(validCustomer);
    }

    @Test
    @DisplayName("Kunde mit aktiven Verträgen kann nicht gelöscht werden")
    void deleteCustomer_withActiveContracts_throwsBusinessLogicException() {
        Contract activeContract = new Contract("Vertrag", LocalDate.now(), validCustomer);
        activeContract.setContractStatus(ContractStatus.ACTIVE);
        validCustomer.getContracts().add(activeContract);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));

        assertThatThrownBy(() -> customerService.deleteCustomerById(customerId))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("aktive Verträge");

        verify(customerRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Löschen eines nicht existierenden Kunden wirft ResourceNotFoundException")
    void deleteCustomer_notFound_throwsResourceNotFoundException() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.deleteCustomerById(customerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(customerId.toString());
    }

    // ========== Abfragen ==========

    @Test
    @DisplayName("getCustomerById gibt Kunden zurück wenn vorhanden")
    void getCustomerById_found() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));

        Optional<Customer> result = customerService.getCustomerById(customerId);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("max@test.de");
    }

    @Test
    @DisplayName("getCustomerById gibt leeres Optional zurück wenn nicht vorhanden")
    void getCustomerById_notFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        Optional<Customer> result = customerService.getCustomerById(customerId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail gibt true zurück wenn E-Mail vergeben")
    void existsByEmail_true() {
        when(customerRepository.existsByEmail("max@test.de")).thenReturn(true);

        assertThat(customerService.existsByEmail("max@test.de")).isTrue();
    }

    @Test
    @DisplayName("getTotalCustomerCount gibt korrekte Anzahl zurück")
    void getTotalCustomerCount() {
        when(customerRepository.count()).thenReturn(42L);

        assertThat(customerService.getTotalCustomerCount()).isEqualTo(42L);
    }
}
