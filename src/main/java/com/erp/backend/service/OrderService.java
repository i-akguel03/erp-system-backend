package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.dto.OrderDTO;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.CustomerRepository;
import com.erp.backend.repository.OrderRepository;
import com.erp.backend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final AngebotService angebotService;
    private final NumberGeneratorService numberGeneratorService;

    public OrderService(OrderRepository repository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        AngebotService angebotService,
                        NumberGeneratorService numberGeneratorService) {
        this.repository = repository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.angebotService = angebotService;
        this.numberGeneratorService = numberGeneratorService;
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        List<Order> orders = repository.findAll();
        logger.info("Fetched {} orders", orders.size());
        return orders;
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findAllAsDTO() {
        return repository.findAll().stream()
                .map(OrderDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<Order> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Order> findById(Long id) {
        Optional<Order> order = repository.findById(id);
        if (order.isPresent()) {
            logger.info("Found order with id={}", id);
        } else {
            logger.warn("No order found with id={}", id);
        }
        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findByStatus(OrderStatus status) {
        return repository.findByStatus(status).stream()
                .map(OrderDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findByCustomer(UUID customerId) {
        return repository.findByCustomerId(customerId).stream()
                .map(OrderDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Order save(Order order) {
        if (order.getOrderNumber() == null) {
            order.setOrderNumber(numberGeneratorService.generateOrderNumber());
        }
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.ENTWURF);
        }
        if (order.getOrderSource() == null) {
            order.setOrderSource(OrderSource.MANUELL);
        }
        Order saved = repository.save(order);
        logger.info("Saved order with id={} number={}", saved.getId(), saved.getOrderNumber());
        return saved;
    }

    public OrderDTO auftragAusAngebotErstellen(UUID angebotId) {
        Order order = angebotService.zuAuftragKonvertieren(angebotId);
        order.setOrderNumber(numberGeneratorService.generateOrderNumber());
        Order saved = repository.save(order);
        logger.info("Auftrag {} aus Angebot erstellt", saved.getOrderNumber());
        return OrderDTO.fromEntity(saved);
    }

    public OrderDTO statusAendern(Long id, OrderStatus neuerStatus) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auftrag nicht gefunden: " + id));

        if (OrderStatus.STORNIERT.equals(order.getStatus())) {
            throw new BusinessLogicException("Stornierter Auftrag kann nicht mehr geändert werden");
        }

        order.setStatus(neuerStatus);
        return OrderDTO.fromEntity(repository.save(order));
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
        logger.info("Deleted order with id={}", id);
    }
}