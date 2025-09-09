# ERP System – Fullstack mit Spring Boot & Angular

Ein **ERP-System** zur Verwaltung von Kunden, Verträgen, Abonnements und Fälligkeitsplänen.  
Das Projekt ist als Fullstack-Anwendung mit **Spring Boot (Java)**, **Angular (TypeScript)**, **MongoDB / MSSQL** und **JWT-Authentifizierung** umgesetzt.  

---

## ✨ Features

- 🔐 **Authentifizierung & Autorisierung** mit JWT (Login/Register)
- 👥 **Kundenverwaltung** (CRUD)
- 📄 **Vertragsverwaltung** inkl. Abos & Fälligkeitspläne
- 📌 **Geschäftslogik**: z. B. Kunde kann nicht gelöscht werden, wenn aktive Verträge bestehen
- 🗃️ **Mehrere Datenbanken**: MongoDB und MSSQL
- 🐳 **Docker & Docker Compose** für Deployment
- 📖 **Swagger API-Dokumentation**

---

## 🏗️ Architektur

```mermaid
graph TD
  A[👩‍💻 Angular Frontend] -->|REST + JWT| B[⚙️ Spring Boot Backend]
  B --> C[(🗄️ MongoDB)]
  B --> D[(🗄️ MSSQL)]
  B --> E[🔑 Auth Service (JWT)]
