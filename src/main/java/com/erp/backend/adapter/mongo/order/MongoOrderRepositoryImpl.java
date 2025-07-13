package com.erp.backend.adapter.mongo.order;

import com.erp.backend.domain.Order;
import com.erp.backend.domain.OrderItem;
import com.erp.backend.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MongoOrderRepositoryImpl implements OrderRepository {

    private final MongoOrderDataRepository mongoRepo;

    public MongoOrderRepositoryImpl(MongoOrderDataRepository mongoRepo) {
        this.mongoRepo = mongoRepo;
    }

    private Order toDomain(OrderDocument doc) {
        List<OrderItem> items = doc.getItems().stream()
                .map(i -> new OrderItem(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .collect(Collectors.toList());
        return new Order(doc.getId(), doc.getCustomerId(), items, doc.getOrderDate(), doc.getTotalPrice());
    }

    private OrderDocument toDocument(Order order) {
        List<OrderItemDocument> items = order.getItems().stream()
                .map(i -> new OrderItemDocument(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .collect(Collectors.toList());
        return new OrderDocument(order.getId(), order.getCustomerId(), items, order.getOrderDate(), order.getTotalPrice());
    }

    @Override
    public Order save(Order order) {
        return toDomain(mongoRepo.save(toDocument(order)));
    }

    @Override
    public List<Order> findAll() {
        return mongoRepo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Order> findById(String id) {
        return mongoRepo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        mongoRepo.deleteById(id);
    }
}
