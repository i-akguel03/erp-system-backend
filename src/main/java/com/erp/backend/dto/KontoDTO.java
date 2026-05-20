package com.erp.backend.dto;

import com.erp.backend.domain.Konto;
import com.erp.backend.domain.KontoTyp;

import java.math.BigDecimal;

public class KontoDTO {

    private Long kontonummer;
    private String bezeichnung;
    private KontoTyp kontoTyp;
    private String kontoKlasse;
    private boolean sammelkonto;
    private boolean aktiv;
    private String beschreibungLang;
    private BigDecimal saldo;

    public KontoDTO() {}

    public static KontoDTO fromEntity(Konto k) {
        KontoDTO dto = new KontoDTO();
        dto.setKontonummer(k.getKontonummer());
        dto.setBezeichnung(k.getBezeichnung());
        dto.setKontoTyp(k.getKontoTyp());
        dto.setKontoKlasse(k.getKontoKlasse());
        dto.setSammelkonto(k.isSammelkonto());
        dto.setAktiv(k.isAktiv());
        dto.setBeschreibungLang(k.getBeschreibungLang());
        return dto;
    }

    // Getter & Setter
    public Long getKontonummer() { return kontonummer; }
    public void setKontonummer(Long kontonummer) { this.kontonummer = kontonummer; }

    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }

    public KontoTyp getKontoTyp() { return kontoTyp; }
    public void setKontoTyp(KontoTyp kontoTyp) { this.kontoTyp = kontoTyp; }

    public String getKontoKlasse() { return kontoKlasse; }
    public void setKontoKlasse(String kontoKlasse) { this.kontoKlasse = kontoKlasse; }

    public boolean isSammelkonto() { return sammelkonto; }
    public void setSammelkonto(boolean sammelkonto) { this.sammelkonto = sammelkonto; }

    public boolean isAktiv() { return aktiv; }
    public void setAktiv(boolean aktiv) { this.aktiv = aktiv; }

    public String getBeschreibungLang() { return beschreibungLang; }
    public void setBeschreibungLang(String beschreibungLang) { this.beschreibungLang = beschreibungLang; }

    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
}