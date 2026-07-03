# Release notes

## 3.5 — 2026-07-03

**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.5

### Highlights (EN)

RouteConverter 3.5 is a trust-and-stabilisation release. The Windows
installers and the standalone Java artifacts are now **code-signed** via the
SignPath Foundation, so Windows SmartScreen and Defender stop warning about
them. The runtime moves to **Java 21** (bundled JRE and minimum requirement),
the macOS bundle now launches cleanly on Apple Silicon, and a broad round of
download-integrity, background-map, and file-format fixes lands. No new
end-user features this time — POI lookup arrived in 3.4.

### Was ist neu (DE)

RouteConverter 3.5 ist ein Vertrauens- und Stabilisierungs-Release. Die
Windows-Installer und die eigenständigen Java-Artefakte sind jetzt über die
SignPath Foundation **signiert**, sodass Windows SmartScreen und Defender
keine Warnung mehr zeigen. Die Laufzeit wechselt auf **Java 21** (gebündelte
JRE und Mindestanforderung), das macOS-Bundle startet nun sauber auf Apple
Silicon, und zahlreiche Korrekturen bei Download-Integrität, Hintergrundkarte
und Dateiformaten kommen hinzu. Keine neuen Endnutzer-Funktionen — POI-Suche
kam mit 3.4.

### New features

- Crash / init diagnostics: the unhandled-error dialog shows the full
  root-cause chain, background-map load failures are logged with a
  startup-complete marker, and the error-report log is cleared after a
  successful send.

### Changes

- **Code signing:** Windows installers and Java artifacts are signed via the
  SignPath Foundation (Authenticode / jar signature); the certificate is held
  in SignPath's HSM and CI never holds it.
- **Java 21:** minimum runtime and bundled JRE move from 17 to 21.
- Windows installers merge the former Bundle + OpenSource builds into one
  installer per product; the standalone macOS `.jar` is no longer published.
- Download and release links now point to `releases.routeconverter.com`.
- Stripped bundled-JRE module set extended (`jdk.net` for httpclient5 5.6,
  `jdk.management`) to prevent `NoClassDefFound` on the minimised runtime.

### Fixes

- macOS: the bundle self-dequarantines so the signed JRE launches on Apple
  Silicon (no more `Killed: 9`).
- Downloads: SHA-1 is treated as authoritative and mtime is ignored when the
  SHA-1 is known; a "locally later than remote" file no longer masks a SHA-1
  mismatch; an outdated file drops its ETag so the re-download is
  unconditional (#106).
- BRouter: outdated cached `.rd5` segments are removed (lookup v11).
- Background world map installs without racing map-view creation (fixes the
  intermittent blank background map).
- KML: geometry-less placemarks are skipped (Kml21 / Kml22Beta).
- GPX: OsmAnd extensions bind as global elements and the TrekBuddy
  `ObjectFactory` is registered (fixes extension binding failures).
- Reverted geojson-jackson 3.0 → 1.14 (3.0 broke GeoJSON read/write).
- gpsbabel Extract receives its fragments; no more spurious "null bytes" log
  when the content length is unknown.

### Known issues

(None reported at release time.)

### Upgrade notes

- **Java runtime requirement is now 21 or later** (was 17). The bundled
  installer ships a JRE 21; if you run the standalone jar on your own JVM,
  upgrade to Java 21+.
- Settings + saved routes carry over from 3.4 unchanged.

### Downloads

| OS | File | URL |
|---|---|---|
| Archive | All artefacts for 3.5 | https://releases.routeconverter.com/previous-releases/3.5/ |
| API docs | Aggregated Javadoc (always current release) | https://static.routeconverter.com/javadoc/ |

### Acknowledgements

- Full merged-PR list: see the GitHub Release auto-notes.

## 3.4 — 2026-06-07

**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.4

### Highlights (EN)

RouteConverter 3.4 brings Mapsforge POI lookup directly into the map view —
the existing Online Services panel now includes Mapsforge / OpenAndroMaps
POI databases and a Mapsforge geocoding service in FindPlace. The Find
Places dialog has been rebuilt with a sortable table, queries every
configured geocoding service in parallel, and shows the category and type
of each POI in a dedicated column. Under the hood the build moves to
Java 21 and the third-party repository now lives at
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
POIs. Unter der Haube läuft der Build mit Java 21, das Drittanbieter-Repo
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
| Archive | All artefacts for 3.4 | https://releases.routeconverter.com/previous-releases/3.4/ |
| API docs | Aggregated Javadoc (always current release) | https://static.routeconverter.com/javadoc/ |

### Acknowledgements

- @lundefugl (#99) — timezone fix.
- @kimmerin (#100, first contribution) — WBT-202 zero-distance validation fix.
- Full merged-PR list: see the GitHub Release auto-notes.
