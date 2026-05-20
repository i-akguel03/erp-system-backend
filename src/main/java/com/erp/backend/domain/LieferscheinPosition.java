package com.erp.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "lieferschein_positionen")
public class LieferscheinPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieferschein_id", nullable = false)
    private Lieferschein lieferschein;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "produkt_name", nullable = false)
    private String produktName;

    @Column(name = "menge", nullable = false)
    private int menge = 1;

    @Column(name = "einheit")
    private String einheit;

    @Column(name = "notiz")
    private String notiz;

    public LieferscheinPosition() {}

    public LieferscheinPosition(Product product, int menge) {
        this.product = product;
        this.produktName = product.getName();
        this.menge = menge;
        this.einheit = product.getUnit();
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lieferschein getLieferschein() { return lieferschein; }
    public void setLieferschein(Lieferschein lieferschein) { this.lieferschein = lieferschein; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getProduktName() { return produktName; }
    public void setProduktName(String produktName) { this.produktName = produktName; }

    public int getMenge() { return menge; }
    public void setMenge(int menge) { this.menge = menge; }

    public String getEinheit() { return einheit; }
    public void setEinheit(String einheit) { this.einheit = einheit; }

    public String getNotiz() { return notiz; }
    public void setNotiz(String notiz) { this.notiz = notiz; }
}