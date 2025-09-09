# ERP System – Fullstack Developer Portfolio

Ein **funktionsfähiges ERP-System** mit Kunden-, Produkt- und Vertragsverwaltung.  
Entwickelt als **Portfolio-Projekt** mit modernen Technologien: **Spring Boot**, **Angular 20**, **Bootstrap 5** und **PostgreSQL**.

---

## 🎯 Live Demo & Code

🌐 **[Live Demo](https://erp-system-frontend-tan.vercel.app/)** - Vercel Deployment  
📚 **[API Dokumentation](https://erp-system-backend-yo8w.onrender.com/swagger-ui/index.html)** - Interaktive Swagger UI

> **Demo Login:** `demo@test.com` / `password123` (falls verfügbar)


---

## 📸 Anwendungs-Screenshots

### Dashboard & Navigation
![ERP Dashboard](screenshots/dashboard.png)
*Saubere Angular 20 Oberfläche mit Bootstrap 5 Styling*

### Kundenverwaltung  
![Customer CRUD](screenshots/customers.png) 
*Vollständige CRUD-Operationen mit Formular-Validierung*

### Vertragssystem
![Contract Management](screenshots/contracts.png)
*Geschäftslogik: Kunden mit aktiven Verträgen können nicht gelöscht werden*

---

## ✅ Was ich entwickelt habe

### Kernnfunktionen (100% funktionsfähig)
- **🔐 JWT Authentifizierung** - Sicheres Login/Register mit Token-Management
- **👥 Kundenverwaltung** - Erstellen, Lesen, Bearbeiten, Löschen mit Adressverknüpfung
- **📦 Produktkatalog** - Dynamische Produktverwaltung mit Kategorien  
- **📋 Vertragssystem** - Verknüpfung von Kunden und Produkten mit Abo-Logik
- **⏰ Fälligkeitspläne** - Automatische Generierung von Zahlungsplänen
- **🛡️ Geschäftsvalidierung** - Verhindert Löschen von Kunden mit aktiven Verträgen
- **📖 REST API** - OpenAPI/Swagger Dokumentation
- **📱 Responsive UI** - Mobile-first Bootstrap 5 Design

### Technische Umsetzung
- **Backend:** Spring Boot 3, Spring Security (JWT), JPA/Hibernate
- **Frontend:** Angular 20, TypeScript, Bootstrap 5, Reactive Forms
- **Datenbank:** PostgreSQL mit korrekten Beziehungen und Constraints
- **Deployment:** Automatisierte CI/CD über Git (Render + Vercel)

---

## 🧠 Problemlösung & Geschäftslogik

### Praxisnahe Szenarien, die ich gelöst habe

**Problem:** Wie verhindert man Dateninkonsistenz?
```java
@PreRemove
public void checkActiveContracts() {
    if (hasActiveContracts()) {
        throw new BusinessException("Kunde mit aktiven Verträgen kann nicht gelöscht werden");
    }
}





















```

**Problem:** Wie generiert man automatische Zahlungspläne?
```typescript
generateDueSchedule(contract: Contract): DueSchedule[] {
  const schedules = [];
  for (let i = 0; i < contract.duration; i++) {
    schedules.push({
      dueDate: addMonths(contract.startDate, i),
      amount: contract.monthlyFee
    });
  }
  return schedules;
}
```

**Problem:** Wie handhabt man Authentifizierung app-weit?
- JWT-Interceptor fügt automatisch Token zu Requests hinzu
- Route Guards schützen vor unbefugtem Zugriff
- Token-Refresh-Logik verhindert Session-Timeouts

---

## 🏗️ Architektur-Entscheidungen






### Warum Spring Boot?
- **Schnelle Entwicklung** durch Auto-Konfiguration
- **Enterprise-ready** Security und Validierung
- **Einfaches Testing** mit eingebautem Test-Framework



### Warum Angular 20?
- **Neueste Features** wie Standalone Components
- **Typsicherheit** durch TypeScript
- **Reactive Forms** für komplexe Validierung


### Warum Bootstrap statt Material?
- **Schnellere Entwicklung** mit Utility Classes
- **Kleinere Bundle-Größe** für bessere Performance
- **Mehr anpassbar** für einzigartige Designs

---

## 🚀 Entwicklungsprozess

### Git Workflow & CI/CD
```bash
git push origin main
# → Löst automatisch aus:
# → Backend Deployment zu Render
# → Frontend Deployment zu Vercel
# → Zero-Downtime Updates


















```

### Code-Qualität
- **Konsistente Namenskonventionen**
- **Service Layer** Trennung
- **DTO Pattern** für API-Antworten
- **Error Handling** mit Custom Exceptions

---

## 📈 Nächste Entwicklungsphase

### Aktuell in Planung (Ehrliche Roadmap)
- **📄 Rechnungsgenerierung** - PDF-Erstellung aus Verträgen
- **🔄 Batch-Verarbeitung** - Monatliche Abrechnungsautomatisierung  
- **📊 Basis-Analytics** - Kunden- und Umsatzmetriken
- **🧪 Test-Suite** - Unit- und Integrationstests
- **📝 Audit-Logging** - Verfolgung von Benutzeraktionen für Compliance

*Diese Features stellen realistische nächste Schritte für Geschäftswert dar.*

---

## 💡 Was dieses Projekt zeigt








### Für Junior Fullstack Positionen
- **Vollständiger Entwicklungszyklus** von Datenbank bis UI
- **Moderner Tech-Stack** mit neuesten Versionen
- **Production Deployment** Erfahrung
- **Geschäftslogik** Verständnis
- **Clean Code** Praktiken

### Bewiesene technische Fähigkeiten
- RESTful API Design und Consumption
- Datenbankbeziehungen und Constraints  
- Frontend State Management
- Authentifizierung und Autorisierung
- Responsive Webentwicklung
- Versionskontrolle und Deployment

---

## 🛠️ Lokale Entwicklung






### Voraussetzungen
- Java 21 + Maven 3.9+
- Node.js 18+ + Angular CLI
- PostgreSQL 14+

### Quick Start
```bash
# Backend
./mvnw spring-boot:run

# Frontend  
cd frontend && npm install && ng serve

# Zugriff
Frontend: http://localhost:4200
Backend: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui.html
```

---

## 📝 Technische Details & Metriken

**Backend Architektur:** 5+ Service Classes mit Enterprise-Patterns  
**API Endpoints:** 25+ RESTful Endpunkte mit vollständigen CRUD-Operationen  
**Database Schema:** 6 Kern-Entitäten mit 15+ Beziehungen und Constraints  
**Business Logic:** 500+ Lines komplexer Geschäftslogik pro Service  
**Frontend Komponenten:** 15+ wiederverwendbare Angular Komponenten  
**Code Coverage:** Unit Tests für kritische Business Logic geplant  
**Entwicklungszeit:** ~8 Wochen Teilzeit-Entwicklung (inkl. Dokumentation)  

### Implementierte Design Patterns
- **Repository Pattern** - Datenzugriff-Abstraktion
- **DTO Pattern** - API-Layer Datenkapselung  
- **Service Layer Pattern** - Geschäftslogik-Trennung
- **Builder Pattern** - Für komplexe Entity-Erstellung
- **Strategy Pattern** - Für verschiedene Billing-Zyklen
- **Observer Pattern** - Event-basierte Architektur (geplant)  

---

## 📜 Lizenz

MIT Lizenz - Siehe [LICENSE](LICENSE) für Details.

---
