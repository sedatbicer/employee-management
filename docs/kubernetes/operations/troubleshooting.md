# Troubleshooting

## 1. Zweck und Geltungsbereich

Dieses Dokument beschreibt die Diagnose und Behebung typischer Probleme in der lokalen Kubernetes-Umgebung des Employee-Management-Systems.

Die Umgebung basiert auf:

- Windows
- Docker Desktop
- WSL2
- Docker Desktop Kubernetes
- Kubernetes Context `docker-desktop`
- Kustomize
- Angular und Nginx
- Spring Boot
- MySQL 8.4

Das Dokument enthält konkrete Diagnosebefehle und mögliche Lösungswege.

Besonderer Wert wird auf den Schutz der bestehenden Datenbankdaten und Docker-Compose-Umgebung gelegt.

## 2. Allgemeine Diagnose

Bei Problemen sollte zunächst der Zustand des Clusters geprüft werden.

### 2.1 Kubernetes Context prüfen

```powershell
kubectl config current-context
```

Erwartete Ausgabe:

```text
docker-desktop
```

Ist ein anderer Context aktiv, sollten keine Änderungen vorgenommen werden, bevor das Zielcluster eindeutig geprüft wurde.

### 2.2 Node-Status prüfen

```powershell
kubectl get nodes
```

Der lokale Kubernetes-Node sollte den Status `Ready` besitzen.

### 2.3 Namespaces prüfen

```powershell
kubectl get namespaces
```

Erwartete Anwendungs-Namespaces:

```text
employee-frontend
employee-backend
employee-database
```

### 2.4 Alle Anwendungs-Pods anzeigen

```powershell
kubectl get pods -A
```

### 2.5 Deployments anzeigen

```powershell
kubectl get deployments -A
```

### 2.6 StatefulSets anzeigen

```powershell
kubectl get statefulsets -n employee-database
```

### 2.7 Services anzeigen

```powershell
kubectl get services -A
```

### 2.8 Events anzeigen

```powershell
kubectl get events -A --sort-by=.metadata.creationTimestamp
```

Events liefern häufig erste Hinweise auf fehlgeschlagene Starts, nicht verfügbare Images, Probe-Fehler oder Probleme bei der Ressourcenplanung.

## 3. Kubernetes-Node ist nicht Ready

### Symptome

- Pods starten nicht.
- Deployments bleiben unvollständig.
- Kubernetes-Befehle liefern Fehler.
- Der Node besitzt nicht den Status `Ready`.

### Diagnose

```powershell
kubectl get nodes
```

Details:

```powershell
kubectl describe node desktop-control-plane
```

Docker-Desktop-Kubernetes sollte außerdem in Docker Desktop als aktiv angezeigt werden.

### Mögliche Ursachen

- Kubernetes in Docker Desktop ist nicht vollständig gestartet.
- Docker Desktop hat zu wenig Ressourcen.
- WSL2 ist nicht verfügbar.
- Der Kubernetes-Control-Plane-Node befindet sich noch in der Initialisierung.

### Vorgehen

1. Docker Desktop öffnen.
2. Den Kubernetes-Status überprüfen.
3. Warten, bis der Cluster vollständig gestartet ist.
4. Den Node-Status erneut prüfen.

```powershell
kubectl get nodes
```

Ein Zurücksetzen der Docker-Desktop-Umgebung ist kein regulärer Troubleshooting-Schritt, da dabei bestehende Daten und Volumes gefährdet werden können.

## 4. Pod startet nicht

### Symptome

Ein Pod zeigt beispielsweise einen der folgenden Zustände:

```text
Pending
CrashLoopBackOff
ImagePullBackOff
ErrImagePull
CreateContainerConfigError
```

### Diagnose

```powershell
kubectl get pods -A
```

Pod-Details:

```powershell
kubectl describe pod POD_NAME -n NAMESPACE
```

Logs:

```powershell
kubectl logs POD_NAME -n NAMESPACE
```

Bei vorherigen Container-Starts:

```powershell
kubectl logs POD_NAME -n NAMESPACE --previous
```

### Vorgehen

1. Pod-Status prüfen.
2. Events im Abschnitt `Events` von `kubectl describe` kontrollieren.
3. Container-Logs prüfen.
4. Konfiguration, Secrets, Images und Ressourcenanforderungen kontrollieren.

Nicht jeder fehlgeschlagene Start erfordert das Löschen des Pods. Zunächst sollte die eigentliche Ursache ermittelt werden.

## 5. ImagePullBackOff oder ErrImagePull

### Symptome

Ein Pod kann das angegebene Container-Image nicht laden.

### Diagnose

```powershell
kubectl describe pod POD_NAME -n NAMESPACE
```

Image-Konfiguration anzeigen:

```powershell
kubectl get deployment DEPLOYMENT_NAME -n NAMESPACE -o yaml
```

### Mögliche Ursachen

- Das Image wurde lokal nicht gebaut.
- Der Image-Name stimmt nicht mit dem Deployment überein.
- Der Image-Tag stimmt nicht.
- Das Image ist nicht im Kubernetes-Container-Runtime verfügbar.
- Die Pull-Policy passt nicht zur lokalen Entwicklungsumgebung.

### Lokale Images prüfen

```powershell
docker images
```

### Backend-Images bauen

Employee Service:

```powershell
docker build -t employee-service:1.0.4 .\employee-service
```

Auth Service:

```powershell
docker build -t auth-service:1.0.5 .\auth-service
```

### Frontend-Image bauen

```powershell
docker build -t employee-frontend:1.0.0 .\employee-frontend
```

Die Images müssen im lokalen Docker-Desktop-Kubernetes-Kontext verfügbar sein.

Die Deployments verwenden für die lokale Umgebung:

```yaml
imagePullPolicy: IfNotPresent
```

Damit kann Kubernetes ein bereits lokal verfügbares Image verwenden.

Die Verfügbarkeit lokaler Images sollte nach Änderungen an Docker Desktop oder der Container-Runtime erneut geprüft werden.

## 6. Deployment bleibt nicht verfügbar

### Symptome

Ein Deployment zeigt weniger verfügbare Replikate als erwartet.

### Diagnose

```powershell
kubectl get deployments -A
```

Details:

```powershell
kubectl describe deployment DEPLOYMENT_NAME -n NAMESPACE
```

Pods:

```powershell
kubectl get pods -n NAMESPACE
```

Events:

```powershell
kubectl get events -n NAMESPACE --sort-by=.metadata.creationTimestamp
```

### Mögliche Ursachen

- Container startet nicht.
- Readiness Probe schlägt fehl.
- Image ist nicht verfügbar.
- Konfiguration fehlt.
- Ein benötigtes Secret existiert nicht.
- Ressourcen können nicht zugewiesen werden.

### Vorgehen

Die Deployment- und Pod-Details prüfen und die Ursache anhand der Events und Logs beheben.

Anschließend den Rollout kontrollieren:

```powershell
kubectl rollout status deployment/DEPLOYMENT_NAME -n NAMESPACE
```

## 7. Backend ist nicht erreichbar

### Symptome

- Frontend zeigt API-Fehler.
- API-Anfragen liefern Fehlerantworten.
- Employee- oder Auth-Funktionen funktionieren nicht.

### Diagnose

Employee Service:

```powershell
kubectl get pods -n employee-backend
```

```powershell
kubectl get service employee-service -n employee-backend
```

Auth Service:

```powershell
kubectl get service auth-service -n employee-backend
```

Logs:

```powershell
kubectl logs deployment/employee-service -n employee-backend
```

```powershell
kubectl logs deployment/auth-service -n employee-backend
```

### Service-Adressen prüfen

Employee Service:

```text
employee-service.employee-backend.svc.cluster.local:8081
```

Auth Service:

```text
auth-service.employee-backend.svc.cluster.local:8082
```

### Backend Health Checks prüfen

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

Die Health-Endpunkte wurden für beide Backend-Anwendungen erfolgreich geprüft.

## 8. Datenbankverbindung schlägt fehl

### Symptome

- Spring Boot meldet Verbindungsfehler.
- Backend-Anwendung startet nicht vollständig.
- SQL-Verbindungen schlagen fehl.
- Health Checks können fehlschlagen.

### Diagnose

Datenbank-Pods:

```powershell
kubectl get pods -n employee-database
```

Services:

```powershell
kubectl get services -n employee-database
```

Employee MySQL Logs:

```powershell
kubectl logs statefulset/employee-mysql -n employee-database
```

Auth MySQL Logs:

```powershell
kubectl logs statefulset/auth-mysql -n employee-database
```

### Verbindungsadressen prüfen

Employee Database:

```text
jdbc:mysql://employee-mysql.employee-database.svc.cluster.local:3306/people_hub
```

Auth Database:

```text
jdbc:mysql://auth-mysql.employee-database.svc.cluster.local:3306/people_hub_auth
```

### Secrets prüfen

```powershell
kubectl get secrets -n employee-database
```

```powershell
kubectl get secrets -n employee-backend
```

Die Secret-Namen müssen mit den Referenzen in den jeweiligen StatefulSets und Deployments übereinstimmen.

### StatefulSets prüfen

```powershell
kubectl describe statefulset employee-mysql -n employee-database
```

```powershell
kubectl describe statefulset auth-mysql -n employee-database
```

Bei Datenbankproblemen dürfen die PVCs nicht als erster Lösungsversuch gelöscht werden.

## 9. PersistentVolumeClaim bleibt Pending

### Symptome

Ein PVC zeigt:

```text
Pending
```

### Diagnose

```powershell
kubectl get pvc -n employee-database
```

Details:

```powershell
kubectl describe pvc employee-mysql-data -n employee-database
```

```powershell
kubectl describe pvc auth-mysql-data -n employee-database
```

StorageClass prüfen:

```powershell
kubectl get storageclass
```

### Mögliche Ursache

Die lokale StorageClass `standard` verwendet:

```text
WaitForFirstConsumer
```

Der PVC kann deshalb zunächst `Pending` bleiben, bis ein passender Pod eingeplant wird.

### Vorgehen

1. Prüfen, ob das zugehörige StatefulSet existiert.
2. Prüfen, ob der Datenbank-Pod eingeplant werden kann.
3. Events des PVC und des StatefulSets kontrollieren.
4. Den Bindungsstatus erneut prüfen.

```powershell
kubectl get pvc -n employee-database
```

Ein PVC im Status `Pending` bedeutet bei dieser StorageClass nicht automatisch, dass ein Fehler vorliegt.

**Wichtig:** PVCs nicht löschen, um einen Pending-Status zu beheben.

## 10. Datenbank-Pod startet nicht

### Diagnose

```powershell
kubectl get pods -n employee-database
```

```powershell
kubectl describe pod POD_NAME -n employee-database
```

```powershell
kubectl logs POD_NAME -n employee-database
```

### Mögliche Ursachen

- Datenbank benötigt mehr Zeit für die Initialisierung.
- Secret fehlt oder enthält falsche Zugangsdaten.
- Persistenter Speicher ist nicht verfügbar.
- MySQL kann nicht auf seine Daten zugreifen.
- Ressourcen reichen nicht aus.

### Startup Probe berücksichtigen

Die MySQL-StatefulSets verwenden eine Startup Probe mit:

- Prüfintervall: 5 Sekunden
- Failure Threshold: 60

Damit wird eine Initialisierungszeit von bis zu ungefähr fünf Minuten berücksichtigt.

Ein vorübergehender Probe-Fehler während des Starts muss deshalb nicht automatisch auf einen dauerhaften Fehler hinweisen.

## 11. Frontend ist nicht erreichbar

### Symptome

- Browser zeigt einen Verbindungsfehler.
- `localhost:8080` ist nicht erreichbar.
- Die Anwendung lädt nicht.

### Diagnose

Frontend-Pod:

```powershell
kubectl get pods -n employee-frontend
```

Frontend-Service:

```powershell
kubectl get service employee-frontend -n employee-frontend
```

Logs:

```powershell
kubectl logs deployment/employee-frontend -n employee-frontend
```

### Port-Forwarding prüfen

Die Anwendung wird lokal über folgenden Befehl bereitgestellt:

```powershell
kubectl port-forward -n employee-frontend service/employee-frontend 8080:80
```

Der Port-Forwarding-Prozess muss während des Zugriffs geöffnet bleiben.

Anschließend:

```text
http://localhost:8080/
```

Wenn das PowerShell-Fenster geschlossen oder der Prozess mit `Ctrl+C` beendet wird, endet die Weiterleitung.

Ein Verbindungsfehler auf `localhost:8080` kann daher auch einfach bedeuten, dass aktuell kein Port-Forwarding läuft.

## 12. API-Anfragen und CORS-Fehler

### Symptome

- Browser meldet `CORS`-Fehler.
- API-Anfragen liefern `403 Invalid CORS request`.
- Login funktioniert nicht.

### Aktuelle Architektur

Die Angular-Anwendung verwendet relative API-Pfade:

```text
/api/employees
/api/auth
```

Nginx leitet diese Anfragen intern an die Backend-Services weiter.

Der vorgesehene Zugriffsweg ist:

```text
Browser
  -> Frontend / Nginx
  -> Backend Service
```

Die Spring-Security-Konfiguration verwendet:

```java
.cors(cors -> cors.disable())
```

### Vorgehen

1. Prüfen, ob das Frontend über `http://localhost:8080/` aufgerufen wird.
2. Prüfen, ob der Frontend-Port-Forwarding-Prozess läuft.
3. Nginx-Konfiguration prüfen.
4. Backend-Logs kontrollieren.
5. Sicherstellen, dass API-Aufrufe die relativen `/api/...`-Pfade verwenden.

Ein direkter Zugriff über eine separat gestartete Angular-Entwicklungsumgebung auf Port 4200 ist mit der aktuellen CORS-Konfiguration nicht als unterstützter Zugriffsweg vorgesehen.

Die ConfigMap `backend-cors-config` ist noch vorhanden, enthält aber derzeit keine aktive CORS-Konfiguration.

## 13. NetworkPolicy blockiert Kommunikation

### Symptome

- Frontend erreicht das Backend nicht.
- Backend erreicht die Datenbank nicht.
- Services sind vorhanden, aber Verbindungen schlagen fehl.

### Diagnose

Policies anzeigen:

```powershell
kubectl get networkpolicies -n employee-frontend
```

```powershell
kubectl get networkpolicies -n employee-backend
```

```powershell
kubectl get networkpolicies -n employee-database
```

Details anzeigen:

```powershell
kubectl describe networkpolicy backend-allow-frontend -n employee-backend
```

```powershell
kubectl describe networkpolicy employee-mysql-allow-employee-service -n employee-database
```

### Kommunikationswege kontrollieren

Die vorgesehenen Verbindungen sind:

| Quelle | Ziel | Port |
|---|---|---:|
| Frontend | Employee Service | 8081 |
| Frontend | Auth Service | 8082 |
| Employee Service | Employee MySQL | 3306 |
| Auth Service | Auth MySQL | 3306 |

### Wichtige Einschränkung

Die NetworkPolicies wurden angewendet und die Anwendung funktioniert über den vorgesehenen Frontend-Zugriffsweg.

Ein gezielter Negativtest mit einem separaten Test-Pod wurde jedoch nicht durchgeführt.

Die tatsächliche Durchsetzung der NetworkPolicies wurde deshalb nicht vollständig funktional nachgewiesen.

## 14. Metrics Server liefert keine Metriken

### Symptome

Folgende Befehle liefern Fehler oder keine Metriken:

```powershell
kubectl top nodes
```

```powershell
kubectl top pods -A
```

### Diagnose

```powershell
kubectl get deployment metrics-server -n kube-system
```

```powershell
kubectl get pods -n kube-system
```

```powershell
kubectl logs deployment/metrics-server -n kube-system
```

### Bekannter Fehler in Docker Desktop

Die lokale Docker-Desktop-Umgebung hatte ein Kubelet-Zertifikat ohne passende IP-SAN-Einträge.

Der Metrics Server meldete:

```text
x509: cannot validate certificate for 172.19.0.4 because it doesn't contain any IP SANs
```

### Lokale Lösung

Für die lokale Entwicklungsumgebung wurde folgende Option im Metrics-Server-Deployment ergänzt:

```text
--kubelet-insecure-tls
```

Prüfen:

```powershell
kubectl get deployment metrics-server -n kube-system -o jsonpath="{.spec.template.spec.containers[0].args}"
```

Anschließend:

```powershell
kubectl rollout status deployment/metrics-server -n kube-system
```

Die Option deaktiviert die Prüfung der Kubelet-Zertifikate.

Sie ist nur eine lokale Entwicklungsmaßnahme und darf nicht unverändert in AWS EKS oder einer produktiven Umgebung verwendet werden.

## 15. ConfigMap-Änderungen werden nicht übernommen

### Symptome

- Eine geänderte Nginx-Konfiguration wird nicht aktiv.
- Die Anwendung verhält sich weiterhin wie zuvor.

### Hintergrund

Die Nginx-Konfiguration wird aus einer ConfigMap eingebunden.

Die ConfigMap-Datei wird über einen Mount mit `subPath` bereitgestellt.

Änderungen an der ConfigMap werden bei dieser Art des Mounts nicht automatisch in der bereits laufenden Anwendung sichtbar.

### Diagnose

ConfigMap prüfen:

```powershell
kubectl get configmap employee-frontend-nginx-config -n employee-frontend -o yaml
```

Frontend-Deployment prüfen:

```powershell
kubectl describe deployment employee-frontend -n employee-frontend
```

### Vorgehen

Nach einer geprüften ConfigMap-Änderung kann der Frontend-Rollout neu gestartet werden:

```powershell
kubectl rollout restart deployment/employee-frontend -n employee-frontend
```

Danach den Rollout prüfen:

```powershell
kubectl rollout status deployment/employee-frontend -n employee-frontend
```

Ein Neustart sollte erst erfolgen, nachdem die Konfiguration geprüft wurde.

## 16. Resource-Probleme

### Symptome

- Pods bleiben Pending.
- Container werden mit `OOMKilled` beendet.
- Anwendungen reagieren langsam.
- Probes schlagen unter Last fehl.

### Diagnose

Node-Auslastung:

```powershell
kubectl top nodes
```

Pod-Auslastung:

```powershell
kubectl top pods -A
```

Ressourcen eines Deployments:

```powershell
kubectl describe deployment DEPLOYMENT_NAME -n NAMESPACE
```

Events:

```powershell
kubectl get events -A --sort-by=.metadata.creationTimestamp
```

### Bekannte Ressourcenwerte

| Komponente | CPU Request | RAM Request | CPU Limit | RAM Limit |
|---|---:|---:|---:|---:|
| Employee Service | 250m | 512Mi | 1 | 1Gi |
| Auth Service | 250m | 512Mi | 1 | 1Gi |
| Employee MySQL | 250m | 512Mi | 1 | 1Gi |
| Auth MySQL | 250m | 512Mi | 1 | 1Gi |
| Frontend | 100m | 128Mi | 500m | 256Mi |

Die Datenbank-Pods hatten bei einer früheren Messung jeweils ungefähr 450Mi RAM genutzt.

Das ist ein hoher Anteil des konfigurierten Requests von 512Mi.

Eine Anpassung sollte auf Basis weiterer Messungen und des tatsächlichen Lastverhaltens erfolgen.

## 17. Kustomize-Fehler

### Symptome

- `kubectl kustomize` schlägt fehl.
- Ressourcen werden nicht gefunden.
- Kustomize kann eine Datei nicht laden.
- Ein Overlay enthält nicht die erwarteten Ressourcen.

### Basis prüfen

```powershell
kubectl kustomize .\k8s\base
```

### Docker-Desktop-Overlay prüfen

```powershell
kubectl kustomize .\k8s\overlays\docker-desktop
```

### AWS-EKS-Overlay

Das AWS-EKS-Overlay ist derzeit eine Vorlage und wurde nicht für eine AWS-Umgebung getestet.

### Serverseitige Validierung der lokalen Umgebung

```powershell
kubectl apply --dry-run=server -k .\k8s\overlays\docker-desktop
```

Dieser Befehl prüft die Ressourcen gegen die Kubernetes-API, ohne sie anzuwenden.

Die serverseitige Validierung der lokalen Basis und des Docker-Desktop-Overlays wurde erfolgreich durchgeführt.

## 18. Login funktioniert nicht

### Diagnose

Frontend-Zugriff prüfen:

```text
http://localhost:8080/
```

Auth-Pod prüfen:

```powershell
kubectl get pods -n employee-backend
```

Auth-Logs prüfen:

```powershell
kubectl logs deployment/auth-service -n employee-backend
```

Auth-Service prüfen:

```powershell
kubectl get service auth-service -n employee-backend
```

Health-Endpunkt prüfen:

```powershell
kubectl port-forward -n employee-backend service/auth-service 18082:8082
```

In einem zweiten PowerShell-Fenster:

```powershell
Invoke-RestMethod http://localhost:18082/actuator/health/readiness
```

### Mögliche Ursachen

- Auth Service ist nicht verfügbar.
- Datenbankverbindung fehlt.
- Secret-Werte stimmen nicht.
- Der initiale Admin-Zugang ist nicht korrekt konfiguriert.
- Die Anfrage wird nicht über den Nginx-Reverse-Proxy geleitet.

Vertrauliche Zugangsdaten dürfen bei der Fehlersuche nicht in Logs oder Screenshots veröffentlicht werden.

## 19. Vorsicht bei destruktiven Befehlen

Einige Kubernetes- und Docker-Befehle können Daten dauerhaft löschen.

Insbesondere sollten folgende Operationen nicht als allgemeine Problemlösung verwendet werden:

```powershell
kubectl delete pvc
```

```powershell
kubectl delete namespace employee-database
```

```powershell
docker volume prune
```

```powershell
docker system prune
```

Auch ein Zurücksetzen von Docker Desktop oder das Entfernen der WSL2-Distribution kann Daten gefährden.

### Schutzregeln

- Vor Änderungen immer den aktuellen Context prüfen.
- Vor Datenbankoperationen den Zustand der PVCs kontrollieren.
- Vor destruktiven Aktionen ein überprüftes Backup sicherstellen.
- Die ursprünglichen Docker-Compose-Volumes erhalten.
- Kubernetes-Datenbank-PVCs nicht unüberlegt löschen.
- Keine SQL-Dumps erneut importieren, ohne den Datenbestand zu prüfen.

## 20. Nützliche Diagnosebefehle

### Cluster

```powershell
kubectl config current-context
kubectl get nodes
kubectl get namespaces
kubectl cluster-info
```

### Pods und Deployments

```powershell
kubectl get pods -A
kubectl get deployments -A
kubectl get statefulsets -A
kubectl get services -A
```

### Logs und Events

```powershell
kubectl logs POD_NAME -n NAMESPACE
kubectl describe pod POD_NAME -n NAMESPACE
kubectl get events -A --sort-by=.metadata.creationTimestamp
```

### Ressourcen

```powershell
kubectl top nodes
kubectl top pods -A
```

### Persistenz

```powershell
kubectl get pvc -n employee-database
kubectl get pv
kubectl get storageclass
```

### NetworkPolicies

```powershell
kubectl get networkpolicies -A
```

### Kustomize

```powershell
kubectl kustomize .\k8s\base
kubectl kustomize .\k8s\overlays\docker-desktop
```

### Rollouts

```powershell
kubectl rollout status deployment/DEPLOYMENT_NAME -n NAMESPACE
kubectl rollout history deployment/DEPLOYMENT_NAME -n NAMESPACE
```

## 21. Zusammenfassung

Die Fehlersuche sollte grundsätzlich mit der Überprüfung des Cluster-Zustands, der Pods, Deployments, Services, Events und Logs beginnen.

Für die lokale Umgebung sind insbesondere folgende Besonderheiten relevant:

- Der Kubernetes Context muss `docker-desktop` sein.
- Lokale Container-Images müssen für Kubernetes verfügbar sein.
- Das Frontend wird über Port-Forwarding auf Port 8080 aufgerufen.
- Die Backend-Anwendungen verwenden Actuator Health Checks.
- Die MySQL-Datenbanken verwenden persistente Volumes.
- Die StorageClass `standard` verwendet `WaitForFirstConsumer`.
- Der Metrics Server benötigt lokal `--kubelet-insecure-tls`.
- Die Nginx-Konfiguration wird über eine ConfigMap mit `subPath` eingebunden.
- Die NetworkPolicy-Durchsetzung wurde nicht mit einem gezielten Negativtest nachgewiesen.

Die wichtigste Schutzmaßnahme bei der Fehlersuche ist, Datenbank-PVCs und bestehende Docker-Compose-Volumes nicht als einfache Reparaturmaßnahme zu löschen.