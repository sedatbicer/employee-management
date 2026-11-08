# Local Kubernetes Environment – Implementation Guide

## 1. Zweck

Dieses Dokument beschreibt die Implementierung des Employee-Management-Systems auf einer lokalen Kubernetes-Umgebung mit Docker Desktop.

Es dokumentiert die technischen Voraussetzungen, die Kubernetes-Struktur, die Container-Images, die Deployments und die wichtigsten Konfigurationsentscheidungen.

Die Dokumentation orientiert sich am tatsächlich erreichten Implementierungsstand.

Die lokale Umgebung dient der Entwicklung, Integration und technischen Überprüfung der Kubernetes-Konfiguration.

Eine produktive Bereitstellung auf AWS EKS ist nicht Bestandteil dieser Implementierung.

## 2. Technische Voraussetzungen

Die lokale Umgebung wurde auf einem Windows-System mit Docker Desktop und WSL2 eingerichtet.

### 2.1 Verwendete Komponenten

| Komponente | Version beziehungsweise Konfiguration |
|---|---|
| Betriebssystem | Windows |
| Docker Desktop | 4.69.0.224084 |
| Docker Engine | 29.4.0 |
| Kubernetes | 1.36.1 |
| Kubernetes-Kontext | `docker-desktop` |
| WSL2 | Aktiviert |
| Java | 21 |
| MySQL | 8.4 |
| Kubernetes CLI | `kubectl` |

Die Versionsangaben beziehen sich auf den dokumentierten Stand der lokalen Implementierung.

### 2.2 WSL2-Konfiguration

Die lokale WSL2-Umgebung verwendet folgende Ressourcenbegrenzung:

```ini
[wsl2]
memory=10GB
processors=6
swap=4GB
```

Damit stehen der WSL2-Umgebung maximal 10 GB Arbeitsspeicher, sechs Prozessoren und 4 GB Swap zur Verfügung.

Die tatsächliche Verfügbarkeit hängt zusätzlich von den Ressourcen des Windows-Hosts ab.

## 3. Kubernetes-Cluster

Docker Desktop stellt die lokale Kubernetes-Umgebung bereit.

Der aktive Kontext lautet:

```text
docker-desktop
```

Der Cluster verwendet einen einzelnen Node.

Der Node trägt den Namen:

```text
desktop-control-plane
```

Die lokale Umgebung ist nicht als hochverfügbare Kubernetes-Architektur ausgelegt.

Sie dient ausschließlich der lokalen Entwicklung und Überprüfung.

### 3.1 Cluster überprüfen

Der Clusterstatus kann mit folgendem Befehl kontrolliert werden:

```powershell
kubectl cluster-info
```

Die Nodes werden mit folgendem Befehl angezeigt:

```powershell
kubectl get nodes
```

Der erwartete Node-Status ist:

```text
Ready
```

## 4. Kubernetes-Verzeichnisstruktur

Die Kubernetes-Manifeste sind nach Anwendungskomponenten und Umgebungen organisiert.

```text
k8s/
│
├── base/
│   ├── kustomization.yaml
│   │
│   ├── employee-frontend/
│   │   ├── configmap.yaml
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── network-policy.yaml
│   │   └── kustomization.yaml
│   │
│   ├── employee-backend/
│   │   ├── auth-service/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   └── kustomization.yaml
│   │   │
│   │   ├── employee-service/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   └── kustomization.yaml
│   │   │
│   │   ├── configmap.yaml
│   │   ├── network-policy.yaml
│   │   └── kustomization.yaml
│   │
│   └── database/
│       ├── auth-mysql/
│       │   ├── pvc.yaml
│       │   ├── service.yaml
│       │   ├── statefulset.yaml
│       │   └── kustomization.yaml
│       │
│       ├── employee-mysql/
│       │   ├── pvc.yaml
│       │   ├── service.yaml
│       │   ├── statefulset.yaml
│       │   └── kustomization.yaml
│       │
│       ├── network-policy.yaml
│       └── kustomization.yaml
│
└── overlays/
    ├── docker-desktop/
    │   └── kustomization.yaml
    │
    └── aws-eks/
        └── kustomization.yaml
```

Die gemeinsame Basis enthält die allgemeinen Kubernetes-Ressourcen.

Die Umgebungs-Overlays ermöglichen eine spätere Trennung der lokalen und cloudbasierten Konfiguration.

## 5. Namespace-Struktur

Die Anwendung verwendet drei Namespaces.

| Namespace | Komponenten |
|---|---|
| `employee-frontend` | Angular und Nginx |
| `employee-backend` | Employee Service und Auth Service |
| `employee-database` | Employee MySQL und Auth MySQL |

Die Trennung dient der logischen Organisation der Ressourcen und bildet die Grundlage für differenzierte Netzwerk- und Zugriffskontrollen.

Die Namespaces verwenden einheitliche Labels.

Beispiel:

```yaml
metadata:
  labels:
    app.kubernetes.io/part-of: employee-management
    app.kubernetes.io/component: backend
```

Die konkreten Komponentenlabels sind:

- `frontend`
- `backend`
- `database`

## 6. Frontend-Implementierung

### 6.1 Docker-Image

Das Angular-Frontend wird als Docker-Image gebaut und über Nginx ausgeliefert.

Verwendetes Image:

```text
employee-frontend:1.0.0
```

Build-Befehl:

```powershell
docker build -t employee-frontend:1.0.0 .\employee-frontend
```

### 6.2 Kubernetes Deployment

Das Frontend wird über ein Deployment im Namespace `employee-frontend` betrieben.

Die wesentlichen Eigenschaften sind:

| Eigenschaft | Wert |
|---|---|
| Deployment | `employee-frontend` |
| Image | `employee-frontend:1.0.0` |
| Container-Port | 80 |
| Service-Port | 80 |
| Image Pull Policy | `IfNotPresent` |
| CPU Request | 100m |
| RAM Request | 128Mi |
| CPU Limit | 500m |
| RAM Limit | 256Mi |

### 6.3 Kubernetes Service

Der Service heißt:

```text
employee-frontend
```

Er stellt die Frontend-Anwendung innerhalb des Clusters über TCP-Port 80 bereit.

### 6.4 Nginx-Konfiguration

Die Nginx-Konfiguration wird über die ConfigMap bereitgestellt:

```text
employee-frontend-nginx-config
```

Die Konfiguration leitet API-Anfragen an die Backend-Services weiter.

Die Angular-Anwendung wird über den Nginx-Webserver ausgeliefert.

Die ConfigMap wird unter folgendem Pfad eingebunden:

```text
/etc/nginx/conf.d/default.conf
```

Da der Mount `subPath` verwendet, ist nach Änderungen an der ConfigMap ein Neustart des Frontend-Deployments erforderlich.

## 7. Backend-Implementierung

Das Backend besteht aus zwei eigenständigen Spring-Boot-Services.

Beide Services verwenden Java 21 und werden als separate Container betrieben.

### 7.1 Employee Service

Docker-Image:

```text
employee-service:1.0.4
```

Build-Befehl:

```powershell
docker build -t employee-service:1.0.4 .\employee-service
```

Deployment:

```text
employee-service
```

Namespace:

```text
employee-backend
```

Container-Port:

```text
8081
```

Kubernetes Service:

```text
employee-service
```

Interne Service-Adresse:

```text
employee-service.employee-backend.svc.cluster.local
```

Ressourcen:

| Ressource | Wert |
|---|---:|
| CPU Request | 250m |
| RAM Request | 512Mi |
| CPU Limit | 1 |
| RAM Limit | 1Gi |

Die Datenbankverbindung wird über Umgebungsvariablen konfiguriert.

Datenbank-URL:

```text
jdbc:mysql://employee-mysql.employee-database.svc.cluster.local:3306/people_hub
```

Die Zugangsdaten und das JWT-Secret werden über das Kubernetes Secret `employee-service-secrets` bereitgestellt.

### 7.2 Auth Service

Docker-Image:

```text
auth-service:1.0.5
```

Build-Befehl:

```powershell
docker build -t auth-service:1.0.5 .\auth-service
```

Deployment:

```text
auth-service
```

Namespace:

```text
employee-backend
```

Container-Port:

```text
8082
```

Kubernetes Service:

```text
auth-service
```

Interne Service-Adresse:

```text
auth-service.employee-backend.svc.cluster.local
```

Ressourcen:

| Ressource | Wert |
|---|---:|
| CPU Request | 250m |
| RAM Request | 512Mi |
| CPU Limit | 1 |
| RAM Limit | 1Gi |

Datenbank-URL:

```text
jdbc:mysql://auth-mysql.employee-database.svc.cluster.local:3306/people_hub_auth
```

Die Zugangsdaten, das JWT-Secret sowie die Administrator-Konfiguration werden über das Kubernetes Secret `auth-service-secrets` bereitgestellt.

## 8. Datenbank-Implementierung

Die Datenbankebene besteht aus zwei MySQL-Instanzen.

Beide Datenbanken werden über Kubernetes StatefulSets verwaltet.

### 8.1 Employee MySQL

| Eigenschaft | Wert |
|---|---|
| StatefulSet | `employee-mysql` |
| Service | `employee-mysql` |
| Namespace | `employee-database` |
| Image | MySQL 8.4 |
| Datenbank | `people_hub` |
| Port | 3306 |
| PVC | `employee-mysql-data` |
| Speichergröße | 2Gi |
| Replicas | 1 |

### 8.2 Auth MySQL

| Eigenschaft | Wert |
|---|---|
| StatefulSet | `auth-mysql` |
| Service | `auth-mysql` |
| Namespace | `employee-database` |
| Image | MySQL 8.4 |
| Datenbank | `people_hub_auth` |
| Port | 3306 |
| PVC | `auth-mysql-data` |
| Speichergröße | 2Gi |
| Replicas | 1 |

### 8.3 Persistent Storage

Für beide Datenbanken werden PersistentVolumeClaims mit der StorageClass `standard` verwendet.

Die Claims sind auf jeweils 2 GiB und den Access Mode `ReadWriteOnce` konfiguriert.

Die lokale StorageClass verwendet die Reclaim Policy `Delete`.

Deshalb müssen Löschoperationen an PVCs besonders sorgfältig behandelt werden.

Die Docker-Compose-Volumes der bisherigen lokalen Datenbankumgebung bleiben von den Kubernetes-PVCs getrennt.

## 9. Übernahme der bestehenden Daten

Die vorhandenen Datenbanken aus der Docker-Compose-Umgebung wurden über SQL-Dumps in die Kubernetes-Datenbanken übernommen.

Die Backup-Dateien befinden sich im Projektverzeichnis:

```text
backup/
├── people_hub.sql
└── people_hub_auth.sql
```

Die Daten wurden einmalig importiert und anschließend überprüft.

Ein erneuter Import ist für den normalen Betrieb nicht vorgesehen.

Die bestehenden Docker-Compose-Datenbanken und ihre Volumes bleiben unverändert bestehen.

Details zur Migration und zur Datensicherung sind in der separaten Datenbankdokumentation beschrieben.

## 10. Kubernetes Secrets und ConfigMaps

Sensible Konfigurationswerte werden über Kubernetes Secrets bereitgestellt.

Die Backend-Secrets befinden sich im Namespace `employee-backend`.

Die Datenbank-Secrets befinden sich im Namespace `employee-database`.

Zu den vertraulichen Werten gehören:

- Datenbank-Benutzernamen
- Datenbank-Passwörter
- MySQL-Root-Passwörter
- JWT-Secret
- Administrator-E-Mail
- Administrator-Passwort

Nicht vertrauliche Konfigurationen werden über ConfigMaps bereitgestellt.

Die Nginx-Konfiguration des Frontends wird beispielsweise über eine ConfigMap eingebunden.

Die konkreten Zugangsdaten werden nicht in Git oder in dieser Dokumentation hinterlegt.

## 11. Health Checks

Beide Spring-Boot-Anwendungen verwenden Spring Boot Actuator.

Die Kubernetes-Deployments enthalten:

- Startup Probe
- Liveness Probe
- Readiness Probe

Die Prüfungen verwenden die Endpunkte:

```text
/actuator/health/liveness
/actuator/health/readiness
```

Die Endpunkte sind in den Spring-Security-Konfigurationen explizit freigegeben.

Die Health Checks wurden für beide Services über Port-Forwarding erfolgreich geprüft.

## 12. Ressourcenverwaltung

Die Anwendungskomponenten besitzen definierte Resource Requests und Limits.

| Komponente | CPU Request | RAM Request | CPU Limit | RAM Limit |
|---|---:|---:|---:|---:|
| Frontend | 100m | 128Mi | 500m | 256Mi |
| Employee Service | 250m | 512Mi | 1 CPU | 1Gi |
| Auth Service | 250m | 512Mi | 1 CPU | 1Gi |
| Employee MySQL | 250m | 512Mi | 1 CPU | 1Gi |
| Auth MySQL | 250m | 512Mi | 1 CPU | 1Gi |

Die Ressourcenanforderungen und Limits werden direkt in den Kubernetes-Workload-Manifests definiert.

Die tatsächliche Auslastung wird über den Metrics Server ermittelt.

## 13. Metrics Server

Der Metrics Server wurde in der lokalen Kubernetes-Umgebung installiert.

Installationsbefehl:

```powershell
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

Die installierte Version ist:

```text
v0.9.1
```

Aufgrund eines lokalen Zertifikatsproblems mit dem Docker-Desktop-Kubelet wurde folgende Option gesetzt:

```text
--kubelet-insecure-tls
```

Damit konnte die Metrikerfassung erfolgreich aktiviert werden.

Diese Konfiguration ist eine lokale Entwicklungs-Ausnahme und darf nicht ungeprüft für AWS EKS übernommen werden.

Die Messwerte können mit folgenden Befehlen abgerufen werden:

```powershell
kubectl top nodes
```

```powershell
kubectl top pods -A
```

Der Metrics Server stellt aktuelle Ressourcenmesswerte bereit und ist keine vollständige Monitoring-Plattform.

## 14. NetworkPolicies

Für die drei Anwendungsschichten wurden NetworkPolicies erstellt.

Die Policies beschränken den eingehenden Netzwerkverkehr.

Folgende Ressourcen wurden eingerichtet:

| Policy | Namespace | Zweck |
|---|---|---|
| `employee-frontend-ingress` | `employee-frontend` | Eingehender Frontend-Verkehr |
| `backend-allow-frontend` | `employee-backend` | Zugriff vom Frontend auf die Backend-Services |
| `employee-mysql-allow-employee-service` | `employee-database` | Zugriff des Employee Service auf Employee MySQL |
| `auth-mysql-allow-auth-service` | `employee-database` | Zugriff des Auth Service auf Auth MySQL |

Die Anwendung wurde anschließend über den vorgesehenen Frontend-Zugriffsweg verwendet.

Separate Negativtests mit nicht erlaubten Netzwerkverbindungen wurden nicht durchgeführt.

Die tatsächliche Durchsetzung der Regeln ist daher noch nicht vollständig funktional nachgewiesen.

## 15. Kustomize

Die Kubernetes-Ressourcen werden mit Kustomize verwaltet.

Die Basis befindet sich unter:

```text
k8s/base/
```

Das lokale Overlay befindet sich unter:

```text
k8s/overlays/docker-desktop/
```

Das vorbereitete AWS-Overlay befindet sich unter:

```text
k8s/overlays/aws-eks/
```

### 15.1 Gesamte Basis

```powershell
kubectl kustomize .\k8s\base
```

### 15.2 Docker-Desktop-Overlay

```powershell
kubectl kustomize .\k8s\overlays\docker-desktop
```

### 15.3 Lokale Bereitstellung

```powershell
kubectl apply -k .\k8s\overlays\docker-desktop
```

Die gemeinsame Basis wurde einschließlich ihrer Ressourcen erfolgreich durch Kustomize verarbeitet.

Die Server-seitige Validierung der Basis und des Docker-Desktop-Overlays wurde ebenfalls erfolgreich durchgeführt.

Die Validierung hat keine neue Bereitstellung ausgelöst.

## 16. Lokaler Zugriff

Die Anwendung wird lokal über Port-Forwarding bereitgestellt.

Befehl:

```powershell
kubectl port-forward -n employee-frontend service/employee-frontend 8080:80
```

Browser-Adresse:

```text
http://localhost:8080/
```

Das Port-Forwarding-Fenster muss während der Verwendung geöffnet bleiben.

Die Anwendung wurde über diesen Zugriffsweg erfolgreich geöffnet und verwendet.

## 17. Aktueller Implementierungsstatus

| Bereich | Implementierungsstand |
|---|---|
| Docker-Desktop-Kubernetes | Erfolgreich eingerichtet |
| Drei Namespaces | Implementiert |
| Frontend | Implementiert und erreichbar |
| Employee Service | Implementiert |
| Auth Service | Implementiert |
| Employee MySQL | Implementiert |
| Auth MySQL | Implementiert |
| Datenmigration | Durchgeführt |
| Persistent Storage | Implementiert |
| Kubernetes Secrets | Erstellt und eingebunden |
| ConfigMaps | Implementiert |
| NetworkPolicies | Erstellt und angewendet |
| Health Checks | Erfolgreich geprüft |
| Resource Requests und Limits | Implementiert |
| Metrics Server | Installiert und funktionsfähig |
| Kustomize Base | Erfolgreich verarbeitet |
| Docker-Desktop-Overlay | Erfolgreich verarbeitet |
| AWS-EKS-Overlay | Grundgerüst vorhanden |
| AWS-EKS-Bereitstellung | Noch nicht durchgeführt |

## 18. Bekannte Einschränkungen

Die lokale Implementierung besitzt folgende Grenzen:

- Ein einzelner Kubernetes-Node
- Keine hochverfügbare Datenbankarchitektur
- Lokale Container-Images statt einer zentralen Registry
- Port-Forwarding statt produktivem Ingress
- Lokale TLS-Ausnahme beim Metrics Server
- Keine vollständigen Negativtests der NetworkPolicies
- Noch keine produktive AWS-EKS-Konfiguration

Diese Einschränkungen müssen bei einer späteren Übertragung auf eine Cloud- oder Produktionsumgebung berücksichtigt werden.

## 19. Ergebnis

Das Employee-Management-System wurde lokal auf Docker Desktop mit Kubernetes implementiert.

Die Anwendung ist in Frontend, Backend und Datenbanken getrennt.

Die beiden Spring-Boot-Services kommunizieren über Kubernetes Services mit ihren jeweiligen MySQL-Datenbanken.

Die Datenbankdaten werden persistent gespeichert.

Die Kubernetes-Ressourcen werden über Kustomize organisiert.

Health Checks, Ressourcenlimits, Kubernetes Secrets und NetworkPolicies sind eingerichtet.

Die lokale Anwendung ist über den Frontend-Service erreichbar und wurde erfolgreich verwendet.

Die Implementierung bildet die technische Grundlage für die spätere Vorbereitung einer AWS-EKS-Umgebung.