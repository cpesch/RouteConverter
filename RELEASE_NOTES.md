# Release notes

Curated user-facing release notes for RouteConverter. One section per
shipped version, newest at the top.

The GitHub Release body for each tag (`gh release view <version>`)
auto-lists merged PRs from `--generate-notes`; this file holds the
maintainer's curated copy that the auto-list doesn't replace.

Authoring template + Phase 4b distribution map (where each block goes —
WordPress, forum, Latest Versions widget, RouteConverter.xml catalog)
live in the maintainer-private `rc-content` repo at
`release-notes/TEMPLATE.md`.

---

## 3.4 — 2026-06-07

**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.4

### Highlights (EN)

RouteConverter 3.4 brings Mapsforge POI lookup directly into the map view —
the existing Online Services panel now includes Mapsforge / OpenAndroMaps
POI databases and a Mapsforge geocoding service in FindPlace. The Find
Places dialog has been rebuilt with a sortable table, queries every
configured geocoding service in parallel, and shows the category and type
of each POI in a dedicated column. Under the hood the build moves to
Java 25 and the third-party repository now lives at
`mvn.routeconverter.com`. Plus the usual round of fixes — timezone
handling for tracks with an offset, WBT-202 tracks that start at zero
distance, stale-download avoidance, and a number of smaller UI polish
items.

### Was ist neu (DE)

RouteConverter 3.4 holt das Mapsforge-POI-Lookup direkt in die Kartenansicht:
Die Online Services nutzen jetzt Mapsforge- und OpenAndroMaps-POI-Datenbanken
sowie einen Mapsforge-Geocoding-Dienst in FindPlace. Der Find-Places-Dialog
wurde neu aufgebaut — sortierbare Tabelle, parallele Abfrage aller
konfigurierten Geocoding-Dienste, eigene Spalte für Kategorie und Typ jedes
POIs. Unter der Haube läuft der Build mit Java 25, das Drittanbieter-Repo
liegt jetzt auf `mvn.routeconverter.com`. Dazu die üblichen Korrekturen:
Zeitzonen mit Offset, WBT-202-Tracks mit Null-Distanz am Anfang,
Stale-Download-Vermeidung und einige kleinere UI-Verbesserungen.

### New features

- Mapsforge / OpenAndroMaps POI lookup, integrated into the Online Services panel.
- Read Mapsforge POI databases.
- Mapsforge geocoding service for FindPlace.
- FindPlace dialog: JTable replaces JList, queries all geocoding services
  in parallel, shows every matched position.
- POI category and type shown in a dedicated column.
- Catalog tooling driven by `datasource <source>`: `ScanWebsite`,
  `MirrorCatalog`, `CatalogMirrorApp`.

### Changes

- Build now runs on Java 25 (Java 17 stays the runtime floor).
- Third-party Maven artefacts are now served from `mvn.routeconverter.com`
  (was the legacy SVN-hosted repo).
- "Find places" opens with Cmd-F on macOS.
- Default scan directory no longer includes the directory RouteConverter
  was started from (avoids accidental large scans when launched from a
  data dir).

### Fixes

- Tracks with a non-UTC timezone offset are no longer converted to the
  wrong UTC time (#99, thanks @lundefugl).
- WBT-202 tracks that start at zero distance no longer fail validation
  (#100, thanks @kimmerin — first contribution).
- NPE when positions have no coordinates.
- Stale downloads avoided.
- Theme styles preserved across theme switching.
- Infinite loops when collecting files.
- Duplicate menu entries.
- Comparator contract violation.
- Non-visible layers filtered out.
- Files.walk() wrapped in try-with-resources.
- Java 17 Windows pipeline stabilised.

### Known issues

(None reported at release time.)

### Upgrade notes

- Java runtime requirement stays at 17 or later (bundled installer
  ships a JRE).
- Settings + saved routes carry over from 3.3 unchanged.

### Downloads

| OS | File | URL |
|---|---|---|
| Windows | `RouteConverterWindowsOpenSource.exe` | https://static.routeconverter.com/downloads/RouteConverterWindows.exe |
| Windows (Bundle, includes JRE) | `RouteConverterWindowsBundle.exe` | https://static.routeconverter.com/downloads/RouteConverterWindowsBundle.exe |
| Windows (Portable) | `RouteConverterPortable.paf.exe` | https://static.routeconverter.com/downloads/RouteConverterPortable.paf.exe |
| Linux | `RouteConverterLinuxOpenSource.jar` | https://static.routeconverter.com/downloads/RouteConverterLinux.jar |
| Mac | `RouteConverterMacOpenSource-app.zip` | https://static.routeconverter.com/downloads/RouteConverterMac.zip |
| Mac (jar) | `RouteConverterMacOpenSource.jar` | https://static.routeconverter.com/downloads/RouteConverterMacOpenSource.jar |
| CmdLine | `RouteConverterCmdLine.jar` | https://static.routeconverter.com/downloads/RouteConverterCmdLine.jar |
| TimeAlbumPro (Win) | `TimeAlbumProWindows.exe` | https://static.routeconverter.com/downloads/TimeAlbumProWindows.exe |
| TimeAlbumPro (Win Bundle) | `TimeAlbumProWindowsBundle.exe` | https://static.routeconverter.com/downloads/TimeAlbumProWindowsBundle.exe |
| TimeAlbumPro (Linux) | `TimeAlbumProLinux.jar` | https://static.routeconverter.com/downloads/TimeAlbumProLinux.jar |
| TimeAlbumPro (Mac) | `TimeAlbumProMac-app.zip` | https://static.routeconverter.com/downloads/TimeAlbumProMac.zip |
| Archive | All artefacts for 3.4 | https://static.routeconverter.com/downloads/previous-releases/3.4/ |
| API docs | Aggregated Javadoc 3.4 | https://static.routeconverter.com/javadoc/3.4/ |

### Acknowledgements

- @lundefugl (#99) — timezone fix.
- @kimmerin (#100, first contribution) — WBT-202 zero-distance validation fix.
- Full merged-PR list: see the GitHub Release auto-notes.
