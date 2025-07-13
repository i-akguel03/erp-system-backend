# erp-system-backend
Spring Boot Backend für ein Vertriebs-ERP-System

Projektbeschreibung: ERP-System Backend
Dieses Projekt implementiert das Backend eines modularen ERP-Systems mit Fokus auf Vertriebsprozesse. Es basiert auf Spring Boot und MongoDB und stellt RESTful APIs für die Verwaltung von Kunden, Produkten, Bestellungen, Inventar und Zahlungen bereit.

Architektur & Aufbau
    Domänenorientiertes Design:
    Das System verwendet klare Domänenmodelle (Customer, Product, Order, Inventory, Payment) zur Abstraktion der Geschäftsobjekte. Diese Modelle sind unabhängig von der konkreten Datenbankschicht.
    
    Adapter Pattern für Datenzugriff:
    Für die Datenpersistenz wird das Repository-Pattern eingesetzt, implementiert durch spezifische MongoDB-Adapter (MongoCustomerRepositoryImpl, MongoOrderRepositoryImpl etc.). So ist die Datenbankanbindung modular und könnte theoretisch gegen eine andere DB ausgetauscht werden.
    
    MongoDB als NoSQL-Datenbank:
    Daten werden in Collections mit dokumentorientiertem Schema gespeichert. Die Entitäten sind in Document-Klassen abgebildet, die zwischen Domänen- und Persistenzobjekten übersetzen.
    
    REST-API mit Spring Web:
    Endpunkte für CRUD-Operationen sind in Controller-Klassen organisiert und bedienen Frontend-Anfragen.
    Beispiel: /api/customers, /api/products, /api/orders.
    
    Security:
    Einfache Basic-Auth-Authentifizierung schützt die Endpunkte, konfiguriert mit Spring Security.
    
    Testabdeckung:
    Unit- und Integrationstests gewährleisten Stabilität und korrekte Funktionsweise der Services und Controller.
    
    Konfigurierbar und containerfähig:
    Die Anwendung ist über YAML konfigurierbar und läuft in einem Docker-Container, inkl. MongoDB als Container-Dienst.

ERP-System Backend – Kurzbeschreibung
Ein modular aufgebautes Backend für ein ERP-System, das Kernfunktionen wie Kunden-, Produkt-, Bestell-, Inventar- und Zahlungsverwaltung bereitstellt. Es basiert auf Spring Boot mit MongoDB als NoSQL-Datenbank und bietet eine RESTful API für Frontend und externe Systeme.

Das Design setzt auf klare Domänenmodelle und das Repository-Pattern mit MongoDB-Adaptern, um Flexibilität und Testbarkeit zu gewährleisten. Die Anwendung ist containerisiert und nutzt Basic-Auth für einfachen Schutz der Endpunkte.
