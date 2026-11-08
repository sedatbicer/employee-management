# Employee Management – Kubernetes, CI/CD & GitOps Documentation

## 1. Überblick

Diese Dokumentation beschreibt die Architektur, Implementierung, Konfiguration und den Betrieb der Kubernetes-Umgebung des Employee-Management-Projekts.

Die Anwendung besteht aus einem Angular-Frontend, zwei Spring-Boot-Backend-Services und zwei MySQL-Datenbanken.

Die Kubernetes-Umgebung wurde zunächst lokal mit Docker Desktop eingerichtet. Darauf aufbauend wurde eine automatisierte CI/CD- und GitOps-Architektur implementiert.

Die kontinuierliche Integration und Bereitstellung basiert auf folgenden Technologien:

- **GitHub Actions** für automatisierte Tests, Builds und Integrationsprüfungen
- **Docker** für die Containerisierung der Anwendung
- **GitHub Container Registry (GHCR)** für die Speicherung der Container-Images
- **Argo CD** für die deklarative Bereitstellung und Synchronisierung der Kubernetes-Ressourcen
- **Argo CD Image Updater** für die automatische Erkennung neuer Container-Images und die Aktualisierung der Image-Tags im Git-Repository
- **Kustomize** für die Verwaltung der Kubernetes-Konfigurationen

Eine spätere Bereitstellung auf AWS Elastic Kubernetes Service (EKS) ist vorgesehen. Die AWS-Konfiguration befindet sich derzeit in Vorbereitung und wurde noch nicht deployt oder funktional getestet.

Diese Dokumentation dient als technische Referenz für die Entwicklung, Implementierung, Wartung und den Betrieb der Kubernetes- und GitOps-Umgebung.

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

Die Kommunikation zwischen den Anwendungskomponenten erfolgt über interne Kubernetes Services.

Der lokale Zugriff auf die Anwendung erfolgt über den Frontend-Service und Kubernetes Port-Forwarding.

### 2.2 Infrastruktur- und Deployment-Architektur

Neben der eigentlichen Anwendung wurde eine automatisierte Bereitstellungsarchitektur eingerichtet.

```text
                  Entwickler
                      |
                      v
                 GitHub Repo
                      |
                      v
               GitHub Actions
                      |
          +-----------+-----------+
          |                       |
          v                       v
     Backend Tests           Frontend Build
          |                       |
          +-----------+-----------+
                      |
                      v
             Docker Integration
                Tests
                      |
                      v
              Docker Image Build
                      |
                      v
             GitHub Container
                 Registry
                  (GHCR)
                      |
                      v
              Argo CD Image
                 Updater
                      |
                      v
               Git Repository
              Image-Tag-Update
                      |
                      v
                  Argo CD
                      |
                      v
             Kubernetes Cluster
              (Docker Desktop)
                      |
                      v
              Laufende Anwendung
```

Die CI/CD-Pipeline übernimmt die Prüfung und Erstellung der Container-Images. Der GitOps-Prozess übernimmt anschließend die Aktualisierung der Kubernetes-Deployment-Konfiguration.

Dadurch werden Build-Prozess und Deployment-Prozess voneinander getrennt.

## 3. Dokumentationsstruktur

Die Dokumentation ist nach Themenbereichen organisiert.

```text
docs/
│
└── kubernetes/
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
    ├── cicd/
    │   ├── ci-cd-pipeline.md
    │   ├── container-registry.md
    │   └── gitops-and-argocd.md
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

Die einzelnen Dokumente behandeln unterschiedliche technische Themen und ergänzen sich gegenseitig.

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
- Zusammenspiel der Kubernetes-Ressourcen

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
- Lokaler Zugriff über Port-Forwarding

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

## 7. CI/CD und Container-Management

Dieser Bereich beschreibt die automatisierte Integration, Prüfung und Erstellung der Container-Images.

### 7.1 CI/CD-Pipeline mit GitHub Actions

Datei:

`cicd/ci-cd-pipeline.md`

Die CI/CD-Pipeline wurde mit GitHub Actions implementiert.

Sie automatisiert wesentliche Schritte der Softwareentwicklung und stellt sicher, dass Änderungen vor der Veröffentlichung der Container-Images geprüft werden.

Die Pipeline wird bei Push-Ereignissen auf den Branches `main` und `ci-cd-pipeline` sowie bei Pull Requests gegen `main` ausgeführt.

Die Pipeline besteht aus folgenden Jobs:

**Backend Tests**

- Automatisierte Tests für den Employee Service
- Automatisierte Tests für den Auth Service
- Java 21 und Maven
- Temporäre MySQL-Testdatenbank
- Ausführung der Maven-Tests
- Bereitstellung der Testberichte als GitHub Actions Artefakte

**Frontend Build & Security**

- Einrichtung der Node.js-Umgebung
- Installation der Abhängigkeiten mit `npm ci`
- Sicherheitsprüfung mit `npm audit`
- Angular Production Build
- Bereitstellung der Build-Artefakte

**Dependency Review**

- Prüfung von Abhängigkeitsänderungen bei Pull Requests
- Bewertung erkannter Sicherheitslücken
- Abbruch bei Sicherheitsproblemen ab dem definierten Schweregrad High

**Docker Build & Integration Tests**

- Aufbau der Docker-Umgebung mit Docker Compose
- Erstellung und Start der benötigten Container
- Integrationstests der Anwendungskomponenten
- Prüfung der Health-Endpunkte
- Überprüfung der Erreichbarkeit des Frontends
- Ausgabe von Container-Logs bei Fehlern
- Bereinigung der Testumgebung nach Abschluss

**Container Images in GHCR Publish**

Dieser Job wird bei erfolgreichen Push-Workflows ausgeführt.

- Anmeldung bei GitHub Container Registry
- Erstellung der Docker-Images
- Veröffentlichung der Images in GHCR
- Verwendung von Commit-basierten Image-Tags
- Zusätzliche Branch-Tags zur Identifizierung der jeweiligen Entwicklungsstände

Die Veröffentlichung erfolgt für drei Container-Images:

- `ghcr.io/sedatbicer/employee-frontend`
- `ghcr.io/sedatbicer/employee-service`
- `ghcr.io/sedatbicer/auth-service`

Für die eindeutige Zuordnung zu einem Git-Commit wird das Tag-Schema `sha-<commit-sha>` verwendet.

Damit lässt sich nachvollziehen, welcher Quellcode-Stand zur Erstellung eines Images verwendet wurde.

**Besonderheit: Vermeidung unnötiger Pipeline-Ausführungen**

Image Updater aktualisiert die Kubernetes-Konfiguration im Git-Repository.

Damit diese automatischen Änderungen nicht erneut die vollständige CI/CD-Pipeline auslösen, wird die von Image Updater verwaltete Kustomize-Datei über `paths-ignore` beim Push-Trigger ausgeschlossen.

Dadurch werden unnötige Builds und wiederholte Image-Veröffentlichungen vermieden.

### 7.2 Container Registry – GitHub Container Registry

Datei:

`cicd/container-registry.md`

Für die zentrale Speicherung der Docker-Images wird GitHub Container Registry (GHCR) verwendet.

Die drei Anwendungskomponenten werden als separate Images veröffentlicht:

| Anwendung | Container Image |
|---|---|
| Frontend | `ghcr.io/sedatbicer/employee-frontend` |
| Employee Service | `ghcr.io/sedatbicer/employee-service` |
| Auth Service | `ghcr.io/sedatbicer/auth-service` |

Die Images werden mit Commit-basierten Tags versehen.

Beispiel:

```text
ghcr.io/sedatbicer/employee-frontend:sha-<commit-sha>
```

Diese Vorgehensweise ermöglicht:

- Eindeutige Versionierung der Images
- Nachvollziehbarkeit des Quellcode-Stands
- Automatische Erkennung neuer Versionen
- Reproduzierbare Image-Zuordnung
- Trennung zwischen Image-Erstellung und Deployment

Die Images werden über GHCR bereitgestellt und durch die Kubernetes-Workloads aus der Registry geladen.

Für den Zugriff auf private Images werden Kubernetes Image Pull Secrets verwendet.

### 7.3 GitOps mit Argo CD

Datei:

`cicd/gitops-and-argocd.md`

Für die deklarative Verwaltung und Bereitstellung der Kubernetes-Ressourcen wurde Argo CD installiert.

Argo CD überwacht das konfigurierte Git-Repository und gleicht den dort definierten Soll-Zustand mit dem tatsächlichen Zustand des Kubernetes-Clusters ab.

Die Anwendungskonfiguration wird über Kustomize verwaltet.

**Eingerichtete Argo CD Applications**

Es wurden zwei Argo CD Applications eingerichtet:

| Application | Aufgabe |
|---|---|
| `employee-management-local` | Verwaltung und Bereitstellung der Anwendung |
| `employee-management-platform` | Verwaltung der Image-Updater- und Registry-Konfiguration |

Die Application `employee-management-local` verwendet:

- Git-Repository: `sedatbicer/employee-management`
- Branch: `ci-cd-pipeline`
- Kustomize-Pfad: `k8s/overlays/docker-desktop`
- Kubernetes-Ziel: lokaler Docker-Desktop-Cluster

Die Synchronisierung erfolgt automatisch über Auto-Sync.

Die Optionen Prune und Self Heal sind derzeit deaktiviert.

**GitOps-Prinzip**

Die Kubernetes-Konfiguration im Git-Repository stellt den gewünschten Zustand der Anwendung dar.

Argo CD vergleicht diesen Zustand mit den tatsächlich vorhandenen Kubernetes-Ressourcen.

Bei Änderungen im Git-Repository erkennt Argo CD die Abweichungen und synchronisiert die konfigurierten Ressourcen entsprechend.

Dadurch wird Git zur zentralen Quelle für den deklarativen Deployment-Zustand.

### 7.4 Automatische Image-Aktualisierung mit Argo CD Image Updater

Für die automatische Erkennung neuer Container-Images wurde Argo CD Image Updater installiert.

Die Komponente überwacht die drei Anwendungskomponenten in GHCR:

- Frontend
- Employee Service
- Auth Service

Die Image-Aktualisierung verwendet die Strategie `newest-build`.

Über eine reguläre Ausdrucksfilterung werden ausschließlich Images mit Commit-basierten SHA-Tags berücksichtigt:

```text
^sha-[a-f0-9]{40}$
```

Dadurch werden nur Images berücksichtigt, die dem definierten Versionierungsschema entsprechen.

**Automatischer Ablauf**

1. GitHub Actions führt die CI/CD-Pipeline aus.
2. Die automatisierten Tests und Integrationstests werden durchgeführt.
3. Bei erfolgreichem Abschluss werden die neuen Docker-Images in GHCR veröffentlicht.
4. Argo CD Image Updater erkennt die neuen Image-Tags.
5. Image Updater aktualisiert die Image-Tags in der Kustomize-Konfiguration.
6. Die Änderung wird automatisch in den Branch `ci-cd-pipeline` zurückgeschrieben.
7. Argo CD erkennt die Änderung im Git-Repository.
8. Argo CD synchronisiert die aktualisierten Kubernetes-Ressourcen.
9. Kubernetes startet die Workloads mit den neuen Images.

Der Prozess ermöglicht eine automatisierte Aktualisierung der Anwendung, ohne dass die Image-Tags manuell in der Kubernetes-Konfiguration geändert werden müssen.

**Git Write-back**

Für die Rückschreibung der Image-Änderungen verwendet Image Updater eine separate Git-Zugangskonfiguration.

Die Schreibberechtigung ist auf das benötigte Git-Repository beschränkt.

Der Git-Zugang für Argo CD zum Auslesen des Repositorys ist davon getrennt und verwendet einen Read-only-Zugriff.

### 7.5 Zugangsdaten und Registry-Authentifizierung

Für den Zugriff auf private Container-Images und das Git-Write-back werden getrennte Kubernetes Secrets verwendet.

Aktuell werden folgende Secrets verwendet:

| Secret | Namespace | Zweck |
|---|---|---|
| `ghcr-pull-secret` | `employee-frontend` | Abruf privater Frontend-Images |
| `ghcr-pull-secret` | `employee-backend` | Abruf privater Backend-Images |
| `ghcr-image-updater` | `argocd` | Registry-Zugriff für Image Updater |
| `git-writeback-credentials` | `argocd` | Git-Write-back durch Image Updater |

Die Secrets werden manuell im Kubernetes-Cluster verwaltet und nicht im Git-Repository gespeichert.

Eine spätere zentrale und verschlüsselte Verwaltung mit Sealed Secrets ist vorgesehen.

Vertrauliche Werte, Tokens und Zugangsdaten werden nicht in dieser Dokumentation hinterlegt.

## 8. Sicherheit

### Security and Network Policies

Datei:

`security/security-and-network-policies.md`

Dieses Dokument beschreibt die grundlegenden Sicherheitsmaßnahmen der Kubernetes-Umgebung.

Behandelt werden:

- Namespace-Trennung
- Kubernetes Secrets
- Zugangsdaten
- Image Pull Secrets
- GitHub- und Registry-Zugriffsberechtigungen
- NetworkPolicies
- Erlaubte Kommunikationswege
- Spring Security und Health-Endpunkte
- Sicherheitsgrenzen der lokalen Umgebung
- Noch offene Sicherheitsmaßnahmen für AWS EKS

Die Git-Zugriffe und Registry-Zugriffe werden über getrennte Zugangsdaten verwaltet.

Die Zugangsdaten für Image Pull, Image Updater und Git Write-back werden nicht im Git-Repository gespeichert.

Die NetworkPolicies wurden erstellt und angewendet. Funktionale Negativtests zur Überprüfung unerlaubter Netzwerkverbindungen stehen noch aus.

## 9. Betrieb und Wartung

### 9.1 Kubernetes Command Reference

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
- Rollout-Status und Deployment-Überprüfung

### 9.2 Monitoring und Ressourcenverwaltung

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

### 9.3 Fehleranalyse

Datei:

`operations/troubleshooting.md`

Dieses Dokument unterstützt bei der Analyse typischer Probleme.

Behandelt werden:

- Pods starten nicht
- Container werden neu gestartet
- Image-Probleme und ImagePullBackOff
- Registry-Authentifizierung
- Datenbankverbindungen
- PersistentVolumeClaims
- Health Checks
- Netzwerkverbindungen
- Nginx und Frontend
- Argo CD Synchronisierungsprobleme
- Image Updater und Git Write-back
- Metrics Server

## 10. AWS EKS

### Vorbereitung und Bereitstellung

Datei:

`aws-eks/preparation-and-deployment-guide.md`

Dieses Dokument beschreibt die geplante Übertragung der lokalen Kubernetes-Architektur auf AWS EKS.

Behandelt werden:

- Zielarchitektur
- Amazon EKS
- Amazon ECR
- Persistenter Speicher mit AWS EBS
- Load Balancer und Ingress
- Secret-Verwaltung
- Netzwerk- und Sicherheitskonfiguration
- Anpassung der Kustomize-Overlays
- Übertragung der GitOps-Architektur
- Geplante Bereitstellungsschritte

**Aktueller Status:** Die AWS-EKS-Umgebung wurde noch nicht eingerichtet oder getestet. Die Dokumentation beschreibt die Vorbereitung und die erforderlichen späteren Schritte.

## 11. Aktueller Implementierungsstand

Die folgende Übersicht unterscheidet zwischen implementierten und erfolgreich geprüften Funktionen sowie zukünftigen Arbeiten.

| Bereich | Status |
|---|---|
| Docker-Desktop-Kubernetes | Implementiert und getestet |
| Namespace-Trennung | Implementiert |
| Frontend mit Nginx | Implementiert und getestet |
| Employee Service | Implementiert und getestet |
| Auth Service | Implementiert und getestet |
| Employee MySQL | Implementiert |
| Auth MySQL | Implementiert |
| Datenmigration | Durchgeführt |
| PersistentVolumeClaims | Implementiert |
| Kubernetes Secrets | Implementiert |
| NetworkPolicies | Erstellt und angewendet |
| Funktionale Negativtests der NetworkPolicies | Ausstehend |
| Health Checks | Implementiert und geprüft |
| Resource Requests und Limits | Implementiert |
| Metrics Server | Installiert und funktionsfähig |
| Kustomize Base | Implementiert |
| Docker-Desktop-Overlay | Implementiert |
| AWS-EKS-Overlay | Grundgerüst vorhanden |
| GitHub Actions CI/CD | Implementiert und erfolgreich getestet |
| Backend Unit Tests | Erfolgreich getestet |
| Frontend Build und Security Check | Erfolgreich getestet |
| Docker Integration Tests | Erfolgreich getestet |
| GHCR Image Publishing | Erfolgreich getestet |
| Argo CD | Installiert und funktionsfähig |
| Argo CD Applications | Implementiert und synchronisiert |
| Argo CD Image Updater | Installiert und getestet |
| Automatische Image-Erkennung | Erfolgreich getestet |
| Git Write-back | Erfolgreich getestet |
| Automatische GitOps-Synchronisierung | Erfolgreich getestet |
| Sealed Secrets | Ausstehend |
| AWS-EKS-Bereitstellung | Ausstehend |
| Produktionsgeeignete AWS-Sicherheitskonfiguration | Ausstehend |

## 12. Dokumentationsgrundsätze

Die Dokumentation orientiert sich am tatsächlich implementierten Projektstand.

Dabei gelten folgende Grundsätze:

1. Implementierte Funktionen werden als umgesetzt dokumentiert.
2. Erfolgreich geprüfte Funktionen werden als geprüft gekennzeichnet.
3. Nicht durchgeführte Tests werden nicht als erfolgreich dargestellt.
4. Noch nicht implementierte Funktionen werden als offen oder geplant gekennzeichnet.
5. Zugangsdaten und vertrauliche Werte werden niemals im Klartext dokumentiert.
6. Änderungen an persistenten Datenbankressourcen müssen mit besonderer Vorsicht behandelt werden.
7. Lokale Docker-Desktop-Besonderheiten werden von produktiven AWS-EKS-Anforderungen getrennt.
8. CI/CD- und GitOps-Abläufe werden entsprechend ihrer tatsächlichen Implementierung dokumentiert.
9. Automatische Änderungen durch Image Updater werden von manuellen Änderungen an der Anwendungskonfiguration unterschieden.

## 13. Ziel der Dokumentation

Die Dokumentation soll eine nachvollziehbare technische Grundlage für die weitere Entwicklung und den Betrieb des Employee-Management-Systems bilden.

Sie ermöglicht:

- Das Verständnis der Gesamtarchitektur
- Die nachvollziehbare Verwaltung der Kubernetes-Ressourcen
- Die Wartung und Fehleranalyse
- Die sichere Verwaltung der Datenbanken
- Die Überprüfung der aktuellen Implementierung
- Die Nachvollziehbarkeit der CI/CD-Prozesse
- Das Verständnis der automatisierten Image-Veröffentlichung
- Die Verwaltung der GitOps-Deployment-Prozesse
- Die spätere Vorbereitung der AWS-EKS-Bereitstellung

Die Dokumentation wird bei wesentlichen Änderungen an Architektur, Implementierung oder Betriebsabläufen aktualisiert.