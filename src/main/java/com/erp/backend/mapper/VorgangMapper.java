package com.erp.backend.mapper;

import com.erp.backend.domain.Vorgang;
import com.erp.backend.dto.VorgangDTO;
import org.springframework.stereotype.Component;

@Component
public class VorgangMapper {

    public VorgangDTO toDTO(Vorgang vorgang) {
        if (vorgang == null) {
            return null;
        }

        VorgangDTO dto = new VorgangDTO(
                vorgang.getId(),
                vorgang.getVorgangsnummer(),
                vorgang.getTyp(),
                vorgang.getStatus(),
                vorgang.getTitel(),
                vorgang.getBeschreibung(),
                vorgang.getStartZeitpunkt(),
                vorgang.getEndeZeitpunkt(),
                vorgang.getAusgeloestVon(),
                vorgang.isAutomatisch(),
                vorgang.getAnzahlVerarbeitet(),
                vorgang.getAnzahlErfolgreich(),
                vorgang.getAnzahlFehler(),
                vorgang.getGesamtbetrag(),
                vorgang.getFehlerprotokoll()
        );

        // Berechnete Felder hinzufügen
        dto.setDauerInMs(vorgang.getDauerInMs());
        dto.setErfolgsquote(vorgang.getErfolgsquote());

        return dto;
    }

    public Vorgang toEntity(VorgangDTO dto) {
        if (dto == null) {
            return null;
        }

        Vorgang vorgang = new Vorgang();
        vorgang.setId(dto.getId());
        vorgang.setVorgangsnummer(dto.getVorgangsnummer());
        vorgang.setTyp(dto.getTyp());
        vorgang.setStatus(dto.getStatus());
        vorgang.setTitel(dto.getTitel());
        vorgang.setBeschreibung(dto.getBeschreibung());
        vorgang.setStartZeitpunkt(dto.getStartZeitpunkt());
        vorgang.setEndeZeitpunkt(dto.getEndeZeitpunkt());
        vorgang.setAusgeloestVon(dto.getAusgeloestVon());
        vorgang.setAutomatisch(dto.isAutomatisch());
        vorgang.setAnzahlVerarbeitet(dto.getAnzahlVerarbeitet());
        vorgang.setAnzahlErfolgreich(dto.getAnzahlErfolgreich());
        vorgang.setAnzahlFehler(dto.getAnzahlFehler());
        vorgang.setGesamtbetrag(dto.getGesamtbetrag());
        vorgang.setFehlerprotokoll(dto.getFehlerprotokoll());

        return vorgang;
    }
}