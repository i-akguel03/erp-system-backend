package com.erp.backend.dto;

import com.erp.backend.domain.Address;
import com.erp.backend.domain.Lieferschein;
import com.erp.backend.domain.LieferscheinStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class LieferscheinDTO {

    private UUID id;
    private String lieferscheinnummer;
    private Long auftragId;
    private String auftragNummer;
    private UUID customerId;
    private String customerName;
    private LieferscheinStatus status;
    private LocalDate lieferDatum;
    private List<LieferscheinPositionDTO> positionen;
    private AddressDTO lieferAdresse;
    private String notizen;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LieferscheinDTO() {}

    public static LieferscheinDTO fromEntity(Lieferschein l) {
        LieferscheinDTO dto = new LieferscheinDTO();
        dto.setId(l.getId());
        dto.setLieferscheinnummer(l.getLieferscheinnummer());
        dto.setStatus(l.getStatus());
        dto.setLieferDatum(l.getLieferDatum());
        dto.setNotizen(l.getNotizen());
        dto.setCreatedAt(l.getCreatedAt());
        dto.setUpdatedAt(l.getUpdatedAt());

        if (l.getAuftrag() != null) {
            dto.setAuftragId(l.getAuftrag().getId());
            dto.setAuftragNummer(l.getAuftrag().getOrderNumber());
        }
        if (l.getCustomer() != null) {
            dto.setCustomerId(l.getCustomer().getId());
            dto.setCustomerName(l.getCustomer().getName());
        }
        if (l.getLieferAdresse() != null) {
            Address a = l.getLieferAdresse();
            dto.setLieferAdresse(new AddressDTO(a.getId(), a.getStreet(), a.getPostalCode(), a.getCity(), a.getCountry()));
        }
        if (l.getPositionen() != null) {
            dto.setPositionen(l.getPositionen().stream().map(p -> {
                LieferscheinPositionDTO pdto = new LieferscheinPositionDTO();
                pdto.setId(p.getId());
                pdto.setProduktName(p.getProduktName());
                pdto.setMenge(p.getMenge());
                pdto.setEinheit(p.getEinheit());
                pdto.setNotiz(p.getNotiz());
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

    public String getLieferscheinnummer() { return lieferscheinnummer; }
    public void setLieferscheinnummer(String lieferscheinnummer) { this.lieferscheinnummer = lieferscheinnummer; }

    public Long getAuftragId() { return auftragId; }
    public void setAuftragId(Long auftragId) { this.auftragId = auftragId; }

    public String getAuftragNummer() { return auftragNummer; }
    public void setAuftragNummer(String auftragNummer) { this.auftragNummer = auftragNummer; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public LieferscheinStatus getStatus() { return status; }
    public void setStatus(LieferscheinStatus status) { this.status = status; }

    public LocalDate getLieferDatum() { return lieferDatum; }
    public void setLieferDatum(LocalDate lieferDatum) { this.lieferDatum = lieferDatum; }

    public List<LieferscheinPositionDTO> getPositionen() { return positionen; }
    public void setPositionen(List<LieferscheinPositionDTO> positionen) { this.positionen = positionen; }

    public AddressDTO getLieferAdresse() { return lieferAdresse; }
    public void setLieferAdresse(AddressDTO lieferAdresse) { this.lieferAdresse = lieferAdresse; }

    public String getNotizen() { return notizen; }
    public void setNotizen(String notizen) { this.notizen = notizen; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}