package com.erp.backend.dto;

import java.util.UUID;

public class LieferscheinPositionDTO {

    private Long id;
    private UUID productId;
    private String produktName;
    private int menge;
    private String einheit;
    private String notiz;

    public LieferscheinPositionDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public String getProduktName() { return produktName; }
    public void setProduktName(String produktName) { this.produktName = produktName; }

    public int getMenge() { return menge; }
    public void setMenge(int menge) { this.menge = menge; }

    public String getEinheit() { return einheit; }
    public void setEinheit(String einheit) { this.einheit = einheit; }

    public String getNotiz() { return notiz; }
    public void setNotiz(String notiz) { this.notiz = notiz; }
}