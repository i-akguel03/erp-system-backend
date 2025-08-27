package com.erp.backend.service;

import com.erp.backend.domain.Address;
import com.erp.backend.repository.AddressRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {

    private final AddressRepository repository;

    public AddressService(AddressRepository repository) {
        this.repository = repository;
    }

    public Address save(Address address) {
        return repository.save(address);
    }

    public List<Address> findAll() {
        return repository.findAll();
    }

    public Optional<Address> findById(Long id) {
        return repository.findById(id);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public void initTestAddresses() {
        if(repository.count() > 0) return; // nur einmal

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

        repository.saveAll(testAddresses);
    }
}
