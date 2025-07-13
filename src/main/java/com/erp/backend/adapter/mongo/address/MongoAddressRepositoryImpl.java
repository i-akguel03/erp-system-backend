package com.erp.backend.adapter.mongo.address;

import com.erp.backend.domain.Address;
import com.erp.backend.repository.AddressRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MongoAddressRepositoryImpl implements AddressRepository {

    private final MongoAddressDataRepository mongoRepo;

    public MongoAddressRepositoryImpl(MongoAddressDataRepository mongoRepo) {
        this.mongoRepo = mongoRepo;
    }

    private Address toDomain(AddressDocument doc) {
        return new Address(doc.getId(), doc.getStreet(), doc.getPostalCode(), doc.getCity(), doc.getCountry());
    }

    private AddressDocument toDocument(Address address) {
        return new AddressDocument(address.getId(), address.getStreet(), address.getPostalCode(), address.getCity(), address.getCountry());
    }

    @Override
    public Address save(Address address) {
        return toDomain(mongoRepo.save(toDocument(address)));
    }

    @Override
    public List<Address> findAll() {
        return mongoRepo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Address> findById(String id) {
        return mongoRepo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        mongoRepo.deleteById(id);
    }
}
