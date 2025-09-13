package com.erp.backend.domain;

public enum DueStatus {
    /**
     * Fälligkeit ist aktiv und kann abgerechnet werden
     */
    ACTIVE,

    /**
     * Fälligkeit ist pausiert - wird nicht abgerechnet aber kann reaktiviert werden
     */
    PAUSED,

    /**
     * Fälligkeit ist ausgesetzt - längerfristige Unterbrechung
     */
    SUSPENDED,

    /**
     * Fälligkeit wurde bereits abgerechnet/erledigt
     */
    COMPLETED
}