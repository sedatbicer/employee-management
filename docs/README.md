# Employee Management – Kubernetes Documentation

## 1. Überblick

Diese Dokumentation beschreibt die Architektur, Implementierung, Konfiguration und den Betrieb der Kubernetes-Umgebung des Employee-Management-Projekts.

Die Anwendung besteht aus einem Angular-Frontend, zwei Spring-Boot-Backend-Services und zwei MySQL-Datenbanken.

Die Kubernetes-Umgebung wurde zunächst lokal mit Docker Desktop eingerichtet.

Eine spätere Bereitstellung auf AWS Elastic Kubernetes Service (EKS) ist vorgesehen. Die AWS-Konfiguration befindet sich derzeit in Vorbereitung und ist noch nicht deployt oder funktional getestet.

Diese Dokumentation soll sowohl als technische Referenz für die Entwicklung als auch als Betriebs- und Wartungsdokumentation dienen.

## 2. Projektarchitektur

Die Anwendung ist in drei logische Ebenen unterteilt.

| Ebene | Namespace | Komponenten |
|---|---|---|
| Frontend | `employee-frontend` | Angular, Nginx |
| Backend | `employee-backend` | Employee Service, Auth Service |
| Datenbank | `employee-database` | Employee MySQL, Auth MySQL |

### 2.1 Architekturübersicht

```text
                         Benutzer
                            |
                            v
                    +---------------+
                    |    Browser    |
                    +---------------+
                            |
                            | HTTP
                            v
              +----------------------------+
              |     employee-frontend      |
              |                            |
              | Angular Application        |
              | Nginx Reverse Proxy        |
              +----------------------------+
                            |
                 +----------+----------+
                 |                     |
                 v                     v
       +------------------+  +------------------+
       | Employee Service |  |   Auth Service   |
       |     Port 8081    |  |    Port 8082     |
       +------------------+  +------------------+
                 |                     |
                 v                     v
       +------------------+  +------------------+
       |  Employee MySQL  |  |    Auth MySQL    |
       |     people_hub   |  | people_hub_auth  |
       +------------------+  +------------------+
```

Die Kommunikation zwischen den Komponenten erfolgt über interne Kubernetes Services.

Der Zugriff auf die Anwendung erfolgt lokal über den Frontend-Service und Port-Forwarding.

## 3. Dokumentationsstruktur

Die Dokumentation ist nach Themenbereichen organisiert.

```text
docs/
│
├── README.md
│
├── architecture-decisions/
│   └── ADR-002-namespace-separation.md
│
├── architecture/
│   └── system-architecture.md
│
├── implementation/
│   ├── local-kubernetes-environment.md
│   └── database-and-data-migration.md
│
├── security/
│   └── security-and-network-policies.md
│
├── operations/
│   ├── kubernetes-command-reference.md
│   ├── monitoring-and-resource-management.md
│   └── troubleshooting.md
│
└── aws-eks/
    └── preparation-and-deployment-guide.md
```

Die Dokumente haben unterschiedliche Aufgaben und ergänzen sich gegenseitig.

## 4. Architekturentscheidungen

### ADR-002: Namespace Separation

Datei:

`architecture-decisions/ADR-002-namespace-separation.md`

Dieses Architekturentscheidungsdokument beschreibt die Aufteilung der Kubernetes-Anwendung in drei Namespaces.

Es dokumentiert:

- Den architektonischen Hintergrund
- Die getroffene Entscheidung
- Die verwendeten Namespaces
- Die Gründe für die Trennung
- Betrachtete Alternativen
- Technische und sicherheitsbezogene Konsequenzen
- Den aktuellen Implementierungsstatus

## 5. Architektur

### Systemarchitektur

Datei:

`architecture/system-architecture.md`

Dieses Dokument beschreibt den technischen Aufbau der gesamten Anwendung.

Dazu gehören:

- Angular-Frontend und Nginx
- Employee Service
- Auth Service
- Employee MySQL
- Auth MySQL
- Kubernetes Services und DNS
- Kommunikation zwischen den Anwendungsschichten
- Kustomize-Grundstruktur

## 6. Implementierung

### 6.1 Lokale Kubernetes-Umgebung

Datei:

`implementation/local-kubernetes-environment.md`

Dieses Dokument beschreibt die Einrichtung und den aktuellen technischen Stand der lokalen Docker-Desktop-Kubernetes-Umgebung.

Behandelt werden:

- Voraussetzungen
- Docker Desktop und WSL2
- Kubernetes-Konfiguration
- Namespace-Struktur
- Deployment der Anwendung
- Container-Images
- Services
- Persistent Storage
- Health Checks
- Ressourcenlimits
- Kustomize

### 6.2 Datenbanken und Datenmigration

Datei:

`implementation/database-and-data-migration.md`

Dieses Dokument beschreibt die beiden MySQL-Datenbanken und die Übernahme der bestehenden Daten aus der Docker-Compose-Umgebung.

Behandelt werden:

- Datenbankarchitektur
- MySQL StatefulSets
- PersistentVolumeClaims
- StorageClass
- Datenbankzugänge
- Backup-Erstellung
- SQL-Dumps
- Datenimport
- Datenintegrität
- Schutz vorhandener Daten

## 7. Sicherheit

### Security and Network Policies

Datei:

`security/security-and-network-policies.md`

Dieses Dokument beschreibt die grundlegenden Sicherheitsmaßnahmen der Kubernetes-Umgebung.

Behandelt werden:

- Namespace-Trennung
- Kubernetes Secrets
- Zugangsdaten
- NetworkPolicies
- Erlaubte Kommunikationswege
- Spring Security und Health-Endpunkte
- Sicherheitsgrenzen der lokalen Umgebung
- Noch offene Sicherheitsmaßnahmen für AWS EKS

## 8. Betrieb und Wartung

### 8.1 Kubernetes Command Reference

Datei:

`operations/kubernetes-command-reference.md`

Die Befehlsreferenz enthält die wichtigsten Kubernetes-Kommandos für den täglichen Betrieb.

Dazu gehören:

- Cluster- und Namespace-Verwaltung
- Deployments und Pods
- Services
- Logs
- Health Checks
- StatefulSets
- PersistentVolumeClaims
- ConfigMaps und Secrets
- Port-Forwarding
- Kustomize
- Metrics Server

### 8.2 Monitoring und Ressourcenverwaltung

Datei:

`operations/monitoring-and-resource-management.md`

Dieses Dokument beschreibt die Überwachung der Kubernetes-Workloads.

Behandelt werden:

- Metrics Server
- CPU- und RAM-Verbrauch
- Resource Requests
- Resource Limits
- Kubernetes Probes
- Auswertung der aktuellen Messwerte
- Grenzen des lokalen Monitorings

### 8.3 Fehleranalyse

Datei:

`operations/troubleshooting.md`

Dieses Dokument unterstützt bei der Analyse typischer Probleme.

Behandelt werden:

- Pods starten nicht
- Container werden neu gestartet
- Image-Probleme
- Datenbankverbindungen
- PersistentVolumeClaims
- Health Checks
- Netzwerkverbindungen
- Nginx und Frontend
- Metrics Server

## 9. AWS EKS

### Vorbereitung und Bereitstellung

Datei:

`aws-eks/preparation-and-deployment-guide.md`

Dieses Dokument beschreibt die geplante Übertragung der lokalen Kubernetes-Architektur auf AWS EKS.

Behandelt werden:

- Zielarchitektur
- Amazon ECR
- Amazon EKS
- Persistenter Speicher mit AWS EBS
- Load Balancer und Ingress
- Secret-Verwaltung
- Netzwerk- und Sicherheitskonfiguration
- Anpassung der Kustomize-Overlays
- Geplante Bereitstellungsschritte

**Aktueller Status:** Die AWS-EKS-Umgebung wurde noch nicht eingerichtet oder getestet. Die Dokumentation beschreibt die Vorbereitung und die erforderlichen späteren Schritte.

## 10. Aktueller Implementierungsstand

Die folgende Übersicht unterscheidet zwischen lokal umgesetzten Funktionen und zukünftigen Arbeiten.

| Bereich | Status |
|---|---|
| Docker-Desktop-Kubernetes | Implementiert |
| Namespace-Trennung | Implementiert |
| Frontend mit Nginx | Implementiert |
| Employee Service | Implementiert |
| Auth Service | Implementiert |
| Employee MySQL | Implementiert |
| Auth MySQL | Implementiert |
| Datenmigration | Durchgeführt |
| PersistentVolumeClaims | Implementiert |
| Kubernetes Secrets | Implementiert |
| NetworkPolicies | Erstellt und angewendet |
| Health Checks | Implementiert und geprüft |
| Resource Requests und Limits | Implementiert |
| Metrics Server | Installiert und funktionsfähig |
| Kustomize Base | Implementiert |
| Docker-Desktop-Overlay | Implementiert |
| AWS-EKS-Overlay | Grundgerüst vorhanden |
| AWS-EKS-Bereitstellung | Ausstehend |
| Funktionale Negativtests der NetworkPolicies | Ausstehend |
| Produktionsgeeignete AWS-Sicherheitskonfiguration | Ausstehend |

## 11. Dokumentationsgrundsätze

Die Dokumentation orientiert sich am tatsächlich implementierten Projektstand.

Dabei gelten folgende Grundsätze:

1. Implementierte Funktionen werden als umgesetzt dokumentiert.
2. Erfolgreich geprüfte Funktionen werden als geprüft gekennzeichnet.
3. Nicht durchgeführte Tests werden nicht als erfolgreich dargestellt.
4. Noch nicht implementierte Funktionen werden als offen oder geplant gekennzeichnet.
5. Zugangsdaten und vertrauliche Werte werden niemals im Klartext dokumentiert.
6. Änderungen an persistenten Datenbankressourcen müssen mit besonderer Vorsicht behandelt werden.
7. Lokale Docker-Desktop-Besonderheiten werden von produktiven AWS-EKS-Anforderungen getrennt.

## 12. Ziel der Dokumentation

Die Dokumentation soll eine nachvollziehbare technische Grundlage für die weitere Entwicklung und den Betrieb des Employee-Management-Systems bilden.

Sie ermöglicht:

- Das Verständnis der Gesamtarchitektur
- Die nachvollziehbare Verwaltung der Kubernetes-Ressourcen
- Die Wartung und Fehleranalyse
- Die sichere Verwaltung der Datenbanken
- Die Überprüfung der aktuellen Implementierung
- Die spätere Vorbereitung der AWS-EKS-Bereitstellung

Die Dokumentation wird bei wesentlichen Änderungen an Architektur, Implementierung oder Betriebsabläufen aktualisiert.