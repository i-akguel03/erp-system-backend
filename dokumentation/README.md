# ERP-System Backend — Dokumentation

Dieses Verzeichnis enthält die technische Dokumentation des ERP-System Backends.

---

## Inhalt

| Dokument | Beschreibung |
|----------|--------------|
| [CRM-Modul](crm/crm-modul.md) | Architektur, Datenmodell, Services |
| [CRM API-Referenz](crm/api-referenz.md) | Alle Endpunkte mit Beispiel-Requests |
| [CRM Konfiguration & Troubleshooting](crm/konfiguration-troubleshooting.md) | Umgebungsvariablen, häufige Fehler, Lösungen |

---

## Technologie-Stack (Kurzübersicht)

| Komponente | Technologie |
|------------|-------------|
| Framework | Spring Boot 3.5.3 |
| Sprache | Java 21 |
| Datenbank | PostgreSQL (Hibernate 6 ORM) |
| Authentifizierung | JWT + Spring Security |
| Build | Maven 3 |
| API-Docs | Swagger UI (`/swagger-ui.html`) |
| Soft-Delete | Hibernate `@SQLDelete` + `@SQLRestriction` |
| Datei-Upload | Lokales Filesystem (konfigurierbar) |

---

## Generelle Architekturregeln

- **IDs**: UUID für alle Domain-Entitäten
- **Soft-Delete**: Nie physisch löschen — immer `deleted = true` setzen via `@SQLDelete`
- **DTOs**: Jede Entität hat ein `XyzDto` mit statischer `fromEntity()`-Methode
- **Services**: `@Transactional` auf Klassenebene, `readOnly = true` für Lesemethoden
- **RBAC**: `@PreAuthorize` auf jedem Controller-Endpunkt (nie offen lassen)
- **Pagination**: Query-Params `paginated`, `page`, `size`, `sortBy`, `sortDirection` — Response-Header `X-Total-Count`, `X-Total-Pages`, `X-Current-Page`
