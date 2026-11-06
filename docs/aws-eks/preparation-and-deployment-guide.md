# AWS EKS Preparation and Deployment Guide

## 1. Zweck und Geltungsbereich

Dieses Dokument beschreibt die geplante Überführung des Employee-Management-Systems von der lokalen Docker-Desktop-Kubernetes-Umgebung auf Amazon Elastic Kubernetes Service (EKS).

Es dient als technische Vorbereitung für eine spätere AWS-Bereitstellung.

Die aktuelle Implementierung wurde lokal mit Docker Desktop Kubernetes umgesetzt.

Die AWS-EKS-Umgebung ist noch nicht eingerichtet.

Dieses Dokument enthält deshalb:

- Die Zielarchitektur für AWS EKS
- Die Unterschiede zwischen lokaler Umgebung und AWS
- Die vorbereitete Kustomize-Struktur
- Die Anforderungen an Container-Images
- Die Anforderungen an Datenbank-Persistenz
- Die Anforderungen an Secrets und Berechtigungen
- Die Anforderungen an Netzwerk und Sicherheit
- Eine spätere Deployment-Reihenfolge
- Platzhalter für AWS-spezifische Konfigurationswerte
- Eine Checkliste für die spätere Umsetzung

Es werden keine konkreten AWS-Ressourcen oder bereits durchgeführten Deployments vorausgesetzt.

## 2. Aktueller Implementierungsstand

Die Anwendung läuft derzeit in einer lokalen Kubernetes-Umgebung.

| Bereich | Aktueller Stand |
|---|---|
| Kubernetes-Plattform | Docker Desktop Kubernetes |
| Kubernetes Context | `docker-desktop` |
| Frontend | Angular mit integriertem Nginx |
| Backend | Employee Service und Auth Service |
| Datenbanken | Zwei MySQL-8.4-Instanzen |
| Persistenz | Lokale PersistentVolumeClaims |
| Netzwerkregeln | Kubernetes NetworkPolicies |
| Konfiguration | ConfigMaps und Secrets |
| Deployment-Verwaltung | Kustomize |
| AWS EKS | Noch nicht eingerichtet |

Die AWS-Umgebung ist eine zukünftige Zielplattform.

## 3. Zielarchitektur auf AWS EKS

Die geplante Architektur soll die bestehende logische Trennung der Anwendung beibehalten.

Die drei Kubernetes-Namespaces bleiben bestehen:

```text
employee-frontend
employee-backend
employee-database
```

### 3.1 Geplante Architektur

```text
                 Internet
                    |
                    v
          AWS Load Balancer
                    |
                    v
             employee-frontend
                    |
                    v
              Nginx / Angular
                    |
          +---------+---------+
          |                   |
          v                   v
    Employee Service     Auth Service
          |                   |
          v                   v
    Employee MySQL        Auth MySQL
          |                   |
          v                   v
       EBS Volume          EBS Volume
```

Die Darstellung ist ein Architekturvorschlag und keine bereits bereitgestellte AWS-Infrastruktur.

### 3.2 Geplante AWS-Komponenten

| Komponente | Vorgesehene Aufgabe |
|---|---|
| Amazon EKS | Kubernetes-Control-Plane |
| EKS Worker Nodes | Ausführung der Anwendungspods |
| Amazon VPC | Netzwerkisolierung |
| Public Subnets | Optionaler externer Load-Balancer-Zugriff |
| Private Subnets | Worker Nodes und interne Workloads |
| AWS Load Balancer | Externer Zugriff auf das Frontend |
| Amazon EBS | Persistenter Speicher für Datenbanken |
| AWS IAM | AWS-Berechtigungsverwaltung |
| Kubernetes Secrets | Bereitstellung vertraulicher Konfiguration |
| Amazon CloudWatch | AWS-seitiges Monitoring |

Die endgültige Auswahl und Konfiguration dieser Komponenten muss bei der tatsächlichen AWS-Planung festgelegt werden.

## 4. Kustomize-Struktur

Das Projekt verwendet Kustomize zur Verwaltung der Kubernetes-Manifeste.

Die aktuelle Struktur:

```text
k8s/
├── base/
│   ├── employee-frontend/
│   ├── employee-backend/
│   ├── database/
│   └── kustomization.yaml
│
└── overlays/
    ├── docker-desktop/
    │   └── kustomization.yaml
    │
    └── aws-eks/
        └── kustomization.yaml
```

### 4.1 Base

Der Ordner `k8s/base/` enthält die gemeinsamen Kubernetes-Ressourcen.

Dazu gehören:

- Frontend Deployment und Service
- Backend Deployments und Services
- Datenbank StatefulSets und Services
- PersistentVolumeClaims
- ConfigMaps
- NetworkPolicies

### 4.2 Docker-Desktop-Overlay

Das lokale Overlay befindet sich unter:

```text
k8s/overlays/docker-desktop/
```

Es bindet die gemeinsame Basis ein.

### 4.3 AWS-EKS-Overlay

Das AWS-Overlay befindet sich unter:

```text
k8s/overlays/aws-eks/
```

Der aktuelle Stand ist eine vorbereitete Vorlage.

Die Datei enthält derzeit lediglich die Einbindung der gemeinsamen Basis:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - ../../base
```

Es sind noch keine AWS-spezifischen Anpassungen enthalten.

Insbesondere fehlen derzeit noch die konkreten Angaben für:

- AWS StorageClasses
- AWS Load Balancer
- Externe Container-Registry
- AWS-spezifische Secrets
- Produktionsressourcen
- TLS und DNS
- AWS IAM und RBAC

Das Overlay wurde nicht gegen einen AWS-EKS-Cluster getestet.

## 5. Container-Images

### 5.1 Aktuelle Images

Die lokale Umgebung verwendet folgende Anwendungsimages:

| Anwendung | Lokales Image |
|---|---|
| Frontend | `employee-frontend:1.0.0` |
| Employee Service | `employee-service:1.0.4` |
| Auth Service | `auth-service:1.0.5` |

Die Images sind für die lokale Entwicklungsumgebung vorgesehen.

### 5.2 Externe Container-Registry

Für AWS EKS müssen die Images in einer Registry verfügbar sein, auf die die EKS-Worker-Nodes zugreifen können.

Eine mögliche Lösung ist Amazon Elastic Container Registry (ECR).

Vorgesehene Repository-Namen:

```text
<aws-account-id>.dkr.ecr.<aws-region>.amazonaws.com/employee-frontend
```

```text
<aws-account-id>.dkr.ecr.<aws-region>.amazonaws.com/employee-service
```

```text
<aws-account-id>.dkr.ecr.<aws-region>.amazonaws.com/auth-service
```

Die Platzhalter müssen später durch die tatsächlichen AWS-Werte ersetzt werden.

### 5.3 Image-Tags

Für eine nachvollziehbare Bereitstellung sollten eindeutige Image-Tags verwendet werden.

Beispiel:

```text
employee-frontend:<version>
employee-service:<version>
auth-service:<version>
```

Für produktive Deployments sollten keine unkontrollierten `latest`-Tags verwendet werden.

Die Image-Versionen müssen mit den Deployment-Manifesten übereinstimmen.

## 6. Datenbank-Persistenz auf AWS

### 6.1 Aktuelle lokale Lösung

Die lokale Umgebung verwendet:

- Employee MySQL
- Auth MySQL
- Jeweils ein StatefulSet
- Jeweils einen PersistentVolumeClaim
- Jeweils 2Gi Speicher
- StorageClass `standard`

Die lokale StorageClass verwendet den Provisioner:

```text
rancher.io/local-path
```

Diese StorageClass ist keine geeignete Speicherlösung für AWS EKS.

### 6.2 Geplante AWS-Lösung

Für persistente Datenbankvolumes auf AWS kann Amazon EBS verwendet werden.

Dazu muss eine geeignete EBS CSI Driver-Integration und StorageClass eingerichtet werden.

Die endgültige StorageClass muss auf die gewählte EBS-Konfiguration abgestimmt werden.

Mögliche Parameter sind:

- EBS-Volume-Typ
- Verfügbarkeitszone
- Volume-Größe
- IOPS und Durchsatz
- Verschlüsselung
- Volume-Reclaim-Policy

### 6.3 Beispiel für eine spätere StorageClass

Die folgende YAML-Datei ist ein Muster und noch keine verwendbare Produktionskonfiguration.

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: aws-ebs-placeholder
provisioner: ebs.csi.aws.com
parameters:
  type: <ebs-volume-type>
  encrypted: "true"
volumeBindingMode: WaitForFirstConsumer
reclaimPolicy: Retain
allowVolumeExpansion: true
```

Die Platzhalter müssen anhand der tatsächlichen AWS-Anforderungen ausgefüllt werden.

Die Reclaim Policy `Retain` ist als Schutzmaßnahme für Datenbankvolumes vorgesehen. Sie ersetzt jedoch kein Backup.

### 6.4 Datenmigration

Die beiden bestehenden SQL-Dumps bilden die Grundlage für eine spätere Datenübernahme:

```text
backup/
├── people_hub.sql
└── people_hub_auth.sql
```

Die Datenbanken heißen:

```text
people_hub
people_hub_auth
```

Vor einer AWS-Migration müssen folgende Punkte geklärt werden:

- Welcher Datenbestand ist die verbindliche Quelle?
- Wie werden Schreibzugriffe während der Migration behandelt?
- Wann wird das finale Backup erstellt?
- Wie wird die Datenintegrität überprüft?
- Wie erfolgt die Rückfallstrategie?
- Wie wird ein Datenverlust während der Umschaltung verhindert?

Die vorhandenen SQL-Dumps sind eine Grundlage, aber keine automatisch aktuelle Sicherung der laufenden Datenbanken.

Ein erneuter Import darf erst nach einer geplanten Migration und einer Prüfung des Zielbestands erfolgen.

### 6.5 Hochverfügbarkeit

Die aktuelle lokale Datenbankarchitektur verwendet jeweils eine MySQL-Instanz.

Eine einfache Erhöhung der StatefulSet-Replikate ist keine ausreichende Hochverfügbarkeitsstrategie für MySQL.

Für einen produktiven AWS-Betrieb müssen folgende Optionen bewertet werden:

- MySQL auf EKS mit einem geeigneten Replikations- und Backup-Konzept
- Ein verwalteter Datenbankdienst wie Amazon RDS
- Eine geeignete Multi-AZ-Architektur

Die Entscheidung hängt unter anderem von Wartungsaufwand, Verfügbarkeit, Kosten und Datenbankanforderungen ab.

## 7. Netzwerk und Zugriff

### 7.1 Lokale Architektur

Die lokale Umgebung verwendet einen Kubernetes Service für das Frontend.

Der Zugriff erfolgt aktuell über Port-Forwarding:

```powershell
kubectl port-forward -n employee-frontend service/employee-frontend 8080:80
```

### 7.2 Geplanter AWS-Zugriff

Für AWS EKS ist ein externer Zugriff über einen geeigneten Load Balancer vorgesehen.

Mögliche Architektur:

```text
Internet
   |
   v
AWS Load Balancer
   |
   v
employee-frontend Service
   |
   v
Nginx / Angular
```

Die konkrete Umsetzung muss festlegen:

- Load-Balancer-Typ
- Ingress- oder Gateway-Konfiguration
- DNS-Domain
- TLS-Zertifikat
- Erlaubte eingehende Ports
- Netzwerkzugriff auf die Worker Nodes

Die lokale Port-Forwarding-Lösung wird dadurch nicht automatisch zu einer produktiven externen Zugriffslösung.

### 7.3 NetworkPolicies

Die bestehenden NetworkPolicies bilden eine Grundlage für die Netzwerksegmentierung.

Vor dem AWS-Einsatz muss überprüft werden, ob der gewählte EKS-Netzwerkstack die benötigten NetworkPolicy-Funktionen unterstützt.

Die Policies müssen außerdem unter realen Bedingungen funktional getestet werden.

Die aktuelle lokale Implementierung enthält noch keinen gezielten Negativtest für blockierte Verbindungen.

### 7.4 DNS und TLS

Für den produktiven Zugriff sollten eine geeignete DNS-Domain und TLS-Zertifikate vorgesehen werden.

Die konkrete Zertifikatsverwaltung muss vor der Bereitstellung festgelegt werden.

## 8. Secrets und Berechtigungen

### 8.1 Aktuelle Secrets

Die lokale Umgebung verwendet folgende Kubernetes Secrets:

| Secret | Namespace |
|---|---|
| `employee-mysql-credentials` | `employee-database` |
| `auth-mysql-credentials` | `employee-database` |
| `employee-service-secrets` | `employee-backend` |
| `auth-service-secrets` | `employee-backend` |

### 8.2 AWS-Anforderungen

Für AWS müssen insbesondere folgende Punkte geplant werden:

- Zugriff auf AWS-Ressourcen über geeignete IAM-Rollen
- Least-Privilege-Prinzip
- Verschlüsselte Speicherung vertraulicher Daten
- Sicherer Zugriff auf externe Secret-Management-Systeme
- Rotation von Zugangsdaten
- Beschränkung des Kubernetes-Secret-Zugriffs über RBAC

Die bestehenden lokalen Secret-Werte dürfen nicht unkontrolliert in eine AWS-Produktionsumgebung übernommen werden.

Für die spätere Umgebung sollten neue, geeignete Zugangsdaten erstellt werden.

## 9. Ressourcenplanung

Die lokalen Deployments verwenden folgende Resource Requests und Limits:

| Komponente | CPU Request | RAM Request | CPU Limit | RAM Limit |
|---|---:|---:|---:|---:|
| Employee Service | 250m | 512Mi | 1 | 1Gi |
| Auth Service | 250m | 512Mi | 1 | 1Gi |
| Employee MySQL | 250m | 512Mi | 1 | 1Gi |
| Auth MySQL | 250m | 512Mi | 1 | 1Gi |
| Frontend | 100m | 128Mi | 500m | 256Mi |

Diese Werte sind Ausgangswerte für die spätere Planung.

Sie sind keine bestätigten Produktionswerte.

Vor einer produktiven Bereitstellung müssen unter realistischen Lastbedingungen folgende Aspekte untersucht werden:

- CPU-Auslastung
- RAM-Auslastung
- Datenbanklast
- Anzahl gleichzeitiger Benutzer
- API-Antwortzeiten
- Startverhalten der Anwendungen
- Skalierungsbedarf

## 10. Monitoring und Betrieb

Der lokale Metrics Server stellt grundlegende CPU- und RAM-Metriken bereit.

In der lokalen Umgebung wurde dafür eine Docker-Desktop-spezifische TLS-Ausnahme verwendet:

```text
--kubelet-insecure-tls
```

Diese Einstellung darf nicht unverändert in AWS EKS übernommen werden.

Für AWS müssen die regulären und vertrauenswürdigen TLS-Verbindungen zu den Kubelets verwendet werden.

Für die spätere Betriebsumgebung sollten außerdem folgende Funktionen vorgesehen werden:

- CloudWatch-Metriken
- Zentralisierte Logs
- Kubernetes- und Anwendungsmonitoring
- Alarmierung
- Datenbankmonitoring
- Überwachung der Load-Balancer-Zustände
- Überwachung der persistenten Volumes

## 11. Vorgesehene Deployment-Reihenfolge

Die folgende Reihenfolge ist ein Vorschlag für die spätere Umsetzung.

Es handelt sich nicht um bereits ausgeführte AWS-Schritte.

### Phase 1: AWS-Grundlage

- AWS-Region festlegen
- AWS-Account und Berechtigungen prüfen
- VPC und Subnetze planen
- EKS-Cluster konfigurieren
- Worker-Node-Konzept festlegen

### Phase 2: Infrastrukturkomponenten

- EBS CSI Driver einrichten
- StorageClass konfigurieren
- Netzwerk- und Sicherheitskonfiguration prüfen
- Load-Balancer-Integration vorbereiten
- Monitoring und Logging vorbereiten

### Phase 3: Container-Images

- Images bauen
- Images in ECR bereitstellen
- Image-Tags festlegen
- Zugriff der Worker Nodes auf ECR prüfen

### Phase 4: Kubernetes-Konfiguration

- Namespaces bereitstellen
- Secrets sicher bereitstellen
- ConfigMaps überprüfen
- Datenbank-Persistenz konfigurieren
- NetworkPolicies überprüfen

### Phase 5: Datenbanken

- Datenbankstrategie festlegen
- Backup- und Wiederherstellungsprozess planen
- Ziel-Datenbanken bereitstellen
- Datenmigration durchführen
- Datenintegrität prüfen

### Phase 6: Backend und Frontend

- Employee Service bereitstellen
- Auth Service bereitstellen
- Health Checks überprüfen
- Frontend bereitstellen
- Reverse Proxy überprüfen

### Phase 7: Externer Zugriff

- Load Balancer konfigurieren
- DNS konfigurieren
- TLS aktivieren
- Externe Erreichbarkeit prüfen

### Phase 8: Abnahme

- Funktionale Tests
- Netzwerk- und Sicherheitstests
- Datenbanktests
- Backup- und Recovery-Test
- Monitoring überprüfen
- Betriebsdokumentation aktualisieren

## 12. AWS-spezifische Platzhalter

Die folgenden Werte müssen vor der späteren Umsetzung festgelegt werden.

| Parameter | Platzhalter | Beschreibung |
|---|---|---|
| AWS Account ID | `<aws-account-id>` | AWS-Konto-ID |
| AWS Region | `<aws-region>` | Gewählte AWS-Region |
| EKS Cluster | `<eks-cluster-name>` | Name des EKS-Clusters |
| VPC | `<vpc-id>` | VPC-ID |
| Private Subnets | `<private-subnet-ids>` | IDs der privaten Subnetze |
| Public Subnets | `<public-subnet-ids>` | IDs der öffentlichen Subnetze |
| ECR Frontend | `<frontend-ecr-uri>` | Frontend-Image-Repository |
| ECR Employee | `<employee-ecr-uri>` | Employee-Service-Repository |
| ECR Auth | `<auth-ecr-uri>` | Auth-Service-Repository |
| DNS Domain | `<application-domain>` | Externe Anwendungsdomain |
| TLS Certificate | `<certificate-arn>` | Zertifikatsreferenz |
| StorageClass | `<aws-storage-class>` | AWS-Speicherklasse |
| Node Instance Type | `<node-instance-type>` | Worker-Node-Instanztyp |
| Node Count | `<node-count>` | Anzahl der Worker Nodes |

Diese Werte sind bewusst offen und müssen anhand der späteren AWS-Planung festgelegt werden.

## 13. Sicherheits- und Betriebscheckliste

Vor einem produktiven AWS-Einsatz müssen mindestens folgende Punkte geklärt sein.

### Infrastruktur

- [ ] AWS-Region festgelegt
- [ ] VPC- und Subnetzkonzept definiert
- [ ] EKS-Cluster eingerichtet
- [ ] Worker Nodes konfiguriert
- [ ] Zugriff auf den Cluster geregelt

### Images

- [ ] Container-Images erstellt
- [ ] ECR-Repositories eingerichtet
- [ ] Images in ECR veröffentlicht
- [ ] Image-Tags festgelegt

### Datenbanken

- [ ] Datenbankstrategie entschieden
- [ ] Persistenz konfiguriert
- [ ] Backup-Konzept erstellt
- [ ] Recovery-Konzept dokumentiert
- [ ] Datenmigration geplant
- [ ] Datenintegrität überprüft

### Sicherheit

- [ ] IAM-Berechtigungen geprüft
- [ ] RBAC eingerichtet
- [ ] Secrets sicher bereitgestellt
- [ ] Netzwerkregeln geprüft
- [ ] TLS konfiguriert
- [ ] Container-Images überprüft

### Betrieb

- [ ] Monitoring eingerichtet
- [ ] Logging eingerichtet
- [ ] Alarmierung eingerichtet
- [ ] Ressourcenwerte geprüft
- [ ] Wiederherstellung getestet
- [ ] Betriebsdokumentation aktualisiert

## 14. Aktueller Status der AWS-Vorbereitung

| Bereich | Status |
|---|---|
| Lokale Kubernetes-Architektur | Implementiert |
| Kustomize-Basis | Implementiert |
| Docker-Desktop-Overlay | Implementiert und lokal validiert |
| AWS-EKS-Overlay | Als Vorlage angelegt |
| AWS-spezifische Ressourcen | Noch nicht implementiert |
| EKS-Cluster | Nicht erstellt |
| VPC und Subnetze | Nicht erstellt |
| ECR-Repositories | Nicht erstellt |
| AWS StorageClass | Nicht eingerichtet |
| AWS Load Balancer | Nicht eingerichtet |
| AWS Secrets-Integration | Nicht eingerichtet |
| AWS Monitoring | Nicht eingerichtet |
| AWS Deployment | Nicht durchgeführt |
| AWS-Tests | Nicht durchgeführt |

## 15. Zusammenfassung

Die bestehende Kubernetes-Architektur des Employee-Management-Systems bildet eine Grundlage für eine spätere Bereitstellung auf Amazon EKS.

Die gemeinsame Kustomize-Basis ermöglicht die Wiederverwendung der Anwendungsressourcen.

Das AWS-EKS-Overlay ist derzeit lediglich eine Vorlage und enthält noch keine AWS-spezifischen Anpassungen.

Vor einer tatsächlichen Bereitstellung müssen insbesondere die Infrastruktur, Container-Registry, Persistenz, Secret-Verwaltung, Netzwerkzugriffe, TLS und das Monitoring geplant und eingerichtet werden.

Die lokale Docker-Desktop-Umgebung bleibt bis zu einer bewusst geplanten Migration bestehen.

Es wurden keine AWS-Ressourcen erstellt und keine AWS-Deployments oder AWS-Tests durchgeführt.