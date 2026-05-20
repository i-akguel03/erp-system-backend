package com.erp.backend.dto;

import com.erp.backend.domain.BuchungsTyp;
import com.erp.backend.domain.Buchungsposition;

import java.math.BigDecimal;

public class BuchungspositionDTO {

    private Long id;
    private Long kontonummer;
    private String kontoBezeichnung;
    private BuchungsTyp buchungsTyp;
    private BigDecimal betrag;
    private String beschreibung;
    private String kostenstelle;

    public BuchungspositionDTO() {}

    public static BuchungspositionDTO fromEntity(Buchungsposition p) {
        BuchungspositionDTO dto = new BuchungspositionDTO();
        dto.setId(p.getId());
        dto.setBuchungsTyp(p.getBuchungsTyp());
        dto.setBetrag(p.getBetrag());
        dto.setBeschreibung(p.getBeschreibung());
        dto.setKostenstelle(p.getKostenstelle());
        if (p.getKonto() != null) {
            dto.setKontonummer(p.getKonto().getKontonummer());
            dto.setKontoBezeichnung(p.getKonto().getBezeichnung());
        }
        return dto;
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getKontonummer() { return kontonummer; }
    public void setKontonummer(Long kontonummer) { this.kontonummer = kontonummer; }

    public String getKontoBezeichnung() { return kontoBezeichnung; }
    public void setKontoBezeichnung(String kontoBezeichnung) { this.kontoBezeichnung = kontoBezeichnung; }

    public BuchungsTyp getBuchungsTyp() { return buchungsTyp; }
    public void setBuchungsTyp(BuchungsTyp buchungsTyp) { this.buchungsTyp = buchungsTyp; }

    public BigDecimal getBetrag() { return betrag; }
    public void setBetrag(BigDecimal betrag) { this.betrag = betrag; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public String getKostenstelle() { return kostenstelle; }
    public void setKostenstelle(String kostenstelle) { this.kostenstelle = kostenstelle; }
}