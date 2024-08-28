---
title: Publikationstyp Änderung
identifier: intranda_metadata_changeType
description: Plugin für die Änderungen des Publikationstyps im Goobi workflow
published: true
---

## Einführung
Dieses Plugin ermöglicht die Änderung des Publikationstyps in Goobi workflow.

## Installation
Um das Plugin nutzen zu können, müssen folgende Dateien installiert werden:

```bash
/opt/digiverso/goobi/mete/metadata/plugin_intranda_metadataeditor_changeType.jar
/opt/digiverso/goobi/plugins/GUI/plugin_intranda_metadataeditor_changeType-GUI.jar
/opt/digiverso/goobi/config/plugin_intranda_metadata_changeType.xml
```

Nach der Installation steht die Funktionalität des Plugins innerhalb der REST-API von Goobi workflow zur Verfügung.

## Überblick und Funktionsweise
Nachdem das Plugin installiert wurde, erscheint im Metadateneditor eine neue Funktion im Menü, die alle installierten und konfigurierten Plugins auflistet. Um das Plugin zur Änderung des Publikationstyps nutzen zu können, müssen zunächst im konfigurierten Projekt Templates erstellt werden. Diese Templates müssen mit den gewünschten Metadaten vorbelegt und die Vorgangseigenschaft für das Label muss vergeben werden. Sobald die Templates erstellt sind, stehen sie in einer Auswahlliste zur Verfügung.

![Funktionalität des Plugins](screen1_de.png)

Wählt der Benutzer das Plugin aus, öffnet sich ein Dialogfenster, in dem die vorhandenen Vorlagen für die verschiedenen Publikationstypen aufgelistet werden. Der Benutzer kann den gewünschten Publikationstyp auswählen und die Änderung speichern.

![Hier kann der Typ ausgewählt werden](screen2_de.png)

Beim Wechsel des Publikationstyps wird zuerst ein Backup der bestehenden Metadatendatei erstellt. Danach werden die Metadaten des ausgewählten Templates in den Vorgang kopiert. Wenn der alte Datensatz bereits Paginierung und Seitenzuweisungen enthält, werden diese Daten ebenfalls übernommen.

Abschließend wird für jedes konfigurierte Metadatum geprüft, ob es im alten Datensatz vorhanden war. Falls ja, wird dieses Metadatum, einschließlich Personen oder Gruppen, in den neuen Datensatz übertragen. Sollte im neuen Datensatz bereits ein entsprechendes Feld mit einer Default-Belegung vorhanden sein, wird dieses mit den originalen Daten überschrieben.

## Konfiguration
Die Konfiguration des Plugins erfolgt in der Datei `plugin_intranda_metadata_changeType.xml` wie hier aufgezeigt:

{{CONFIG_CONTENT}}

Die folgende Tabelle enthält eine Zusammenstellung der Parameter und ihrer Beschreibungen:

Parameter               | Erläuterung
------------------------|------------------------------------
`<section>`                      | ist wiederholbar und erlaubt somit unterschiedliche Konfigurationen für verschiedene Projekte |
`<project>`                      | legt fest, für welche Projekt(e) der aktuelle Bereich gilt. Das Feld ist wiederholbar, um so eine gemeinsame Konfiguration für mehrere Projekte verwenden zu können. |
`<titleProperty>`                      | enthält den Namen der Vorgangseigenschaft, in dem das zu verwendende Label steht |
`<templateProject>`                      | Name des Projekts, aus dem die Templates gelesen werden sollen. Es werden alle Vorgänge aus dem Projekt aufgelistet, die über ein Label verfügen. |
`<metadata>`                      | Liste an Metadaten, die aus der originalen Datei in die neue Datei überführt werden sollen | 
