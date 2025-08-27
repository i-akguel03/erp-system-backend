package com.erp.backend.repository;

import com.erp.backend.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByProductNumber(String productNumber);

    Optional<Product> findByName(String name);

    boolean existsByProductNumber(String productNumber);

    List<Product> findByActiveTrue();

    List<Product> findByNameContainingIgnoreCase(String keyword);
}
