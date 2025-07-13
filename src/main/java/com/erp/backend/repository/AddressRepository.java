package com.erp.backend.repository;

import com.erp.backend.domain.Address;

import java.util.List;
import java.util.Optional;

public interface AddressRepository {
    Address save(Address address);
    List<Address> findAll();
    Optional<Address> findById(String id);
    void deleteById(String id);
}
