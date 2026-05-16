package com.erp.backend.controller;

import com.erp.backend.domain.Order;
import com.erp.backend.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'ORDERS_READ')")
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders(
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        if (paginated) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Order> orderPage = service.findAll(pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(orderPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(orderPage.getTotalPages()))
                    .header("X-Current-Page", String.valueOf(page))
                    .body(orderPage.getContent());
        }
        return ResponseEntity.ok(service.findAll());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'ORDERS_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order saved = service.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order updated) {
        return service.findById(id)
                .map(existing -> {
                    existing.setCustomer(updated.getCustomer());
                    existing.setItems(updated.getItems());  // statt setProducts
                    existing.setTotalPrice(updated.getTotalPrice()); // statt setTotalAmount
                    existing.setOrderDate(updated.getOrderDate());
                    return ResponseEntity.ok(service.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
