# CRM-Modul — API-Referenz

Alle Endpunkte sind unter `/api/crm/` erreichbar. JWT-Token muss im Header mitgeschickt werden:

```
Authorization: Bearer <token>
```

---

## Dokumente — `/api/crm/documents`

### Datei hochladen

**POST** `/api/crm/documents/upload`
**Rolle:** `ADMIN`
**Content-Type:** `multipart/form-data`

| Parameter | Typ | Pflicht | Beschreibung |
|-----------|-----|---------|--------------|
| `file` | MultipartFile | ✅ | Die Datei (max. 50 MB) |
| `customerId` | UUID | ✅ oder `contractId` | Zugehöriger Kunde |
| `contractId` | UUID | ✅ oder `customerId` | Zugehöriger Vertrag |
| `documentType` | ENUM | ❌ | `EMAIL`, `BILD`, `PDF`, `VERTRAG`, `ANGEBOT`, `RECHNUNG`, `SONSTIGES` |
| `description` | String | ❌ | Beschreibung der Datei |

```bash
# Beispiel: PDF für einen Kunden hochladen
curl -X POST http://localhost:8080/api/crm/documents/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@/pfad/zum/vertrag.pdf" \
  -F "customerId=550e8400-e29b-41d4-a716-446655440000" \
  -F "documentType=VERTRAG" \
  -F "description=Unterzeichneter Rahmenvertrag 2025"
```

**Antwort (201 Created):**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "originalFileName": "vertrag.pdf",
  "mimeType": "application/pdf",
  "fileSize": 204800,
  "description": "Unterzeichneter Rahmenvertrag 2025",
  "documentType": "VERTRAG",
  "uploadedBy": "admin",
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "customerName": "Max Müller",
  "contractId": null,
  "contractTitle": null,
  "createdAt": "2026-05-31T13:00:00",
  "updatedAt": "2026-05-31T13:00:00"
}
```

---

### Datei herunterladen

**GET** `/api/crm/documents/{id}/download`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

```bash
curl -X GET http://localhost:8080/api/crm/documents/a1b2c3d4.../download \
  -H "Authorization: Bearer <token>" \
  --output vertrag.pdf
```

**Antwort:** Binärdaten der Datei mit Headers:
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="vertrag.pdf"
Content-Length: 204800
```

---

### Dokument-Metadaten abrufen

**GET** `/api/crm/documents/{id}`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

```bash
curl -X GET http://localhost:8080/api/crm/documents/a1b2c3d4... \
  -H "Authorization: Bearer <token>"
```

---

### Dokumente eines Kunden abrufen

**GET** `/api/crm/documents/by-customer/{customerId}`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

| Query-Param | Standard | Beschreibung |
|-------------|----------|--------------|
| `paginated` | `false` | Pagination aktivieren |
| `page` | `0` | Seitennummer (0-basiert) |
| `size` | `20` | Einträge pro Seite |
| `sortBy` | `createdAt` | Sortierfeld |
| `sortDirection` | `DESC` | `ASC` oder `DESC` |

```bash
# Alle Dokumente eines Kunden (nicht paginiert)
curl -X GET "http://localhost:8080/api/crm/documents/by-customer/550e8400..." \
  -H "Authorization: Bearer <token>"

# Paginiert, Seite 1
curl -X GET "http://localhost:8080/api/crm/documents/by-customer/550e8400...?paginated=true&page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

**Paginierte Antwort-Header:**
```
X-Total-Count: 42
X-Total-Pages: 5
X-Current-Page: 0
```

---

### Dokumente eines Vertrags abrufen

**GET** `/api/crm/documents/by-contract/{contractId}`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

Gleiche Query-Parameter wie bei Kunden-Endpoint.

---

### Dokument-Metadaten aktualisieren

**PATCH** `/api/crm/documents/{id}`
**Rolle:** `ADMIN`

| Query-Param | Beschreibung |
|-------------|--------------|
| `description` | Neue Beschreibung |
| `documentType` | Neuer Dokumenttyp |

```bash
curl -X PATCH "http://localhost:8080/api/crm/documents/a1b2c3d4...?description=Aktualisiert&documentType=RECHNUNG" \
  -H "Authorization: Bearer <token>"
```

---

### Dokument löschen (Soft-Delete)

**DELETE** `/api/crm/documents/{id}`
**Rolle:** `ADMIN`

```bash
curl -X DELETE http://localhost:8080/api/crm/documents/a1b2c3d4... \
  -H "Authorization: Bearer <token>"
```

**Antwort:** `204 No Content`

> **Hinweis:** Die physische Datei auf dem Server wird ebenfalls gelöscht. Der DB-Eintrag bleibt mit `deleted = true` erhalten.

---

---

## Notizen — `/api/crm/notes`

### Notiz erstellen

**POST** `/api/crm/notes`
**Rolle:** `ADMIN`, `USER`

| Query-Param | Pflicht | Beschreibung |
|-------------|---------|--------------|
| `customerId` | ✅ oder `contractId` | Zugehöriger Kunde |
| `contractId` | ✅ oder `customerId` | Zugehöriger Vertrag |

**Request Body:**
```json
{
  "title": "Kundengespräch vom 31.05.2026",
  "content": "Der Kunde hat Interesse an einer Erweiterung des Vertrags signalisiert. Follow-up bis 15.06.",
  "priority": "HOCH"
}
```

```bash
curl -X POST "http://localhost:8080/api/crm/notes?customerId=550e8400..." \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title": "Kundengespräch", "content": "...", "priority": "HOCH"}'
```

**Antwort (201 Created):**
```json
{
  "id": "b2c3d4e5-...",
  "title": "Kundengespräch vom 31.05.2026",
  "content": "Der Kunde hat Interesse...",
  "priority": "HOCH",
  "createdBy": "admin",
  "customerId": "550e8400-...",
  "customerName": "Max Müller",
  "contractId": null,
  "contractTitle": null,
  "createdAt": "2026-05-31T14:00:00",
  "updatedAt": "2026-05-31T14:00:00"
}
```

---

### Notiz abrufen

**GET** `/api/crm/notes/{id}`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

---

### Notizen eines Kunden abrufen

**GET** `/api/crm/notes/by-customer/{customerId}`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

Sortierung: neueste zuerst (`createdAt DESC`).

| Query-Param | Standard |
|-------------|----------|
| `paginated` | `false` |
| `page` | `0` |
| `size` | `20` |
| `sortBy` | `createdAt` |
| `sortDirection` | `DESC` |

---

### Notizen eines Vertrags abrufen

**GET** `/api/crm/notes/by-contract/{contractId}`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

---

### Notiz aktualisieren

**PUT** `/api/crm/notes/{id}`
**Rolle:** `ADMIN`, `USER`

```json
{
  "title": "Geänderter Titel",
  "content": "Aktualisierter Inhalt",
  "priority": "MITTEL"
}
```

---

### Notiz löschen

**DELETE** `/api/crm/notes/{id}`
**Rolle:** `ADMIN`

**Antwort:** `204 No Content`

---

---

## Aktivitäten — `/api/crm/activities`

### Aktivität erstellen

**POST** `/api/crm/activities`
**Rolle:** `ADMIN`, `USER`

| Query-Param | Pflicht | Beschreibung |
|-------------|---------|--------------|
| `customerId` | ✅ oder `contractId` | Zugehöriger Kunde |
| `contractId` | ✅ oder `customerId` | Zugehöriger Vertrag |

**Request Body:**
```json
{
  "title": "Telefonat wegen Vertragsverlängerung",
  "description": "Kunde wünscht Angebot für 2 weitere Jahre",
  "activityType": "ANRUF",
  "status": "OFFEN",
  "activityDate": "2026-06-01T10:00:00",
  "dueDate": "2026-06-05T17:00:00",
  "contactPerson": "Frau Schmidt"
}
```

**Verfügbare `activityType`-Werte:** `ANRUF`, `EMAIL`, `MEETING`, `AUFGABE`, `BESUCH`, `SONSTIGES`

**Verfügbare `status`-Werte:** `OFFEN`, `IN_BEARBEITUNG`, `ABGESCHLOSSEN`, `ABGESAGT`

```bash
curl -X POST "http://localhost:8080/api/crm/activities?customerId=550e8400..." \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Telefonat wegen Vertragsverlängerung",
    "activityType": "ANRUF",
    "dueDate": "2026-06-05T17:00:00"
  }'
```

---

### Aktivität abrufen

**GET** `/api/crm/activities/{id}`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

---

### Aktivitäten eines Kunden abrufen

**GET** `/api/crm/activities/by-customer/{customerId}`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

Sortierung: neueste `activityDate` zuerst.

| Query-Param | Standard |
|-------------|----------|
| `paginated` | `false` |
| `page` | `0` |
| `size` | `20` |
| `sortBy` | `activityDate` |
| `sortDirection` | `DESC` |

---

### Aktivitäten eines Vertrags abrufen

**GET** `/api/crm/activities/by-contract/{contractId}`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

---

### Überfällige Aktivitäten abrufen

**GET** `/api/crm/activities/overdue`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

Gibt alle Aktivitäten zurück, bei denen `due_date < jetzt` und `status = OFFEN`.

```bash
curl -X GET http://localhost:8080/api/crm/activities/overdue \
  -H "Authorization: Bearer <token>"
```

---

### Aktivität aktualisieren

**PUT** `/api/crm/activities/{id}`
**Rolle:** `ADMIN`, `USER`

Felder wie beim Erstellen. Alle optionalen Felder können weggelassen werden (werden nicht überschrieben wenn `null`).

---

### Aktivität abschließen

**PATCH** `/api/crm/activities/{id}/complete`
**Rolle:** `ADMIN`, `USER`

Setzt `status = ABGESCHLOSSEN` und optional das Ergebnis.

| Query-Param | Beschreibung |
|-------------|--------------|
| `result` | Ergebnis/Nachbereitung (optional) |

```bash
curl -X PATCH "http://localhost:8080/api/crm/activities/b2c3d4.../complete?result=Angebot%20wurde%20akzeptiert" \
  -H "Authorization: Bearer <token>"
```

---

### Aktivität löschen

**DELETE** `/api/crm/activities/{id}`
**Rolle:** `ADMIN`

**Antwort:** `204 No Content`

---

---

## Ansprechpartner — `/api/crm/contacts`

### Ansprechpartner erstellen

**POST** `/api/crm/contacts`
**Rolle:** `ADMIN`

| Query-Param | Pflicht | Beschreibung |
|-------------|---------|--------------|
| `customerId` | ✅ | Zugehöriger Kunde |

**Request Body:**
```json
{
  "firstName": "Anna",
  "lastName": "Becker",
  "email": "a.becker@firma.de",
  "phone": "+49 30 12345678",
  "mobile": "+49 170 9876543",
  "position": "Einkaufsleiterin",
  "department": "Einkauf",
  "notes": "Bevorzugt Kontakt per E-Mail",
  "primaryContact": true
}
```

```bash
curl -X POST "http://localhost:8080/api/crm/contacts?customerId=550e8400..." \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"firstName": "Anna", "lastName": "Becker", "email": "a.becker@firma.de", "primaryContact": true}'
```

**Antwort (201 Created):**
```json
{
  "id": "c3d4e5f6-...",
  "firstName": "Anna",
  "lastName": "Becker",
  "fullName": "Anna Becker",
  "email": "a.becker@firma.de",
  "phone": "+49 30 12345678",
  "mobile": "+49 170 9876543",
  "position": "Einkaufsleiterin",
  "department": "Einkauf",
  "notes": "Bevorzugt Kontakt per E-Mail",
  "primaryContact": true,
  "customerId": "550e8400-...",
  "customerName": "Max Müller",
  "createdAt": "2026-05-31T14:30:00",
  "updatedAt": "2026-05-31T14:30:00"
}
```

> **Hinweis:** Wenn `primaryContact: true` gesetzt wird, wird ein eventuell bestehender Primär-Kontakt desselben Kunden automatisch auf `false` gesetzt.

---

### Ansprechpartner abrufen

**GET** `/api/crm/contacts/{id}`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

---

### Ansprechpartner eines Kunden abrufen

**GET** `/api/crm/contacts/by-customer/{customerId}`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

Sortierung: Primär-Kontakt zuerst, dann alphabetisch nach Nachname.

| Query-Param | Standard |
|-------------|----------|
| `paginated` | `false` |
| `page` | `0` |
| `size` | `20` |
| `sortBy` | `lastName` |
| `sortDirection` | `ASC` |

---

### Ansprechpartner suchen

**GET** `/api/crm/contacts/search?q={suchbegriff}`
**Rolle:** `ADMIN`, `USER`, `CRM_READ`

Suche in Vor- und Nachname (case-insensitive).

```bash
curl -X GET "http://localhost:8080/api/crm/contacts/search?q=becker" \
  -H "Authorization: Bearer <token>"
```

---

### Ansprechpartner aktualisieren

**PUT** `/api/crm/contacts/{id}`
**Rolle:** `ADMIN`

Gleiche Felder wie beim Erstellen. Pflichtfelder: `firstName`, `lastName`.

---

### Als Hauptansprechpartner setzen

**PATCH** `/api/crm/contacts/{id}/set-primary`
**Rolle:** `ADMIN`

Setzt diesen Kontakt als Primär-Kontakt und deaktiviert den bisherigen.

```bash
curl -X PATCH http://localhost:8080/api/crm/contacts/c3d4e5.../set-primary \
  -H "Authorization: Bearer <token>"
```

---

### Ansprechpartner löschen

**DELETE** `/api/crm/contacts/{id}`
**Rolle:** `ADMIN`

**Antwort:** `204 No Content`

---

## Fehlerformate

Alle Fehler haben folgendes Format:

```json
{
  "statusCode": 404,
  "error": "Not Found",
  "message": "Dokument nicht gefunden: a1b2c3d4-..."
}
```

```json
{
  "statusCode": 400,
  "error": "Bad Request",
  "message": "Dokument muss einem Kunden oder Vertrag zugeordnet sein"
}
```

```json
{
  "statusCode": 400,
  "error": "Validation Failed",
  "message": "Eingabe enthält ungültige Felder",
  "fieldErrors": [
    { "field": "title", "message": "Titel darf nicht leer sein" }
  ]
}
```
