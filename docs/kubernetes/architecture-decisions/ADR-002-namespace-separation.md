# ADR-002: Namespace Separation

## 1. Status

**Status:** Accepted  
**Entscheidungsdatum:** 04.11.2026  
**Projekt:** Employee Management  
**Bereich:** Kubernetes-Architektur

---

## 2. Kontext

Die Employee-Management-Anwendung besteht aus mehreren technischen Komponenten, die unterschiedliche Aufgaben innerhalb der Gesamtarchitektur übernehmen.

Die Anwendung umfasst:

- Angular-Frontend mit integriertem Nginx-Webserver
- Employee Service auf Basis von Spring Boot
- Auth Service auf Basis von Spring Boot
- Employee MySQL Database
- Auth MySQL Database

Die Anwendung wird zunächst lokal mit Kubernetes auf Docker Desktop betrieben. Eine spätere Bereitstellung in einer AWS-EKS-Umgebung ist vorgesehen.

Für die Verwaltung der Kubernetes-Ressourcen wird eine Architektur benötigt, die eine klare Trennung der Anwendungsschichten ermöglicht und gleichzeitig eine übersichtliche Verwaltung, Wartbarkeit und Erweiterbarkeit unterstützt.

Insbesondere sollen Frontend, Backend und Datenbanken organisatorisch voneinander getrennt werden.

Darüber hinaus soll die Architektur eine Grundlage für spätere Sicherheitsmaßnahmen wie RBAC und NetworkPolicies schaffen.

## 3. Entscheidung

Die Kubernetes-Ressourcen der Anwendung werden auf drei separate Namespaces verteilt.

| Namespace | Zugeordnete Komponenten | Verantwortlichkeit |
|---|---|---|
| `employee-frontend` | Angular und integrierter Nginx | Darstellung und Weiterleitung von API-Anfragen |
| `employee-backend` | Employee Service und Auth Service | Geschäftslogik und Authentifizierung |
| `employee-database` | Employee MySQL und Auth MySQL | Persistente Datenhaltung |

Alle drei Namespaces erhalten einheitliche organisatorische Labels.

Beispiel für den Frontend-Namespace:

```yaml
app.kubernetes.io/part-of: employee-management
app.kubernetes.io/component: frontend
```

Für die anderen Namespaces wird der Wert von `app.kubernetes.io/component` entsprechend angepasst:

- `frontend`
- `backend`
- `database`

Die Labels ermöglichen eine einheitliche Identifikation und Gruppierung der Ressourcen.

## 4. Architekturübersicht

Die logische Architektur der Kubernetes-Umgebung sieht folgendermaßen aus:

```text
                       Browser
                          |
                          | HTTP
                          v
              +------------------------+
              |   employee-frontend    |
              |                        |
              |   Angular              |
              |   Nginx                |
              +------------------------+
                          |
                          | HTTP / REST
                          | Kubernetes Service DNS
                          v
              +------------------------+
              |   employee-backend     |
              |                        |
              |   Employee Service     |
              |   Auth Service         |
              +------------------------+
                          |
                          | TCP 3306
                          | Kubernetes Service DNS
                          v
              +------------------------+
              |   employee-database    |
              |                        |
              |   Employee MySQL       |
              |   Auth MySQL           |
              +------------------------+
```

Die Kommunikation zwischen den Anwendungskomponenten erfolgt über Kubernetes Services.

Da sich die Komponenten in unterschiedlichen Namespaces befinden, werden vollständig qualifizierte Kubernetes-Service-Adressen verwendet.

Beispiele:

**Employee Service:**

```text
employee-service.employee-backend.svc.cluster.local
```

**Auth Service:**

```text
auth-service.employee-backend.svc.cluster.local
```

**Employee MySQL:**

```text
employee-mysql.employee-database.svc.cluster.local
```

**Auth MySQL:**

```text
auth-mysql.employee-database.svc.cluster.local
```

Diese DNS-Namen ermöglichen eine stabile Kommunikation unabhängig von den dynamischen IP-Adressen einzelner Pods.

## 5. Begründung der Entscheidung

### 5.1 Logische Trennung der Anwendungsschichten

Die Aufteilung in drei Namespaces trennt die Anwendung nach ihren technischen Verantwortlichkeiten.

Das Frontend übernimmt die Darstellung und Weiterleitung der API-Anfragen.

Das Backend enthält die Geschäftslogik und Authentifizierungsfunktionen.

Die Datenbanken übernehmen die dauerhafte Speicherung der Anwendungsdaten.

Durch diese Struktur können die einzelnen Bereiche unabhängig voneinander verwaltet werden.

### 5.2 Bessere Wartbarkeit

Die Ressourcen lassen sich gezielt innerhalb ihres jeweiligen Namespace untersuchen und verwalten.

Beispielsweise können sämtliche Backend-Pods mit folgendem Befehl angezeigt werden:

```powershell
kubectl get pods -n employee-backend
```

Die Trennung erleichtert außerdem die Fehleranalyse, da Ressourcen und Ereignisse gezielt einem Anwendungsbereich zugeordnet werden können.

### 5.3 Grundlage für Zugriffskontrollen

Namespaces ermöglichen eine differenzierte Verwaltung von Kubernetes-Ressourcen.

Darauf aufbauend können später beispielsweise folgende Sicherheitsmaßnahmen umgesetzt werden:

- Rollenbasierte Zugriffskontrolle mit RBAC
- Separate ServiceAccounts
- Namespace-spezifische Berechtigungen
- Begrenzung des Ressourcenverbrauchs über ResourceQuotas

Die Namespace-Trennung allein stellt jedoch keine vollständige Sicherheitsisolierung dar.

### 5.4 Grundlage für Netzwerkisolierung

Die Namespace-Struktur ermöglicht die gezielte Definition von Netzwerkregeln über Kubernetes NetworkPolicies.

Damit kann festgelegt werden, welche Anwendungskomponenten miteinander kommunizieren dürfen.

Für das Projekt wurde folgende Kommunikationsstruktur umgesetzt:

| Quelle | Ziel | Erlaubter Verkehr |
|---|---|---|
| Frontend | Frontend-Nginx | TCP 80 |
| Frontend | Employee Service | TCP 8081 |
| Frontend | Auth Service | TCP 8082 |
| Employee Service | Employee MySQL | TCP 3306 |
| Auth Service | Auth MySQL | TCP 3306 |

Die NetworkPolicies beschränken den eingehenden Netzwerkverkehr der ausgewählten Pods.

Ausgehender Netzwerkverkehr wird durch diese Regeln nicht zusätzlich eingeschränkt.

Die tatsächliche Durchsetzung der NetworkPolicies wurde im Projekt nicht durch separate Negativtests mit absichtlich unzulässigem Netzwerkverkehr nachgewiesen.

### 5.5 Portierbarkeit zwischen Umgebungen

Die Namespace-Struktur ist nicht an Docker Desktop gebunden.

Sie kann auch in einer späteren AWS-EKS-Umgebung verwendet werden.

Dadurch bleibt die grundlegende logische Architektur über verschiedene Kubernetes-Umgebungen hinweg erhalten.

## 6. Betrachtete Alternativen

### 6.1 Alternative 1: Ein gemeinsamer Namespace

Alle Anwendungskomponenten werden innerhalb eines einzigen Namespaces betrieben.

**Vorteile:**

- Einfachere Anfangskonfiguration
- Weniger Verwaltungsaufwand
- Weniger namespace-übergreifende DNS-Konfiguration

**Nachteile:**

- Geringere logische Trennung
- Weniger übersichtliche Ressourcenverwaltung
- Weniger klare Grundlage für differenzierte Berechtigungen
- Schwieriger zu strukturierende Netzwerkregeln

Diese Alternative wurde aufgrund der gewünschten Trennung der Anwendungsschichten nicht gewählt.

### 6.2 Alternative 2: Ein Namespace pro Anwendungskomponente

Für jede Anwendungskomponente wird ein eigener Namespace angelegt.

**Vorteile:**

- Sehr feingranulare organisatorische Trennung
- Individuelle Berechtigungen und Richtlinien
- Separate Verwaltung jeder Komponente

**Nachteile:**

- Höherer Verwaltungsaufwand
- Zusätzliche Komplexität bei der Netzwerkverwaltung
- Mehr Konfigurationsaufwand
- Für die aktuelle Projektgröße unnötig umfangreich

Diese Alternative wurde für das aktuelle Projekt nicht gewählt.

## 7. Technische Umsetzung

Die Namespaces werden über Kubernetes-Manifeste verwaltet.

Die zugehörigen Ressourcen befinden sich im Projekt unter:

```text
k8s/
└── base/
    ├── namespaces.yaml
    ├── employee-frontend/
    ├── employee-backend/
    └── database/
```

Die einzelnen Komponenten werden innerhalb ihrer vorgesehenen Namespaces bereitgestellt.

Die Namespace-Zugehörigkeit wird in den Kubernetes-Ressourcen über das Feld `metadata.namespace` festgelegt.

Beispiel:

```yaml
metadata:
  name: employee-service
  namespace: employee-backend
```

Die Kommunikation zwischen den Anwendungsschichten erfolgt über interne ClusterIP-Services.

Die Services stellen stabile Netzwerk-Endpunkte für die jeweils zugehörigen Pods bereit.

## 8. Implementierungsstatus

Die Namespace-Struktur wurde lokal auf Docker Desktop erfolgreich umgesetzt.

| Namespace | Status | Inhalt |
|---|---|---|
| `employee-frontend` | Active | Angular und Nginx |
| `employee-backend` | Active | Employee Service und Auth Service |
| `employee-database` | Active | Beide MySQL-Datenbanken |

Die zugehörigen Labels wurden ebenfalls eingerichtet.

Zusätzlich wurden die wesentlichen Kubernetes-Ressourcen in den jeweiligen Namespaces bereitgestellt.

Die NetworkPolicies wurden erstellt und angewendet.

Die Kommunikation der Anwendung über die vorgesehenen Services wurde im Rahmen der lokalen Anwendung überprüft.

Eine vollständige funktionale Überprüfung aller erlaubten und verbotenen Netzwerkverbindungen wurde nicht durchgeführt.

## 9. Konsequenzen

### 9.1 Positive Konsequenzen

- Klare Trennung zwischen Frontend, Backend und Datenbanken
- Übersichtliche Verwaltung der Kubernetes-Ressourcen
- Bessere Wartbarkeit und Fehleranalyse
- Grundlage für RBAC und NetworkPolicies
- Einheitliche Struktur für lokale und cloudbasierte Deployments
- Gute Erweiterbarkeit für zukünftige Anwendungskomponenten

### 9.2 Technische Konsequenzen

Die Nginx-Konfiguration muss die Backend-Services über ihre namespace-qualifizierten DNS-Namen erreichen.

Die Backend-Services müssen wiederum die Datenbanken über deren Service-Adressen im Namespace `employee-database` erreichen können.

Die Kubernetes-NetworkPolicies müssen diese Kommunikationswege berücksichtigen.

Änderungen an Namespace-Namen oder Service-Namen können Anpassungen an den entsprechenden Konfigurationen erforderlich machen.

### 9.3 Sicherheitskonsequenzen

Die Trennung in Namespaces stellt zunächst eine organisatorische und logische Struktur dar.

Sie ersetzt keine zusätzlichen Sicherheitsmechanismen.

Für eine produktive Umgebung müssen insbesondere folgende Maßnahmen berücksichtigt werden:

- Geeignete RBAC-Rollen und ServiceAccounts
- Verschlüsselte Speicherung von Kubernetes Secrets
- Überprüfung der NetworkPolicy-Durchsetzung
- Geeignete Pod-Sicherheitsrichtlinien
- Regelmäßige Überprüfung der Ressourcenberechtigungen

## 10. Noch offene beziehungsweise zukünftige Maßnahmen

Folgende Themen sind für die weitere Entwicklung relevant:

- RBAC-Berechtigungen detailliert definieren und überprüfen
- Netzwerkisolierung funktional testen
- Produktionsgeeignete Secret-Verwaltung für AWS EKS vorbereiten
- Cloud-spezifische Speicher- und Netzwerkressourcen konfigurieren
- Sicherheitskonfigurationen für die spätere EKS-Umgebung ergänzen

Diese Punkte sind nicht Bestandteil der ursprünglichen Namespace-Entscheidung und werden separat behandelt.

## 11. Ergebnis

Die Namespace-Trennung wurde als verbindliche Architekturentscheidung für das Employee-Management-Projekt festgelegt und lokal erfolgreich implementiert.

Die Anwendung ist in die drei logischen Bereiche Frontend, Backend und Datenbanken unterteilt.

Die Struktur bildet die Grundlage für die lokale Kubernetes-Umgebung auf Docker Desktop sowie für die spätere Übertragung der Architektur auf AWS EKS.

**Die Entscheidung ist angenommen und umgesetzt.**