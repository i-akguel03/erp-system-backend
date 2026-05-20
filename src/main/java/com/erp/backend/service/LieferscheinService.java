package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.dto.LieferscheinDTO;
import com.erp.backend.dto.LieferscheinPositionDTO;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.AddressRepository;
import com.erp.backend.repository.LieferscheinRepository;
import com.erp.backend.repository.OrderRepository;
import com.erp.backend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class LieferscheinService {

    private static final Logger logger = LoggerFactory.getLogger(LieferscheinService.class);

    private final LieferscheinRepository lieferscheinRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final NumberGeneratorService numberGeneratorService;

    public LieferscheinService(LieferscheinRepository lieferscheinRepository,
                                OrderRepository orderRepository,
                                ProductRepository productRepository,
                                AddressRepository addressRepository,
                                NumberGeneratorService numberGeneratorService) {
        this.lieferscheinRepository = lieferscheinRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.numberGeneratorService = numberGeneratorService;
    }

    @Transactional(readOnly = true)
    public List<LieferscheinDTO> findAll() {
        return lieferscheinRepository.findAll().stream()
                .map(LieferscheinDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LieferscheinDTO findById(UUID id) {
        Lieferschein ls = lieferscheinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lieferschein nicht gefunden: " + id));
        return LieferscheinDTO.fromEntity(ls);
    }

    @Transactional(readOnly = true)
    public List<LieferscheinDTO> findByAuftrag(Long auftragId) {
        return lieferscheinRepository.findByAuftragId(auftragId).stream()
                .map(LieferscheinDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public LieferscheinDTO erstellen(Long auftragId, List<LieferscheinPositionDTO> positionen,
                                     LocalDate lieferDatum, Long lieferAdresseId, String notizen) {
        Order auftrag = orderRepository.findById(auftragId)
                .orElseThrow(() -> new ResourceNotFoundException("Auftrag nicht gefunden: " + auftragId));

        if (OrderStatus.STORNIERT.equals(auftrag.getStatus())) {
            throw new BusinessLogicException("Für einen stornierten Auftrag kann kein Lieferschein erstellt werden");
        }

        Lieferschein ls = new Lieferschein();
        ls.setLieferscheinnummer(numberGeneratorService.generateLieferscheinNumber());
        ls.setAuftrag(auftrag);
        ls.setCustomer(auftrag.getCustomer());
        ls.setLieferDatum(lieferDatum);
        ls.setNotizen(notizen);
        ls.setStatus(LieferscheinStatus.AUSSTEHEND);

        if (lieferAdresseId != null) {
            addressRepository.findById(lieferAdresseId).ifPresent(ls::setLieferAdresse);
        } else if (auftrag.getCustomer().getShippingAddress() != null) {
            ls.setLieferAdresse(auftrag.getCustomer().getShippingAddress());
        }

        for (LieferscheinPositionDTO pdto : positionen) {
            LieferscheinPosition position = new LieferscheinPosition();
            position.setProduktName(pdto.getProduktName());
            position.setMenge(pdto.getMenge());
            position.setEinheit(pdto.getEinheit());
            position.setNotiz(pdto.getNotiz());

            if (pdto.getProductId() != null) {
                productRepository.findById(pdto.getProductId()).ifPresent(position::setProduct);
            }
            ls.addPosition(position);
        }

        // Auftragsstatus aktualisieren
        if (OrderStatus.BESTAETIGT.equals(auftrag.getStatus())) {
            auftrag.setStatus(OrderStatus.IN_LIEFERUNG);
            orderRepository.save(auftrag);
        }

        Lieferschein saved = lieferscheinRepository.save(ls);
        logger.info("Lieferschein erstellt: {} für Auftrag {}", saved.getLieferscheinnummer(), auftrag.getOrderNumber());
        return LieferscheinDTO.fromEntity(saved);
    }

    public LieferscheinDTO statusAendern(UUID id, LieferscheinStatus neuerStatus) {
        Lieferschein ls = lieferscheinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lieferschein nicht gefunden: " + id));

        if (LieferscheinStatus.STORNIERT.equals(ls.getStatus())) {
            throw new BusinessLogicException("Stornierter Lieferschein kann nicht geändert werden");
        }

        ls.setStatus(neuerStatus);

        if (LieferscheinStatus.GELIEFERT.equals(neuerStatus)) {
            Order auftrag = ls.getAuftrag();
            auftrag.setStatus(OrderStatus.GELIEFERT);
            orderRepository.save(auftrag);
            logger.info("Auftrag {} als geliefert markiert", auftrag.getOrderNumber());
        }

        return LieferscheinDTO.fromEntity(lieferscheinRepository.save(ls));
    }
}