package com.erp.backend.repository;

import com.erp.backend.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // Zusätzliche Abfragen:
    // Optional<Product> findByName(String name);
}
