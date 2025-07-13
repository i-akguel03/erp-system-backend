package com.erp.backend.adapter.mongo.customer;

import com.erp.backend.domain.Customer;
import com.erp.backend.repository.CustomerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MongoCustomerRepositoryImpl implements CustomerRepository {

    private final MongoCustomerDataRepository mongoRepo;

    public MongoCustomerRepositoryImpl(MongoCustomerDataRepository mongoRepo) {
        this.mongoRepo = mongoRepo;
    }

    private Customer toDomain(CustomerDocument doc) {
        Customer customer = new Customer(
                doc.getId(),
                doc.getFirstName(),
                doc.getLastName(),
                doc.getEmail(),
                doc.getTel()
        );
        customer.setBillingAddressId(doc.getBillingAddressId());
        customer.setShippingAddressId(doc.getShippingAddressId());
        customer.setResidentialAddressId(doc.getResidentialAddressId());
        return customer;
    }

    private CustomerDocument toDocument(Customer customer) {
        CustomerDocument doc = new CustomerDocument(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getTel()
        );
        doc.setBillingAddressId(customer.getBillingAddressId());
        doc.setShippingAddressId(customer.getShippingAddressId());
        doc.setResidentialAddressId(customer.getResidentialAddressId());
        return doc;
    }

    @Override
    public Customer save(Customer customer) {
        return toDomain(mongoRepo.save(toDocument(customer)));
    }

    @Override
    public List<Customer> findAll() {
        return mongoRepo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Customer> findById(String id) {
        return mongoRepo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        mongoRepo.deleteById(id);
    }
}
