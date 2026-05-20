package com.erp.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class AngebotPositionDTO {

    private Long id;
    private UUID productId;
    private String produktName;
    private String beschreibung;
    private int menge;
    private BigDecimal einzelpreis;
    private BigDecimal steuersatz;
    private BigDecimal nettobetrag;
    private BigDecimal steuerbetrag;
    private BigDecimal bruttobetrag;

    public AngebotPositionDTO() {}

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public String getProduktName() { return produktName; }
    public void setProduktName(String produktName) { this.produktName = produktName; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public int getMenge() { return menge; }
    public void setMenge(int menge) { this.menge = menge; }

    public BigDecimal getEinzelpreis() { return einzelpreis; }
    public void setEinzelpreis(BigDecimal einzelpreis) { this.einzelpreis = einzelpreis; }

    public BigDecimal getSteuersatz() { return steuersatz; }
    public void setSteuersatz(BigDecimal steuersatz) { this.steuersatz = steuersatz; }

    public BigDecimal getNettobetrag() { return nettobetrag; }
    public void setNettobetrag(BigDecimal nettobetrag) { this.nettobetrag = nettobetrag; }

    public BigDecimal getSteuerbetrag() { return steuerbetrag; }
    public void setSteuerbetrag(BigDecimal steuerbetrag) { this.steuerbetrag = steuerbetrag; }

    public BigDecimal getBruttobetrag() { return bruttobetrag; }
    public void setBruttobetrag(BigDecimal bruttobetrag) { this.bruttobetrag = bruttobetrag; }
}