package com.erp.backend.service;

import com.erp.backend.domain.Address;
import com.erp.backend.repository.AddressRepository;
import com.erp.backend.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class AddressService {

    private static final Logger logger = LoggerFactory.getLogger(AddressService.class);

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;

    public AddressService(AddressRepository addressRepository, CustomerRepository customerRepository) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
    }

    public Address save(Address address) {
        return addressRepository.save(address);
    }

    @Transactional(readOnly = true)
    public List<Address> findAll() {
        return addressRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Address> findById(Long id) {
        return addressRepository.findById(id);
    }

    public void deleteById(Long id) {
        // Prüfen, ob die Adresse existiert
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Address not found with ID: " + id
                ));

        // Prüfen, ob die Adresse noch von Customers verwendet wird
        long customerCount = customerRepository.countByResidentialAddress(address);

        if (customerCount > 0) {
            logger.warn("Cannot delete address with id={} - still used by {} customer(s)", id, customerCount);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete address - still used by " + customerCount + " customer(s)"
            );
        }

        // Adresse kann sicher gelöscht werden
        addressRepository.deleteById(id);
        logger.info("Successfully deleted address with id={}", id);
    }

    /**
     * Alternative Methode mit detaillierteren Informationen über verwendende Customers
     */
    public void deleteByIdDetailed(Long id) {
        // Prüfen, ob die Adresse existiert
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Address not found with ID: " + id
                ));

        // Alle Customers finden, die diese Adresse verwenden
        var customersUsingAddress = customerRepository.findByResidentialAddress(address);

        if (!customersUsingAddress.isEmpty()) {
            // Detaillierte Fehlermeldung mit Customer-Namen
            String customerNames = customersUsingAddress.stream()
                    .map(customer -> customer.getFirstName() + " " + customer.getLastName() + " (ID: " + customer.getId() + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            logger.warn("Cannot delete address with id={} - used by customers: {}", id, customerNames);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete address - still used by customers: " + customerNames
            );
        }

        // Adresse kann sicher gelöscht werden
        addressRepository.deleteById(id);
        logger.info("Successfully deleted address with id={}", id);
    }

    /**
     * Prüft, ob eine Adresse von Customers verwendet wird (ohne zu löschen)
     */
    @Transactional(readOnly = true)
    public boolean isAddressInUse(Long id) {
        return addressRepository.findById(id)
                .map(address -> customerRepository.countByResidentialAddress(address) > 0)
                .orElse(false);
    }

    /**
     * Gibt die Anzahl der Customers zurück, die eine bestimmte Adresse verwenden
     */
    @Transactional(readOnly = true)
    public long getUsageCount(Long id) {
        return addressRepository.findById(id)
                .map(address -> customerRepository.countByResidentialAddress(address))
                .orElse(0L);
    }

    public void initTestAddresses() {
        if(addressRepository.count() > 0) return; // nur einmal

        List<Address> testAddresses = List.of(
                new Address("Hauptstraße 1","10115","Berlin","Deutschland"),
                new Address("Musterweg 5","20095","Hamburg","Deutschland"),
                new Address("Bahnhofstraße 12","80331","München","Deutschland"),
                new Address("Goethestraße 8","70173","Stuttgart","Deutschland"),
                new Address("Schulstraße 3","50667","Köln","Deutschland"),
                new Address("Ringstraße 22","04109","Leipzig","Deutschland"),
                new Address("Marktplatz 7","28195","Bremen","Deutschland"),
                new Address("Friedrichstraße 10","90402","Nürnberg","Deutschland"),
                new Address("Rathausplatz 4","99084","Erfurt","Deutschland"),
                new Address("Königsallee 15","40212","Düsseldorf","Deutschland"),
                new Address("Luisenstraße 18","68159","Mannheim","Deutschland"),
                new Address("Bergstraße 9","44135","Dortmund","Deutschland"),
                new Address("Hafenstraße 6","24103","Kiel","Deutschland"),
                new Address("Mozartstraße 20","97070","Würzburg","Deutschland"),
                new Address("Schlossweg 11","01067","Dresden","Deutschland")
        );

        addressRepository.saveAll(testAddresses);
        logger.info("Test addresses initialized: {}", addressRepository.count());
    }
}