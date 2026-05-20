package com.erp.backend.dto;

import com.erp.backend.domain.BelegTyp;
import com.erp.backend.domain.BuchungStatus;
import com.erp.backend.domain.Buchungssatz;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BuchungssatzDTO {

    private UUID id;
    private String buchungsnummer;
    private LocalDate buchungsDatum;
    private LocalDate valutaDatum;
    private String beschreibung;
    private BelegTyp belegTyp;
    private String belegReferenzId;
    private String belegReferenzNummer;
    private int geschaeftsjahr;
    private int monat;
    private BuchungStatus status;
    private String gebuchtVon;
    private LocalDateTime gebuchtAm;
    private List<BuchungspositionDTO> positionen;
    private BigDecimal sumSoll;
    private BigDecimal sumHaben;
    private boolean ausgeglichen;

    public BuchungssatzDTO() {}

    public static BuchungssatzDTO fromEntity(Buchungssatz b) {
        BuchungssatzDTO dto = new BuchungssatzDTO();
        dto.setId(b.getId());
        dto.setBuchungsnummer(b.getBuchungsnummer());
        dto.setBuchungsDatum(b.getBuchungsDatum());
        dto.setValutaDatum(b.getValutaDatum());
        dto.setBeschreibung(b.getBeschreibung());
        dto.setBelegTyp(b.getBelegTyp());
        dto.setBelegReferenzId(b.getBelegReferenzId());
        dto.setBelegReferenzNummer(b.getBelegReferenzNummer());
        dto.setGeschaeftsjahr(b.getGeschaeftsjahr());
        dto.setMonat(b.getMonat());
        dto.setStatus(b.getStatus());
        dto.setGebuchtVon(b.getGebuchtVon());
        dto.setGebuchtAm(b.getGebuchtAm());
        dto.setSumSoll(b.getSumSoll());
        dto.setSumHaben(b.getSumHaben());
        dto.setAusgeglichen(b.isAusgeglichen());
        if (b.getPositionen() != null) {
            dto.setPositionen(b.getPositionen().stream()
                    .map(BuchungspositionDTO::fromEntity)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    // Getter & Setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getBuchungsnummer() { return buchungsnummer; }
    public void setBuchungsnummer(String buchungsnummer) { this.buchungsnummer = buchungsnummer; }

    public LocalDate getBuchungsDatum() { return buchungsDatum; }
    public void setBuchungsDatum(LocalDate buchungsDatum) { this.buchungsDatum = buchungsDatum; }

    public LocalDate getValutaDatum() { return valutaDatum; }
    public void setValutaDatum(LocalDate valutaDatum) { this.valutaDatum = valutaDatum; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public BelegTyp getBelegTyp() { return belegTyp; }
    public void setBelegTyp(BelegTyp belegTyp) { this.belegTyp = belegTyp; }

    public String getBelegReferenzId() { return belegReferenzId; }
    public void setBelegReferenzId(String belegReferenzId) { this.belegReferenzId = belegReferenzId; }

    public String getBelegReferenzNummer() { return belegReferenzNummer; }
    public void setBelegReferenzNummer(String belegReferenzNummer) { this.belegReferenzNummer = belegReferenzNummer; }

    public int getGeschaeftsjahr() { return geschaeftsjahr; }
    public void setGeschaeftsjahr(int geschaeftsjahr) { this.geschaeftsjahr = geschaeftsjahr; }

    public int getMonat() { return monat; }
    public void setMonat(int monat) { this.monat = monat; }

    public BuchungStatus getStatus() { return status; }
    public void setStatus(BuchungStatus status) { this.status = status; }

    public String getGebuchtVon() { return gebuchtVon; }
    public void setGebuchtVon(String gebuchtVon) { this.gebuchtVon = gebuchtVon; }

    public LocalDateTime getGebuchtAm() { return gebuchtAm; }
    public void setGebuchtAm(LocalDateTime gebuchtAm) { this.gebuchtAm = gebuchtAm; }

    public List<BuchungspositionDTO> getPositionen() { return positionen; }
    public void setPositionen(List<BuchungspositionDTO> positionen) { this.positionen = positionen; }

    public BigDecimal getSumSoll() { return sumSoll; }
    public void setSumSoll(BigDecimal sumSoll) { this.sumSoll = sumSoll; }

    public BigDecimal getSumHaben() { return sumHaben; }
    public void setSumHaben(BigDecimal sumHaben) { this.sumHaben = sumHaben; }

    public boolean isAusgeglichen() { return ausgeglichen; }
    public void setAusgeglichen(boolean ausgeglichen) { this.ausgeglichen = ausgeglichen; }
}