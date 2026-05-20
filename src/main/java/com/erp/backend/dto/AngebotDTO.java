package com.erp.backend.dto;

import com.erp.backend.domain.Angebot;
import com.erp.backend.domain.AngebotStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AngebotDTO {

    private UUID id;
    private String angebotsnummer;
    private UUID customerId;
    private String customerName;
    private AngebotStatus status;
    private LocalDate angebotsDatum;
    private LocalDate gueltigBis;
    private List<AngebotPositionDTO> positionen;
    private BigDecimal nettobetrag;
    private BigDecimal steuerbetrag;
    private BigDecimal bruttobetrag;
    private String notizen;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AngebotDTO() {}

    public static AngebotDTO fromEntity(Angebot a) {
        AngebotDTO dto = new AngebotDTO();
        dto.setId(a.getId());
        dto.setAngebotsnummer(a.getAngebotsnummer());
        dto.setStatus(a.getStatus());
        dto.setAngebotsDatum(a.getAngebotsDatum());
        dto.setGueltigBis(a.getGueltigBis());
        dto.setNettobetrag(a.getNettobetrag());
        dto.setSteuerbetrag(a.getSteuerbetrag());
        dto.setBruttobetrag(a.getBruttobetrag());
        dto.setNotizen(a.getNotizen());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());

        if (a.getCustomer() != null) {
            dto.setCustomerId(a.getCustomer().getId());
            dto.setCustomerName(a.getCustomer().getName());
        }

        if (a.getPositionen() != null) {
            dto.setPositionen(a.getPositionen().stream().map(p -> {
                AngebotPositionDTO pdto = new AngebotPositionDTO();
                pdto.setId(p.getId());
                pdto.setProduktName(p.getProduktName());
                pdto.setBeschreibung(p.getBeschreibung());
                pdto.setMenge(p.getMenge());
                pdto.setEinzelpreis(p.getEinzelpreis());
                pdto.setSteuersatz(p.getSteuersatz());
                pdto.setNettobetrag(p.getNettobetrag());
                pdto.setSteuerbetrag(p.getSteuerbetrag());
                pdto.setBruttobetrag(p.getBruttobetrag());
                if (p.getProduct() != null) {
                    pdto.setProductId(p.getProduct().getId());
                }
                return pdto;
            }).collect(Collectors.toList()));
        }

        return dto;
    }

    // Getter & Setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAngebotsnummer() { return angebotsnummer; }
    public void setAngebotsnummer(String angebotsnummer) { this.angebotsnummer = angebotsnummer; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public AngebotStatus getStatus() { return status; }
    public void setStatus(AngebotStatus status) { this.status = status; }

    public LocalDate getAngebotsDatum() { return angebotsDatum; }
    public void setAngebotsDatum(LocalDate angebotsDatum) { this.angebotsDatum = angebotsDatum; }

    public LocalDate getGueltigBis() { return gueltigBis; }
    public void setGueltigBis(LocalDate gueltigBis) { this.gueltigBis = gueltigBis; }

    public List<AngebotPositionDTO> getPositionen() { return positionen; }
    public void setPositionen(List<AngebotPositionDTO> positionen) { this.positionen = positionen; }

    public BigDecimal getNettobetrag() { return nettobetrag; }
    public void setNettobetrag(BigDecimal nettobetrag) { this.nettobetrag = nettobetrag; }

    public BigDecimal getSteuerbetrag() { return steuerbetrag; }
    public void setSteuerbetrag(BigDecimal steuerbetrag) { this.steuerbetrag = steuerbetrag; }

    public BigDecimal getBruttobetrag() { return bruttobetrag; }
    public void setBruttobetrag(BigDecimal bruttobetrag) { this.bruttobetrag = bruttobetrag; }

    public String getNotizen() { return notizen; }
    public void setNotizen(String notizen) { this.notizen = notizen; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}