# Kubernetes Command Reference

## 1. Zweck und Geltungsbereich

Dieses Dokument enthält die wichtigsten Befehle zur Verwaltung und Überprüfung der lokalen Kubernetes-Umgebung des Employee-Management-Projekts.

Die Befehle beziehen sich auf die lokale Kubernetes-Installation von Docker Desktop unter Windows und PowerShell.

Dokumentiert werden:

- Kubernetes-Cluster und Kontext
- Namespaces und Ressourcen
- Kustomize
- Deployments und Pods
- Services und Netzwerkverbindungen
- MySQL StatefulSets und PersistentVolumeClaims
- Kubernetes Secrets
- Health Checks und Probes
- Logs und Fehleranalyse
- Metrics Server und Ressourcenverbrauch
- Port-Forwarding
- AWS-EKS-Vorbereitung

**Wichtig:** Die AWS-EKS-Umgebung wurde bisher nicht bereitgestellt. Die entsprechenden Befehle in diesem Dokument dienen ausschließlich der späteren Orientierung und sind keine Bestätigung einer vorhandenen AWS-Installation.

---

## 2. Voraussetzungen

Für die lokale Ausführung werden folgende Komponenten benötigt:

| Komponente | Verwendung |
|---|---|
| Windows | Lokales Betriebssystem |
| Docker Desktop | Container-Runtime und lokale Kubernetes-Umgebung |
| WSL2 | Linux-basierte Umgebung für Docker Desktop |
| kubectl | Verwaltung des Kubernetes-Clusters |
| PowerShell | Ausführung der Verwaltungsbefehle |
| Kustomize | Zusammenstellung der Kubernetes-Manifeste |

Die lokale Umgebung verwendet:

- Docker Desktop
- Kubernetes-Kontext `docker-desktop`
- Kubernetes-Version `v1.36.1`
- Einen lokalen Kubernetes-Node

Die Umgebung ist für Entwicklung und lokale Tests vorgesehen. Sie stellt keine hochverfügbare Kubernetes-Produktionsumgebung dar.

## 3. Verzeichnisstruktur

Die Kubernetes-Manifeste sind nach Anwendungsschichten und Umgebungen organisiert.

```text
k8s/
├── base/
│   ├── kustomization.yaml
│   ├── employee-frontend/
│   ├── employee-backend/
│   │   ├── auth-service/
│   │   └── employee-service/
│   └── database/
│       ├── auth-mysql/
│       └── employee-mysql/
│
└── overlays/
    ├── docker-desktop/
    │   └── kustomization.yaml
    │
    └── aws-eks/
        └── kustomization.yaml
```

Die Basis enthält die allgemeinen Kubernetes-Ressourcen.

Die Overlays dienen dazu, umgebungsspezifische Anpassungen vorzunehmen, ohne die gemeinsame Basis verändern zu müssen.

Die Datenbankressourcen befinden sich im Verzeichnis `database`. Der zugehörige Kubernetes-Namespace heißt dagegen `employee-database`.

## 4. Verbindung zum Kubernetes-Cluster

### 4.1 Aktiven Kontext anzeigen

```powershell
kubectl config current-context
```

Erwarteter Kontext:

```text
docker-desktop
```

### 4.2 Verfügbare Kontexte anzeigen

```powershell
kubectl config get-contexts
```

### 4.3 Kubernetes-Cluster überprüfen

```powershell
kubectl cluster-info
```

### 4.4 Kubernetes-Version anzeigen

```powershell
kubectl version
```

### 4.5 Nodes anzeigen

```powershell
kubectl get nodes
```

Ausführliche Informationen:

```powershell
kubectl describe nodes
```

### 4.6 Node-Ressourcen anzeigen

```powershell
kubectl top nodes
```

Dieser Befehl benötigt einen funktionierenden Metrics Server.

## 5. Namespaces verwalten

Die Anwendung verwendet drei Namespaces:

| Namespace | Zweck |
|---|---|
| `employee-frontend` | Angular und Nginx |
| `employee-backend` | Employee Service und Auth Service |
| `employee-database` | Employee MySQL und Auth MySQL |

### 5.1 Alle Namespaces anzeigen

```powershell
kubectl get namespaces
```

### 5.2 Ressourcen eines Namespace anzeigen

Frontend:

```powershell
kubectl get all -n employee-frontend
```

Backend:

```powershell
kubectl get all -n employee-backend
```

Datenbanken:

```powershell
kubectl get all -n employee-database
```

### 5.3 Namespace-Details anzeigen

```powershell
kubectl describe namespace employee-frontend
```

Beispielsweise für das Backend:

```powershell
kubectl describe namespace employee-backend
```

### 5.4 Labels anzeigen

```powershell
kubectl get namespaces --show-labels
```

### 5.5 Ressourcen nach Labels filtern

Beispiel für Backend-Pods:

```powershell
kubectl get pods -n employee-backend -l app.kubernetes.io/component=backend
```

Beispiel für Datenbank-Pods:

```powershell
kubectl get pods -n employee-database -l app.kubernetes.io/component=database
```

## 6. Kustomize verwenden

Kustomize fasst mehrere Kubernetes-Manifeste zu einer gemeinsamen Konfiguration zusammen.

Dadurch müssen die einzelnen YAML-Dateien nicht separat angewendet werden.

### 6.1 Gesamte Basis anzeigen

```powershell
kubectl kustomize .\k8s\base
```

### 6.2 Docker-Desktop-Konfiguration anzeigen

```powershell
kubectl kustomize .\k8s\overlays\docker-desktop
```

### 6.3 AWS-EKS-Konfiguration anzeigen

```powershell
kubectl kustomize .\k8s\overlays\aws-eks
```

**Hinweis:** Das AWS-EKS-Overlay ist derzeit nur ein Gerüst. Die Ausgabe der Manifeste bedeutet nicht, dass diese bereits für AWS EKS validiert oder einsatzbereit sind.

### 6.4 Gesamte lokale Anwendung bereitstellen

```powershell
kubectl apply -k .\k8s\overlays\docker-desktop
```

Alternativ kann die Basis direkt verwendet werden:

```powershell
kubectl apply -k .\k8s\base
```

Beide Varianten verwenden die gemeinsame Basis. Für den normalen lokalen Betrieb sollte das Docker-Desktop-Overlay verwendet werden.

### 6.5 Ressourcenstatus nach der Bereitstellung prüfen

```powershell
kubectl get all -n employee-frontend
kubectl get all -n employee-backend
kubectl get all -n employee-database
```

### 6.6 Einzelne Bereiche verwalten

Frontend:

```powershell
kubectl apply -k .\k8s\base\employee-frontend
```

Backend:

```powershell
kubectl apply -k .\k8s\base\employee-backend
```

Datenbanken:

```powershell
kubectl apply -k .\k8s\base\database
```

Diese Befehle wenden jeweils die Ressourcen des angegebenen Bereichs an.

**Achtung:** Die Datenbankkonfiguration enthält StatefulSets und PersistentVolumeClaims. Änderungen und Löschoperationen müssen deshalb besonders sorgfältig geprüft werden.

## 7. Deployments verwalten

### 7.1 Deployments anzeigen

Frontend:

```powershell
kubectl get deployments -n employee-frontend
```

Backend:

```powershell
kubectl get deployments -n employee-backend
```

### 7.2 Details eines Deployments anzeigen

Frontend:

```powershell
kubectl describe deployment employee-frontend -n employee-frontend
```

Employee Service:

```powershell
kubectl describe deployment employee-service -n employee-backend
```

Auth Service:

```powershell
kubectl describe deployment auth-service -n employee-backend
```

### 7.3 Rollout-Status überprüfen

Frontend:

```powershell
kubectl rollout status deployment/employee-frontend -n employee-frontend
```

Employee Service:

```powershell
kubectl rollout status deployment/employee-service -n employee-backend
```

Auth Service:

```powershell
kubectl rollout status deployment/auth-service -n employee-backend
```

### 7.4 Deployment neu starten

Frontend:

```powershell
kubectl rollout restart deployment/employee-frontend -n employee-frontend
```

Employee Service:

```powershell
kubectl rollout restart deployment/employee-service -n employee-backend
```

Auth Service:

```powershell
kubectl rollout restart deployment/auth-service -n employee-backend
```

Ein Neustart ist beispielsweise erforderlich, wenn eine über `subPath` eingebundene ConfigMap geändert wurde.

### 7.5 Rollout-Historie anzeigen

```powershell
kubectl rollout history deployment/employee-service -n employee-backend
```

Für den Auth Service:

```powershell
kubectl rollout history deployment/auth-service -n employee-backend
```

## 8. Pods verwalten

### 8.1 Alle Pods anzeigen

```powershell
kubectl get pods -A
```

### 8.2 Pods eines Namespace anzeigen

```powershell
kubectl get pods -n employee-frontend
kubectl get pods -n employee-backend
kubectl get pods -n employee-database
```

### 8.3 Pod-Details anzeigen

```powershell
kubectl describe pod <POD-NAME> -n <NAMESPACE>
```

Beispiel:

```powershell
kubectl describe pod employee-service-<SUFFIX> -n employee-backend
```

Der tatsächliche Pod-Name kann vorher mit `kubectl get pods` ermittelt werden.

### 8.4 Pod-Status kontinuierlich beobachten

```powershell
kubectl get pods -A -w
```

Die Beobachtung wird mit `Ctrl+C` beendet.

## 9. Logs und Fehleranalyse

### 9.1 Logs eines Pods anzeigen

```powershell
kubectl logs <POD-NAME> -n <NAMESPACE>
```

### 9.2 Logs des Employee Service anzeigen

```powershell
kubectl logs deployment/employee-service -n employee-backend
```

### 9.3 Logs des Auth Service anzeigen

```powershell
kubectl logs deployment/auth-service -n employee-backend
```

### 9.4 Logs des Frontends anzeigen

```powershell
kubectl logs deployment/employee-frontend -n employee-frontend
```

### 9.5 Logs kontinuierlich verfolgen

```powershell
kubectl logs deployment/employee-service -n employee-backend -f
```

Beenden mit `Ctrl+C`.

### 9.6 Vorherige Container-Logs anzeigen

Wenn ein Container neu gestartet wurde:

```powershell
kubectl logs <POD-NAME> -n <NAMESPACE> --previous
```

### 9.7 Kubernetes-Ereignisse anzeigen

```powershell
kubectl get events -A --sort-by=.metadata.creationTimestamp
```

Dieser Befehl ist besonders hilfreich bei:

- Image-Pull-Fehlern
- Fehlgeschlagenen Probes
- Nicht startenden Containern
- Problemen mit PersistentVolumeClaims
- Fehlenden Ressourcen

## 10. Services und Netzwerk

### 10.1 Services anzeigen

```powershell
kubectl get services -A
```

### 10.2 Services eines Namespace anzeigen

```powershell
kubectl get services -n employee-frontend
kubectl get services -n employee-backend
kubectl get services -n employee-database
```

### 10.3 Service-Details anzeigen

Frontend:

```powershell
kubectl describe service employee-frontend -n employee-frontend
```

Employee Service:

```powershell
kubectl describe service employee-service -n employee-backend
```

Auth Service:

```powershell
kubectl describe service auth-service -n employee-backend
```

Employee MySQL:

```powershell
kubectl describe service employee-mysql -n employee-database
```

Auth MySQL:

```powershell
kubectl describe service auth-mysql -n employee-database
```

### 10.4 Service-DNS-Namen

Die Services sind innerhalb des Kubernetes-Clusters über folgende DNS-Namen erreichbar:

| Service | DNS-Name | Port |
|---|---|---:|
| Frontend | `employee-frontend.employee-frontend.svc.cluster.local` | 80 |
| Employee Service | `employee-service.employee-backend.svc.cluster.local` | 8081 |
| Auth Service | `auth-service.employee-backend.svc.cluster.local` | 8082 |
| Employee MySQL | `employee-mysql.employee-database.svc.cluster.local` | 3306 |
| Auth MySQL | `auth-mysql.employee-database.svc.cluster.local` | 3306 |

Die Services verwenden interne ClusterIP-Adressen.

Sie sind nicht automatisch aus dem lokalen Netzwerk oder dem Internet erreichbar.

### 10.5 NetworkPolicies anzeigen

```powershell
kubectl get networkpolicies -A
```

Details einer Policy:

```powershell
kubectl describe networkpolicy backend-allow-frontend -n employee-backend
```

Frontend:

```powershell
kubectl describe networkpolicy employee-frontend-ingress -n employee-frontend
```

Datenbanken:

```powershell
kubectl describe networkpolicy employee-mysql-allow-employee-service -n employee-database
kubectl describe networkpolicy auth-mysql-allow-auth-service -n employee-database
```

Die vorhandenen Regeln beschränken den Ingress-Verkehr der ausgewählten Pods.

Eine vollständige funktionale Prüfung der erlaubten und verbotenen Verbindungen wurde bisher nicht durchgeführt.

## 11. MySQL StatefulSets und Speicher

Die beiden MySQL-Datenbanken werden als StatefulSets betrieben.

| StatefulSet | Datenbank | Namespace |
|---|---|---|
| `employee-mysql` | `people_hub` | `employee-database` |
| `auth-mysql` | `people_hub_auth` | `employee-database` |

### 11.1 StatefulSets anzeigen

```powershell
kubectl get statefulsets -n employee-database
```

### 11.2 Details eines StatefulSets anzeigen

```powershell
kubectl describe statefulset employee-mysql -n employee-database
```

```powershell
kubectl describe statefulset auth-mysql -n employee-database
```

### 11.3 PersistentVolumeClaims anzeigen

```powershell
kubectl get pvc -n employee-database
```

### 11.4 PersistentVolumes anzeigen

```powershell
kubectl get pv
```

### 11.5 StorageClasses anzeigen

```powershell
kubectl get storageclass
```

Die lokale Umgebung verwendet die StorageClass `standard`.

Die PVCs sind mit jeweils 2 GiB Speicherplatz konfiguriert und verwenden den Access Mode `ReadWriteOnce`.

**Wichtiger Sicherheitshinweis:**

Die lokale StorageClass verwendet die Reclaim Policy `Delete`.

Das Löschen eines PersistentVolumeClaims kann daher zum Verlust der zugehörigen persistenten Daten führen.

Folgende Befehle dürfen nicht unüberlegt ausgeführt werden:

```powershell
kubectl delete pvc <PVC-NAME> -n employee-database
```

```powershell
kubectl delete namespace employee-database
```

Auch das Löschen eines StatefulSets oder eines gesamten Kubernetes-Overlays kann Auswirkungen auf die Datenbankumgebung haben.

Vor Änderungen an Datenbankressourcen müssen vorhandene Backups geprüft und die Auswirkungen auf die PersistentVolumeClaims verstanden werden.

## 12. Kubernetes Secrets

Die Anwendung verwendet Kubernetes Secrets zur Bereitstellung sensibler Konfigurationswerte.

### 12.1 Secrets anzeigen

Frontend- und Backend-Namespaces:

```powershell
kubectl get secrets -n employee-backend
```

Datenbank-Namespace:

```powershell
kubectl get secrets -n employee-database
```

### 12.2 Secret-Metadaten anzeigen

```powershell
kubectl describe secret employee-service-secrets -n employee-backend
```

```powershell
kubectl describe secret auth-service-secrets -n employee-backend
```

**Achtung:** Secret-Inhalte nicht unbedacht mit `kubectl get secret -o yaml` ausgeben oder in Logs, Screenshots und öffentlichen Repositories veröffentlichen.

Kubernetes Secrets sind standardmäßig lediglich Base64-kodiert. Base64 ist keine Verschlüsselung.

Für produktive Umgebungen müssen geeignete Verschlüsselungs- und Zugriffsschutzmaßnahmen vorgesehen werden.

## 13. Health Checks und Kubernetes Probes

Die beiden Spring-Boot-Services stellen Health-Endpunkte über Spring Boot Actuator bereit.

### 13.1 Employee Service

Liveness:

```text
/actuator/health/liveness
```

Readiness:

```text
/actuator/health/readiness
```

### 13.2 Auth Service

Liveness:

```text
/actuator/health/liveness
```

Readiness:

```text
/actuator/health/readiness
```

### 13.3 Direkter Test über Port-Forwarding

Employee Service:

```powershell
kubectl port-forward -n employee-backend service/employee-service 18081:8081
```

In einem zweiten PowerShell-Fenster:

```powershell
Invoke-RestMethod http://localhost:18081/actuator/health/liveness
```

```powershell
Invoke-RestMethod http://localhost:18081/actuator/health/readiness
```

Auth Service:

```powershell
kubectl port-forward -n employee-backend service/auth-service 18082:8082
```

In einem zweiten PowerShell-Fenster:

```powershell
Invoke-RestMethod http://localhost:18082/actuator/health/liveness
```

```powershell
Invoke-RestMethod http://localhost:18082/actuator/health/readiness
```

Die erfolgreichen Prüfungen liefern einen Health-Status mit `UP`.

### 13.4 Bedeutung der Probes

| Probe | Aufgabe |
|---|---|
| Startup Probe | Prüft, ob die Anwendung gestartet ist |
| Liveness Probe | Prüft, ob der Container weiterhin funktionsfähig ist |
| Readiness Probe | Prüft, ob der Pod Anfragen entgegennehmen kann |

Kubernetes verwendet diese Informationen, um den Zustand der Container zu überwachen und die Verteilung von Anfragen zu steuern.

## 14. Metrics Server und Ressourcenverbrauch

Der Metrics Server stellt Kubernetes aktuelle Ressourcenmesswerte zur Verfügung.

Diese Messwerte können mit `kubectl top` abgefragt werden.

### 14.1 Metrics Server Deployment anzeigen

```powershell
kubectl get deployment metrics-server -n kube-system
```

### 14.2 Metrics Server Pods anzeigen

```powershell
kubectl get pods -n kube-system -l k8s-app=metrics-server
```

### 14.3 Ressourcenverbrauch der Nodes

```powershell
kubectl top nodes
```

### 14.4 Ressourcenverbrauch aller Pods

```powershell
kubectl top pods -A
```

### 14.5 Ressourcenverbrauch eines Namespace

```powershell
kubectl top pods -n employee-backend
```

```powershell
kubectl top pods -n employee-database
```

### 14.6 Bedeutung der Messwerte

`kubectl top` zeigt den aktuell ermittelten CPU- und Speicherverbrauch.

Die Werte sind Momentaufnahmen und keine langfristige Performance-Analyse.

Der Metrics Server kann unter anderem als Datenquelle für Horizontal Pod Autoscaler verwendet werden.

Er ersetzt jedoch keine umfassende Monitoring-Lösung wie Prometheus und Grafana.

### 14.7 Lokale Besonderheit: TLS-Konfiguration

In der lokalen Docker-Desktop-Umgebung konnte der Metrics Server das Kubelet-Zertifikat zunächst nicht verifizieren.

Ursache war, dass das verwendete Zertifikat die IP-Adresse des Kubelets nicht als IP-SAN enthielt.

Für die lokale Entwicklungsumgebung wurde deshalb folgende Argument-Option am Metrics Server gesetzt:

```text
--kubelet-insecure-tls
```

Damit funktioniert die lokale Erfassung der Node- und Pod-Metriken.

**Wichtig:** Diese Option deaktiviert die Zertifikatsprüfung der Kubelet-Verbindung und ist ausschließlich als lokale Docker-Desktop-Ausnahme dokumentiert.

Sie darf nicht ungeprüft in einer produktiven AWS-EKS-Umgebung übernommen werden.

Für EKS muss eine geeignete vertrauenswürdige TLS-Konfiguration verwendet werden.

## 15. Port-Forwarding

Da die lokalen Kubernetes-Services als ClusterIP betrieben werden, kann für den Zugriff vom Windows-Host Port-Forwarding verwendet werden.

### 15.1 Frontend öffnen

```powershell
kubectl port-forward -n employee-frontend service/employee-frontend 8080:80
```

Danach im Browser öffnen:

```text
http://localhost:8080/
```

Das Frontend ist der vorgesehene Einstiegspunkt für die lokale Anwendung.

Nginx leitet die API-Anfragen intern an die Backend-Services weiter.

Das PowerShell-Fenster mit dem Port-Forwarding muss geöffnet bleiben.

Beenden mit `Ctrl+C`.

### 15.2 Employee Service direkt erreichen

```powershell
kubectl port-forward -n employee-backend service/employee-service 18081:8081
```

Lokaler Zugriff:

```text
http://localhost:18081/
```

Dieser Zugriff ist insbesondere für technische Prüfungen wie die Actuator-Health-Endpunkte vorgesehen.

### 15.3 Auth Service direkt erreichen

```powershell
kubectl port-forward -n employee-backend service/auth-service 18082:8082
```

Lokaler Zugriff:

```text
http://localhost:18082/
```

### 15.4 Datenbank-Port-Forwarding

Ein direkter Datenbankzugriff kann bei Bedarf über Port-Forwarding hergestellt werden.

Employee MySQL:

```powershell
kubectl port-forward -n employee-database service/employee-mysql 13306:3306
```

Auth MySQL:

```powershell
kubectl port-forward -n employee-database service/auth-mysql 13307:3306
```

Diese Befehle stellen lediglich eine lokale TCP-Weiterleitung bereit. Für eine Datenbankverbindung werden weiterhin gültige Zugangsdaten benötigt.

## 16. Container-Images

Die lokale Kubernetes-Umgebung verwendet folgende Images:

| Komponente | Image |
|---|---|
| Frontend | `employee-frontend:1.0.0` |
| Employee Service | `employee-service:1.0.4` |
| Auth Service | `auth-service:1.0.5` |
| MySQL | MySQL 8.4 |

Die lokalen Anwendungsimages werden in Docker Desktop gebaut und für die lokalen Deployments verwendet.

Die Deployments verwenden für die lokalen Anwendungsimages `imagePullPolicy: IfNotPresent`.

### 16.1 Frontend-Image bauen

```powershell
docker build -t employee-frontend:1.0.0 .\employee-frontend
```

### 16.2 Employee-Service-Image bauen

```powershell
docker build -t employee-service:1.0.4 .\employee-service
```

### 16.3 Auth-Service-Image bauen

```powershell
docker build -t auth-service:1.0.5 .\auth-service
```

Nach einer Änderung am Anwendungscode muss das entsprechende Image neu gebaut und das Deployment mit dem passenden Image-Tag aktualisiert werden.

Die Versionsnummern müssen dabei konsistent in den Image-Referenzen der Kubernetes-Manifeste und beim Build verwendet werden.

## 17. Ressourcenanforderungen und Limits

Die Workloads besitzen konfigurierte CPU- und Speicheranforderungen.

| Komponente | CPU Request | RAM Request | CPU Limit | RAM Limit |
|---|---:|---:|---:|---:|
| Frontend | 100m | 128Mi | 500m | 256Mi |
| Employee Service | 250m | 512Mi | 1 CPU | 1Gi |
| Auth Service | 250m | 512Mi | 1 CPU | 1Gi |
| Employee MySQL | 250m | 512Mi | 1 CPU | 1Gi |
| Auth MySQL | 250m | 512Mi | 1 CPU | 1Gi |

Die Requests beschreiben die Ressourcen, die Kubernetes für die Planung der Pods berücksichtigt.

Die Limits begrenzen den maximal zulässigen Ressourcenverbrauch der Container.

### 17.1 Ressourcen eines Deployments anzeigen

```powershell
kubectl describe deployment employee-service -n employee-backend
```

### 17.2 Ressourcen eines Pods anzeigen

```powershell
kubectl describe pod <POD-NAME> -n employee-backend
```

### 17.3 Tatsächlichen Verbrauch prüfen

```powershell
kubectl top pods -A
```

Die tatsächlichen Verbrauchswerte können abhängig von Last, Startphase und Datenbankaktivität variieren.

## 18. ConfigMaps

ConfigMaps enthalten nicht vertrauliche Konfigurationswerte.

Die Frontend-Nginx-Konfiguration wird beispielsweise über eine ConfigMap eingebunden.

### 18.1 ConfigMaps anzeigen

```powershell
kubectl get configmaps -A
```

### 18.2 Frontend-Nginx-ConfigMap anzeigen

```powershell
kubectl describe configmap employee-frontend-nginx-config -n employee-frontend
```

### 18.3 ConfigMap-Inhalt ausgeben

```powershell
kubectl get configmap employee-frontend-nginx-config -n employee-frontend -o yaml
```

Die Nginx-Konfiguration wird im Frontend-Deployment über einen Volume Mount eingebunden.

Da die Datei mit `subPath` eingebunden ist, werden Änderungen nicht automatisch in einen bereits laufenden Container übernommen.

Nach einer Änderung muss das Frontend-Deployment neu gestartet werden:

```powershell
kubectl rollout restart deployment/employee-frontend -n employee-frontend
```

## 19. Datenbank-Backups

Vor Änderungen an Datenbankressourcen oder vor risikoreichen Wartungsarbeiten müssen geeignete Backups vorhanden sein.

Die lokalen Datenbank-Dumps befinden sich im Projektverzeichnis:

```text
backup/
├── people_hub.sql
└── people_hub_auth.sql
```

Die Dumps wurden aus den bestehenden Docker-Compose-Datenbanken erstellt und einmalig in die Kubernetes-Datenbanken importiert.

Ein erneuter Import ist im normalen Betrieb nicht erforderlich.

Die Docker-Compose-Datenbanken und ihre Volumes bleiben von der Kubernetes-Verwaltung getrennt.

**Wichtig:** Vorhandene Backup-Dateien nicht überschreiben, löschen oder erneut importieren, ohne den Zweck und die Auswirkungen zu prüfen.

## 20. Typische Fehler und Prüfungen

### 20.1 Pod startet nicht

Status prüfen:

```powershell
kubectl get pods -A
```

Details anzeigen:

```powershell
kubectl describe pod <POD-NAME> -n <NAMESPACE>
```

Logs anzeigen:

```powershell
kubectl logs <POD-NAME> -n <NAMESPACE>
```

Ereignisse anzeigen:

```powershell
kubectl get events -A --sort-by=.metadata.creationTimestamp
```

### 20.2 Image kann nicht gefunden werden

Mögliche Ursachen:

- Image wurde nicht gebaut.
- Image-Tag stimmt nicht mit dem Deployment überein.
- Image ist für die lokale Kubernetes-Runtime nicht verfügbar.
- Image-Pull-Policy oder Registry-Konfiguration ist nicht passend.

Deployment-Image prüfen:

```powershell
kubectl describe deployment employee-service -n employee-backend
```

### 20.3 Datenbank-Pod startet nicht

StatefulSet prüfen:

```powershell
kubectl get statefulsets -n employee-database
```

Pods prüfen:

```powershell
kubectl get pods -n employee-database
```

PersistentVolumeClaims prüfen:

```powershell
kubectl get pvc -n employee-database
```

Datenbank-Logs prüfen:

```powershell
kubectl logs statefulset/employee-mysql -n employee-database
```

```powershell
kubectl logs statefulset/auth-mysql -n employee-database
```

### 20.4 PersistentVolumeClaim bleibt Pending

StorageClasses prüfen:

```powershell
kubectl get storageclass
```

PVC-Details prüfen:

```powershell
kubectl describe pvc <PVC-NAME> -n employee-database
```

Bei `WaitForFirstConsumer` kann ein PVC zunächst `Pending` bleiben, bis ein zugehöriger Pod eingeplant wird.

### 20.5 Health Check schlägt fehl

Pod-Status prüfen:

```powershell
kubectl get pods -n employee-backend
```

Deployment-Ereignisse prüfen:

```powershell
kubectl describe deployment employee-service -n employee-backend
```

Anwendungslogs prüfen:

```powershell
kubectl logs deployment/employee-service -n employee-backend
```

Die Actuator-Endpunkte können zusätzlich über Port-Forwarding direkt geprüft werden.

### 20.6 Metrics Server liefert keine Werte

Deployment prüfen:

```powershell
kubectl get deployment metrics-server -n kube-system
```

Pod prüfen:

```powershell
kubectl get pods -n kube-system -l k8s-app=metrics-server
```

Logs prüfen:

```powershell
kubectl logs deployment/metrics-server -n kube-system
```

Anschließend erneut testen:

```powershell
kubectl top nodes
```

```powershell
kubectl top pods -A
```

### 20.7 Frontend ist nicht erreichbar

Zunächst prüfen, ob das Frontend-Pod läuft:

```powershell
kubectl get pods -n employee-frontend
```

Service prüfen:

```powershell
kubectl get service employee-frontend -n employee-frontend
```

Port-Forwarding starten:

```powershell
kubectl port-forward -n employee-frontend service/employee-frontend 8080:80
```

Im Browser öffnen:

```text
http://localhost:8080/
```

Das Port-Forwarding-Fenster muss während des Zugriffs geöffnet bleiben.

---

## 21. Wichtige Sicherheits- und Löschhinweise

Die folgenden Befehle können Ressourcen oder Daten dauerhaft entfernen.

Sie sind nicht Bestandteil des normalen Betriebsablaufs.

### 21.1 PVCs nicht unüberlegt löschen

```powershell
kubectl delete pvc <PVC-NAME> -n employee-database
```

Durch die lokale Reclaim Policy `Delete` können dabei persistente Daten verloren gehen.

### 21.2 Datenbank-Namespace nicht unüberlegt löschen

```powershell
kubectl delete namespace employee-database
```

Das Löschen des Namespace entfernt die darin verwalteten Ressourcen und kann den Zugriff auf die Datenbankdaten zerstören.

### 21.3 Gesamte Kubernetes-Ressourcen nicht unüberlegt löschen

```powershell
kubectl delete -k .\k8s\overlays\docker-desktop
```

Dieser Befehl kann auch Datenbankressourcen und PersistentVolumeClaims betreffen.

Vor einer solchen Operation müssen die Auswirkungen anhand der konkreten Manifeste und der vorhandenen Speicherressourcen geprüft werden.

**Niemals zur normalen Fehlerbehebung verwenden:**

- `docker system prune`
- `docker volume prune`
- Docker Desktop Factory Reset
- Löschen der WSL2-Distribution
- Löschen bestehender Docker-Compose-Volumes

Die bestehenden Docker-Compose-Datenbanken und deren Volumes müssen erhalten bleiben.

## 22. AWS-EKS-Vorbereitung

Die spätere AWS-EKS-Umgebung wird über ein separates Kustomize-Overlay vorbereitet.

Verzeichnis:

```text
k8s/overlays/aws-eks/
```

Aktueller Status:

- Overlay-Verzeichnis angelegt
- Kustomize-Konfiguration als Ausgangspunkt vorhanden
- Gemeinsame Kubernetes-Basis eingebunden
- Noch keine AWS-spezifische Bereitstellung durchgeführt
- Keine EKS-Tests durchgeführt
- Keine AWS-CLI-Konfiguration durchgeführt

Vor einem späteren Einsatz müssen insbesondere folgende Themen angepasst werden:

1. Container-Images in Amazon ECR bereitstellen und Image-Referenzen anpassen.
2. Persistenten Speicher über geeignete AWS-EBS-Konfigurationen vorbereiten.
3. Externe Erreichbarkeit über geeignete AWS-Load-Balancer- und Ingress-Ressourcen planen.
4. Secrets sicher verwalten.
5. Netzwerk- und Zugriffsregeln für EKS überprüfen.
6. Die TLS-Konfiguration des Metrics Servers produktionsgeeignet einrichten.
7. Ressourcenanforderungen und Skalierung für die Zielumgebung überprüfen.

Das bestehende AWS-Overlay ist ein Vorbereitungsschritt und keine freigegebene Produktionskonfiguration.

---

## 23. Zusammenfassung

Diese Befehlsreferenz beschreibt die wichtigsten Verwaltungs- und Diagnoseoperationen für die lokale Kubernetes-Umgebung des Employee-Management-Projekts.

Die zentrale Verwaltung erfolgt über `kubectl` und Kustomize.

Die Anwendung wird über drei Namespaces strukturiert:

- `employee-frontend`
- `employee-backend`
- `employee-database`

Die lokale Anwendung ist über den Frontend-Service und Port-Forwarding erreichbar.

Health Checks, Ressourcenverbrauch, Logs und Netzwerkregeln können mit den beschriebenen Befehlen überprüft werden.

Besondere Vorsicht ist bei persistenten Datenbankressourcen erforderlich. Änderungen oder Löschoperationen an PersistentVolumeClaims und Datenbankressourcen dürfen nur nach vorheriger Prüfung durchgeführt werden.

Die Dokumentation unterscheidet ausdrücklich zwischen dem bereits umgesetzten lokalen Kubernetes-Betrieb und der noch ausstehenden AWS-EKS-Bereitstellung.