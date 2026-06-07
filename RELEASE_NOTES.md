# Release notes

Authoring source for RouteConverter release notes. One section per shipped
version, newest at the top. Filled by the maintainer during the Phase 4b
Nacharbeit step of [`rc-meta/docs/00018`](../rc-meta/docs/00018-release-automation-3.4.md);
the GitHub Release auto-notes (`gh release create --generate-notes`) only
list merged PRs and aren't user-facing — this file holds the curated copy
that goes to the website, the forum, and the Latest Versions widget.

## Distribution map (Phase 4b)

For each release, copy each block to its destination:

| Block | Destination |
|---|---|
| `## <version>` heading + `### Highlights (EN)` | WordPress post on `www.routeconverter.com/releases/<version>/` (EN site) |
| `### Was ist neu (DE)` | WordPress post on `www.routeconverter.de/releases/<version>/` (DE site) |
| `### New features` + `### Changes` + `### Fixes` | RouteConverter forum post (EN + DE thread) |
| Whole `## <version>` block | GH Release body — replaces the auto-generated PR list |
| `### Downloads` table | "Latest Versions" widget on both sites (paste the URLs) |
| Version + date | `RouteConverter.xml` + `RouteConverter_de.xml` download-catalog entries |

After publishing, tick the checklist at the end of the version section.

---

## Template — copy this block, replace `X.Y`, fill in

```markdown
## X.Y — YYYY-MM-DD

**GitHub Release:** https://github.com/cpesch/RouteConverter/releases/tag/X.Y

### Highlights (EN)

One short paragraph (3–5 sentences) selling the version. What changed
that a regular user would care about. No version-number lists, no
dependency bumps. Tone matches existing www.routeconverter.com posts.

### Was ist neu (DE)

Gleicher Absatz auf Deutsch. Nicht wörtlich übersetzt — denselben
Inhalt, aber idiomatisch.

### New features

- Feature 1 — one sentence on what it does for the user.
- Feature 2 — …

### Changes

- Behaviour change that existing users will notice. Note any UI moves
  or default-value changes here so people can find them.

### Fixes

- Bug fix that warrants user mention (crash, data loss, wrong output).
  Skip purely internal cleanups — those belong in the auto-generated
  GH Release notes.

### Known issues

- Anything shipped that the user might trip over. Empty section is fine;
  delete the heading if so.

### Upgrade notes

- File-format / settings migrations, JDK requirement bumps, OS support
  drops. Empty section is fine; delete the heading if so.

### Downloads

| OS | File | URL |
|---|---|---|
| Windows | `RouteConverterWindowsOpenSource.exe` | https://static.routeconverter.com/downloads/RouteConverterWindows.exe |
| Windows (Portable) | `RouteConverterPortable.paf.exe` | https://static.routeconverter.com/downloads/RouteConverterPortable.paf.exe |
| Linux | `RouteConverterLinuxOpenSource.jar` | https://static.routeconverter.com/downloads/RouteConverterLinux.jar |
| Mac | `RouteConverterMacOpenSource-app.zip` | https://static.routeconverter.com/downloads/RouteConverterMac.zip |
| CmdLine | `RouteConverterCmdLine.jar` | https://static.routeconverter.com/downloads/RouteConverterCmdLine.jar |
| TimeAlbumPro (Win) | `TimeAlbumProWindows.exe` | https://static.routeconverter.com/downloads/TimeAlbumProWindows.exe |
| TimeAlbumPro (Linux) | `TimeAlbumProLinux.jar` | https://static.routeconverter.com/downloads/TimeAlbumProLinux.jar |
| TimeAlbumPro (Mac) | `TimeAlbumProMac-app.zip` | https://static.routeconverter.com/downloads/TimeAlbumProMac.zip |
| Archive | All artefacts for X.Y | https://static.routeconverter.com/downloads/previous-releases/X.Y/ |
| API docs | Aggregated Javadoc | https://static.routeconverter.com/javadoc/X.Y/ |

(Stable top-level URLs above resolve to the symlinks under
`static/downloads/`. They keep working unchanged across releases.)

### Acknowledgements

- Contributors of merged PRs in this release: see GH Release auto-notes.
- Translators: …
- Bug reports: …

### Phase 4b checklist

- [ ] WordPress EN post published (`www.routeconverter.com/releases/X.Y/`)
- [ ] WordPress DE post published (`www.routeconverter.de/releases/X.Y/`)
- [ ] Forum thread posted (EN)
- [ ] Forum thread posted (DE)
- [ ] "Latest Versions" widget updated on both sites
- [ ] `RouteConverter.xml` + `RouteConverter_de.xml` catalog updated
- [ ] GH Release body replaced with the curated block
- [ ] Verify `https://static.routeconverter.com/downloads/RouteConverterLinux.jar`
      resolves to X.Y (and equivalents for other OSes)
- [ ] Verify `https://static.routeconverter.com/downloads/previous-releases/X.Y/`
      lists the archive set with `*.app.zip` rename applied
- [ ] Verify `https://static.routeconverter.com/javadoc/X.Y/` shows X.Y API
```

---

## Releases

<!-- Append new versions above this line. Newest first. -->

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
distance, stale-download avoidance, and a number of UI smaller polish
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

- Build now runs on Java 25 (previously Java 17 was the floor).
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

- Java runtime requirement: 17 or newer (bundled installer ships a JRE).
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

### Phase 4b checklist

- [ ] WordPress EN post published (`www.routeconverter.com/releases/3.4/`)
- [ ] WordPress DE post published (`www.routeconverter.de/releases/3.4/`)
- [ ] Forum thread posted (EN)
- [ ] Forum thread posted (DE)
- [ ] "Latest Versions" widget updated on both sites
- [ ] `RouteConverter.xml` + `RouteConverter_de.xml` catalog updated
- [ ] GH Release body replaced with the curated block above
- [ ] Verify `https://static.routeconverter.com/downloads/RouteConverterLinux.jar`
      resolves to 3.4 (and equivalents for other OSes)
- [ ] Verify `https://static.routeconverter.com/downloads/previous-releases/3.4/`
      lists the archive set with `*.app.zip` rename applied
- [ ] Verify `https://static.routeconverter.com/javadoc/3.4/` shows 3.4 API

---

## Authoring conventions

- **No emoji** in user-facing copy unless the maintainer explicitly wants
  them on that release — Phase 4b destinations (forum, WordPress) render
  inconsistently.
- **Stable download URLs** (`RouteConverterLinux.jar`, not
  `RouteConverterLinuxOpenSource.jar`). The versioned filename only
  appears in the archive directory under `previous-releases/X.Y/`.
- **Keep DE blurb idiomatic**, not a literal translation. Same facts,
  natural phrasing.
- **No commit hashes** in user copy. Reference PR numbers if needed
  (`#NNNN` auto-links on GitHub).
- **Date is the tag date** (when `git push --follow-tags` fired), not
  the day Phase 4b finishes.
