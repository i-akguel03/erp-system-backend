package com.erp.backend.controller;

import com.erp.backend.domain.Order;
import com.erp.backend.domain.OrderStatus;
import com.erp.backend.dto.ExternalOrderRequestDTO;
import com.erp.backend.dto.OrderDTO;
import com.erp.backend.service.ExternalOrderService;
import com.erp.backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin
@Tag(name = "Bestellungen")
public class OrderController {

    private final OrderService service;
    private final ExternalOrderService externalOrderService;

    public OrderController(OrderService service, ExternalOrderService externalOrderService) {
        this.service = service;
        this.externalOrderService = externalOrderService;
    }

    @Operation(summary = "Alle Bestellungen abrufen — optional paginiert")
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

    @Operation(summary = "Alle Bestellungen als DTO")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'ORDERS_READ')")
    @GetMapping("/details")
    public ResponseEntity<List<OrderDTO>> getAllDetails() {
        return ResponseEntity.ok(service.findAllAsDTO());
    }

    @Operation(summary = "Bestellung nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'ORDERS_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Bestellungen nach Status filtern")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'ORDERS_READ')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDTO>> getByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(service.findByStatus(status));
    }

    @Operation(summary = "Bestellungen nach Kunde")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'ORDERS_READ')")
    @GetMapping("/kunde/{customerId}")
    public ResponseEntity<List<OrderDTO>> getByKunde(@PathVariable UUID customerId) {
        return ResponseEntity.ok(service.findByCustomer(customerId));
    }

    @Operation(summary = "Neue Bestellung anlegen")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order saved = service.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Bestellungsstatus ändern")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDTO> statusAendern(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        OrderStatus neuerStatus = OrderStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(service.statusAendern(id, neuerStatus));
    }

    @Operation(summary = "Bestellung aktualisieren")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order updated) {
        return service.findById(id)
                .map(existing -> {
                    existing.setCustomer(updated.getCustomer());
                    existing.setItems(updated.getItems());
                    existing.setTotalPrice(updated.getTotalPrice());
                    existing.setOrderDate(updated.getOrderDate());
                    return ResponseEntity.ok(service.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Bestellung löschen")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Externer Endpunkt für Webshop-Bestellungen
    // Authentifizierung: ROLE_WEBSHOP oder ROLE_ADMIN
    // Der Webshop sendet einen JWT-Token eines dedizierten Webshop-Benutzers
    // -------------------------------------------------------------------------

    @Operation(
        summary = "Webshop-Bestellung entgegennehmen",
        description = "Nimmt eine Bestellung von einem externen Webshop entgegen. " +
                      "Kunde wird per E-Mail gesucht oder neu angelegt. " +
                      "Authentifizierung via JWT mit ROLE_WEBSHOP oder ROLE_ADMIN."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'WEBSHOP')")
    @PostMapping("/external")
    public ResponseEntity<OrderDTO> externalOrder(@Valid @RequestBody ExternalOrderRequestDTO request) {
        OrderDTO order = externalOrderService.verarbeiteBestellung(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}