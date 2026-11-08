# Monitoring and Resource Management

## 1. Zweck und Geltungsbereich

Dieses Dokument beschreibt das Monitoring und die Ressourcenverwaltung der lokalen Kubernetes-Umgebung des Employee-Management-Systems.

Es behandelt:

- Die Überwachung des Cluster-Zustands
- Die Überwachung von Pods und Deployments
- Die Installation und Konfiguration des Metrics Server
- Die Erfassung von CPU- und RAM-Auslastung
- Resource Requests und Limits
- Die tatsächliche Ressourcennutzung der Anwendungen
- Die Überprüfung der Health Checks
- Die Grenzen des aktuellen Monitorings
- Empfehlungen für eine spätere produktive Umgebung

Die beschriebenen Werte beziehen sich auf die lokale Docker-Desktop-Kubernetes-Umgebung.

## 2. Monitoring-Architektur

Die Kubernetes-Umgebung verwendet den Metrics Server zur Bereitstellung von Ressourcenmetriken.

Der Metrics Server sammelt grundlegende CPU- und Speichermetriken der Kubernetes-Nodes und Pods.

Diese Metriken können über die Kubernetes Metrics API abgefragt werden.

Die wichtigsten Werkzeuge sind:

| Werkzeug | Zweck |
|---|---|
| `kubectl get` | Ressourcenstatus anzeigen |
| `kubectl describe` | Details und Events anzeigen |
| `kubectl logs` | Container-Logs anzeigen |
| `kubectl top nodes` | Node-Ressourcenverbrauch anzeigen |
| `kubectl top pods` | Pod-Ressourcenverbrauch anzeigen |
| Metrics Server | CPU- und Speichermetriken bereitstellen |

Der Metrics Server ist kein vollständiges Monitoring- und Alerting-System.

## 3. Metrics Server

### 3.1 Zweck

Der Metrics Server stellt Ressourcenmetriken für Kubernetes bereit.

Er wird unter anderem für folgende Funktionen verwendet:

- Anzeige der CPU-Auslastung von Pods
- Anzeige des Speicherverbrauchs von Pods
- Anzeige der CPU-Auslastung von Nodes
- Anzeige des Speicherverbrauchs von Nodes
- Bereitstellung der Metrics API
- Grundlage für den Horizontal Pod Autoscaler (HPA)

Der Metrics Server ersetzt keine umfassende Monitoring-Plattform wie Prometheus und Grafana.

### 3.2 Installation

Der Metrics Server wurde mit dem offiziellen Kubernetes-SIGs-Manifest installiert.

Verwendeter Befehl:

```powershell
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

Installierte Version:

```text
v0.9.1
```

Namespace:

```text
kube-system
```

Deployment:

```text
metrics-server
```

### 3.3 Lokale TLS-Anpassung

Nach der Installation konnte der Metrics Server zunächst die Kubelet-Zertifikate der Docker-Desktop-Umgebung nicht verifizieren.

Es trat folgender Fehler auf:

```text
tls: failed to verify certificate: x509: cannot validate certificate for 172.19.0.4 because it doesn't contain any IP SANs
```

Die Ursache war, dass das verwendete Kubelet-Zertifikat keine passende IP-Adresse als Subject Alternative Name (SAN) enthielt.

Für die lokale Entwicklungsumgebung wurde deshalb folgende Option zum Metrics-Server-Deployment hinzugefügt:

```text
--kubelet-insecure-tls
```

Die Anpassung wurde direkt am Deployment vorgenommen.

Überprüfen lässt sich die Einstellung mit:

```powershell
kubectl get deployment metrics-server -n kube-system -o jsonpath="{.spec.template.spec.containers[0].args}"
```

Die Ausgabe muss die Option enthalten:

```text
--kubelet-insecure-tls
```

### 3.4 Sicherheitshinweis

Die Option `--kubelet-insecure-tls` deaktiviert die Zertifikatsprüfung bei der Verbindung des Metrics Servers zu den Kubelets.

Diese Einstellung ist ausschließlich eine lokale Übergangslösung für die Docker-Desktop-Entwicklungsumgebung.

Sie darf nicht unverändert in einer produktiven Umgebung oder in AWS EKS übernommen werden.

Für AWS EKS muss eine gültige und vertrauenswürdige Kubelet-TLS-Konfiguration verwendet werden.

### 3.5 Status prüfen

```powershell
kubectl get deployment metrics-server -n kube-system
```

```powershell
kubectl rollout status deployment/metrics-server -n kube-system
```

Die erfolgreiche Bereitstellung wurde überprüft.

## 4. Ressourcenmetriken anzeigen

### 4.1 Node-Auslastung

Mit folgendem Befehl wird die aktuelle Ressourcennutzung der Kubernetes-Nodes angezeigt:

```powershell
kubectl top nodes
```

Die Ausgabe enthält:

- CPU-Auslastung
- CPU-Auslastung in Prozent
- Speicherverbrauch
- Speicherauslastung in Prozent

Zum Zeitpunkt der Messung wurde für den Node `desktop-control-plane` ungefähr Folgendes angezeigt:

| Ressource | Messwert |
|---|---:|
| CPU | 205m |
| CPU-Auslastung | 3 % |
| RAM | 2.908Mi |
| RAM-Auslastung | 29 % |

Die Werte sind eine Momentaufnahme und können je nach laufenden Prozessen und Anwendungen variieren.

### 4.2 Pod-Auslastung

Alle Pods im Cluster:

```powershell
kubectl top pods -A
```

Pods eines bestimmten Namespace:

```powershell
kubectl top pods -n employee-frontend
```

```powershell
kubectl top pods -n employee-backend
```

```powershell
kubectl top pods -n employee-database
```

Die Ausgabe zeigt die aktuelle CPU- und Speichernutzung der Pods.

### 4.3 Nach Ressourcenverbrauch sortieren

CPU-Verbrauch:

```powershell
kubectl top pods -A --sort-by=cpu
```

Speicherverbrauch:

```powershell
kubectl top pods -A --sort-by=memory
```

Diese Befehle helfen dabei, ressourcenintensive Pods zu identifizieren.

## 5. Resource Requests und Limits

### 5.1 Grundprinzip

Kubernetes verwendet Resource Requests und Limits, um den Ressourcenbedarf von Containern zu beschreiben und zu begrenzen.

**Requests:**

Requests definieren die Ressourcen, die Kubernetes bei der Planung eines Pods berücksichtigt.

Sie beeinflussen insbesondere die Platzierung auf einem Node.

**Limits:**

Limits definieren die maximal erlaubte Ressourcennutzung eines Containers.

CPU-Limits können die verfügbare CPU-Zeit begrenzen.

Wird das Speicherlimit überschritten, kann der Container vom Betriebssystem beziehungsweise der Container-Laufzeit beendet werden. In Kubernetes ist dabei häufig der Status `OOMKilled` sichtbar.

### 5.2 Verwendete Ressourcenwerte

Die Backend-Services und Datenbankinstanzen verwenden dieselben Werte.

| Komponente | CPU Request | RAM Request | CPU Limit | RAM Limit |
|---|---:|---:|---:|---:|
| Employee Service | 250m | 512Mi | 1 | 1Gi |
| Auth Service | 250m | 512Mi | 1 | 1Gi |
| Employee MySQL | 250m | 512Mi | 1 | 1Gi |
| Auth MySQL | 250m | 512Mi | 1 | 1Gi |

Das Frontend verwendet:

| Komponente | CPU Request | RAM Request | CPU Limit | RAM Limit |
|---|---:|---:|---:|---:|
| Employee Frontend | 100m | 128Mi | 500m | 256Mi |

### 5.3 Gesamtbedarf

Die Requests der fünf Anwendungs-Workloads ergeben:

| Ressource | Summe |
|---|---:|
| CPU | 1.100m |
| RAM | 2.176Mi |

Die konfigurierten Limits ergeben:

| Ressource | Summe |
|---|---:|
| CPU | 4.500m |
| RAM | 4.352Mi |

Die Werte beschreiben die Summe der konfigurierten Ressourcen für die fünf Anwendungs-Workloads.

Sie berücksichtigen nicht den zusätzlichen Ressourcenverbrauch der Kubernetes-Systemkomponenten, des Metrics Servers oder anderer Prozesse auf dem Docker-Desktop-Node.

## 6. Tatsächliche Ressourcennutzung

### 6.1 Messwerte der Anwendungs-Pods

Zum Zeitpunkt der Messung wurden ungefähr folgende Werte mit `kubectl top pods -A` ermittelt:

| Pod / Komponente | CPU | RAM |
|---|---:|---:|
| Auth Service | 2m | 280Mi |
| Employee Service | 3m | 282Mi |
| Auth MySQL | 13m | 450Mi |
| Employee MySQL | 12m | 450Mi |
| Employee Frontend | 0m | 7Mi |

### 6.2 Interpretation

Die Messwerte zeigen:

- Das Frontend benötigt vergleichsweise wenig Arbeitsspeicher.
- Die beiden Spring-Boot-Anwendungen benötigen deutlich mehr RAM als das Frontend.
- Die MySQL-Instanzen verwenden den größten Anteil des Arbeitsspeichers der Anwendungs-Pods.
- Die CPU-Auslastung der fünf Anwendungs-Pods war zum Messzeitpunkt niedrig.

Die gemessene RAM-Nutzung der Datenbank-Pods lag bei ungefähr 450Mi pro Pod.

Das entspricht einem großen Anteil des jeweiligen RAM-Requests von 512Mi.

Diese Werte sollten bei einer späteren Anpassung der Ressourcen berücksichtigt werden.

### 6.3 Gesamtauslastung der Anwendungen

Die Summe der gemessenen Anwendungswerte betrug ungefähr:

| Ressource | Verbrauch |
|---|---:|
| CPU | 30m |
| RAM | 1.469Mi |

Diese Werte sind eine Momentaufnahme und keine garantierten Mindest- oder Maximalwerte.

Insbesondere können Speicherverbrauch und CPU-Last bei Datenbankoperationen, Anwendungsspitzen oder längeren Laufzeiten steigen.

## 7. Ressourcen konfigurieren und überprüfen

### 7.1 Deployment-Ressourcen anzeigen

Employee Service:

```powershell
kubectl describe deployment employee-service -n employee-backend
```

Auth Service:

```powershell
kubectl describe deployment auth-service -n employee-backend
```

Frontend:

```powershell
kubectl describe deployment employee-frontend -n employee-frontend
```

### 7.2 Datenbank-Ressourcen anzeigen

```powershell
kubectl describe statefulset employee-mysql -n employee-database
```

```powershell
kubectl describe statefulset auth-mysql -n employee-database
```

### 7.3 Ressourcenübersicht anzeigen

```powershell
kubectl get pods -A -o wide
```

```powershell
kubectl top pods -A
```

Für die Ressourcenplanung sollten die konfigurierten Requests und Limits mit den tatsächlichen Messwerten verglichen werden.

## 8. Health Checks und Monitoring

Neben den Ressourcenmetriken überwacht Kubernetes den Zustand der Anwendungspods über Probes.

### 8.1 Backend Health Checks

Employee Service und Auth Service verwenden Spring Boot Actuator.

Die Health-Endpunkte lauten:

```text
/actuator/health/liveness
/actuator/health/readiness
```

Die Deployments verwenden:

- Startup Probe
- Liveness Probe
- Readiness Probe

Die Startup Probe gibt der Anwendung Zeit, vollständig zu starten.

Die Liveness Probe erkennt, ob die Anwendung weiterhin funktionsfähig ist.

Die Readiness Probe prüft, ob die Anwendung Anfragen entgegennehmen kann.

### 8.2 Datenbank Health Checks

Die MySQL-StatefulSets verwenden `mysqladmin ping` für ihre Probes.

Auch hier kommen Startup-, Liveness- und Readiness-Probes zum Einsatz.

### 8.3 Frontend

Das Frontend wird über seinen Kubernetes-Service bereitgestellt.

Die Anwendung kann lokal über Port-Forwarding aufgerufen werden:

```powershell
kubectl port-forward -n employee-frontend service/employee-frontend 8080:80
```

Anschließend ist sie unter folgender Adresse erreichbar:

```text
http://localhost:8080/
```

## 9. Events und Fehleranalyse

Kubernetes-Events helfen bei der Untersuchung von Startproblemen, Ressourcenengpässen und anderen Betriebszuständen.

Alle Events anzeigen:

```powershell
kubectl get events -A --sort-by=.metadata.creationTimestamp
```

Events eines Namespace:

```powershell
kubectl get events -n employee-backend --sort-by=.metadata.creationTimestamp
```

Details eines Pods:

```powershell
kubectl describe pod POD_NAME -n NAMESPACE
```

Besonders relevant sind folgende Ereignisse:

| Event / Status | Bedeutung |
|---|---|
| `FailedScheduling` | Pod kann nicht eingeplant werden |
| `OOMKilled` | Container wurde wegen Speicherüberschreitung beendet |
| `CrashLoopBackOff` | Container startet wiederholt nicht erfolgreich |
| `Unhealthy` | Eine Probe ist fehlgeschlagen |
| `ImagePullBackOff` | Container-Image konnte nicht geladen werden |

## 10. Grenzen des aktuellen Monitorings

Der Metrics Server stellt grundlegende Ressourcenmetriken bereit.

Er bietet jedoch keine vollständige Langzeitüberwachung.

Folgende Funktionen sind aktuell nicht eingerichtet:

- Prometheus
- Grafana
- Langzeitaufzeichnung von Metriken
- Dashboard für die Anwendung
- Zentralisierte Log-Aggregation
- Alertmanager
- Automatisierte Benachrichtigungen
- Verfügbarkeits- und Performance-Historie

Der Metrics Server ist deshalb als grundlegende Monitoring-Komponente der lokalen Umgebung zu verstehen.

Für eine produktive Umgebung sollte ein umfassenderes Monitoring-Konzept eingerichtet werden.

## 11. Empfehlungen für eine spätere produktive Umgebung

### 11.1 Monitoring

Für eine spätere produktive Bereitstellung sollten folgende Komponenten geprüft werden:

- Prometheus zur Erfassung und Speicherung von Metriken
- Grafana zur Visualisierung
- Alertmanager für Benachrichtigungen
- Zentralisierte Log-Aggregation
- Überwachung der Datenbankzustände
- Überwachung der API-Latenzen
- Überwachung der Fehlerquoten

### 11.2 Ressourcenplanung

Die Resource Requests und Limits sollten anhand tatsächlicher Lasttests und Produktionsmesswerte angepasst werden.

Insbesondere bei den MySQL-Instanzen ist der RAM-Verbrauch zu berücksichtigen.

Eine zu niedrige RAM-Begrenzung kann zu OOM-Kills führen.

Zu hohe Requests können dagegen die verfügbare Kapazität des Clusters unnötig reduzieren.

### 11.3 Autoscaling

Der Metrics Server stellt eine Grundlage für den Horizontal Pod Autoscaler bereit.

Ein HPA ist in der aktuellen Umgebung noch nicht eingerichtet.

Vor einer späteren Einführung müssen geeignete Skalierungsgrenzen, Mindest- und Maximalreplikate sowie das Verhalten der Anwendungen unter Last definiert werden.

Für Stateful-Datenbanken ist ein einfaches horizontales Hochskalieren der Replikate keine geeignete Ersatzlösung für ein Datenbank-Replikations- und Hochverfügbarkeitskonzept.

## 12. Aktueller Implementierungsstatus

| Bereich | Status |
|---|---|
| Metrics Server installiert | Ja |
| Metrics Server Deployment verfügbar | Ja |
| Lokale TLS-Anpassung | Aktiv |
| `kubectl top nodes` | Funktioniert |
| `kubectl top pods -A` | Funktioniert |
| Resource Requests | Konfiguriert |
| Resource Limits | Konfiguriert |
| Backend Health Checks | Implementiert |
| Datenbank Health Checks | Implementiert |
| Prometheus | Nicht installiert |
| Grafana | Nicht installiert |
| Alertmanager | Nicht installiert |
| Langzeit-Metrikspeicherung | Nicht eingerichtet |
| HPA | Nicht eingerichtet |

## 13. Zusammenfassung

Die lokale Kubernetes-Umgebung verwendet den Metrics Server zur Anzeige grundlegender CPU- und RAM-Metriken.

Die Ressourcennutzung der fünf Anwendungs-Workloads wurde mit `kubectl top` überprüft.

Für Frontend, Backend und Datenbanken sind Resource Requests und Limits konfiguriert.

Die Anwendungen und Datenbanken verwenden Kubernetes Health Checks, um ihren Zustand zu überwachen.

Der Metrics Server benötigt in der lokalen Docker-Desktop-Umgebung derzeit die Option `--kubelet-insecure-tls`. Diese lokale Ausnahme darf nicht unverändert in AWS EKS oder einer produktiven Umgebung verwendet werden.

Ein umfassendes Monitoring mit Prometheus, Grafana und Alerting ist noch nicht Bestandteil der aktuellen Implementierung.