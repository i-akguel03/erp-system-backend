# ERP System – Fullstack mit Spring Boot & Angular

Ein **ERP-System** zur Verwaltung von Kunden, Adressen, Produkten, Verträgen, Abonnements und Fälligkeitsplänen.  
Fullstack-Anwendung mit **Spring Boot (Java)**, **Angular (TypeScript)**, **PostgreSQL** und **JWT-Authentifizierung**.

## 🎯 Live Demo

🌐 **[Frontend Live Demo](DEIN_FRONTEND_LINK)**  
📚 **[API Dokumentation (Swagger)](DEIN_BACKEND_LINK/swagger-ui.html)**

## 📸 Screenshots

![Dashboard Overview](screenshots/dashboard.png)
*Dashboard mit Kundenübersicht und Navigation*

![Customer Management](screenshots/customers.png) 
*Kunden- und Adressverwaltung mit CRUD-Operationen*

![Contract Details](screenshots/contracts.png)
*Vertragsverwaltung mit Abonnements und Fälligkeiten*

---

## ✨ Features

- 🔐 **Authentifizierung & Autorisierung** mit JWT (Login/Register)
- 👥 **Kunden- und Adressverwaltung** (CRUD)
- 📦 **Produktverwaltung** (CRUD)
- 📋 **Vertragsverwaltung** inkl. Abos & DueSchedules
- 🛡️ **Geschäftslogik**: Kunde kann nicht gelöscht werden, wenn aktive Verträge bestehen
- 📖 **Swagger API-Dokumentation**
- 🎨 **Responsive Design** mit Angular Material
- ⚡ **Real-time Updates** und Validierung

![Tech Stack](screenshots/tech-stack.png)
*Verwendete Technologien und Architektur*

---

## 🏗️ Architektur

```mermaid
graph TD
  A[Angular 20 Frontend] -->|REST + JWT| B[Spring Boot Backend]
  B --> C[PostgreSQL Database]

  subgraph Frontend Stack
    A1[Angular 20 + TypeScript]
    A2[Bootstrap 5]
    A3[Reactive Forms]
    A4[HTTP Interceptors]
  end

  subgraph Backend Layers
    B1[Controller Layer]
    B2[Service Layer] 
    B3[Repository Layer]
    B4[JPA Entities]
  end

  A1 --> A2
  A2 --> A3
  A3 --> A4

  B1 --> B2
  B2 --> B3
  B3 --> B4

  B4 --> Customer[Customer / Address]
  B4 --> Product[Product]
  B4 --> Contract[Contract]
  B4 --> Subscription[Subscription / DueSchedule]
```

---

## 💼 Für Recruiter & Tech-Teams

**Dieses Projekt zeigt praktische Erfahrung mit:**

### Backend Development
- ✅ **Spring Boot 3** - Modern Java Enterprise Development
- ✅ **Spring Security** - JWT Authentication & Authorization  
- ✅ **JPA/Hibernate** - Complex Database Relationships
- ✅ **RESTful APIs** - Clean API Design with OpenAPI/Swagger
- ✅ **Maven** - Dependency Management & Build Automation

### Frontend Development  
- ✅ **Angular 17** - Component-based SPA Architecture
- ✅ **TypeScript** - Type-safe Frontend Development
- ✅ **Angular Material** - Modern UI Components
- ✅ **Responsive Design** - Mobile-first Approach
- ✅ **HTTP Interceptors** - Token Management & Error Handling

### DevOps & Architecture
- ✅ **PostgreSQL** - Relational Database Design
- ✅ **Docker** - Containerization
- ✅ **Clean Architecture** - Separation of Concerns
- ✅ **Unit & Integration Testing** - Quality Assurance

**💡 Highlights:** Komplexe Business Logic, Production-ready Code, Moderne Tech-Stack

---

## 📂 Projektstruktur

```
erp-system-backend/
├── .idea/
├── .mvn/wrapper/
├── logs/
├── src/
│   ├── main/
│   │   ├── java/com/erp/backend/
│   │   │   ├── adapter/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── domain/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── event/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   │       └── event/
│   │   └── resources/
│   └── test/java/com/erp/backend/
│       ├── controller/
│       └── service/
└── target/
```

---

## 🚀 Quickstart

### Voraussetzungen

- **Java 21**
- **Maven 3.9+**
- **PostgreSQL** Datenbank
- **Node.js + Angular CLI** (für das Frontend, falls verwendet)

### Backend starten

1. **PostgreSQL-Datenbank** erstellen (z. B. `erp_db`)

2. In `src/main/resources/application.properties` **DB-Verbindung** konfigurieren:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/erp_db
   spring.datasource.username=dein_user
   spring.datasource.password=dein_passwort
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   ```

3. **Backend starten**:
   ```bash
   mvn spring-boot:run
   ```
   oder in der IDE (z. B. IntelliJ) die Hauptklasse starten.

4. **Backend läuft unter**: http://localhost:8080
5. **Swagger UI**: http://localhost:8080/swagger-ui.html

### Frontend starten

1. In den frontend-Ordner wechseln:
   ```bash
   cd frontend
   npm install
   ng serve
   ```

2. **Frontend läuft unter**: http://localhost:4200

---

## 🔑 Authentifizierung

- **Login/Register** über REST-API
- Nach erfolgreichem Login wird ein **JWT-Token** zurückgegeben
- Für geschützte Endpunkte muss der Token im Header mitgeschickt werden:
  ```
  Authorization: Bearer <token>
  ```

---

## 📜 Lizenz

Dieses Projekt steht unter der [MIT-Lizenz](LICENSE).
