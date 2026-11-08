# Lokales Kubernetes-Deployment

## 1. Ziel und Geltungsbereich

Diese Anleitung beschreibt, wie die Employee-Management-Anwendung in der lokalen Kubernetes-Umgebung von Docker Desktop bereitgestellt und überprüft wird.

Die Anwendung besteht aus folgenden Komponenten:

| Komponente | Namespace | Kubernetes-Ressource |
|---|---|---|
| Angular-Frontend mit Nginx | `employee-frontend` | Deployment und Service |
| Employee Service | `employee-backend` | Deployment und Service |
| Auth Service | `employee-backend` | Deployment und Service |
| Employee MySQL | `employee-database` | StatefulSet, Service und PVC |
| Auth MySQL | `employee-database` | StatefulSet, Service und PVC |

Die Datenbanken verwenden persistente Volumes. Die beiden Backend-Services kommunizieren mit den Datenbanken über Kubernetes-interne DNS-Namen. Das Frontend leitet API-Anfragen über Nginx an die Backend-Services weiter.

Diese Anleitung bezieht sich auf die lokale Docker-Desktop-Umgebung. Ein Deployment auf AWS EKS ist nicht Bestandteil dieser Anleitung.

## 2. Voraussetzungen

Folgende Komponenten müssen installiert und verfügbar sein:

- Docker Desktop mit aktiviertem Kubernetes
- Kubernetes-Kontext `docker-desktop`
- kubectl
- Docker CLI
- PowerShell
- Kustomize-Unterstützung über kubectl

Aktueller lokaler Umgebungsstand:

- Docker Desktop: 4.69.0.224084
- Docker Engine: 29.4.0
- Kubernetes: v1.36.1
- Kubernetes-Kontext: `docker-desktop`

Vor dem Deployment den aktiven Kontext und den Clusterzustand prüfen:

```powershell
kubectl config current-context
kubectl cluster-info
kubectl get nodes
```

Der aktive Kontext muss `docker-desktop` sein. Der lokale Kubernetes-Node muss den Status `Ready` haben.

## 3. Kubernetes-Verzeichnisstruktur

Die Kubernetes-Manifeste sind nach Anwendungskomponenten und Umgebungen strukturiert.

```text
k8s/
├── base/
│   ├── employee-frontend/
│   ├── employee-backend/
│   │   ├── auth-service/
│   │   └── employee-service/
│   ├── database/
│   │   ├── auth-mysql/
│   │   └── employee-mysql/
│   └── kustomization.yaml
└── overlays/
    ├── docker-desktop/
    │   └── kustomization.yaml
    └── aws-eks/
        └── kustomization.yaml
```

Das Verzeichnis `base` enthält die gemeinsamen Kubernetes-Ressourcen. Die Overlays dienen dazu, diese Ressourcen für unterschiedliche Zielumgebungen zusammenzustellen oder später umgebungsspezifisch anzupassen.

Das Docker-Desktop-Overlay verweist derzeit auf die gemeinsame Base. Es enthält aktuell keine zusätzlichen Ressourcenänderungen.

Das AWS-EKS-Overlay ist eine vorbereitete Vorlage. Es ist nicht als produktionsfertige Deployment-Konfiguration zu verstehen.

## 4. Konfiguration und Secrets

Vor dem Start müssen die benötigten Kubernetes-Secrets und ConfigMaps vorhanden sein.

Die Datenbank-Secrets liegen im Namespace `employee-database`:

- `employee-mysql-credentials`
- `auth-mysql-credentials`

Die Backend-Secrets liegen im Namespace `employee-backend`:

- `employee-service-secrets`
- `auth-service-secrets`

Die Secrets enthalten unter anderem Datenbankzugangsdaten, JWT-Schlüssel und die Zugangsdaten für den initialen Administrator.

Die Konfiguration der Backend-Services verwendet außerdem die ConfigMap `backend-cors-config`.

Die Nginx-Konfiguration des Frontends wird über die ConfigMap `employee-frontend-nginx-config` bereitgestellt.

Vorhandene Secrets können überprüft werden, ohne ihre Werte auszugeben:

```powershell
kubectl get secrets -n employee-database
kubectl get secrets -n employee-backend
```

ConfigMaps:

```powershell
kubectl get configmaps -n employee-frontend
kubectl get configmaps -n employee-backend
```

**Wichtig:** Secret-Werte nicht in Logs, Screenshots oder Git-Repositories veröffentlichen. Kubernetes-Secrets sind standardmäßig lediglich Base64-kodiert. Base64 ist keine Verschlüsselung.

## 5. Docker-Images bauen

Die lokalen Images müssen gebaut werden, bevor die zugehörigen Deployments gestartet oder aktualisiert werden.

### 5.1 Frontend

Im Projektverzeichnis:

```powershell
docker build -t employee-frontend:1.0.0 .\employee-frontend
```

### 5.2 Employee Service

```powershell
docker build -t employee-service:1.0.4 .\employee-service
```

### 5.3 Auth Service

```powershell
docker build -t auth-service:1.0.5 .\auth-service
```

Die Image-Versionen entsprechen dem dokumentierten lokalen Stand. Werden Quellcode oder Dockerfiles geändert, müssen die Images erneut gebaut und die Image-Tags in den Deployment-Manifesten bei Bedarf angepasst werden.

Da Docker Desktop die lokale Container-Runtime mit Kubernetes integriert, können lokal gebaute Images in der Regel direkt verwendet werden. Die Deployments verwenden dafür `imagePullPolicy: IfNotPresent`.

Bei Problemen mit nicht gefundenen Images zuerst prüfen, ob der Image-Name und der Tag im Deployment exakt mit dem lokal gebauten Image übereinstimmen.

## 6. Deployment mit Kustomize

Die Ressourcen können über das Docker-Desktop-Overlay zusammengeführt werden.

Zunächst die resultierende Konfiguration anzeigen:

```powershell
kubectl kustomize .\k8s\overlays\docker-desktop
```

Vor einer tatsächlichen Änderung am Cluster empfiehlt sich eine serverseitige Dry-Run-Prüfung:

```powershell
kubectl apply --dry-run=server -k .\k8s\overlays\docker-desktop
```

Dieser Befehl prüft die Ressourcen gegen die Kubernetes-API, ohne sie anzuwenden.

Wenn die Konfiguration geprüft wurde und tatsächlich ausgerollt werden soll, kann folgender Befehl verwendet werden:

```powershell
kubectl apply -k .\k8s\overlays\docker-desktop
```

**Achtung:** `kubectl apply` verändert den Clusterzustand. Vorher sicherstellen, dass die Images, Secrets, ConfigMaps und Manifeste zum gewünschten Stand passen. Insbesondere Datenbank-PVCs nicht löschen oder neu erstellen, um ein Deployment-Problem zu beheben.

## 7. Deployment-Status überprüfen

Nach dem Deployment zunächst die Namespaces prüfen:

```powershell
kubectl get namespaces
```

Anschließend die Pods:

```powershell
kubectl get pods -n employee-frontend
kubectl get pods -n employee-backend
kubectl get pods -n employee-database
```

Services:

```powershell
kubectl get services -n employee-frontend
kubectl get services -n employee-backend
kubectl get services -n employee-database
```

Persistente Volumes:

```powershell
kubectl get pvc -n employee-database
```

Die Datenbank-PVCs müssen den Status `Bound` haben.

Für die Backend-Deployments kann der Rollout-Status separat überprüft werden:

```powershell
kubectl rollout status deployment/employee-service -n employee-backend
kubectl rollout status deployment/auth-service -n employee-backend
```

Für das Frontend:

```powershell
kubectl rollout status deployment/employee-frontend -n employee-frontend
```

Bei StatefulSets:

```powershell
kubectl rollout status statefulset/employee-mysql -n employee-database
kubectl rollout status statefulset/auth-mysql -n employee-database
```

## 8. Health Checks der Backend-Services

Beide Spring-Boot-Services stellen Actuator-Health-Endpunkte bereit.

Die Kubernetes-Probes verwenden:

- Startup Probe: `/actuator/health/liveness`
- Liveness Probe: `/actuator/health/liveness`
- Readiness Probe: `/actuator/health/readiness`

Die Endpunkte können über Port-Forwarding geprüft werden.

Employee Service:

```powershell
kubectl port-forward -n employee-backend service/employee-service 18081:8081
```

In einem zweiten PowerShell-Fenster:

```powershell
Invoke-RestMethod http://localhost:18081/actuator/health/liveness
Invoke-RestMethod http://localhost:18081/actuator/health/readiness
```

Auth Service:

```powershell
kubectl port-forward -n employee-backend service/auth-service 18082:8082
```

In einem zweiten PowerShell-Fenster:

```powershell
Invoke-RestMethod http://localhost:18082/actuator/health/liveness
Invoke-RestMethod http://localhost:18082/actuator/health/readiness
```

Ein erfolgreicher Health Check liefert den Status `UP`.

## 9. Frontend lokal aufrufen

Das Frontend wird über den Kubernetes-Service `employee-frontend` bereitgestellt.

Port-Forwarding starten:

```powershell
kubectl port-forward -n employee-frontend service/employee-frontend 8080:80
```

Anschließend im Browser öffnen:

```text
http://localhost:8080/
```

Das PowerShell-Fenster mit dem Port-Forwarding muss geöffnet bleiben, solange die Verbindung verwendet wird.

Beenden lässt sich der Prozess mit `Ctrl+C`.

Die API-Aufrufe erfolgen über relative Pfade wie `/api/employees` und `/api/auth`. Nginx leitet diese Anfragen intern an die jeweiligen Backend-Services weiter.

Dadurch verwenden Browser und API dieselbe Origin. Eine zusätzliche browserseitige CORS-Konfiguration ist für diesen Zugriffsweg nicht erforderlich.

## 10. Logs und Fehleranalyse

Logs der einzelnen Komponenten können mit folgenden Befehlen abgerufen werden:

Frontend:

```powershell
kubectl logs deployment/employee-frontend -n employee-frontend
```

Employee Service:

```powershell
kubectl logs deployment/employee-service -n employee-backend
```

Auth Service:

```powershell
kubectl logs deployment/auth-service -n employee-backend
```

Datenbanken:

```powershell
kubectl logs statefulset/employee-mysql -n employee-database
kubectl logs statefulset/auth-mysql -n employee-database
```

Bei nicht startenden Pods:

```powershell
kubectl describe pod <pod-name> -n <namespace>
```

Ereignisse anzeigen:

```powershell
kubectl get events -n <namespace> --sort-by=.metadata.creationTimestamp
```

Bei einem Image-Problem insbesondere auf `ErrImagePull` oder `ImagePullBackOff` achten.

Bei Datenbankproblemen zusätzlich den Status der PVCs und StatefulSets prüfen. Persistente Daten nicht durch Löschen von PVCs oder StatefulSets gefährden.

## 11. Ressourcen und Monitoring

Die Ressourcenverwendung kann über Metrics Server geprüft werden:

```powershell
kubectl top nodes
kubectl top pods -A
```

Die konfigurierten Ressourcenanforderungen und Limits lassen sich mit `kubectl describe` anzeigen:

```powershell
kubectl describe deployment employee-frontend -n employee-frontend
kubectl describe deployment employee-service -n employee-backend
kubectl describe deployment auth-service -n employee-backend
```

Die Datenbank-Ressourcen können über die StatefulSets geprüft werden:

```powershell
kubectl describe statefulset employee-mysql -n employee-database
kubectl describe statefulset auth-mysql -n employee-database
```

Die lokale Docker-Desktop-Installation verwendet für Metrics Server eine zusätzliche Option zur Umgehung der Kubelet-Zertifikatsprüfung. Diese lokale Entwicklungsausnahme ist nicht für produktive Cluster oder AWS EKS geeignet.

## 12. Wichtige Hinweise zum Datenbestand

Die beiden MySQL-Datenbanken wurden aus der bestehenden Docker-Compose-Umgebung migriert.

Die Daten wurden in die Kubernetes-Datenbanken importiert und überprüft.

Die ursprünglichen Docker-Compose-Container und deren Volumes bleiben als separate Datenquelle bestehen.

Folgende Regeln sind zu beachten:

- Datenbank-PVCs nicht löschen.
- Keine erneuten Importe der bereits importierten SQL-Dumps durchführen, ohne vorher den Migrationszustand zu prüfen.
- Docker-Compose-Volumes nicht entfernen.
- Vor Änderungen an Datenbankressourcen ein Backup erstellen.
- Datenbankänderungen nicht allein durch das Löschen und Neuerstellen von Pods oder PVCs behandeln.

Die StorageClass `standard` verwendet in der lokalen Umgebung die Reclaim Policy `Delete`. Das Löschen eines PVCs kann deshalb zum Verlust der zugehörigen persistenten Daten führen.

## 13. Aktueller Implementierungsstand

Die lokale Kubernetes-Umgebung wurde auf Docker Desktop eingerichtet.

Folgende Bestandteile sind umgesetzt und überprüft:

- Kubernetes-Cluster und Namespaces
- Frontend-Deployment und Service
- Employee-Service-Deployment und Service
- Auth-Service-Deployment und Service
- Beide MySQL-StatefulSets und Services
- Persistente Datenbank-Volumes
- Migration und Überprüfung der Datenbankdaten
- Kubernetes-Secrets und ConfigMaps
- Health Checks für beide Backend-Services
- Resource Requests und Limits
- NetworkPolicy-Manifeste
- Kustomize-Basisstruktur und Docker-Desktop-Overlay
- Serverseitige Dry-Run-Prüfung der Kustomize-Ressourcen
- Lokaler Zugriff auf das Frontend über Port-Forwarding
- Metrics Server und Ressourcenabfragen

Die NetworkPolicy-Manifeste wurden angewendet. Die tatsächliche Durchsetzung aller erlaubten und blockierten Netzwerkverbindungen wurde jedoch nicht durch separate Funktionstests nachgewiesen.

Das AWS-EKS-Overlay ist lediglich eine vorbereitete Vorlage. Ein AWS-Deployment oder AWS-spezifische Tests wurden nicht durchgeführt.

## 14. Zusammenfassung

Das lokale Deployment wird über Docker Desktop und kubectl verwaltet. Die Kubernetes-Manifeste werden mit Kustomize strukturiert und über das Docker-Desktop-Overlay zusammengeführt.

Die Anwendung besteht aus einem Angular-/Nginx-Frontend, zwei Spring-Boot-Backend-Services und zwei persistent betriebenen MySQL-Datenbanken.

Für den lokalen Zugriff wird das Frontend über Port-Forwarding auf `http://localhost:8080/` bereitgestellt. Die Backend-Kommunikation erfolgt intern über Kubernetes-Service-DNS.

Die vorhandenen Datenbank-PVCs und Docker-Compose-Volumes sind als persistente Datenbestände zu behandeln und dürfen nicht unbedacht gelöscht werden.