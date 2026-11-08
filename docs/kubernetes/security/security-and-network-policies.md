# Security and Network Policies

## 1. Zweck und Geltungsbereich

Dieses Dokument beschreibt die Sicherheitsmaßnahmen der lokalen Kubernetes-Umgebung des Employee-Management-Systems.

Es behandelt:

- Die Trennung der Kubernetes-Namespaces
- Die Verwaltung vertraulicher Zugangsdaten
- Kubernetes Secrets
- NetworkPolicies
- Die erlaubten Kommunikationswege zwischen den Komponenten
- Die Absicherung der Backend-Endpunkte
- Die Kommunikation zwischen Angular, Nginx und den Backend-Services
- Sicherheitsaspekte der lokalen Entwicklungsumgebung
- Noch offene Maßnahmen für einen späteren produktiven Betrieb

Die aktuelle Implementierung ist auf eine lokale Entwicklungsumgebung mit Docker Desktop Kubernetes ausgelegt. Sie ist nicht mit einer vollständig gehärteten Produktionsumgebung gleichzusetzen.

## 2. Sicherheitsarchitektur

Die Anwendung besteht aus drei logisch getrennten Kubernetes-Namespaces.

| Namespace | Komponente | Aufgabe |
|---|---|---|
| `employee-frontend` | Frontend | Angular-Anwendung und Nginx |
| `employee-backend` | Backend | Employee Service und Auth Service |
| `employee-database` | Datenbanken | Employee MySQL und Auth MySQL |

Die Trennung reduziert die direkte Vermischung der Komponenten und ermöglicht gezielte Netzwerkregeln.

Die Kommunikation zwischen den Komponenten erfolgt über Kubernetes Services und deren interne DNS-Namen.

### 2.1 Kommunikationsübersicht

```text
Browser
   |
   | HTTP
   v
employee-frontend
   |
   | Nginx Reverse Proxy
   |
   +----------------------------+
   |                            |
   v                            v
Employee Service           Auth Service
   |                            |
   v                            v
Employee MySQL              Auth MySQL

Namespace:                  Namespace:
employee-backend            employee-database
```

Der Browser kommuniziert mit dem Frontend. Nginx leitet die API-Anfragen intern an die jeweiligen Backend-Services weiter.

Die Datenbankverbindungen werden ausschließlich von den zuständigen Backend-Services aufgebaut.

## 3. Namespace-Trennung

Die Namespaces dienen der logischen Isolation der Anwendungsbereiche.

Jeder Namespace besitzt ein eigenes Set an Ressourcen und kann mit eigenen NetworkPolicies und Secrets verwaltet werden.

### 3.1 Verwendete Labels

Die Namespaces verwenden gemeinsame Projektlabels:

```yaml
app.kubernetes.io/part-of: employee-management
```

Das Component-Label unterscheidet die jeweiligen Bereiche:

```yaml
app.kubernetes.io/component: frontend
```

```yaml
app.kubernetes.io/component: backend
```

```yaml
app.kubernetes.io/component: database
```

Diese Labels unterstützen die gezielte Auswahl von Ressourcen durch Kubernetes NetworkPolicies.

### 3.2 Sicherheitsvorteile

Die Namespace-Trennung ermöglicht:

- Getrennte Verwaltung der Komponenten
- Begrenzung von Netzwerkzugriffen
- Getrennte Ablage von Secrets
- Übersichtliche Ressourcenverwaltung
- Eine Grundlage für spätere RBAC-Regeln

Die Namespace-Trennung allein stellt allerdings keine vollständige Sicherheitsgrenze dar. Dafür müssen zusätzlich geeignete Netzwerk- und Berechtigungskonzepte eingesetzt werden.

## 4. Verwaltung von Secrets

### 4.1 Grundprinzip

Vertrauliche Informationen werden nicht direkt in die Deployment-Manifeste geschrieben.

Dazu gehören insbesondere:

- Datenbank-Benutzernamen
- Datenbank-Passwörter
- JWT-Schlüssel
- Zugangsdaten des initialen Administrators
- MySQL-Root-Passwörter

Diese Werte werden über Kubernetes Secrets bereitgestellt.

### 4.2 Datenbank-Secrets

Die MySQL-Instanzen verwenden jeweils ein eigenes Secret im Namespace `employee-database`.

**Employee MySQL:**

```text
employee-mysql-credentials
```

Enthaltene Schlüssel:

```text
MYSQL_ROOT_PASSWORD
MYSQL_USER
MYSQL_PASSWORD
```

**Auth MySQL:**

```text
auth-mysql-credentials
```

Enthaltene Schlüssel:

```text
MYSQL_ROOT_PASSWORD
MYSQL_USER
MYSQL_PASSWORD
```

### 4.3 Backend-Secrets

Die Backend-Anwendungen verwenden separate Secrets im Namespace `employee-backend`.

**Employee Service:**

```text
employee-service-secrets
```

Enthaltene Schlüssel:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

**Auth Service:**

```text
auth-service-secrets
```

Enthaltene Schlüssel:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
AUTH_ADMIN_EMAIL
AUTH_ADMIN_PASSWORD
```

Die Secrets werden den jeweiligen Deployments als Umgebungsvariablen bereitgestellt.

### 4.4 Secrets überprüfen

Die vorhandenen Secrets können ohne Ausgabe ihrer vertraulichen Werte aufgelistet werden:

```powershell
kubectl get secrets -n employee-database
```

```powershell
kubectl get secrets -n employee-backend
```

Die Namen und Typen der Secrets lassen sich anzeigen, ohne die Werte auszugeben:

```powershell
kubectl describe secret employee-service-secrets -n employee-backend
```

```powershell
kubectl describe secret auth-service-secrets -n employee-backend
```

Die tatsächlichen Secret-Werte sollten nicht in Terminalsitzungen, Screenshots, Dokumentationen oder Logs veröffentlicht werden.

### 4.5 Grenzen von Kubernetes Secrets

Kubernetes Secrets sind standardmäßig Base64-kodiert.

Base64 ist keine Verschlüsselung.

Für einen produktiven Betrieb sind zusätzliche Sicherheitsmaßnahmen notwendig:

- Verschlüsselung der Secret-Daten im Cluster
- Begrenzung des Secret-Zugriffs durch RBAC
- Verwendung eines geeigneten Secret-Management-Systems
- Regelmäßige Rotation vertraulicher Zugangsdaten
- Vermeidung von Zugangsdaten in Git-Repositories
- Schutz von Backup-Dateien und lokalen Konfigurationsdateien

## 5. Netzwerkkommunikation

Die interne Kommunikation erfolgt über Kubernetes Services.

Die Anwendungen verwenden folgende DNS-Adressen:

| Quelle | Ziel | Port |
|---|---|---:|
| Frontend / Nginx | Employee Service | 8081 |
| Frontend / Nginx | Auth Service | 8082 |
| Employee Service | Employee MySQL | 3306 |
| Auth Service | Auth MySQL | 3306 |

### 5.1 Frontend zu Employee Service

```text
employee-service.employee-backend.svc.cluster.local:8081
```

### 5.2 Frontend zu Auth Service

```text
auth-service.employee-backend.svc.cluster.local:8082
```

### 5.3 Employee Service zu Employee MySQL

```text
employee-mysql.employee-database.svc.cluster.local:3306
```

### 5.4 Auth Service zu Auth MySQL

```text
auth-mysql.employee-database.svc.cluster.local:3306
```

Die Datenbankverbindungen werden nicht direkt vom Browser aufgebaut.

## 6. NetworkPolicies

### 6.1 Zweck

Kubernetes NetworkPolicies definieren, welche Netzwerkverbindungen für ausgewählte Pods erlaubt sind.

Sie können den Zugriff auf Services und Pods anhand von Namespace-, Pod- und Port-Regeln begrenzen.

In der aktuellen Umgebung werden gezielte Ingress-Regeln verwendet.

Ingress beschreibt eingehenden Netzwerkverkehr zu einem Pod.

Die bestehenden Policies beschränken nicht allgemein den ausgehenden Netzwerkverkehr der ausgewählten Pods.

### 6.2 Frontend NetworkPolicy

Datei:

```text
k8s/base/employee-frontend/network-policy.yaml
```

Policy:

```text
employee-frontend-ingress
```

Namespace:

```text
employee-frontend
```

Die Policy selektiert den Frontend-Pod über:

```yaml
app.kubernetes.io/name: employee-frontend
```

Sie erlaubt eingehenden TCP-Verkehr auf Port 80.

Die Regel beschränkt den Quellbereich nicht auf einen bestimmten Namespace.

Der Zugriff auf das Frontend wird dadurch nicht auf andere Anwendungs-Namespaces begrenzt.

### 6.3 Backend NetworkPolicy

Datei:

```text
k8s/base/employee-backend/network-policy.yaml
```

Policy:

```text
backend-allow-frontend
```

Namespace:

```text
employee-backend
```

Die Policy selektiert die Backend-Pods über die Projekt- und Component-Labels.

Erlaubt wird eingehender TCP-Verkehr aus dem Namespace:

```text
employee-frontend
```

Zusätzlich muss der Quell-Pod die passenden Frontend-Labels besitzen.

Freigegebene Ports:

| Port | Service |
|---|---|
| 8081 | Employee Service |
| 8082 | Auth Service |

Damit wird der eingehende Backend-Verkehr auf die vorgesehenen Frontend-Pods beschränkt.

### 6.4 Employee MySQL NetworkPolicy

Datei:

```text
k8s/base/database/network-policy.yaml
```

Policy:

```text
employee-mysql-allow-employee-service
```

Namespace:

```text
employee-database
```

Die Policy selektiert den Pod:

```text
employee-mysql
```

Erlaubter Quellbereich:

- Namespace `employee-backend`
- Pod `employee-service`
- Passende Backend- und Projektlabels

Freigegebener Port:

```text
TCP 3306
```

Damit ist der vorgesehene Zugriff auf die Employee-Datenbank auf den Employee Service beschränkt.

### 6.5 Auth MySQL NetworkPolicy

Ebenfalls in:

```text
k8s/base/database/network-policy.yaml
```

Policy:

```text
auth-mysql-allow-auth-service
```

Namespace:

```text
employee-database
```

Die Policy selektiert den Pod:

```text
auth-mysql
```

Erlaubter Quellbereich:

- Namespace `employee-backend`
- Pod `auth-service`
- Passende Backend- und Projektlabels

Freigegebener Port:

```text
TCP 3306
```

Damit ist der vorgesehene Zugriff auf die Auth-Datenbank auf den Auth Service beschränkt.

## 7. Übersicht der NetworkPolicies

| Policy | Namespace | Selektierte Pods | Erlaubter Ingress |
|---|---|---|---|
| `employee-frontend-ingress` | `employee-frontend` | Frontend | TCP 80 aus allen Quellen |
| `backend-allow-frontend` | `employee-backend` | Backend-Pods | TCP 8081 und 8082 vom Frontend |
| `employee-mysql-allow-employee-service` | `employee-database` | Employee MySQL | TCP 3306 vom Employee Service |
| `auth-mysql-allow-auth-service` | `employee-database` | Auth MySQL | TCP 3306 vom Auth Service |

Die Datenbank-Policies befinden sich gemeinsam in:

```text
k8s/base/database/network-policy.yaml
```

## 8. DNS und ausgehender Netzwerkverkehr

Die aktuellen NetworkPolicies sind Ingress-only-Policies.

Sie definieren keine allgemeine Einschränkung des Egress-Verkehrs.

Das bedeutet:

- Ausgehende DNS-Anfragen werden durch diese Policies nicht blockiert.
- Die Backend-Services können ihre Datenbankverbindungen aufbauen.
- Die Anwendungen können weiterhin andere notwendige ausgehende Verbindungen verwenden.

Eine spätere Einführung von Egress-Regeln muss insbesondere DNS, Datenbankverbindungen und weitere erforderliche Ziele berücksichtigen.

Eine pauschale Egress-Sperre ohne passende Freigaben könnte die Anwendung beeinträchtigen.

## 9. Nginx Reverse Proxy und CORS

### 9.1 Same-Origin-Kommunikation

Die Angular-Anwendung verwendet relative API-Pfade.

Beispiele:

```text
/api/employees
/api/auth
```

Nginx nimmt diese Anfragen entgegen und leitet sie intern an die Backend-Services weiter.

Dadurch erfolgt die Browser-Kommunikation über dieselbe Origin.

Der Browser kommuniziert nicht direkt mit den Backend-Services.

### 9.2 Nginx-Konfiguration

Die Konfiguration befindet sich in der ConfigMap:

```text
employee-frontend-nginx-config
```

Die relevanten Proxy-Ziele sind:

```nginx
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
```

### 9.3 CORS-Konfiguration

In beiden Spring-Security-Konfigurationen ist CORS deaktiviert:

```java
.cors(cors -> cors.disable())
```

Das entspricht der aktuellen Same-Origin-Architektur.

Eine frühere konfigurierbare CORS-Lösung führte zu `403 Invalid CORS request`-Antworten, obwohl die Anwendung über den gemeinsamen Frontend-Endpunkt aufgerufen wurde.

Die aktuelle Lösung verwendet deshalb Nginx als zentralen Reverse Proxy.

Die ConfigMap `backend-cors-config` ist noch vorhanden und wird von den Backend-Deployments eingebunden. Sie enthält derzeit keine aktive CORS-Konfiguration und wird für die aktuelle Lösung nicht benötigt.

Ein direkter Zugriff einer separat gestarteten Angular-Entwicklungsumgebung, beispielsweise über `ng serve` auf Port 4200, ist mit der aktuellen deaktivierten CORS-Konfiguration nicht als unterstützter Zugriffsweg vorgesehen.

## 10. Spring Security und Health-Endpunkte

Beide Backend-Anwendungen verwenden Spring Security.

Für die Kubernetes-Health-Checks müssen die Actuator-Endpunkte erreichbar sein.

Deshalb sind die Health-Endpunkte explizit freigegeben:

```java
.requestMatchers("/actuator/health/**").permitAll()
```

Die Anwendungen stellen die Health-Endpunkte über Spring Boot Actuator bereit.

Verwendete Endpunkte:

```text
/actuator/health/liveness
/actuator/health/readiness
```

Die Endpunkte werden von den Kubernetes-Probes verwendet.

Die Freigabe dieser Health-Endpunkte ist bewusst auf die erforderlichen Health-Pfade beschränkt.

Andere administrative oder diagnostische Actuator-Endpunkte werden nicht allgemein freigegeben.

## 11. NetworkPolicies überprüfen

Die vorhandenen Policies können mit folgenden Befehlen angezeigt werden:

```powershell
kubectl get networkpolicies -n employee-frontend
```

```powershell
kubectl get networkpolicies -n employee-backend
```

```powershell
kubectl get networkpolicies -n employee-database
```

Details einer einzelnen Policy:

```powershell
kubectl describe networkpolicy backend-allow-frontend -n employee-backend
```

```powershell
kubectl describe networkpolicy employee-mysql-allow-employee-service -n employee-database
```

Die tatsächlich angewendeten Regeln lassen sich mit `describe` kontrollieren.

### 11.1 Einschränkung der bisherigen Validierung

Die NetworkPolicies wurden erstellt und in Kubernetes angewendet.

Der reguläre Anwendungszugriff über das Frontend funktioniert.

Es wurde jedoch kein separater Funktionstest durchgeführt, der unerlaubte Verbindungen gezielt nachweist.

Die Anwesenheit einer NetworkPolicy allein beweist außerdem nicht, dass die verwendete Kubernetes-Netzwerkimplementierung alle Regeln tatsächlich durchsetzt.

Die lokale Netzwerkimplementierung und ihre NetworkPolicy-Unterstützung müssen bei einer späteren Änderung der Cluster-Konfiguration erneut überprüft werden.

## 12. Sicherheitsgrenzen der lokalen Umgebung

Die lokale Docker-Desktop-Umgebung ist eine Entwicklungsumgebung.

Folgende Punkte sind nicht als produktiv gehärtet anzusehen:

- Es wurde keine vollständige RBAC-Rollenstruktur eingerichtet.
- Es wurde keine produktive Verschlüsselung der Kubernetes Secrets konfiguriert.
- Es gibt keine zentrale Secret-Verwaltung.
- Es gibt keine vollständige Egress-Policy-Strategie.
- Die NetworkPolicy-Durchsetzung wurde nicht durch einen gezielten Negativtest nachgewiesen.
- Es wurde keine vollständige TLS-Absicherung aller internen Verbindungen umgesetzt.
- Die Frontend-Policy erlaubt TCP-Port 80 aus allen Quellen.

Diese Punkte müssen vor einem produktiven Einsatz bewertet werden.

## 13. Sicherheitsmaßnahmen für AWS EKS

Für eine spätere AWS-EKS-Umgebung müssen zusätzliche Maßnahmen vorgesehen werden.

Dazu gehören insbesondere:

### 13.1 Identitäts- und Berechtigungsmanagement

- Geeignete Kubernetes RBAC-Rollen
- Least-Privilege-Prinzip
- Beschränkung administrativer Zugriffe
- Geeignete AWS IAM-Berechtigungen

### 13.2 Secret-Verwaltung

- Verschlüsselte Speicherung von Secrets
- Geeignete Integration eines Secret-Management-Systems
- Regelmäßige Rotation von Passwörtern und Schlüsseln
- Keine vertraulichen Werte in Git

### 13.3 Netzwerk

- Prüfung der NetworkPolicy-Unterstützung des eingesetzten CNI
- Gezielte Ingress- und Egress-Regeln
- Begrenzung externer Zugriffe
- TLS für externe Zugriffe
- Prüfung der Kommunikationswege zwischen den Komponenten

### 13.4 Laufzeitsicherheit

- Regelmäßige Aktualisierung der Container-Images
- Verwendung vertrauenswürdiger Basis-Images
- Sicherheitsprüfung der Images
- Begrenzung von Container-Berechtigungen
- Keine unnötigen privilegierten Container

## 14. Implementierungsstatus

| Maßnahme | Status |
|---|---|
| Namespace-Trennung | Implementiert |
| Projekt- und Component-Labels | Implementiert |
| Datenbank-Secrets | Erstellt |
| Backend-Secrets | Erstellt |
| Frontend NetworkPolicy | Angewendet |
| Backend NetworkPolicy | Angewendet |
| Employee MySQL NetworkPolicy | Angewendet |
| Auth MySQL NetworkPolicy | Angewendet |
| Nginx Reverse Proxy | Implementiert |
| Same-Origin-API-Kommunikation | Implementiert |
| CORS in Spring Security deaktiviert | Implementiert |
| Actuator Health-Endpunkte freigegeben | Implementiert |
| Vollständige RBAC-Konfiguration | Noch offen |
| Verschlüsselung der Kubernetes Secrets | Noch offen |
| Gezielter NetworkPolicy-Negativtest | Noch offen |
| Vollständige Egress-Regeln | Noch offen |
| Produktive TLS- und Zertifikatsstrategie | Noch offen |

## 15. Zusammenfassung

Die Sicherheitsarchitektur verwendet eine Trennung der Frontend-, Backend- und Datenbankkomponenten in drei Kubernetes-Namespaces.

Vertrauliche Zugangsdaten werden über Kubernetes Secrets bereitgestellt.

NetworkPolicies begrenzen den eingehenden Verkehr zu den Backend- und Datenbank-Pods auf die vorgesehenen Kommunikationspartner.

Die Angular-Anwendung kommuniziert über Nginx als Reverse Proxy mit den Backend-Services. Dadurch wird die Same-Origin-Architektur verwendet und CORS ist für den aktuellen Browser-Zugriffsweg nicht erforderlich.

Die vorhandenen Maßnahmen bilden eine Grundlage für die lokale Entwicklung.

Für einen produktiven Einsatz müssen insbesondere RBAC, Secret-Verschlüsselung, NetworkPolicy-Durchsetzung, Egress-Regeln und TLS weiter gehärtet werden.