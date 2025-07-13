package com.erp.backend.adapter.mongo.product;

import com.erp.backend.domain.Product;
import com.erp.backend.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MongoProductRepositoryImpl implements ProductRepository {

    private final MongoProductDataRepository mongoRepo;

    public MongoProductRepositoryImpl(MongoProductDataRepository mongoRepo) {
        this.mongoRepo = mongoRepo;
    }

    private Product toDomain(ProductDocument doc) {
        return new Product(doc.getId(), doc.getName(), doc.getDescription(), doc.getPrice(), doc.getStock());
    }

    private ProductDocument toDocument(Product product) {
        return new ProductDocument(product.getId(), product.getName(), product.getDescription(), product.getPrice(), product.getStock());
    }

    @Override
    public Product save(Product product) {
        return toDomain(mongoRepo.save(toDocument(product)));
    }

    @Override
    public List<Product> findAll() {
        return mongoRepo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Product> findById(String id) {
        return mongoRepo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        mongoRepo.deleteById(id);
    }
}
