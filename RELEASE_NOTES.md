# Release notes

## 3.7 — 2026-10-10

**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.7

### Highlights (EN)

RouteConverter 3.7 reads Google Maps Timeline exports straight from your phone and adds the RTZ route format used by ship navigation systems. GPX track segments now survive reading and writing and show up as gaps on the map and in the elevation profile, and ascent/descent totals are no longer inflated by GPS elevation noise. You can select and delete pauses, set the font size of the user interface, and see on the map where maps, routing, elevation and POI data are available. On the fix side, the app starts again on macOS 15 and earlier, starts faster on Windows, follows your regional date and time settings, and stays responsive when you select thousands of positions. New ways to install: a Debian/Ubuntu package and a Homebrew cask.

### Was ist neu (DE)

RouteConverter 3.7 liest Google-Maps-Zeitachsen direkt vom Smartphone und kennt das RTZ-Routenformat der Schiffsnavigation. Segmente in GPX-Tracks bleiben beim Lesen und Schreiben erhalten und erscheinen als Lücke auf der Karte und im Höhenprofil; GPS-Höhenrauschen bläht die Summen für Anstieg und Abstieg nicht mehr auf. Neu sind außerdem: Pausen auswählen und löschen, eine einstellbare Schriftgröße für die Oberfläche und eine Kartenansicht, die zeigt, wo Karten, Routing-, Höhen- und POI-Daten verfügbar sind. Behoben: Auf macOS 15 und älter startet die App wieder, unter Windows startet sie schneller, Datum und Uhrzeit folgen den Regionseinstellungen, und auch bei tausenden ausgewählten Positionen bleibt sie flüssig. Neu zu installieren als Debian/Ubuntu-Paket und über Homebrew.

### New features

- Google Maps Timeline: reads the on-device exports from the Google Maps app (`location-history.json` / `Timeline.json`) without dropping points (#351)
- RTZ route exchange format (IEC 61174, used by ECDIS ship navigation): read for everyone, write for sponsors
- GPX track segments are preserved when reading and writing. The map and the elevation profile leave a gap where a segment ends, and the time across the gap no longer counts toward the duration
- Select and delete pause positions (speed 0), optionally shifting the times of the following positions so the track runs on without the pauses (#333)
- Font-size setting for the user interface; on Windows the default follows the display scaling (#335, #338)
- Coverage overlay (View menu): shows on the map where maps, routing, elevation and POI data are available
- Command line: new `convert` command with explicit source/target formats, a one-line JSON result and distinct exit codes
- New ways to install: `RouteConverterLinux.deb` for Debian/Ubuntu with bundled Java, a Homebrew cask for macOS (`brew install --cask cpesch/routeconverter/routeconverter`), and a `.sha256` checksum next to every download
- The update dialog opens a release page with context instead of the raw download directory, uses buttons instead of links, nudges harder on very old versions and shows an end-of-life notice for Java 8 and RouteConverter 2.x
- Support RouteConverter: About dialog and Help menu with Donate, a gentle reminder after 100 and 500 starts, and a dialog explaining sponsor features when you try one

### Changes

- Ascent and descent totals ignore elevation changes below 5 m, which suppresses GPS noise; totals are lower than before and closer to other tools (#409)

### Fixes

- macOS: the app runs on macOS 11 and later again. 3.6 through 3.6.5 were built against macOS 26 by mistake, so on macOS 15 (Sequoia) and earlier the icon appeared crossed out and greyed and the app would not start at all (#393, thanks Edgar Kraus)
- Dates and times now follow the system's region settings instead of a 12-hour, month-first format (#415, thanks xyzlabc)
- BRouter kept using routing data downloaded once and never refreshed it, so roads that had reopened since stayed blocked (Blue Ridge Parkway); routing data older than 7 days is now downloaded again. Offline routing also skips a downloaded area that has no roads for the route (#423, thanks Kumba42)
- Windows: the program no longer unpacks 60 MB on every start, which made startup slow (#372)
- Windows Portable: the launcher could not start the program (#373)
- Selecting or deselecting many positions froze the program; drawing large tracks, routes and waypoint lists on the map is faster (#356)
- Copy and paste garbled descriptions with umlauts and other non-ASCII characters (#340)
- "Complete coordinates" overwrote positions that already had coordinates (#339)
- Microsoft Flight Simulator flight plans (.pln): altitudes are read and written in feet, as MSFS stores them. Before, a cruise waypoint at FL360 showed as 36,000 m instead of 10,973 m, and .pln files written by earlier versions carry meters in the feet field — re-export them to fix their altitudes (#417)
- OutdoorActive maps showed no tiles (#395)
- The world-map background left a grey hole after startup (#376)
- BRouter defaulted to the slow "moped" profile (#341)
- Downloads: an interrupted download that was resumed could replace a good local file with a truncated one (#383), temporary files were left behind (#342), the download table could freeze, the remaining size counted only one routing area, and a timeout was reported as "offline"
- With Arabic or Persian language settings, times were written with Eastern Arabic digits, so other programs could not read the files
- With Turkish language settings, file types were not recognized
- Some large files (for example a 6.7 MB `.hst` track) were read as empty (regression in 3.6)
- Dragging a position on the map follows the cursor again and grabs it at the pin tip
- Deleting many positions at once is much faster
- Running out of memory while inserting a route with GraphHopper is reported instead of failing silently (#337)
- Failed elevation lookups are reported instead of skipped silently, and Google is available again as elevation fallback

### Known issues

- Windows with display scaling above 100%: text and icons are not rendered at native resolution; use the new font-size setting to enlarge the text (#343)

### Downloads

| OS | File | URL |
|---|---|---|
| Windows (includes JRE) | `RouteConverterWindows.exe` | https://releases.routeconverter.com/latest/RouteConverterWindows.exe |
| Windows (Portable, includes JRE) | `RouteConverterPortable.paf.exe` | https://releases.routeconverter.com/latest/RouteConverterPortable.paf.exe |
| Linux | `RouteConverterLinux.jar` | https://releases.routeconverter.com/latest/RouteConverterLinux.jar |
| Linux (Debian/Ubuntu package, includes JRE) | `RouteConverterLinux.deb` | https://releases.routeconverter.com/latest/RouteConverterLinux.deb |
| Mac (Intel x64) | `RouteConverterMac-x64-app.zip` | https://releases.routeconverter.com/latest/RouteConverterMac-x64-app.zip |
| Mac (Apple Silicon aarch64) | `RouteConverterMac-aarch64-app.zip` | https://releases.routeconverter.com/latest/RouteConverterMac-aarch64-app.zip |
| Mac (Homebrew) | `brew install --cask cpesch/routeconverter/routeconverter` | https://github.com/cpesch/homebrew-routeconverter |
| CmdLine | `RouteConverterCmdLine.jar` | https://releases.routeconverter.com/latest/RouteConverterCmdLine.jar |
| TimeAlbumPro (Win, includes JRE) | `TimeAlbumProWindows.exe` | https://releases.routeconverter.com/latest/TimeAlbumProWindows.exe |
| TimeAlbumPro (Linux) | `TimeAlbumProLinux.jar` | https://releases.routeconverter.com/latest/TimeAlbumProLinux.jar |
| TimeAlbumPro (Mac Intel x64) | `TimeAlbumProMac-x64-app.zip` | https://releases.routeconverter.com/latest/TimeAlbumProMac-x64-app.zip |
| TimeAlbumPro (Mac Apple Silicon aarch64) | `TimeAlbumProMac-aarch64-app.zip` | https://releases.routeconverter.com/latest/TimeAlbumProMac-aarch64-app.zip |
| Archive | All artefacts for 3.7 | https://releases.routeconverter.com/previous-releases/3.7/ |
| API docs | Aggregated Javadoc (always current release) | https://static.routeconverter.com/javadoc/ |

Every artefact has a `<file>.sha256` checksum next to it.

### Acknowledgements

- Bug reports: Edgar Kraus, Kumba42, xyzlabc, and the forum and support threads behind #333, #335, #356, #372, #395 and #409
- Translators: the Crowdin community

## 3.6 — 2026-08-23

**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/3.6

### Highlights (EN)

RouteConverter 3.6 adds read and write support for the Columbus Fusion track format and the Columbus GNSS GPX extensions — device-local timestamps, waypoint/POI/parking tags, 3-axis acceleration, and real fix-quality/HDOP values that feed new position-list columns (Heading, HDOP, Quality, acceleration). The Browse panel gains distance and duration columns, and every position list stays visible on the map while you edit one. The bundled JavaHelp is replaced by web-based help, and selecting redundant positions on very large tracks is now near-instant and can be cancelled.

### New features

- Added Columbus Fusion track format (Type=GNSS, GNSS with satellite info, GNSS+IMU, IMU) with device-local timestamps, waypoint/POI/parking tags, and 3-axis acceleration
- Added Columbus GNSS GPX support: course/hdop extensions, correct km/h speed for current firmware, cb: namespace extensions and standard hdop for new firmware
- Added position list columns: Heading, HDOP, Quality, acceleration (ax/ay/az) — hidden by default, enable via the column header context menu
- Added Parking count and POI count to the track description (TimeAlbum Pro)
- Elevation, heading, HDOP and acceleration are shown with two decimal places
- NMEA: GGA fix quality and HDOP are now read and written; GPX fix element and Columbus Type 1 VALID column map to the quality attribute
- Added distance and duration columns to the Browse panel for comparing routes at a glance
- All position lists stay visible on the map while you edit one: the other routes, tracks and waypoints are drawn in a subdued, read-only shade
- Added a Mapbox access token field (Options → API Keys) so the Mapbox Satellite layer works with your own token
- Replaced the bundled JavaHelp with web-based help: F1 and the "?" buttons open the matching page in your browser
- Added "Departure time" (Times menu) to fill each position's arrival time from a departure time
- Added "Insert positions" at a fixed interval along a straight line
- Added a "Download POI data" checkbox with remaining-size display and support for the newest POI catalog (v4)
- Added support for current Google Maps URL formats (place links, ?q=, short links) when importing
- Added opt-in anonymous telemetry and crash diagnostics (off by default)
- macOS: files can be opened directly with RouteConverter ("Open with")

### Fixes

- Map view (and profile view) could permanently stop updating after appending several files in quick succession; a race between the background map-update queue and newly loaded files could misclassify a pure append as a mid-list edit, desyncing the map from the position list and crashing the update worker (TimeAlbum Pro)
- Opening a file while a long route was still being routed left the map empty for minutes: the discarded route kept calculating leg after leg on the single map-update thread and the new file's rendering had to wait behind it; replacing the route now cancels the in-flight rendering at the next leg
- Opening a route with thousands of positions froze the map for minutes: every straight-line segment was added to the map as a separate AWT event with its own redraw cycle; the straight-line preview is now added in a single batch
- A waypoint list containing a single position without coordinates showed no waypoint markers at all on the map; positions without coordinates are now skipped instead of suppressing every marker
- Selecting redundant positions on a track with tens of thousands of points is now near-instant and can be cancelled (Douglas-Peucker runs off the UI thread)
- F1 over an open menu now opens the help topic of the highlighted menu item instead of the focused panel
- A background position augmentation could crash when the position list shrank mid-run (IndexOutOfBoundsException)
- The world-map background is now shown under any displayed map, not only Mapsforge
- The online map is restored correctly on startup
- Mapsforge map popups (New/Delete) now trigger on press+release and use the correct focus context
- BRouter: fixed a crash when connecting a freshly added position, an unexpected fallback travel mode, and stale segment/lookup data that did not refresh
- Legacy ANSI-encoded KML/GPX files are now decoded correctly instead of showing mojibake
- OpenStreetMap tiles now send a policy-compliant User-Agent
- The color picker no longer janks and colors can be reset to their defaults
- The Java-update dialog no longer points to an outdated version
- Updated translations (including Chinese, French and Spanish)

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
