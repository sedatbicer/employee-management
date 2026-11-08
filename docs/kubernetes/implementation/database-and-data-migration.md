# Database and Data Migration

## 1. Zweck und Geltungsbereich

Dieses Dokument beschreibt die Datenbankarchitektur des Employee-Management-Systems innerhalb der lokalen Kubernetes-Umgebung.

Es dokumentiert:

- Die beiden MySQL-Datenbanken
- Die Kubernetes StatefulSets
- Die Kubernetes Services
- Die PersistentVolumeClaims
- Die StorageClass
- Die Verwaltung der Zugangsdaten
- Die Erstellung der Datenbank-Backups
- Die Migration der bestehenden Daten
- Die Überprüfung der Datenübernahme
- Die Sicherung und den Schutz persistenter Daten

Ein besonderer Schwerpunkt liegt auf dem Schutz der bestehenden Docker-Compose-Datenbanken und ihrer Volumes.

Die ursprüngliche Docker-Compose-Umgebung bleibt von der Kubernetes-Umgebung getrennt.

## 2. Datenbankarchitektur

Das Employee-Management-System verwendet zwei separate MySQL-Datenbanken.

| Datenbank | Datenbankname | Zuständiger Service |
|---|---|---|
| Employee Database | `people_hub` | Employee Service |
| Auth Database | `people_hub_auth` | Auth Service |

Beide Datenbanken werden mit MySQL 8.4 betrieben.

Sie befinden sich im Kubernetes-Namespace:

```text
employee-database
```

Die Datenbanken werden jeweils als eigener Kubernetes StatefulSet betrieben.

### 2.1 Architekturübersicht

```text
                  employee-backend
                         |
              +----------+----------+
              |                     |
              v                     v
      +---------------+     +---------------+
      | Employee      |     | Auth Service  |
      | Service       |     |               |
      +---------------+     +---------------+
              |                     |
              | TCP 3306            | TCP 3306
              |                     |
              v                     v
      +---------------+     +---------------+
      | Employee      |     | Auth MySQL    |
      | MySQL         |     |               |
      |               |     |               |
      | people_hub    |     | people_hub_   |
      |               |     | auth          |
      +---------------+     +---------------+
              |                     |
              v                     v
      +---------------+     +---------------+
      | employee-     |     | auth-mysql-   |
      | mysql-data    |     | data          |
      | PVC           |     | PVC           |
      +---------------+     +---------------+
```

Die beiden Datenbankinstanzen sind logisch und technisch voneinander getrennt.

## 3. Employee MySQL

### 3.1 Technische Eigenschaften

| Eigenschaft | Wert |
|---|---|
| Datenbankserver | MySQL 8.4 |
| StatefulSet | `employee-mysql` |
| Kubernetes Service | `employee-mysql` |
| Namespace | `employee-database` |
| Datenbankname | `people_hub` |
| Container-Port | 3306 |
| PersistentVolumeClaim | `employee-mysql-data` |
| Speichergröße | 2Gi |
| Access Mode | `ReadWriteOnce` |
| Replicas | 1 |

### 3.2 Interne Service-Adresse

Der Employee Service erreicht die Datenbank über:

```text
employee-mysql.employee-database.svc.cluster.local
```

Die JDBC-Verbindung lautet:

```text
jdbc:mysql://employee-mysql.employee-database.svc.cluster.local:3306/people_hub
```

### 3.3 Kubernetes-Ressourcen

Die zugehörigen Manifeste befinden sich unter:

```text
k8s/base/database/employee-mysql/
```

Die Ressourcen sind:

```text
employee-mysql/
├── pvc.yaml
├── service.yaml
├── statefulset.yaml
└── kustomization.yaml
```

Der PersistentVolumeClaim stellt den dauerhaften Speicher bereit.

Der Service stellt einen stabilen Netzwerk-Endpunkt bereit.

Das StatefulSet verwaltet den MySQL-Pod.

## 4. Auth MySQL

### 4.1 Technische Eigenschaften

| Eigenschaft | Wert |
|---|---|
| Datenbankserver | MySQL 8.4 |
| StatefulSet | `auth-mysql` |
| Kubernetes Service | `auth-mysql` |
| Namespace | `employee-database` |
| Datenbankname | `people_hub_auth` |
| Container-Port | 3306 |
| PersistentVolumeClaim | `auth-mysql-data` |
| Speichergröße | 2Gi |
| Access Mode | `ReadWriteOnce` |
| Replicas | 1 |

### 4.2 Interne Service-Adresse

Der Auth Service erreicht die Datenbank über:

```text
auth-mysql.employee-database.svc.cluster.local
```

Die JDBC-Verbindung lautet:

```text
jdbc:mysql://auth-mysql.employee-database.svc.cluster.local:3306/people_hub_auth
```

### 4.3 Kubernetes-Ressourcen

Die zugehörigen Manifeste befinden sich unter:

```text
k8s/base/database/auth-mysql/
```

Die Ressourcen sind:

```text
auth-mysql/
├── pvc.yaml
├── service.yaml
├── statefulset.yaml
└── kustomization.yaml
```

## 5. Persistente Speicherung

### 5.1 Zweck

Container-Dateisysteme sind grundsätzlich nicht als dauerhafter Speicher für Datenbankdaten vorgesehen.

Werden Daten ausschließlich innerhalb des Container-Dateisystems gespeichert, können sie bei einer Neuerstellung des Containers verloren gehen.

Deshalb verwenden die MySQL-Instanzen PersistentVolumeClaims.

Die PVCs stellen den Speicher unabhängig vom Lebenszyklus des jeweiligen Datenbank-Pods bereit.

### 5.2 Verwendete PersistentVolumeClaims

| PVC | Namespace | Speichergröße |
|---|---|---:|
| `employee-mysql-data` | `employee-database` | 2Gi |
| `auth-mysql-data` | `employee-database` | 2Gi |

Beide PVCs verwenden:

```text
Access Mode: ReadWriteOnce
StorageClass: standard
```

### 5.3 StorageClass

Die lokale Kubernetes-Umgebung verwendet die StorageClass:

```text
standard
```

Die zugrunde liegende Bereitstellung erfolgt über:

```text
rancher.io/local-path
```

Die StorageClass verwendet die Reclaim Policy:

```text
Delete
```

Die Volume-Bindung erfolgt mit:

```text
WaitForFirstConsumer
```

Das bedeutet, dass ein PersistentVolumeClaim zunächst den Status `Pending` haben kann, bis ein passender Pod eingeplant wird.

### 5.4 Bedeutung der Reclaim Policy

Die Reclaim Policy `Delete` ist bei der Verwaltung der Datenbankressourcen besonders wichtig.

Wird ein PersistentVolumeClaim gelöscht, kann Kubernetes das zugehörige PersistentVolume und die zugrunde liegenden Daten ebenfalls entfernen.

Deshalb dürfen die beiden Datenbank-PVCs nicht unüberlegt gelöscht werden.

Folgende Befehle können Datenverlust verursachen:

```powershell
kubectl delete pvc employee-mysql-data -n employee-database
```

```powershell
kubectl delete pvc auth-mysql-data -n employee-database
```

Auch das Löschen des gesamten Datenbank-Namespace kann die persistenten Daten gefährden:

```powershell
kubectl delete namespace employee-database
```

Solche Operationen sind keine normalen Wartungsschritte.

## 6. Datenbank-Zugangsdaten

Die Zugangsdaten der beiden MySQL-Instanzen werden über Kubernetes Secrets bereitgestellt.

### 6.1 Employee MySQL

Secret:

```text
employee-mysql-credentials
```

Namespace:

```text
employee-database
```

Verwendete Schlüssel:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_USER`
- `MYSQL_PASSWORD`

### 6.2 Auth MySQL

Secret:

```text
auth-mysql-credentials
```

Namespace:

```text
employee-database
```

Verwendete Schlüssel:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_USER`
- `MYSQL_PASSWORD`

### 6.3 Backend-Zugangsdaten

Die Backend-Services verwenden separate Secrets im Namespace `employee-backend`.

Employee Service:

```text
employee-service-secrets
```

Enthaltene Schlüssel:

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`

Auth Service:

```text
auth-service-secrets
```

Enthaltene Schlüssel:

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `AUTH_ADMIN_EMAIL`
- `AUTH_ADMIN_PASSWORD`

Die Secrets werden den Anwendungen als Umgebungsvariablen bereitgestellt.

### 6.4 Sicherheitshinweis

Kubernetes Secrets sind standardmäßig Base64-kodiert.

Base64 stellt keine Verschlüsselung dar.

Für eine produktive Umgebung müssen insbesondere folgende Maßnahmen berücksichtigt werden:

- Verschlüsselung der Secret-Daten im Kubernetes-Cluster
- Beschränkung des Zugriffs über RBAC
- Sichere Verwaltung der Zugangsdaten
- Keine Speicherung von Zugangsdaten in Git
- Keine Ausgabe vertraulicher Secret-Werte in Logs

## 7. Datenmigration aus Docker Compose

### 7.1 Ausgangssituation

Vor der Kubernetes-Implementierung wurden die beiden MySQL-Datenbanken innerhalb der bestehenden Docker-Compose-Umgebung betrieben.

Die vorhandenen Daten mussten in die neuen Kubernetes-Datenbanken übernommen werden.

Die ursprünglichen Datenbanken und ihre Docker-Compose-Volumes sollten dabei unverändert erhalten bleiben.

Die Migration wurde deshalb über SQL-Dumps durchgeführt.

### 7.2 Verwendete Backup-Dateien

Die Datenbank-Dumps befinden sich im Projektverzeichnis:

```text
backup/
├── people_hub.sql
└── people_hub_auth.sql
```

Die dokumentierten Dateigrößen zum Zeitpunkt der Erstellung waren:

| Datei | Größe |
|---|---:|
| `people_hub.sql` | 6.848 Bytes |
| `people_hub_auth.sql` | 4.872 Bytes |

Die Backup-Dateien sind in der `.gitignore` berücksichtigt.

Sie werden daher nicht regulär in das Git-Repository übernommen.

### 7.3 Backup der Employee-Datenbank

Der Dump wurde aus dem bestehenden Docker-Compose-Container erstellt.

Verwendeter Befehl:

```powershell
docker exec people-hub-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --events --no-tablespaces --set-gtid-purged=OFF people_hub' > .\backup\people_hub.sql
```

### 7.4 Backup der Auth-Datenbank

Der Dump wurde ebenfalls aus dem bestehenden Docker-Compose-Container erstellt.

Verwendeter Befehl:

```powershell
docker exec people-hub-auth-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --events --no-tablespaces --set-gtid-purged=OFF people_hub_auth' > .\backup\people_hub_auth.sql
```

### 7.5 Bedeutung der Backup-Optionen

| Option | Bedeutung |
|---|---|
| `--single-transaction` | Erstellt für transaktionale Tabellen einen konsistenten Snapshot |
| `--routines` | Berücksichtigt gespeicherte Prozeduren und Funktionen |
| `--triggers` | Berücksichtigt Trigger |
| `--events` | Berücksichtigt MySQL Events |
| `--no-tablespaces` | Lässt Tablespace-Informationen aus |
| `--set-gtid-purged=OFF` | Verhindert das Setzen der GTID-Purged-Anweisung |

Die Option `--single-transaction` ersetzt keine vollständige Wartungsplanung, wenn während des Dumps gleichzeitig Schemaänderungen oder nichttransaktionale Schreiboperationen stattfinden.

## 8. Import der Daten in Kubernetes

Die beiden SQL-Dumps wurden einmalig in die entsprechenden Kubernetes-MySQL-Datenbanken importiert.

Dabei wurde die Employee-Datenbank in `people_hub` und die Auth-Datenbank in `people_hub_auth` übernommen.

Die Migration wurde abgeschlossen und überprüft.

Ein erneuter Import der bestehenden Dumps ist nicht erforderlich.

### 8.1 Trennung der Datenumgebungen

Nach der Migration existieren zwei voneinander getrennte Datenbankumgebungen:

**Docker Compose:**

- Ursprüngliche MySQL-Container
- Bestehende Docker-Compose-Volumes
- Ursprüngliche Datenbestände

**Kubernetes:**

- `employee-mysql` StatefulSet
- `auth-mysql` StatefulSet
- Kubernetes PersistentVolumeClaims
- Übernommene Datenbankinhalte

Die beiden Umgebungen verwenden getrennte Speicherressourcen.

Die Kubernetes-Datenbanken ersetzen nicht automatisch die ursprünglichen Docker-Compose-Datenbanken.

## 9. Überprüfung der Datenbanken

### 9.1 StatefulSets prüfen

```powershell
kubectl get statefulsets -n employee-database
```

Beide StatefulSets sollen den erwarteten Status aufweisen.

### 9.2 Datenbank-Pods prüfen

```powershell
kubectl get pods -n employee-database
```

Die MySQL-Pods sollen den Status `Running` anzeigen.

### 9.3 Services prüfen

```powershell
kubectl get services -n employee-database
```

Erwartete Services:

```text
employee-mysql
auth-mysql
```

### 9.4 PVCs prüfen

```powershell
kubectl get pvc -n employee-database
```

Beide PersistentVolumeClaims sollen den Status `Bound` besitzen.

### 9.5 Datenbank-Logs prüfen

Employee MySQL:

```powershell
kubectl logs statefulset/employee-mysql -n employee-database
```

Auth MySQL:

```powershell
kubectl logs statefulset/auth-mysql -n employee-database
```

Die Logs helfen bei der Überprüfung des Startvorgangs und der Fehleranalyse.

### 9.6 Verbindung über Port-Forwarding

Employee MySQL:

```powershell
kubectl port-forward -n employee-database service/employee-mysql 13306:3306
```

Auth MySQL:

```powershell
kubectl port-forward -n employee-database service/auth-mysql 13307:3306
```

Die Verbindung kann anschließend über einen geeigneten MySQL-Client hergestellt werden.

Dazu werden die jeweils gültigen Zugangsdaten benötigt.

Die Port-Forwarding-Prozesse müssen während der Verbindung geöffnet bleiben.

## 10. MySQL Health Checks

Die MySQL-StatefulSets verwenden Kubernetes-Probes zur Überwachung der Datenbankcontainer.

### 10.1 Startup Probe

Die Startup Probe prüft den Startzustand des MySQL-Servers.

Die Prüfung verwendet:

```text
mysqladmin ping
```

Die Konfiguration sieht eine Prüfperiode von fünf Sekunden und eine Failure Threshold von 60 vor.

Damit wird eine längere Initialisierungsphase berücksichtigt.

### 10.2 Liveness Probe

Die Liveness Probe prüft, ob der MySQL-Prozess weiterhin erreichbar ist.

Sie verwendet ebenfalls:

```text
mysqladmin ping
```

### 10.3 Readiness Probe

Die Readiness Probe prüft, ob der Datenbankcontainer Anfragen entgegennehmen kann.

Auch hierfür wird `mysqladmin ping` verwendet.

Die drei Probes helfen Kubernetes dabei, den Zustand der Datenbank-Pods zu überwachen.

## 11. Ressourcenverwaltung der Datenbanken

Beide MySQL-Instanzen besitzen dieselben Resource Requests und Limits.

| Ressource | Wert |
|---|---:|
| CPU Request | 250m |
| RAM Request | 512Mi |
| CPU Limit | 1 |
| RAM Limit | 1Gi |

Die Werte begrenzen den Ressourcenverbrauch und unterstützen die Ressourcenplanung des Kubernetes-Nodes.

Die tatsächliche Auslastung kann mit folgendem Befehl überprüft werden:

```powershell
kubectl top pods -n employee-database
```

Die Werte sind Momentaufnahmen und können sich mit der Datenbanklast verändern.

## 12. Backup- und Wiederherstellungsstrategie

Die vorhandenen SQL-Dumps dienen als Ausgangspunkt für die Datenmigration.

Sie stellen jedoch keine vollständige automatisierte Backup- und Recovery-Lösung für den laufenden Kubernetes-Betrieb dar.

Für einen produktiven Betrieb muss eine regelmäßige Datensicherungsstrategie eingerichtet werden.

Dazu gehören:

- Regelmäßige logische Datenbank-Backups
- Sicherung außerhalb des Kubernetes-Nodes
- Geschützte Aufbewahrung der Backup-Dateien
- Dokumentierte Wiederherstellungsprozesse
- Regelmäßige Wiederherstellungstests
- Festlegung von Recovery Point Objective (RPO)
- Festlegung von Recovery Time Objective (RTO)

Für eine spätere AWS-EKS-Umgebung müssen geeignete externe Speicher- und Sicherungslösungen vorgesehen werden.

Ein PersistentVolume allein ersetzt kein Backup.

## 13. Kritische Schutzmaßnahmen

Folgende Maßnahmen sind für den aktuellen Betrieb besonders wichtig:

### 13.1 Docker-Compose-Volumes erhalten

Die bestehenden Volumes der ursprünglichen Datenbankumgebung dürfen nicht gelöscht werden.

Insbesondere dürfen folgende Operationen nicht durchgeführt werden:

- Docker Volume Prune
- Docker System Prune
- Löschen der Docker-Compose-Volumes
- Zurücksetzen von Docker Desktop
- Entfernen der WSL2-Distribution

### 13.2 Kubernetes-PVCs erhalten

Die Kubernetes-PVCs enthalten die persistenten Daten der neuen Datenbankinstanzen.

Sie dürfen nicht unüberlegt gelöscht werden.

### 13.3 Keine erneute Migration ohne Anlass

Die Migration wurde bereits durchgeführt.

Ein erneuter Import derselben SQL-Dumps kann zu Inkonsistenzen oder doppelten Daten führen.

Vor einer erneuten Migration muss der Zustand beider Datenbankumgebungen geprüft werden.

### 13.4 Backups vertraulich behandeln

SQL-Dumps können personenbezogene und vertrauliche Daten enthalten.

Sie dürfen nicht öffentlich zugänglich gemacht oder ungeschützt weitergegeben werden.

## 14. Aktueller Implementierungsstatus

| Bereich | Status |
|---|---|
| Employee MySQL | Implementiert |
| Auth MySQL | Implementiert |
| StatefulSets | Implementiert |
| Kubernetes Services | Implementiert |
| PersistentVolumeClaims | Implementiert |
| StorageClass `standard` | Verwendet |
| Datenbank-Secrets | Erstellt |
| Datenbank-Probes | Implementiert |
| Resource Requests und Limits | Implementiert |
| SQL-Dumps erstellt | Erfolgt |
| Employee-Datenmigration | Durchgeführt |
| Auth-Datenmigration | Durchgeführt |
| Datenbank-Pods | Running |
| PVCs | Bound |
| Automatisierte regelmäßige Backups | Noch nicht eingerichtet |
| Dokumentierter Recovery-Test | Noch nicht durchgeführt |

## 15. Bekannte Einschränkungen

Die aktuelle Datenbankarchitektur ist für eine lokale Entwicklungsumgebung ausgelegt.

Folgende Punkte sind noch nicht Bestandteil der Implementierung:

- MySQL-Replikation
- Automatische Datenbank-Failover-Mechanismen
- Hochverfügbarkeit
- Automatisierte regelmäßige Backups
- Automatisierte Wiederherstellungstests
- Produktive externe Backup-Speicherung
- AWS-EBS-basierte Persistenz

Diese Funktionen müssen bei einer späteren produktiven Bereitstellung separat geplant und implementiert werden.

## 16. Zusammenfassung

Das Employee-Management-System verwendet zwei voneinander getrennte MySQL-8.4-Datenbankinstanzen.

Beide Datenbanken werden innerhalb des Kubernetes-Namespaces `employee-database` als StatefulSets betrieben.

Die Daten werden über PersistentVolumeClaims mit jeweils 2 GiB gespeichert.

Die bestehenden Docker-Compose-Datenbanken wurden über SQL-Dumps gesichert und einmalig in die Kubernetes-Datenbanken migriert.

Die ursprünglichen Docker-Compose-Volumes bleiben unverändert bestehen.

Die Kubernetes-Datenbanken verwenden eigene Secrets, Services, Health Checks und Resource Limits.

Besondere Vorsicht ist beim Löschen von PersistentVolumeClaims erforderlich, da die lokale StorageClass die Reclaim Policy `Delete` verwendet.

Die aktuelle Implementierung stellt eine funktionierende lokale Datenbankarchitektur dar. Eine produktive Backup-, Recovery- und Hochverfügbarkeitsstrategie ist noch gesondert umzusetzen.