package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.dto.ExternalOrderRequestDTO;
import com.erp.backend.dto.OrderDTO;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.AddressRepository;
import com.erp.backend.repository.CustomerRepository;
import com.erp.backend.repository.OrderRepository;
import com.erp.backend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ExternalOrderService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalOrderService.class);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final NumberGeneratorService numberGeneratorService;

    public ExternalOrderService(OrderRepository orderRepository,
                                 CustomerRepository customerRepository,
                                 ProductRepository productRepository,
                                 AddressRepository addressRepository,
                                 NumberGeneratorService numberGeneratorService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.numberGeneratorService = numberGeneratorService;
    }

    public OrderDTO verarbeiteBestellung(ExternalOrderRequestDTO request) {
        if (orderRepository.existsByExternalOrderId(request.getExternalOrderId())) {
            throw new BusinessLogicException("Bestellung bereits verarbeitet: " + request.getExternalOrderId());
        }

        Customer customer = findeOderErstelleKunden(request);
        List<OrderItem> items = erstellePositionen(request);

        BigDecimal netto = berechnNetto(items);
        BigDecimal steuer = berechnSteuer(items);
        BigDecimal brutto = netto.add(steuer);

        Order order = new Order();
        order.setOrderNumber(numberGeneratorService.generateOrderNumber());
        order.setCustomer(customer);
        order.setStatus(OrderStatus.BESTAETIGT);
        order.setOrderSource(erkenneQuelle(request.getSource()));
        order.setExternalOrderId(request.getExternalOrderId());
        order.setNettobetrag(netto);
        order.setSteuerbetrag(steuer);
        order.setBruttobetrag(brutto);
        order.setTotalPrice(brutto.doubleValue());
        order.setNotizen(request.getNotizen());

        if (request.getShippingAddress() != null) {
            Address addr = new Address(
                    request.getShippingAddress().getStreet(),
                    request.getShippingAddress().getPostalCode(),
                    request.getShippingAddress().getCity(),
                    request.getShippingAddress().getCountry()
            );
            addressRepository.save(addr);
            order.setLieferAdresse(addr);
        }

        for (OrderItem item : items) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order);
        logger.info("Webshop-Bestellung verarbeitet: {} → Auftrag {}", request.getExternalOrderId(), saved.getOrderNumber());
        return OrderDTO.fromEntity(saved);
    }

    private Customer findeOderErstelleKunden(ExternalOrderRequestDTO request) {
        return customerRepository.findByEmail(request.getCustomerEmail())
                .orElseGet(() -> {
                    Customer neu = new Customer(
                            request.getCustomerFirstName() != null ? request.getCustomerFirstName() : "Unbekannt",
                            request.getCustomerLastName() != null ? request.getCustomerLastName() : "Unbekannt",
                            request.getCustomerEmail(),
                            request.getCustomerTel()
                    );
                    Customer saved = customerRepository.save(neu);
                    logger.info("Neuer Kunde aus Webshop-Bestellung angelegt: {}", request.getCustomerEmail());
                    return saved;
                });
    }

    private List<OrderItem> erstellePositionen(ExternalOrderRequestDTO request) {
        List<OrderItem> items = new ArrayList<>();
        for (ExternalOrderRequestDTO.ExternalOrderItemDTO itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produkt nicht gefunden: " + itemDto.getProductId()));

            if (!product.isActive()) {
                throw new BusinessLogicException("Produkt ist nicht aktiv: " + product.getName());
            }

            double preis = itemDto.getUnitPrice() != null
                    ? itemDto.getUnitPrice().doubleValue()
                    : product.getPrice().doubleValue();

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(preis);
            items.add(item);
        }
        return items;
    }

    private BigDecimal berechnNetto(List<OrderItem> items) {
        return items.stream()
                .map(i -> BigDecimal.valueOf(i.getUnitPrice())
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal berechnSteuer(List<OrderItem> items) {
        return items.stream()
                .map(i -> {
                    BigDecimal taxRate = i.getProduct().getTaxRate();
                    if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) == 0) {
                        return BigDecimal.ZERO;
                    }
                    BigDecimal netto = BigDecimal.valueOf(i.getUnitPrice())
                            .multiply(BigDecimal.valueOf(i.getQuantity()));
                    return netto.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderSource erkenneQuelle(String source) {
        if (source == null) return OrderSource.WEBSHOP;
        try {
            return OrderSource.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OrderSource.API;
        }
    }
}