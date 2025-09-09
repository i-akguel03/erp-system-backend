# ERP System – Fullstack mit Spring Boot & Angular

Ein **ERP-System** zur Verwaltung von Kunden, Adressen, Produkten, Verträgen, Abonnements und Fälligkeitsplänen.  
Fullstack-Anwendung mit **Spring Boot (Java)**, **Angular (TypeScript)**, **PostgreSQL** und **JWT-Authentifizierung**.  

---

## ✨ Features

- Authentifizierung & Autorisierung mit JWT (Login/Register)
- Kunden- und Adressverwaltung (CRUD)
- Produktverwaltung (CRUD)
- Vertragsverwaltung inkl. Abos & DueSchedules
- Geschäftslogik: Kunde kann nicht gelöscht werden, wenn aktive Verträge bestehen
- Swagger API-Dokumentation

---

## 🏗️ Architektur

```mermaid
graph TD
  A[Angular Frontend] -->|REST + JWT| B[Spring Boot Backend]
  B --> C[PostgreSQL Database]

  subgraph Backend Layers
    B1[Controller Layer]
    B2[Service Layer]
    B3[Repository Layer]
    B4[Entities]
  end

  B1 --> B2
  B2 --> B3
  B3 --> B4

  B4 --> Customer[Customer / Address]
  B4 --> Product[Product]
  B4 --> Contract[Contract]
  B4 --> Subscription[Subscription / DueSchedule]
