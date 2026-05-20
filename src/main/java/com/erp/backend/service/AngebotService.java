package com.erp.backend.service;

import com.erp.backend.domain.*;
import com.erp.backend.dto.AngebotDTO;
import com.erp.backend.dto.AngebotPositionDTO;
import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.AngebotRepository;
import com.erp.backend.repository.CustomerRepository;
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
public class AngebotService {

    private static final Logger logger = LoggerFactory.getLogger(AngebotService.class);

    private final AngebotRepository angebotRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final NumberGeneratorService numberGeneratorService;

    public AngebotService(AngebotRepository angebotRepository,
                          CustomerRepository customerRepository,
                          ProductRepository productRepository,
                          NumberGeneratorService numberGeneratorService) {
        this.angebotRepository = angebotRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.numberGeneratorService = numberGeneratorService;
    }

    @Transactional(readOnly = true)
    public List<AngebotDTO> findAll() {
        return angebotRepository.findAll().stream()
                .map(AngebotDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AngebotDTO findById(UUID id) {
        Angebot angebot = angebotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Angebot nicht gefunden: " + id));
        return AngebotDTO.fromEntity(angebot);
    }

    @Transactional(readOnly = true)
    public List<AngebotDTO> findByCustomer(UUID customerId) {
        return angebotRepository.findByCustomerId(customerId).stream()
                .map(AngebotDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AngebotDTO> findByStatus(AngebotStatus status) {
        return angebotRepository.findByStatus(status).stream()
                .map(AngebotDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public AngebotDTO erstellen(UUID customerId, List<AngebotPositionDTO> positionen,
                                LocalDate gueltigBis, String notizen) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Kunde nicht gefunden: " + customerId));

        Angebot angebot = new Angebot();
        angebot.setAngebotsnummer(numberGeneratorService.generateAngebotNumber());
        angebot.setCustomer(customer);
        angebot.setStatus(AngebotStatus.ENTWURF);
        angebot.setGueltigBis(gueltigBis);
        angebot.setNotizen(notizen);

        for (AngebotPositionDTO pdto : positionen) {
            AngebotPosition position = new AngebotPosition();
            position.setProduktName(pdto.getProduktName());
            position.setBeschreibung(pdto.getBeschreibung());
            position.setMenge(pdto.getMenge());
            position.setEinzelpreis(pdto.getEinzelpreis());
            position.setSteuersatz(pdto.getSteuersatz() != null ? pdto.getSteuersatz() : java.math.BigDecimal.ZERO);

            if (pdto.getProductId() != null) {
                productRepository.findById(pdto.getProductId()).ifPresent(position::setProduct);
            }
            angebot.addPosition(position);
        }

        angebot.berechneTotals();
        Angebot saved = angebotRepository.save(angebot);
        logger.info("Angebot erstellt: {}", saved.getAngebotsnummer());
        return AngebotDTO.fromEntity(saved);
    }

    public AngebotDTO statusAendern(UUID id, AngebotStatus neuerStatus) {
        Angebot angebot = angebotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Angebot nicht gefunden: " + id));

        validiereStatusUebergang(angebot.getStatus(), neuerStatus);
        angebot.setStatus(neuerStatus);
        return AngebotDTO.fromEntity(angebotRepository.save(angebot));
    }

    public Order zuAuftragKonvertieren(UUID angebotId) {
        Angebot angebot = angebotRepository.findById(angebotId)
                .orElseThrow(() -> new ResourceNotFoundException("Angebot nicht gefunden: " + angebotId));

        if (AngebotStatus.ABGELEHNT.equals(angebot.getStatus()) ||
                AngebotStatus.ABGELAUFEN.equals(angebot.getStatus())) {
            throw new BusinessLogicException("Angebot kann nicht konvertiert werden — Status: " + angebot.getStatus());
        }

        angebot.setStatus(AngebotStatus.ANGENOMMEN);
        angebotRepository.save(angebot);

        Order order = new Order();
        order.setCustomer(angebot.getCustomer());
        order.setOrderSource(OrderSource.ANGEBOT);
        order.setAngebotId(angebot.getId());
        order.setStatus(OrderStatus.BESTAETIGT);
        order.setNettobetrag(angebot.getNettobetrag());
        order.setSteuerbetrag(angebot.getSteuerbetrag());
        order.setBruttobetrag(angebot.getBruttobetrag());
        order.setTotalPrice(angebot.getBruttobetrag().doubleValue());
        order.setNotizen(angebot.getNotizen());

        logger.info("Angebot {} zu Auftrag konvertiert", angebot.getAngebotsnummer());
        return order;
    }

    public void loeschen(UUID id) {
        Angebot angebot = angebotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Angebot nicht gefunden: " + id));

        if (AngebotStatus.ANGENOMMEN.equals(angebot.getStatus())) {
            throw new BusinessLogicException("Angenommenes Angebot kann nicht gelöscht werden");
        }
        angebotRepository.delete(angebot);
        logger.info("Angebot gelöscht: {}", angebot.getAngebotsnummer());
    }

    private void validiereStatusUebergang(AngebotStatus aktuell, AngebotStatus neu) {
        if (AngebotStatus.ANGENOMMEN.equals(aktuell) || AngebotStatus.ABGELEHNT.equals(aktuell)) {
            throw new BusinessLogicException("Status kann von " + aktuell + " nicht mehr geändert werden");
        }
    }
}