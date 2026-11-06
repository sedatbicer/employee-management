# System Architecture – Employee Management

## 1. Zweck und Geltungsbereich

Dieses Dokument beschreibt die technische Architektur des Employee-Management-Systems innerhalb der lokalen Kubernetes-Umgebung.

Es erläutert die einzelnen Anwendungskomponenten, deren Verantwortlichkeiten sowie die Kommunikation zwischen Frontend, Backend und Datenbanken.

Die Architektur basiert auf folgenden Technologien:

| Bereich | Technologie |
|---|---|
| Frontend | Angular |
| Webserver und Reverse Proxy | Nginx |
| Backend | Java 21 und Spring Boot |
| Authentifizierung | Spring Security und JWT |
| Datenbanken | MySQL 8.4 |
| Containerisierung | Docker |
| Container-Orchestrierung | Kubernetes |
| Konfigurationsverwaltung | Kubernetes ConfigMaps und Secrets |
| Deployment-Verwaltung | Kustomize |
| Lokale Kubernetes-Umgebung | Docker Desktop |

Die Architektur ist für den lokalen Betrieb ausgelegt und bildet die Grundlage für eine spätere Bereitstellung auf AWS EKS.

## 2. Gesamtarchitektur

Das System besteht aus fünf zentralen Anwendungskomponenten.

1. Angular-Frontend mit integriertem Nginx
2. Employee Service
3. Auth Service
4. Employee MySQL Database
5. Auth MySQL Database

Die Komponenten werden in drei Kubernetes-Namespaces betrieben.

```text
                           Benutzer
                              |
                              v
                       +-------------+
                       |   Browser   |
                       +-------------+
                              |
                              | HTTP
                              v
                 +-------------------------+
                 |    employee-frontend    |
                 |                         |
                 |      Angular            |
                 |      Nginx              |
                 +-------------------------+
                              |
                 +------------+------------+
                 |                         |
                 | /api/employees          | /api/auth
                 v                         v
        +------------------+     +------------------+
        | Employee Service |     |   Auth Service   |
        |                  |     |                  |
        | Spring Boot      |     | Spring Boot      |
        | Port 8081        |     | Port 8082        |
        +------------------+     +------------------+
                 |                         |
                 | TCP 3306                | TCP 3306
                 v                         v
        +------------------+     +------------------+
        | Employee MySQL   |     | Auth MySQL       |
        |                  |     |                  |
        | people_hub       |     | people_hub_auth  |
        +------------------+     +------------------+
```

Die drei Namespace-Ebenen sind:

| Namespace | Inhalt |
|---|---|
| `employee-frontend` | Angular und Nginx |
| `employee-backend` | Employee Service und Auth Service |
| `employee-database` | Beide MySQL-Datenbanken |

## 3. Frontend-Architektur

### 3.1 Angular

Das Frontend stellt die Benutzeroberfläche der Anwendung bereit.

Die Angular-Anwendung wird gebaut und anschließend innerhalb eines Nginx-Containers ausgeliefert.

Der Browser lädt die statischen Frontend-Dateien über HTTP.

### 3.2 Nginx

Nginx übernimmt zwei Aufgaben:

- Auslieferung der Angular-Anwendung
- Weiterleitung der API-Anfragen an die Backend-Services

Die Nginx-Konfiguration wird über eine Kubernetes ConfigMap bereitgestellt.

Die verwendete ConfigMap heißt:

```text
employee-frontend-nginx-config
```

Sie befindet sich im Namespace:

```text
employee-frontend
```

Die Konfiguration wird innerhalb des Containers unter folgendem Pfad eingebunden:

```text
/etc/nginx/conf.d/default.conf
```

### 3.3 Reverse-Proxy-Konfiguration

Die wesentlichen Weiterleitungsregeln lauten:

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location /api/employees {
        proxy_pass http://employee-service.employee-backend.svc.cluster.local:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /api/auth {
        proxy_pass http://auth-service.employee-backend.svc.cluster.local:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

Die Regeln ermöglichen eine Kommunikation zwischen dem Frontend und den Backend-Services über Kubernetes-interne DNS-Adressen.

### 3.4 Same-Origin-Kommunikation

Der Browser greift ausschließlich auf den Frontend-Endpunkt zu.

Die API-Anfragen werden über relative Pfade angesprochen:

```text
/api/employees
/api/auth
```

Der Browser kommuniziert somit mit derselben Origin.

Nginx übernimmt die Weiterleitung innerhalb des Kubernetes-Clusters.

Dadurch ist für diesen Kommunikationsweg keine browserseitige Cross-Origin-Freigabe erforderlich.

Die CORS-Konfiguration wurde in den Spring-Security-Konfigurationen der beiden Backend-Services deaktiviert:

```java
.cors(cors -> cors.disable())
```

Diese Entscheidung gilt für die aktuelle Same-Origin-Architektur.

Ein direkter Zugriff auf die Backend-Services durch ein separat gestartetes Angular-Development-Frontend, beispielsweise über `ng serve` auf Port 4200, ist damit nicht als unterstützter Browser-Zugriffsweg eingerichtet.

## 4. Backend-Architektur

Das Backend besteht aus zwei unabhängigen Spring-Boot-Anwendungen.

### 4.1 Employee Service

Der Employee Service ist für die fachliche Verwaltung der Mitarbeiterdaten zuständig.

Technische Eigenschaften:

| Eigenschaft | Wert |
|---|---|
| Anwendung | Employee Service |
| Technologie | Spring Boot |
| Programmiersprache | Java 21 |
| Container-Port | 8081 |
| Kubernetes Service | `employee-service` |
| Namespace | `employee-backend` |
| Datenbank | `people_hub` |
| Datenbankserver | Employee MySQL |

Der Service stellt seine API über den Kubernetes-Service bereit.

Interner DNS-Name:

```text
employee-service.employee-backend.svc.cluster.local
```

Datenbankverbindung:

```text
jdbc:mysql://employee-mysql.employee-database.svc.cluster.local:3306/people_hub
```

### 4.2 Auth Service

Der Auth Service übernimmt die Authentifizierungsfunktionen der Anwendung.

Technische Eigenschaften:

| Eigenschaft | Wert |
|---|---|
| Anwendung | Auth Service |
| Technologie | Spring Boot |
| Programmiersprache | Java 21 |
| Container-Port | 8082 |
| Kubernetes Service | `auth-service` |
| Namespace | `employee-backend` |
| Datenbank | `people_hub_auth` |
| Datenbankserver | Auth MySQL |

Interner DNS-Name:

```text
auth-service.employee-backend.svc.cluster.local
```

Datenbankverbindung:

```text
jdbc:mysql://auth-mysql.employee-database.svc.cluster.local:3306/people_hub_auth
```

Die JWT-Konfiguration wird über Kubernetes Secrets bereitgestellt.

Die konfigurierte Gültigkeitsdauer der JWT-Tokens beträgt:

```text
3600000 Millisekunden
```

Dies entspricht einer Stunde.

### 4.3 Containerisierung der Backend-Services

Beide Backend-Anwendungen verwenden mehrstufige Docker-Builds.

Die Build-Phase verwendet Maven mit Java 21.

Die Runtime-Phase verwendet ein Java-21-JRE-Image.

Die Anwendungen werden unter einem dedizierten Benutzer mit der UID 1001 ausgeführt.

Dadurch wird vermieden, dass die Anwendung standardmäßig als Root-Benutzer ausgeführt wird.

Die Images werden lokal mit folgenden Tags verwendet:

| Anwendung | Image |
|---|---|
| Employee Service | `employee-service:1.0.4` |
| Auth Service | `auth-service:1.0.5` |

Die Images enthalten die Spring-Boot-Anwendungen einschließlich der für Kubernetes erforderlichen Health-Endpunkte.

## 5. Datenbankarchitektur

Das System verwendet zwei voneinander getrennte MySQL-Datenbanken.

### 5.1 Employee MySQL

Die Employee-Datenbank speichert die Daten des Employee Service.

| Eigenschaft | Wert |
|---|---|
| Datenbankserver | MySQL 8.4 |
| StatefulSet | `employee-mysql` |
| Service | `employee-mysql` |
| Namespace | `employee-database` |
| Datenbankname | `people_hub` |
| Port | 3306 |
| PersistentVolumeClaim | `employee-mysql-data` |
| Speichergröße | 2 GiB |

Der Employee Service greift ausschließlich über den internen MySQL-Service auf diese Datenbank zu.

### 5.2 Auth MySQL

Die Auth-Datenbank speichert die Authentifizierungsdaten des Auth Service.

| Eigenschaft | Wert |
|---|---|
| Datenbankserver | MySQL 8.4 |
| StatefulSet | `auth-mysql` |
| Service | `auth-mysql` |
| Namespace | `employee-database` |
| Datenbankname | `people_hub_auth` |
| Port | 3306 |
| PersistentVolumeClaim | `auth-mysql-data` |
| Speichergröße | 2 GiB |

Der Auth Service greift über den internen MySQL-Service auf diese Datenbank zu.

### 5.3 Trennung der Datenbanken

Die beiden Datenbanken werden separat betrieben.

Daraus ergeben sich folgende Vorteile:

- Getrennte Datenhaltung
- Unabhängige Zugangsdaten
- Separate Persistenz
- Unabhängige Wartung
- Klare Zuordnung zwischen Backend-Service und Datenbank

Die beiden MySQL-Instanzen verwenden jeweils ein StatefulSet mit einem Replica.

Die lokale Konfiguration ist nicht als hochverfügbare Datenbankarchitektur ausgelegt.

## 6. Kubernetes Services und DNS

Kubernetes Services stellen stabile Netzwerk-Endpunkte für Pods bereit.

Die Anwendung verwendet interne ClusterIP-Services.

| Service | Namespace | Port |
|---|---|---:|
| `employee-frontend` | `employee-frontend` | 80 |
| `employee-service` | `employee-backend` | 8081 |
| `auth-service` | `employee-backend` | 8082 |
| `employee-mysql` | `employee-database` | 3306 |
| `auth-mysql` | `employee-database` | 3306 |

Die Services können innerhalb des Clusters über Kubernetes DNS angesprochen werden.

Der vollständige DNS-Aufbau lautet:

```text
<SERVICE>.<NAMESPACE>.svc.cluster.local
```

Beispiel:

```text
employee-service.employee-backend.svc.cluster.local
```

Die DNS-Auflösung ermöglicht die Kommunikation zwischen verschiedenen Namespaces, ohne dass feste Pod-IP-Adressen in den Anwendungskonfigurationen verwendet werden müssen.

## 7. Datenfluss einer Benutzeranfrage

### 7.1 Aufruf der Anwendung

Der Benutzer öffnet im Browser die lokale Anwendung.

Der lokale Zugriff erfolgt über:

```text
http://localhost:8080/
```

Die Verbindung wird über Kubernetes Port-Forwarding an den Frontend-Service weitergeleitet.

### 7.2 Auslieferung des Frontends

Der Frontend-Service leitet die Anfrage an einen Frontend-Pod weiter.

Nginx liefert die Angular-Dateien aus.

### 7.3 API-Anfrage

Wenn Angular eine API-Anfrage ausführt, wird ein relativer API-Pfad verwendet.

Beispiel:

```text
/api/employees
```

Nginx erkennt den API-Pfad und leitet die Anfrage an den Employee Service weiter.

### 7.4 Verarbeitung im Backend

Der Employee Service verarbeitet die Anfrage.

Abhängig von der jeweiligen Geschäftsoperation kann der Service Daten aus der Employee MySQL Database lesen oder dort speichern.

### 7.5 Datenbankzugriff

Der Datenbankzugriff erfolgt über den internen Kubernetes-Service.

Die Verbindung verwendet TCP-Port 3306.

### 7.6 Antwort an den Browser

Nach erfolgreicher Verarbeitung gibt der Employee Service eine HTTP-Antwort zurück.

Diese wird über Nginx an den Browser weitergeleitet.

Der Browser verarbeitet anschließend die Antwort innerhalb der Angular-Anwendung.

### 7.7 Authentifizierungsablauf

Bei Authentifizierungsanfragen wird der API-Pfad `/api/auth` verwendet.

Nginx leitet diese Anfragen an den Auth Service weiter.

Der Auth Service verarbeitet die Authentifizierungsanfrage und verwendet seine separate Auth-Datenbank.

Die JWT-Funktionen werden durch den Auth Service bereitgestellt.

## 8. Konfigurationsverwaltung

Die Konfiguration der Anwendung wird über Kubernetes ConfigMaps und Secrets bereitgestellt.

### 8.1 ConfigMaps

ConfigMaps enthalten nicht vertrauliche Konfigurationswerte.

Im Projekt werden sie unter anderem für die Nginx-Konfiguration und die gemeinsame Backend-Konfiguration verwendet.

Die Backend-Deployments referenzieren die ConfigMap:

```text
backend-cors-config
```

Diese ConfigMap ist aktuell leer und wird über `envFrom` eingebunden.

Die frühere CORS-Konfiguration wird nicht mehr für die aktuelle Same-Origin-Kommunikation verwendet.

### 8.2 Secrets

Sensible Werte werden über Kubernetes Secrets bereitgestellt.

Dazu gehören:

- Datenbank-Benutzernamen
- Datenbank-Passwörter
- JWT-Secret
- Auth-Administrator-E-Mail
- Auth-Administrator-Passwort

Die Secrets werden über Umgebungsvariablen an die jeweiligen Container übergeben.

Die konkreten Zugangsdaten werden nicht in diesem Dokument aufgeführt.

Kubernetes Secrets sind standardmäßig nicht automatisch verschlüsselt gespeichert. Für produktive Umgebungen ist eine geeignete Secret- und Verschlüsselungsstrategie erforderlich.

## 9. Health Checks und Verfügbarkeit

Die beiden Spring-Boot-Services verwenden Spring Boot Actuator.

Die Kubernetes-Deployments definieren drei Probes:

- Startup Probe
- Liveness Probe
- Readiness Probe

Die Endpunkte sind:

```text
/actuator/health/liveness
/actuator/health/readiness
```

Die Startup Probe berücksichtigt die Startzeit der Spring-Boot-Anwendung.

Die Liveness Probe überprüft, ob die Anwendung weiterhin funktionsfähig ist.

Die Readiness Probe prüft, ob der Pod für die Verarbeitung von Anfragen bereit ist.

Die Health-Endpunkte sind in den Security-Konfigurationen explizit freigegeben.

Die lokale Implementierung wurde durch direkte HTTP-Aufrufe über Port-Forwarding erfolgreich überprüft.

## 10. Ressourcenverwaltung

Für die Anwendungskomponenten sind CPU- und RAM-Requests sowie Limits definiert.

| Komponente | CPU Request | RAM Request | CPU Limit | RAM Limit |
|---|---:|---:|---:|---:|
| Frontend | 100m | 128Mi | 500m | 256Mi |
| Employee Service | 250m | 512Mi | 1 CPU | 1Gi |
| Auth Service | 250m | 512Mi | 1 CPU | 1Gi |
| Employee MySQL | 250m | 512Mi | 1 CPU | 1Gi |
| Auth MySQL | 250m | 512Mi | 1 CPU | 1Gi |

Diese Werte dienen der Ressourcenplanung und Begrenzung des Ressourcenverbrauchs.

Der tatsächliche Verbrauch kann über den Metrics Server abgefragt werden.

Die Werte sind für die aktuelle lokale Umgebung dokumentiert und müssen vor einer produktiven Bereitstellung anhand der tatsächlichen Last überprüft werden.

## 11. Persistente Speicherung

Die Datenbanken verwenden PersistentVolumeClaims.

Für beide Datenbanken ist jeweils ein Speicherplatz von 2 GiB vorgesehen.

Die lokale StorageClass heißt:

```text
standard
```

Die Reclaim Policy dieser StorageClass ist:

```text
Delete
```

Das bedeutet, dass das Löschen eines PVC auch die zugehörigen persistenten Daten gefährden kann.

Die Datenbank-PVCs dürfen daher nicht als temporäre Ressourcen betrachtet oder unüberlegt gelöscht werden.

Die bestehenden Docker-Compose-Daten und deren Volumes sind von den Kubernetes-PersistentVolumes getrennt.

## 12. Netzwerk- und Sicherheitsarchitektur

Die Anwendung verwendet NetworkPolicies zur Einschränkung des eingehenden Netzwerkverkehrs.

Die vorgesehenen Verbindungen sind:

| Quelle | Ziel | Port |
|---|---|---:|
| Frontend | Frontend-Nginx | TCP 80 |
| Frontend | Employee Service | TCP 8081 |
| Frontend | Auth Service | TCP 8082 |
| Employee Service | Employee MySQL | TCP 3306 |
| Auth Service | Auth MySQL | TCP 3306 |

Die NetworkPolicies sind als Ingress-Regeln implementiert.

Der ausgehende Netzwerkverkehr wird durch diese Policies nicht zusätzlich eingeschränkt.

Die Anwendungskommunikation über die vorgesehenen Services wurde lokal überprüft.

Die vollständige funktionale Durchsetzung aller erlaubten und verbotenen Netzwerkverbindungen wurde bisher nicht durch separate Negativtests nachgewiesen.

## 13. Kustomize-Architektur

Die Kubernetes-Konfiguration wird mit Kustomize organisiert.

Die Projektstruktur trennt gemeinsame Ressourcen von umgebungsspezifischen Konfigurationen.

```text
k8s/
├── base/
│   ├── kustomization.yaml
│   ├── employee-frontend/
│   ├── employee-backend/
│   └── database/
│
└── overlays/
    ├── docker-desktop/
    │   └── kustomization.yaml
    │
    └── aws-eks/
        └── kustomization.yaml
```

Die Basis enthält die allgemeinen Anwendungsressourcen.

Das Docker-Desktop-Overlay bindet die Basis für die lokale Umgebung ein.

Das AWS-EKS-Overlay ist aktuell nur ein Grundgerüst. AWS-spezifische Images, Speicher- und Netzwerkressourcen müssen noch ergänzt werden.

## 14. Lokale Betriebsumgebung

Die lokale Kubernetes-Umgebung läuft innerhalb von Docker Desktop.

Der verwendete Kubernetes-Kontext lautet:

```text
docker-desktop
```

Die Umgebung verwendet einen einzelnen Kubernetes-Node.

Sie ist daher für lokale Entwicklung und technische Überprüfungen geeignet, bietet aber keine hochverfügbare Clusterarchitektur.

Der lokale Zugriff auf die Anwendung wird über Port-Forwarding hergestellt:

```powershell
kubectl port-forward -n employee-frontend service/employee-frontend 8080:80
```

Der Browserzugriff erfolgt anschließend über:

```text
http://localhost:8080/
```

## 15. Architekturgrenzen und bekannte Einschränkungen

Folgende Einschränkungen sind für die aktuelle Architektur relevant:

1. Die lokale Kubernetes-Umgebung besteht aus einem einzelnen Node.
2. Die Datenbanken werden jeweils mit einem StatefulSet-Replica betrieben.
3. Die Anwendung verwendet lokale Docker-Desktop-Images.
4. Die externe Erreichbarkeit wird derzeit über Port-Forwarding hergestellt.
5. Eine produktive Ingress- und Load-Balancer-Konfiguration ist noch nicht implementiert.
6. Die NetworkPolicies wurden erstellt und angewendet, aber nicht vollständig funktional negativ getestet.
7. Die Secret-Verwaltung entspricht noch keiner vollständigen produktiven AWS-Sicherheitsarchitektur.
8. Das AWS-EKS-Overlay ist noch nicht für eine Bereitstellung freigegeben.
9. Das lokale Metrics-Server-Setup verwendet eine TLS-Ausnahme für Docker Desktop.

Diese Einschränkungen müssen bei einer späteren Übertragung auf eine produktive Umgebung berücksichtigt werden.

## 16. Zusammenfassung

Die Employee-Management-Anwendung verwendet eine klar getrennte Frontend-, Backend- und Datenbankarchitektur.

Das Angular-Frontend wird über Nginx ausgeliefert. Nginx übernimmt gleichzeitig die Weiterleitung der API-Anfragen an die beiden Spring-Boot-Services.

Jeder Backend-Service verwendet eine eigene MySQL-Datenbank.

Die Kubernetes-Ressourcen werden über drei Namespaces organisiert und mit Kustomize verwaltet.

PersistentVolumeClaims stellen die dauerhafte Speicherung der Datenbankdaten sicher.

Kubernetes Secrets, NetworkPolicies, Health Checks und Resource Limits bilden die Grundlage der lokalen Sicherheits- und Betriebsarchitektur.

Die Architektur ist lokal auf Docker Desktop implementiert und dient als Ausgangspunkt für die spätere AWS-EKS-Bereitstellung.