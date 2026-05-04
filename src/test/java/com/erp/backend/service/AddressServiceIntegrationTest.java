package com.erp.backend.service;

import com.erp.backend.BaseIntegrationTest;
import com.erp.backend.domain.Address;
import com.erp.backend.domain.Customer;
import com.erp.backend.repository.AddressRepository;
import com.erp.backend.repository.CustomerRepository;
import com.erp.backend.service.init.InitDataOrchestrator;
import com.erp.backend.service.init.InitMode;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;

/**
 * Integrationstests für AddressService mit Testcontainers (PostgreSQL)
 */
class AddressServiceTest extends BaseIntegrationTest {

    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @MockBean
    private InitDataOrchestrator initDataOrchestrator;  // Mock für problematische Bean

    @Test
    void shouldCreateAndFindAddress() {
        // Given
        Address address = new Address("Teststraße 1", "10115", "Berlin", "Deutschland");

        // When
        Address saved = addressService.save(address);
        Address found = addressService.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(found.getStreet()).isEqualTo("Teststraße 1");
        assertThat(found.getPostalCode()).isEqualTo("10115");
        assertThat(found.getCity()).isEqualTo("Berlin");
        assertThat(found.getCountry()).isEqualTo("Deutschland");
        assertThat(addressRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldDeleteUnusedAddress() {
        // Given
        Address address = new Address("Freie Adresse", "12345", "Teststadt", "Deutschland");
        Address saved = addressService.save(address);

        // When
        addressService.deleteById(saved.getId());

        // Then
        assertThat(addressService.isAddressInUse(saved.getId())).isFalse();
        assertThat(addressRepository.count()).isZero();
    }

    @Test
    void shouldPreventDeletionOfUsedAddress() {
        // Given
        Address address = addressService.save(
                new Address("Verwendete Adresse", "67890", "Berlin", "Deutschland")
        );
        Customer customer = new Customer("Test", "User", "test@example.com", "+49123456");
        customer.setResidentialAddress(address);
        customerRepository.save(customer);

        // When & Then
        assertThat(addressService.isAddressInUse(address.getId())).isTrue();
        assertThrows(ResponseStatusException.class,
                () -> addressService.deleteById(address.getId()));
        assertThat(addressRepository.count()).isOne();
    }

    @Test
    void shouldReturnCorrectUsageCount() {
        // Given
        Address sharedAddress = addressService.save(
                new Address("Gemeinsame Adresse", "11111", "Berlin", "Deutschland")
        );

        Customer customer1 = new Customer("Max", "Mustermann", "max@test.de", "+49123");
        customer1.setResidentialAddress(sharedAddress);
        customerRepository.save(customer1);

        Customer customer2 = new Customer("Anna", "Test", "anna@test.de", "+49876");
        customer2.setResidentialAddress(sharedAddress);
        customerRepository.save(customer2);

        // When
        long usageCount = addressService.getUsageCount(sharedAddress.getId());

        // Then
        assertThat(usageCount).isEqualTo(2);
    }

    @Test
    void shouldInitializeTestAddressesIdempotent() {
        // Given
        assertThat(addressRepository.count()).isZero();

        // When & Then - Mock verhindert InitDataOrchestrator-Probleme
        doNothing().when(initDataOrchestrator).initializeData(InitMode.BASIC, null, null);  // Optional: Mock falls aufgerufen

        addressService.initTestAddresses();
        long countAfterFirst = addressRepository.count();

        addressService.initTestAddresses(); // 2x aufrufen
        long countAfterSecond = addressRepository.count();

        // Then - Erwartung anpassen je nach initTestAddresses-Implementierung
        assertThat(countAfterFirst).isGreaterThan(0);
        assertThat(countAfterSecond).isEqualTo(countAfterFirst); // Idempotent!
    }

    @Test
    void shouldHandleNonExistentAddressGracefully() {
        // When & Then
        assertThat(addressService.findById(999L)).isEmpty();
        assertThrows(ResponseStatusException.class,
                () -> addressService.deleteById(999L));
    }

    @Test
    void shouldUpdateExistingAddress() {
        // Given
        Address address = addressService.save(
                new Address("Alte Straße 1", "20095", "Hamburg", "Deutschland")
        );

        // When
        address.setStreet("Neue Straße 99");
        address.setCity("München");
        Address updated = addressService.save(address);

        // Then
        assertThat(updated.getStreet()).isEqualTo("Neue Straße 99");
        assertThat(updated.getCity()).isEqualTo("München");
        assertThat(addressRepository.count()).isOne();
    }

    @Test
    void shouldFindAllAddresses() {
        // Given
        addressService.save(new Address("Straße 1", "10115", "Berlin", "Deutschland"));
        addressService.save(new Address("Straße 2", "20095", "Hamburg", "Deutschland"));
        addressService.save(new Address("Straße 3", "80331", "München", "Deutschland"));

        // When
        var allAddresses = addressService.findAll();

        // Then
        assertThat(allAddresses).hasSize(3);
    }

    @Test
    void shouldHandleMultipleCustomersWithSameAddress() {
        // Given
        Address popularAddress = addressService.save(
                new Address("Hauptstraße 1", "60311", "Frankfurt", "Deutschland")
        );

        // When - 3 Kunden mit gleicher Adresse
        for (int i = 1; i <= 3; i++) {
            Customer customer = new Customer(
                    "Vorname" + i,
                    "Nachname" + i,
                    "test" + i + "@example.com",
                    "+4912345678" + i
            );
            customer.setResidentialAddress(popularAddress);
            customerRepository.save(customer);
        }

        // Then
        assertThat(addressService.getUsageCount(popularAddress.getId())).isEqualTo(3);
        assertThat(addressService.isAddressInUse(popularAddress.getId())).isTrue();
    }
}
