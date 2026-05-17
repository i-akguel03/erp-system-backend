package com.erp.backend.repository;

import com.erp.backend.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    @Query("""
            SELECT a FROM Address a
            WHERE LOWER(a.street)     LIKE %:query%
               OR LOWER(a.postalCode) LIKE %:query%
               OR LOWER(a.city)       LIKE %:query%
               OR LOWER(a.country)    LIKE %:query%
            ORDER BY a.city, a.street
            """)
    List<Address> searchByQuery(@Param("query") String query);
}