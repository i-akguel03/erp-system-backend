package com.erp.backend.event;

import com.erp.backend.domain.Subscription;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

/**
 * SubscriptionCancelledEvent
 *
 * Dieses Event wird ausgelöst, wenn ein Abonnement (Subscription) gekündigt wird.
 *
 * Konzepte für Studenten:
 *
 * 1. **ApplicationEvent**
 *    - Spring bietet die Möglichkeit, Events auszulösen, auf die andere Komponenten
 *      reagieren können.
 *    - `ApplicationEvent` ist die Basisklasse für solche Events.
 *    - Wenn ein Event ausgelöst wird, können alle Listener (`@EventListener`) darauf reagieren.
 *
 * 2. **Warum Events?**
 *    - Trennen von Logik: Die Kündigung selbst muss nur das Event auslösen.
 *    - Andere Komponenten (z.B. Fälligkeitspläne, Benachrichtigungen) können unabhängig reagieren.
 *    - Das fördert lose Kopplung (Loose Coupling) im System.
 *
 * 3. **Event-Daten**
 *    - Wir speichern hier die relevanten Informationen für Listener:
 *      a) Subscription: das gekündigte Abonnement.
 *      b) cancellationDate: das Datum, ab wann das Abonnement gekündigt ist.
 */
public class SubscriptionCancelledEvent extends ApplicationEvent {

    // Die Subscription, die gekündigt wurde
    private final Subscription subscription;

    // Das Datum der Kündigung
    private final LocalDate cancellationDate;

    /**
     * Konstruktor für das Event.
     *
     * @param source          Die Quelle des Events. Oft die Service-Klasse, die das Event auslöst.
     * @param subscription    Das gekündigte Abonnement
     * @param cancellationDate Datum der Kündigung
     */
    public SubscriptionCancelledEvent(Object source, Subscription subscription, LocalDate cancellationDate) {
        super(source); // Muss an ApplicationEvent übergeben werden
        this.subscription = subscription;
        this.cancellationDate = cancellationDate;
    }

    /**
     * Getter für das Abonnement
     * @return Subscription
     */
    public Subscription getSubscription() {
        return subscription;
    }

    /**
     * Getter für das Kündigungsdatum
     * @return LocalDate
     */
    public LocalDate getCancellationDate() {
        return cancellationDate;
    }
}
