package com.erp.backend.service;

import com.erp.backend.domain.Address;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.AddressRepository;
import com.erp.backend.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Page<Address> findAll(Pageable pageable) {
        return addressRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Address> findById(Long id) {
        return addressRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Address> search(String query) {
        return addressRepository.searchByQuery(query.toLowerCase());
    }

    public void deleteById(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adresse nicht gefunden mit ID: " + id));

        long usageCount = countUsages(address);
        if (usageCount > 0) {
            logger.warn("Cannot delete address id={} – still referenced by {} customer(s)", id, usageCount);
            throw new BusinessLogicException(
                    "Adresse kann nicht gelöscht werden – wird noch von " + usageCount + " Kunden als Wohn-, Rechnungs- oder Lieferadresse verwendet.");
        }

        addressRepository.deleteById(id);
        logger.info("Deleted address id={}", id);
    }

    @Transactional(readOnly = true)
    public boolean isAddressInUse(Long id) {
        return addressRepository.findById(id)
                .map(address -> countUsages(address) > 0)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public long getUsageCount(Long id) {
        return addressRepository.findById(id)
                .map(this::countUsages)
                .orElse(0L);
    }

    private long countUsages(Address address) {
        return customerRepository.countByResidentialAddress(address)
             + customerRepository.countByBillingAddress(address)
             + customerRepository.countByShippingAddress(address);
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