package com.erp.backend.service;

import com.erp.backend.domain.Product;
import com.erp.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    public Optional<Product> getProductById(String id) {
        return repository.findById(id);
    }

    public Product createOrUpdateProduct(Product product) {
        return repository.save(product);
    }

    public void deleteProductById(String id) {
        repository.deleteById(id);
    }
}
