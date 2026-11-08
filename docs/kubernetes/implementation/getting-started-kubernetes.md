# Getting Started – Employee Management mit Kubernetes

## 1. Ziel dieser Anleitung

Diese Anleitung beschreibt, wie ein neuer Entwickler das Employee-Management-Projekt lokal mit Kubernetes unter Docker Desktop in Betrieb nehmen kann.

Sie führt von der Installation der benötigten Werkzeuge über die Konfiguration und den Image-Build bis zum Deployment und zum Aufruf der Anwendung im Browser.

Die lokale Kubernetes-Umgebung besteht aus fünf Workloads:

| Komponente | Technologie | Namespace |
|---|---|---|
| Frontend | Angular und Nginx | `employee-frontend` |
| Employee Service | Spring Boot | `employee-backend` |
| Auth Service | Spring Boot | `employee-backend` |
| Employee Database | MySQL 8.4 | `employee-database` |
| Auth Database | MySQL 8.4 | `employee-database` |

Das Frontend kommuniziert über Nginx mit den beiden Backend-Services. Die Backend-Services greifen über interne Kubernetes-Services auf ihre jeweiligen MySQL-Datenbanken zu.

**Hinweis:** Diese Anleitung behandelt ausschließlich die lokale Docker-Desktop-Umgebung. Ein Deployment auf AWS EKS ist nicht Bestandteil dieser Schritte.

## 2. Voraussetzungen installieren

Für den Betrieb werden folgende Werkzeuge benötigt:

- Docker Desktop
- Kubernetes über Docker Desktop
- kubectl
- Git
- PowerShell
- Ein Webbrowser

### 2.1 Docker Desktop

Docker Desktop installieren und starten.

Anschließend in den Einstellungen von Docker Desktop Kubernetes aktivieren.

Nach dem Start muss der Kubernetes-Cluster verfügbar sein.

### 2.2 Kubernetes-Kontext überprüfen

PowerShell öffnen und Folgendes ausführen:

```powershell
kubectl config current-context
kubectl cluster-info
kubectl get nodes
```

Der aktive Kontext muss lauten:

```text
docker-desktop
```

Der Kubernetes-Node muss den Status `Ready` haben.

Falls ein anderer Kontext aktiv ist, kann der lokale Kontext ausgewählt werden:

```powershell
kubectl config use-context docker-desktop
```

### 2.3 Versionen überprüfen

```powershell
docker --version
kubectl version
git --version
```

Die verwendete lokale Entwicklungsumgebung wurde mit Docker Desktop, Docker Engine und Kubernetes betrieben. Andere Versionen können funktionieren, sollten aber bei Problemen mit den verwendeten Versionen abgeglichen werden.

## 3. Repository herunterladen

Das Projekt-Repository klonen:

```powershell
git clone <GIT-REPOSITORY-URL>
```

In das Projektverzeichnis wechseln:

```powershell
cd <PROJEKTVERZEICHNIS>
```

Die Platzhalter müssen durch die tatsächliche Repository-URL und den tatsächlichen Projektverzeichnisnamen ersetzt werden.

Die Verzeichnisstruktur sollte unter anderem folgende Bestandteile enthalten:

```text
projekt/
├── employee-frontend/
├── employee-service/
├── auth-service/
├── k8s/
│   ├── base/
│   │   ├── employee-frontend/
│   │   ├── employee-backend/
│   │   ├── database/
│   │   └── kustomization.yaml
│   └── overlays/
│       ├── docker-desktop/
│       └── aws-eks/
├── docs/
└── backup/
```

Die Verzeichnisstruktur kann je nach aktuellem Projektstand weitere Dateien enthalten.

## 4. Umgebungsvariablen und Zugangsdaten vorbereiten

Vor dem Deployment müssen die benötigten Zugangsdaten vorbereitet werden.

Dazu gehören insbesondere:

- Datenbank-Root-Passwörter
- Datenbank-Benutzer und Passwörter
- JWT-Secret
- Zugangsdaten des initialen Administrators

Die Zugangsdaten dürfen nicht in öffentliche Git-Repositories eingecheckt werden.

Eine `.env.example` sollte ausschließlich Platzhalter enthalten. Die tatsächliche `.env` wird lokal erstellt und gehört in `.gitignore`.

### 4.1 Benötigte Variablen

Für die lokale Konfiguration werden folgende Variablen benötigt:

```dotenv
EMPLOYEE_MYSQL_ROOT_PASSWORD=<EMPLOYEE-MYSQL-ROOT-PASSWORD>
EMPLOYEE_DB_USER=<EMPLOYEE-DATABASE-USER>
EMPLOYEE_DB_PASSWORD=<EMPLOYEE-DATABASE-PASSWORD>

AUTH_MYSQL_ROOT_PASSWORD=<AUTH-MYSQL-ROOT-PASSWORD>
AUTH_DB_USER=<AUTH-DATABASE-USER>
AUTH_DB_PASSWORD=<AUTH-DATABASE-PASSWORD>

JWT_SECRET=<LANGER-ZUFAELLIGER-JWT-SCHLUESSEL>

AUTH_ADMIN_EMAIL=<ADMIN-EMAIL>
AUTH_ADMIN_PASSWORD=<ADMIN-PASSWORD>
```

Die Werte müssen durch eigene sichere Zugangsdaten ersetzt werden.

### 4.2 Kubernetes-Secrets

Die Kubernetes-Secrets werden in den Namespaces angelegt, in denen die jeweiligen Pods laufen.

Im Namespace `employee-database` werden benötigt:

- `employee-mysql-credentials`
- `auth-mysql-credentials`

Im Namespace `employee-backend` werden benötigt:

- `employee-service-secrets`
- `auth-service-secrets`

Die Secret-Keys müssen zu den Referenzen in den Deployment-Manifesten passen.

| Secret | Keys |
|---|---|
| `employee-mysql-credentials` | `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD` |
| `auth-mysql-credentials` | `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD` |
| `employee-service-secrets` | `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` |
| `auth-service-secrets` | `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `AUTH_ADMIN_EMAIL`, `AUTH_ADMIN_PASSWORD` |

Die Secrets können beispielsweise mit `kubectl create secret generic` und vorbereiteten Env-Dateien angelegt werden.

Beispiel für ein Env-File:

```dotenv
MYSQL_ROOT_PASSWORD=<ROOT-PASSWORD>
MYSQL_USER=<DATABASE-USER>
MYSQL_PASSWORD=<DATABASE-PASSWORD>
```

Anschließend:

```powershell
kubectl create namespace employee-database
kubectl create namespace employee-backend
kubectl create namespace employee-frontend
```

Die Datenbank-Secrets werden mit den passenden Env-Dateien angelegt:

```powershell
kubectl create secret generic employee-mysql-credentials `
  --namespace employee-database `
  --from-env-file=<PFAD-ZUR-EMPLOYEE-MYSQL-ENV-DATEI>

kubectl create secret generic auth-mysql-credentials `
  --namespace employee-database `
  --from-env-file=<PFAD-ZUR-AUTH-MYSQL-ENV-DATEI>
```

Für die Backend-Secrets müssen die Env-Dateien die oben aufgeführten Key-Namen enthalten:

```powershell
kubectl create secret generic employee-service-secrets `
  --namespace employee-backend `
  --from-env-file=<PFAD-ZUR-EMPLOYEE-SERVICE-ENV-DATEI>

kubectl create secret generic auth-service-secrets `
  --namespace employee-backend `
  --from-env-file=<PFAD-ZUR-AUTH-SERVICE-ENV-DATEI>
```

Die Platzhalter sind durch die tatsächlichen Dateipfade zu ersetzen.

Die Env-Dateien sollten nur temporär und lokal gespeichert werden. Sie dürfen nicht in Git eingecheckt werden.

**Wichtig:** Kubernetes-Secrets sind standardmäßig Base64-kodiert. Das bedeutet nicht, dass ihre Werte automatisch verschlüsselt sind.

## 5. Docker-Images bauen

Vor dem Deployment müssen die lokalen Container-Images erstellt werden.

Die folgenden Image-Tags entsprechen dem dokumentierten Projektstand.

### 5.1 Frontend bauen

```powershell
docker build -t employee-frontend:1.0.0 .\employee-frontend
```

### 5.2 Employee Service bauen

```powershell
docker build -t employee-service:1.0.4 .\employee-service
```

### 5.3 Auth Service bauen

```powershell
docker build -t auth-service:1.0.5 .\auth-service
```

Nach dem Build die Images überprüfen:

```powershell
docker images
```

Die Image-Namen und Versionen müssen mit den Angaben in den Kubernetes-Deployment-Manifesten übereinstimmen.

Die Deployments verwenden `imagePullPolicy: IfNotPresent`. Dadurch können die lokal verfügbaren Images verwendet werden.

## 6. Datenbanken und persistente Speicherung

Die Anwendung verwendet zwei getrennte MySQL-Datenbanken:

| Datenbank | Datenbankname | StatefulSet |
|---|---|---|
| Employee Database | `people_hub` | `employee-mysql` |
| Auth Database | `people_hub_auth` | `auth-mysql` |

Die Datenbanken werden als StatefulSets betrieben.

Für die dauerhafte Speicherung werden PersistentVolumeClaims verwendet:

- `employee-mysql-data`
- `auth-mysql-data`

Die PVCs befinden sich im Namespace `employee-database`.

### 6.1 Datenbankinitialisierung

Vor dem ersten produktiven Start muss geklärt sein, wie die Datenbanken initialisiert werden.

Mögliche Ausgangspunkte sind:

- Eine neue, leere Datenbank, sofern die Anwendung die benötigten Tabellen und Daten selbst erzeugt.
- Ein vorhandener SQL-Dump, der in die jeweilige Datenbank importiert wird.

Die ursprünglichen SQL-Dumps aus der bisherigen Entwicklungsumgebung sind nicht automatisch Bestandteil eines frisch geklonten Git-Repositories, da das Backup-Verzeichnis lokal gehalten wird.

Ein neuer Entwickler benötigt deshalb entweder eigene SQL-Dumps oder eine dokumentierte Initialisierung der Datenbanken.

**Achtung:** Vorhandene Datenbank-PVCs nicht löschen, um Datenbankprobleme zu beheben. Die lokale StorageClass `standard` verwendet die Reclaim Policy `Delete`. Das Löschen eines PVCs kann zum Verlust der gespeicherten Daten führen.

## 7. Kubernetes-Konfiguration überprüfen

Vor dem Deployment die Kustomize-Konfiguration prüfen:

```powershell
kubectl kustomize .\k8s\overlays\docker-desktop
```

Die Ressourcen werden dabei aus der gemeinsamen Base und dem Docker-Desktop-Overlay zusammengesetzt.

Anschließend kann eine serverseitige Dry-Run-Prüfung durchgeführt werden:

```powershell
kubectl apply --dry-run=server -k .\k8s\overlays\docker-desktop
```

Dieser Befehl überprüft die Ressourcen gegen die Kubernetes-API, ohne sie tatsächlich anzuwenden.

Falls Fehler auftreten, müssen zunächst die betroffenen Manifeste korrigiert werden.

## 8. Anwendung deployen

Wenn die Images gebaut, die Secrets angelegt und die Konfiguration geprüft wurden, kann das Deployment gestartet werden.

```powershell
kubectl apply -k .\k8s\overlays\docker-desktop
```

Kustomize erstellt beziehungsweise aktualisiert die in der Konfiguration definierten Kubernetes-Ressourcen.

Dazu gehören:

- Deployments
- Services
- StatefulSets
- PersistentVolumeClaims
- ConfigMaps
- NetworkPolicies

Die Secrets werden separat vorbereitet und sind nicht automatisch durch die Kustomize-Base angelegt.

## 9. Deployment-Status überprüfen

Nach dem Deployment den Zustand des Clusters überprüfen.

### 9.1 Namespaces

```powershell
kubectl get namespaces
```

### 9.2 Pods

```powershell
kubectl get pods -n employee-frontend
kubectl get pods -n employee-backend
kubectl get pods -n employee-database
```

Die Pods sollten nach erfolgreichem Start den Status `Running` haben.

### 9.3 Services

```powershell
kubectl get services -n employee-frontend
kubectl get services -n employee-backend
kubectl get services -n employee-database
```

### 9.4 Persistente Volumes

```powershell
kubectl get pvc -n employee-database
```

Die Datenbank-PVCs sollten den Status `Bound` erreichen.

### 9.5 Rollout-Status

```powershell
kubectl rollout status deployment/employee-frontend -n employee-frontend

kubectl rollout status deployment/employee-service -n employee-backend

kubectl rollout status deployment/auth-service -n employee-backend
```

Die Datenbank-StatefulSets können ebenfalls überprüft werden:

```powershell
kubectl rollout status statefulset/employee-mysql -n employee-database

kubectl rollout status statefulset/auth-mysql -n employee-database
```

## 10. Frontend aufrufen

Das Frontend wird über den Kubernetes-Service `employee-frontend` erreichbar gemacht.

Dazu ein Port-Forwarding starten:

```powershell
kubectl port-forward -n employee-frontend service/employee-frontend 8080:80
```

Das PowerShell-Fenster muss geöffnet bleiben.

Im Browser folgende Adresse öffnen:

```text
http://localhost:8080/
```

Die Angular-Anwendung wird über Nginx ausgeliefert.

Nginx leitet die API-Anfragen intern an die Backend-Services weiter:

| API-Pfad | Ziel |
|---|---|
| `/api/employees` | Employee Service, Port 8081 |
| `/api/auth` | Auth Service, Port 8082 |

Die Kommunikation zwischen den Services erfolgt über die internen Kubernetes-DNS-Namen.

## 11. Funktionstest

Nach dem Start sollte die Anwendung schrittweise geprüft werden.

### 11.1 Frontend

- Login-Seite wird im Browser angezeigt.
- Statische Inhalte werden geladen.
- Nginx liefert die Angular-Anwendung aus.

### 11.2 Backend

- Employee Service ist erreichbar.
- Auth Service ist erreichbar.
- Die Actuator-Health-Endpunkte liefern `UP`.

### 11.3 Datenbanken

- Beide MySQL-Pods laufen.
- Beide PVCs sind gebunden.
- Die Backend-Services können auf ihre jeweiligen Datenbanken zugreifen.

### 11.4 Anwendung

- Login mit einem gültigen Administratorkonto.
- Anmeldung wird erfolgreich verarbeitet.
- Mitarbeiterdaten können geladen werden.
- Änderungen werden gespeichert und bleiben nach einem Pod-Neustart erhalten.

Ein vollständiger Funktionstest muss nach dem jeweiligen Projektstand durchgeführt werden. Ein erfolgreiches Deployment allein beweist noch nicht, dass alle Anwendungsfunktionen korrekt arbeiten.

## 12. Fehleranalyse

Bei Problemen zunächst den Zustand der Pods prüfen:

```powershell
kubectl get pods -A
```

Bei einem fehlerhaften Pod:

```powershell
kubectl describe pod <POD-NAME> -n <NAMESPACE>
```

Logs abrufen:

```powershell
kubectl logs <POD-NAME> -n <NAMESPACE>
```

Die letzten Kubernetes-Ereignisse anzeigen:

```powershell
kubectl get events -A --sort-by=.metadata.creationTimestamp
```

Häufige Fehler:

| Fehler | Mögliche Ursache |
|---|---|
| `ImagePullBackOff` | Image fehlt oder Image-Tag stimmt nicht |
| `CrashLoopBackOff` | Anwendung startet nicht korrekt |
| `CreateContainerConfigError` | Secret oder ConfigMap fehlt |
| PVC bleibt `Pending` | StorageClass oder Pod-Zuordnung prüfen |
| Datenbankverbindung schlägt fehl | Service-DNS, Secret oder Datenbankstatus prüfen |
| Login funktioniert nicht | Auth-Service-Logs und Zugangsdaten prüfen |
| Frontend nicht erreichbar | Port-Forwarding und Service prüfen |

## 13. Wichtige Sicherheits- und Betriebshinweise

- Keine echten Passwörter in Git einchecken.
- Keine Kubernetes-Secrets in Screenshots oder Logs veröffentlichen.
- Datenbank-PVCs nicht unbedacht löschen.
- Vor Datenbankänderungen Backups erstellen.
- Die lokale Kubernetes-Konfiguration nicht ungeprüft in produktive Umgebungen übernehmen.
- Die lokale Metrics-Server-Konfiguration enthält eine Docker-Desktop-spezifische TLS-Ausnahme. Diese ist nicht für AWS EKS geeignet.
- Die NetworkPolicy-Manifeste sind vorhanden, ihre vollständige Durchsetzung muss bei Bedarf separat getestet werden.

## 14. Zusammenfassung

Mit dieser Anleitung kann ein neuer Entwickler die lokale Kubernetes-Umgebung vorbereiten und die Employee-Management-Anwendung unter Docker Desktop deployen.

Der Ablauf ist:

1. Docker Desktop und Kubernetes vorbereiten.
2. Repository klonen.
3. Zugangsdaten und Kubernetes-Secrets erstellen.
4. Datenbankinitialisierung sicherstellen.
5. Docker-Images bauen.
6. Kustomize-Konfiguration prüfen.
7. Anwendung deployen.
8. Pods, Services und PVCs kontrollieren.
9. Frontend über Port-Forwarding aufrufen.
10. Login und Anwendungsfunktionen testen.

Das Ziel ist eine reproduzierbare lokale Entwicklungsumgebung, die von mehreren Entwicklern unabhängig eingerichtet und betrieben werden kann.