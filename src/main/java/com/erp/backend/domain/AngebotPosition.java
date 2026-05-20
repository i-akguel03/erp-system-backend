package com.erp.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "angebot_positionen")
public class AngebotPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "angebot_id", nullable = false)
    private Angebot angebot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "produkt_name", nullable = false)
    private String produktName;

    @Column(name = "beschreibung")
    private String beschreibung;

    @Column(name = "menge", nullable = false)
    private int menge = 1;

    @Column(name = "einzelpreis", precision = 10, scale = 2, nullable = false)
    private BigDecimal einzelpreis = BigDecimal.ZERO;

    @Column(name = "steuersatz", precision = 5, scale = 2)
    private BigDecimal steuersatz = BigDecimal.ZERO;

    public AngebotPosition() {}

    public AngebotPosition(Product product, int menge) {
        this.product = product;
        this.produktName = product.getName();
        this.menge = menge;
        this.einzelpreis = product.getPrice();
        this.steuersatz = product.getTaxRate() != null ? product.getTaxRate() : BigDecimal.ZERO;
    }

    public BigDecimal getNettobetrag() {
        return einzelpreis.multiply(BigDecimal.valueOf(menge)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getSteuerbetrag() {
        if (steuersatz == null || steuersatz.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getNettobetrag()
                .multiply(steuersatz)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getBruttobetrag() {
        return getNettobetrag().add(getSteuerbetrag());
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Angebot getAngebot() { return angebot; }
    public void setAngebot(Angebot angebot) { this.angebot = angebot; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

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
}