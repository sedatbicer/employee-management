# Employee Management – Mitarbeiterverwaltung mit Angular Router und AG Grid

Die ursprüngliche Oberfläche wurde auf mehrere echte Routen umgestellt. Die gemeinsame Kopf- und Seitenleiste bleibt stehen; der Router zeigt den jeweiligen Seiteninhalt im `router-outlet`.

## Seiten
- `/mitarbeiter` – Übersicht mit AG Grid, Suche, Filtern und CSV-Export
- `/mitarbeiter/neu` – neuen Mitarbeiter anlegen
- `/mitarbeiter/:id` – Mitarbeiterdetails
- `/mitarbeiter/:id/bearbeiten` – Mitarbeiter bearbeiten

Löschen ist auf der Detailseite möglich. Mitarbeiterdaten bleiben wie zuvor im `localStorage` des Browsers gespeichert.

## Start
Voraussetzung: Node.js und npm.

```bash
npm install
npm start
```

Danach die von Angular angezeigte lokale Adresse öffnen.

## Aufbau
- `src/app/app.routes.ts` enthält die Routen.
- `src/app/app.component.ts` stellt den gemeinsamen Rahmen und den `router-outlet` bereit.
- `src/app/features/employees/employee-list.component.*` enthält die Übersicht.
- `employee-form-page.component.ts` ist die separate Erstellen-/Bearbeiten-Seite.
- `employee-detail-page.component.ts` zeigt einzelne Mitarbeiterdaten.
- `src/app/core/services/employee.service.ts` verwaltet die Daten und speichert Änderungen lokal.


## Deutsche AG-Grid-Oberfläche

Die AG-Grid-Beschriftungen sind auf Deutsch konfiguriert, einschließlich Seitennavigation, Filtermenüs und leerer Tabellenansicht.
