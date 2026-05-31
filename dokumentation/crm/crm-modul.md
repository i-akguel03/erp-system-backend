# CRM-Modul — Architektur & Datenmodell

## Überblick

Das CRM-Modul ermöglicht es, Dokumente, Notizen, Aktivitäten und Ansprechpartner direkt mit **Kunden** (`Customer`) oder **Verträgen** (`Contract`) zu verknüpfen. Es ist vollständig in die bestehende ERP-Architektur integriert (Soft-Delete, RBAC, Pagination, Audit-Logging).

---

## Modulstruktur

```
domain/
  CrmDocument.java       Dateispeicherung (PDF, Bild, Mail, ...)
  CrmNote.java           Freitext-Notizen mit Priorität
  CrmActivity.java       Aktivitäten (Anruf, Meeting, Mail, ...)
  CrmContact.java        Ansprechpartner eines Kunden

repository/
  CrmDocumentRepository.java
  CrmNoteRepository.java
  CrmActivityRepository.java
  CrmContactRepository.java

service/
  FileStorageService.java     Dateisystem-Verwaltung (Upload/Download/Delete)
  CrmDocumentService.java
  CrmNoteService.java
  CrmActivityService.java
  CrmContactService.java

controller/
  CrmDocumentController.java   /api/crm/documents
  CrmNoteController.java       /api/crm/notes
  CrmActivityController.java   /api/crm/activities
  CrmContactController.java    /api/crm/contacts

dto/
  CrmDocumentDto.java
  CrmNoteDto.java
  CrmActivityDto.java
  CrmContactDto.java
```

---

## Datenmodell

### CrmDocument — Dokumente & Dateien

**Tabelle:** `crm_documents`

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | UUID (PK) | Primärschlüssel |
| `original_file_name` | VARCHAR | Originaldateiname beim Upload |
| `stored_file_name` | VARCHAR | Generierter UUID-basierter Dateiname auf dem Server |
| `file_path` | VARCHAR | Relativer Pfad ab Upload-Root (z.B. `customers/uuid/bild.png`) |
| `mime_type` | VARCHAR | MIME-Type (z.B. `application/pdf`, `image/jpeg`) |
| `file_size` | BIGINT | Dateigröße in Bytes |
| `description` | VARCHAR(1000) | Optionale Beschreibung |
| `document_type` | ENUM | `EMAIL`, `BILD`, `PDF`, `VERTRAG`, `ANGEBOT`, `RECHNUNG`, `SONSTIGES` |
| `uploaded_by` | VARCHAR | Benutzername aus JWT |
| `customer_id` | UUID (FK) | Verknüpfung zu `customers` (optional) |
| `contract_id` | UUID (FK) | Verknüpfung zu `contracts` (optional) |
| `created_at` | TIMESTAMP | Automatisch gesetzt via `@PrePersist` |
| `updated_at` | TIMESTAMP | Automatisch aktualisiert via `@PreUpdate` |
| `deleted` | BOOLEAN | Soft-Delete Flag (Standard: `false`) |

**Regeln:**
- Mindestens `customer_id` **oder** `contract_id` muss gesetzt sein
- Die Datei wird physisch im Dateisystem gespeichert; in der DB steht nur der Pfad
- Beim Soft-Delete wird die Datei physisch gelöscht, der DB-Eintrag bleibt mit `deleted = true`
- Max. Dateigröße: 50 MB

---

### CrmNote — Notizen

**Tabelle:** `crm_notes`

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | UUID (PK) | Primärschlüssel |
| `title` | VARCHAR | Pflichtfeld: Überschrift der Notiz |
| `content` | TEXT | Freitext (unbegrenzt) |
| `priority` | ENUM | `NIEDRIG`, `MITTEL` (Standard), `HOCH` |
| `created_by` | VARCHAR | Benutzername aus JWT |
| `customer_id` | UUID (FK) | Verknüpfung zu `customers` (optional) |
| `contract_id` | UUID (FK) | Verknüpfung zu `contracts` (optional) |
| `created_at` | TIMESTAMP | Automatisch gesetzt |
| `updated_at` | TIMESTAMP | Automatisch aktualisiert |
| `deleted` | BOOLEAN | Soft-Delete Flag |

**Regeln:**
- Mindestens `customer_id` **oder** `contract_id` muss gesetzt sein
- Listen sind absteigend nach `created_at` sortiert (neueste zuerst)

---

### CrmActivity — Aktivitäten

**Tabelle:** `crm_activities`

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | UUID (PK) | Primärschlüssel |
| `title` | VARCHAR | Pflichtfeld: Kurztitel |
| `description` | TEXT | Detailbeschreibung |
| `activity_type` | ENUM | `ANRUF`, `EMAIL`, `MEETING`, `AUFGABE`, `BESUCH`, `SONSTIGES` |
| `status` | ENUM | `OFFEN` (Standard), `IN_BEARBEITUNG`, `ABGESCHLOSSEN`, `ABGESAGT` |
| `activity_date` | TIMESTAMP | Wann die Aktivität stattgefunden hat / stattfindet |
| `due_date` | TIMESTAMP | Fälligkeitsdatum (für Überfälligkeits-Check) |
| `contact_person` | VARCHAR | Name des Ansprechpartners bei der Aktivität |
| `result` | TEXT | Ergebnis/Nachbereitung (wird beim Abschließen gesetzt) |
| `created_by` | VARCHAR | Benutzername aus JWT |
| `customer_id` | UUID (FK) | Verknüpfung zu `customers` (optional) |
| `contract_id` | UUID (FK) | Verknüpfung zu `contracts` (optional) |
| `created_at` | TIMESTAMP | Automatisch gesetzt |
| `updated_at` | TIMESTAMP | Automatisch aktualisiert |
| `deleted` | BOOLEAN | Soft-Delete Flag |

**Status-Übergänge:**
```
OFFEN → IN_BEARBEITUNG → ABGESCHLOSSEN
OFFEN → ABGESAGT
IN_BEARBEITUNG → ABGESAGT
```

**Überfällig:** Eine Aktivität gilt als überfällig wenn `due_date < jetzt` und `status = OFFEN`.

---

### CrmContact — Ansprechpartner

**Tabelle:** `crm_contacts`

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | UUID (PK) | Primärschlüssel |
| `first_name` | VARCHAR | Pflichtfeld: Vorname |
| `last_name` | VARCHAR | Pflichtfeld: Nachname |
| `email` | VARCHAR | E-Mail (Format-Validierung) |
| `phone` | VARCHAR | Telefonnummer |
| `mobile` | VARCHAR | Mobilnummer |
| `position` | VARCHAR | Position/Stelle im Unternehmen |
| `department` | VARCHAR | Abteilung |
| `notes` | VARCHAR(1000) | Freie Anmerkungen |
| `primary_contact` | BOOLEAN | Hauptansprechpartner des Kunden (max. 1 pro Kunde) |
| `customer_id` | UUID (FK) | Verknüpfung zu `customers` (Pflicht) |
| `created_at` | TIMESTAMP | Automatisch gesetzt |
| `updated_at` | TIMESTAMP | Automatisch aktualisiert |
| `deleted` | BOOLEAN | Soft-Delete Flag |

**Regeln:**
- Pro Kunde kann es nur **einen** `primary_contact = true` geben
- Wenn ein neuer Primär-Kontakt gesetzt wird, wird der alte automatisch zurückgesetzt
- Liste ist sortiert: Primär-Kontakt zuerst, dann alphabetisch nach Nachname
- `CrmContact` ist **nur** an `Customer` gebunden (nicht an Vertrag)

---

## Beziehungsdiagramm

```
Customer (1) ──────────────────────────────────────────────── (n) CrmDocument
Customer (1) ──────────────────────────────────────────────── (n) CrmNote
Customer (1) ──────────────────────────────────────────────── (n) CrmActivity
Customer (1) ──────────────────────────────────────────────── (n) CrmContact

Contract (1) ──────────────────────────────────────────────── (n) CrmDocument
Contract (1) ──────────────────────────────────────────────── (n) CrmNote
Contract (1) ──────────────────────────────────────────────── (n) CrmActivity

CrmDocument ──────── kann Customer ODER Contract referenzieren (nicht beide Pflicht)
CrmNote     ──────── kann Customer ODER Contract referenzieren
CrmActivity ──────── kann Customer ODER Contract referenzieren
CrmContact  ──────── immer an einen Customer gebunden
```

---

## FileStorageService

Der `FileStorageService` verwaltet die physische Speicherung von Dateien auf dem Server.

### Verzeichnisstruktur

```
uploads/crm/                          ← Upload-Root (konfigurierbar)
  customers/
    {customer-uuid}/
      abc123.pdf
      def456.jpg
  contracts/
    {contract-uuid}/
      vertrag_v2.pdf
```

### Sicherheit

- **Pfad-Traversal-Schutz**: Jeder angeforderte Pfad wird mit `normalize()` geprüft; Pfade außerhalb des Upload-Roots werden mit `BusinessLogicException` abgelehnt
- **Dateiname**: Der originale Dateiname wird in der DB gespeichert; auf dem Dateisystem wird ein zufälliger UUID-Dateiname verwendet, um Kollisionen und Directory Traversal zu verhindern
- **Maximale Dateigröße**: 50 MB (konfiguriert in `application.yml`)

### Ablauf beim Upload

```
1. Controller empfängt MultipartFile
2. CrmDocumentService validiert: customerId oder contractId vorhanden
3. FileStorageService.store() wird aufgerufen:
   a. Datei wird auf Größe geprüft (max 50 MB)
   b. UUID-Dateiname wird generiert (inkl. Originalendung)
   c. Datei wird in uploads/crm/{customers|contracts}/{uuid}/ gespeichert
   d. Relativer Pfad wird zurückgegeben
4. CrmDocument-Entity wird mit Pfad, Metadaten und Referenz gespeichert
```

### Ablauf beim Download

```
1. Controller empfängt GET /api/crm/documents/{id}/download
2. CrmDocumentService.findById() lädt Metadaten aus DB
3. FileStorageService.load() liest Bytes vom Dateisystem
4. Response: Content-Disposition: attachment; filename="originalname.pdf"
             Content-Type: application/pdf (aus DB)
```

---

## RBAC — Rollenberechtigungen

| Rolle | Lesen | Erstellen | Aktualisieren | Löschen |
|-------|-------|-----------|---------------|---------|
| `ADMIN` | ✅ | ✅ | ✅ | ✅ |
| `USER` | ✅ | ✅ (Notizen, Aktivitäten) | ✅ | ❌ |
| `CRM_READ` | ✅ | ❌ | ❌ | ❌ |

**Hinweis:** Dokumente und Ansprechpartner anlegen/löschen ist nur für `ADMIN` erlaubt. Notizen und Aktivitäten können auch `USER` erstellen und bearbeiten.

---

## Soft-Delete — Funktionsweise

Alle CRM-Entitäten verwenden Hibernate-Annotations für Soft-Delete:

```java
@SQLDelete(sql = "UPDATE crm_notes SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class CrmNote { ... }
```

**Effekt:**
- `repository.delete(entity)` → führt `UPDATE ... SET deleted = true` aus (kein `DELETE`)
- Alle `findBy...`-Methoden filtern automatisch `deleted = false`
- Gelöschte Einträge sind aus der API nicht mehr erreichbar
- In der Datenbank bleiben die Daten erhalten (für Audit/Recovery)

**Sonderfall Dokumente:** Beim Soft-Delete eines `CrmDocument` wird die physische Datei auf dem Dateisystem zusätzlich gelöscht (`FileStorageService.delete()`), da die Bytes keinen Mehrwert haben wenn der DB-Eintrag als gelöscht markiert ist.
