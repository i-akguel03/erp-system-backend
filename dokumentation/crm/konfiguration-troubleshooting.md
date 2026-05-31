# CRM-Modul — Konfiguration & Troubleshooting

---

## Konfiguration

### Umgebungsvariablen

| Variable | Standard | Beschreibung |
|----------|----------|--------------|
| `CRM_UPLOAD_DIR` | `uploads/crm` | Verzeichnis für hochgeladene Dateien (absoluter oder relativer Pfad) |

### application.yml — relevante Einträge

```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 50MB       # Max. Größe einer einzelnen Datei
      max-request-size: 55MB    # Max. Größe des gesamten Requests

app:
  crm:
    upload-dir: ${CRM_UPLOAD_DIR:uploads/crm}
```

### Produktionsumgebung (Render / Docker)

Beim Deployment auf Render oder in einem Docker-Container gilt:

1. **Persistentes Volume einbinden**: Das Upload-Verzeichnis muss auf einem persistenten Volume liegen, sonst gehen alle Dateien beim Neustart verloren.

   ```yaml
   # Docker Compose Beispiel
   volumes:
     - /mnt/data/crm-uploads:/app/uploads/crm
   ```

   ```
   # Render: Disk hinzufügen unter "Disks" im Service-Settings
   Mount Path: /app/uploads/crm
   ```

2. **Umgebungsvariable setzen**:
   ```
   CRM_UPLOAD_DIR=/app/uploads/crm
   ```

3. **Schreibrechte prüfen**: Der Prozess muss Schreibrechte auf das Verzeichnis haben.

---

## Troubleshooting

### Datei-Upload schlägt fehl (413 Request Entity Too Large)

**Symptom:** Fehler beim Upload großer Dateien.

**Ursache:** Spring oder Nginx/Reverse-Proxy hat ein Limit gesetzt.

**Lösung Spring:**
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 55MB
```

**Lösung Nginx (falls als Reverse-Proxy):**
```nginx
client_max_body_size 55M;
```

---

### Upload-Verzeichnis konnte nicht erstellt werden (Startup-Fehler)

**Symptom:**
```
RuntimeException: Upload-Verzeichnis konnte nicht erstellt werden: /app/uploads/crm
```

**Ursachen & Lösungen:**

1. **Keine Schreibrechte**: Verzeichnis gehört einem anderen User.
   ```bash
   chown -R appuser:appuser /app/uploads/crm
   chmod 755 /app/uploads/crm
   ```

2. **Pfad existiert nicht**: Übergeordnetes Verzeichnis fehlt.
   ```bash
   mkdir -p /app/uploads/crm
   ```

3. **Falscher Pfad in Konfiguration**: `CRM_UPLOAD_DIR` zeigt auf ungültiges Verzeichnis.
   → Umgebungsvariable prüfen.

---

### Datei nicht gefunden beim Download (404)

**Symptom:**
```json
{"statusCode": 404, "message": "Datei nicht gefunden: customers/uuid/datei.pdf"}
```

**Ursachen:**

1. **Datei wurde physisch gelöscht** (z.B. nach Deployment ohne persistentes Volume).
   - Prüfen ob das Upload-Verzeichnis korrekt gemountet ist.
   - In Render: Disk konfiguriert und an richtigen Mount-Path gebunden?

2. **Soft-Delete wurde aufgerufen**: Das Dokument ist in der DB als `deleted = true` markiert und die Datei wurde gelöscht. Das ist korrektes Verhalten.

3. **Falsches Upload-Verzeichnis**: `CRM_UPLOAD_DIR` zeigt nicht auf dasselbe Verzeichnis wie beim Upload.

**Diagnose:**
```sql
-- Prüfen ob Dokument in DB existiert (inkl. gelöschter)
SELECT id, original_file_name, file_path, deleted 
FROM crm_documents 
WHERE id = '<uuid>';
```

---

### "Dokument muss einem Kunden oder Vertrag zugeordnet sein" (400)

**Ursache:** Beim Upload wurden weder `customerId` noch `contractId` als Query-Parameter mitgeschickt.

**Lösung:** Mindestens einen der beiden Parameter im Request angeben:
```bash
# Richtig:
POST /api/crm/documents/upload?customerId=550e8400...

# Falsch:
POST /api/crm/documents/upload   (ohne Parameter)
```

---

### Soft-Delete funktioniert nicht — Einträge tauchen weiterhin auf

**Symptom:** Nach `DELETE` ist der Eintrag noch in der API sichtbar.

**Mögliche Ursachen:**

1. **Eigene JPQL-Query ohne `deleted = false` Filter**: `@SQLRestriction` gilt nur für automatische Spring Data Queries. Manuelle `@Query`-Annotationen müssen `WHERE x.deleted = false` explizit enthalten.

   ```java
   // Falsch — @SQLRestriction greift hier NICHT:
   @Query("SELECT n FROM CrmNote n WHERE n.customer.id = :id")
   
   // Richtig:
   @Query("SELECT n FROM CrmNote n WHERE n.customer.id = :id AND n.deleted = false")
   ```

2. **`deleteById()` statt `delete(entity)`**: `deleteById()` nutzt intern `getReference()` was den Hibernate-Proxy erzeugt, ohne die Entity zu laden. Der `@SQLDelete`-Hook wird dann nicht zuverlässig ausgeführt.

   ```java
   // Falsch:
   repository.deleteById(id);
   
   // Richtig:
   Entity entity = repository.findById(id).orElseThrow(...);
   repository.delete(entity);
   ```

---

### Primär-Kontakt wird nicht zurückgesetzt

**Symptom:** Mehrere Ansprechpartner desselben Kunden haben `primaryContact = true`.

**Ursache:** Daten wurden direkt in die DB eingefügt (z.B. via SQL-Skript) ohne die Service-Logik zu nutzen.

**Lösung (SQL):**
```sql
-- Alles für diesen Kunden auf false setzen
UPDATE crm_contacts SET primary_contact = false 
WHERE customer_id = '<uuid>';

-- Dann den gewünschten Primär-Kontakt setzen
UPDATE crm_contacts SET primary_contact = true 
WHERE id = '<contact-uuid>';
```

**Prävention:** Immer den API-Endpunkt `PATCH /api/crm/contacts/{id}/set-primary` nutzen, niemals direkt in die DB schreiben.

---

### Aktivität erscheint nicht in `/overdue`

**Symptom:** Eine Aktivität mit vergangener `due_date` und Status `OFFEN` erscheint nicht.

**Ursache:** Die Aktivität wurde mit `status = IN_BEARBEITUNG` erstellt oder manuell geändert. Der Overdue-Endpunkt filtert nur `status = OFFEN`.

**Prüfen:**
```sql
SELECT id, title, due_date, status 
FROM crm_activities 
WHERE due_date < NOW() AND deleted = false;
```

---

### CORS-Fehler beim Datei-Download im Frontend

**Symptom:** Browser blockiert den Download-Request.

**Ursache:** Der `Content-Disposition` Header ist nicht in der CORS-Konfiguration als exposed Header eingetragen.

**Lösung** in `SecurityConfig.java` (CORS-Konfiguration):
```java
corsConfiguration.addExposedHeader("Content-Disposition");
corsConfiguration.addExposedHeader("Content-Length");
```

---

## Datenbankabfragen (Diagnose)

```sql
-- Alle Dokumente eines Kunden (inkl. gelöschte)
SELECT id, original_file_name, document_type, file_size, deleted, created_at
FROM crm_documents
WHERE customer_id = '<uuid>'
ORDER BY created_at DESC;

-- Alle offenen Aktivitäten mit Fälligkeit
SELECT id, title, activity_type, due_date, contact_person
FROM crm_activities
WHERE status = 'OFFEN' AND deleted = false
ORDER BY due_date ASC;

-- Primär-Kontakt eines Kunden
SELECT id, first_name, last_name, email, position
FROM crm_contacts
WHERE customer_id = '<uuid>' AND primary_contact = true AND deleted = false;

-- Anzahl CRM-Einträge pro Kunde
SELECT 
  c.id,
  c.first_name || ' ' || c.last_name AS name,
  COUNT(DISTINCT doc.id) AS dokumente,
  COUNT(DISTINCT n.id) AS notizen,
  COUNT(DISTINCT a.id) AS aktivitaeten,
  COUNT(DISTINCT con.id) AS ansprechpartner
FROM customers c
LEFT JOIN crm_documents doc ON doc.customer_id = c.id AND doc.deleted = false
LEFT JOIN crm_notes n ON n.customer_id = c.id AND n.deleted = false
LEFT JOIN crm_activities a ON a.customer_id = c.id AND a.deleted = false
LEFT JOIN crm_contacts con ON con.customer_id = c.id AND con.deleted = false
WHERE c.deleted = false
GROUP BY c.id, c.first_name, c.last_name
ORDER BY name;
```

---

## Erweiterung des Moduls

### Neuen Dokumenttyp hinzufügen

In `CrmDocument.java` das Enum erweitern:
```java
public enum DocumentType {
    EMAIL, BILD, PDF, VERTRAG, ANGEBOT, RECHNUNG, SONSTIGES,
    PROTOKOLL  // ← neu hinzufügen
}
```
Keine DB-Migration nötig da `ENUM → STRING` gespeichert wird.

### Neuen Aktivitätstyp hinzufügen

In `CrmActivity.java`:
```java
public enum ActivityType {
    ANRUF, EMAIL, MEETING, AUFGABE, BESUCH, SONSTIGES,
    WEBINAR  // ← neu hinzufügen
}
```

### Dokumente auch für Bestellungen (Orders) aktivieren

1. In `CrmDocument.java` ein neues `@ManyToOne`-Feld für `Order` hinzufügen:
   ```java
   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "order_id")
   private Order order;
   ```
2. In `CrmDocumentService` die Validierung anpassen (mind. eines der drei Felder gesetzt).
3. In `CrmDocumentRepository` neue `findByOrderId()`-Methoden hinzufügen.
4. In `CrmDocumentController` einen neuen Endpunkt `GET /by-order/{orderId}` anlegen.

### Externe Dateispeicherung (S3/MinIO)

Aktuell wird das lokale Dateisystem verwendet. Für Cloud-Deployment kann `FileStorageService` durch eine S3-Implementierung ersetzt werden:

1. AWS SDK oder MinIO-Client Dependency in `pom.xml` hinzufügen
2. `FileStorageService` mit einer Interface-Abstraktion versehen:
   ```java
   public interface FileStorageService {
       String store(MultipartFile file, String subDirectory);
       byte[] load(String path);
       void delete(String path);
   }
   ```
3. `LocalFileStorageService` und `S3FileStorageService` als Implementierungen anlegen
4. Via `@Profile("prod")` / `@Profile("local")` die Implementierung wählen
