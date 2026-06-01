# Plan: collapse catalog-sources.zsh + catalog-mirror-jobs.zsh into datasource XML

Handover document for a later session. Self-contained — assumes no prior context.

## Goal

Eliminate both shell config files:

- `download-tools/catalog-sources.zsh` — 8-arg rows feeding `ScanWebsite` CLI flags.
- `download-tools/catalog-mirror-jobs.zsh` — 5-arg rows feeding `wget` mirror jobs.

Make `<datasource id="...">` XML in `datasource-catalog.xsd` the single source of truth for both scan and mirror configuration.

## Background

### Current shell-file shapes

`catalog-sources.zsh` rows (see `add_catalog_source`):
```
id, url, baseUrl, level, extensions, type, includes, excludes
```
Feeds `ScanWebsite.java` CLI flags `--url --baseUrl --level --extension --type --include --exclude`.

`catalog-mirror-jobs.zsh` rows (see `add_mirror_job`):
```
id, url, level, accept patterns, cleanup paths
```
Feeds `wget -m -np -e robots=off --wait 1 -P <mirror> [-l N] [--accept …] <url>` plus post-success `rm` of cleanup paths.

### Datasource XML (datasource/src/main/doc/datasource-catalog.xsd)

`datasourceType` already carries: `id, name, href, baseUrl, directory, action` plus `<file>`, `<map>`, `<theme>` children with `uri` + checksums + optional bounding box.

### Driver

`sync-download-catalog.sh` sources both .zsh files, iterates `CATALOG_IDS` / `MIRROR_IDS` arrays, dispatches `SnapshotCatalog`, `ScanWebsite`, `UpdateCatalog` jars + wget.

### Server backward-compat (RouteConverter 3.3)

Server omits `<source>` from `/v1/datasources/<id>.xml` by default. Deployed 3.3 clients use JAXB bindings without a `<source>` binding and would reject the document otherwise.

Tools that need `<source>` must append `?includeSource=true` to every read of `/v1/datasources/<id>.xml`. PUT path unchanged — server accepts `<source>` on write regardless of query param.

Reference: `docs/00008-server-plan-backward-compatible-datasource-source-for-3-3-clients.md`.

Affected reads:
- `SnapshotCatalog` full-snapshot fetch.
- Any direct `DataSourceManager` GET against `/v1/datasources/`.
- `UpdateCatalog` read-before-merge step (if present).
- `MigrateCatalogSources` snapshot load.
- `SnapshotCatalogLoader` in the GUI.

### User direction

- Datasource is authoritative for URL. **Mirror url = datasource `baseUrl`** — drop redundant mirror url field. Scan url stays distinct (it scrapes an HTML index page; `baseUrl` resolves relative `<file uri>` later — they often differ, e.g. `andromaps-themes`).
- Scan config and mirror config share `level`, `include`, `exclude`. Collapse both into one `<source>` element with a boolean `mirror` flag.

## Schema extension (additive, backward compatible)

Add one optional child element `<source>` to `datasourceType` in `datasource/src/main/doc/datasource-catalog.xsd`:

```xml
<xsd:complexType name="datasourceType">
  <xsd:sequence>
    <xsd:element name="source" type="sourceType" minOccurs="0"/>
    <xsd:element name="file"   .../>
    <xsd:element name="map"    .../>
    <xsd:element name="theme"  .../>
  </xsd:sequence>
  <xsd:attribute name="id"        use="required"/>
  <xsd:attribute name="name"/>
  <xsd:attribute name="href"/>
  <xsd:attribute name="baseUrl"/>
  <xsd:attribute name="directory"/>
  <xsd:attribute name="action"    type="actionType"/>
</xsd:complexType>

<xsd:complexType name="sourceType">
  <xsd:sequence>
    <xsd:element name="include" type="xsd:string" minOccurs="0" maxOccurs="unbounded"/>
    <xsd:element name="exclude" type="xsd:string" minOccurs="0" maxOccurs="unbounded"/>
  </xsd:sequence>
  <xsd:attribute name="url"   type="xsd:string"/>
  <xsd:attribute name="level" type="xsd:int"/>
</xsd:complexType>
```

Semantics:

- `<source url=…>` is the starting point for both scan (HTML index `ScanWebsite` crawls) and mirror (wget target).
- `url` is **optional**. When absent, falls back to `datasource@baseUrl`. Most ids can omit `url` since their scan index = mirror root = `baseUrl`. Only ids with a distinct HTML index (e.g. `andromaps-themes`) need it.
- Presence of `<source>` element triggers both scan AND mirror phases. No separate enable flag.
- `<include>` = positive glob filter (`*.zip`, `*-latest.osm.pbf`) applied by scan AND passed straight to wget `--accept`. Subsumes the old separate `<extension>` field — a bare extension is just `*.<ext>`.
- `<exclude>` = negative glob filter applied by scan (drop matching URI from catalog) AND by post-wget cleanup (`rm` files in mirror tree matching the glob, relative to mirror root). Same glob syntax as `<include>`.
- `<source level=…>` = depth, used by both scan and wget.
- `ScanWebsite` derives target collection (`<file>`/`<map>`/`<theme>`) without an explicit `type` attribute — per-id behavior preserved by existing logic or extension heuristic. Drops one config knob vs current `catalog-sources.zsh`.

Design notes:

- Lists as repeated elements, not pipe-separated attrs — XML-native, no parsing.
- All `<source>` attributes optional. Element itself is the marker.
- Absence of `<source>` = neither scan nor mirror runs for this id. Use this for ids hosted on RouteConverter's own static server (srtm, sonny, routeconverter-maps, routeconverter-themes) — no automation needed.
- Empty `<source/>` = scan + mirror against `baseUrl` with no filters and default level.
- Operator can still scope a run with `--id <single-id>` from `sync-download-catalog.sh`.

## Migration examples

`openandromaps` (scan + mirror against baseUrl — url omitted):

```xml
<datasource id="openandromaps" name="Openandromaps"
            baseUrl="https://ftp.gwdg.de/pub/misc/openstreetmap/openandromaps/mapsV5/"
            directory="maps/openandromaps5" action="Extract">
  <source>
    <include>*.zip</include>
  </source>
  <map uri="…">…</map>
</datasource>
```

`andromaps-themes` (HTML index ≠ baseUrl — url required):

```xml
<datasource id="andromaps-themes"
            baseUrl="https://www.openandromaps.org/wp-content/files/themes/" …>
  <source url="https://www.openandromaps.org/kartenlegende/andromaps_hc/">
    <include>*.zip</include>
  </source>
  <theme uri="…">…</theme>
</datasource>
```

`brouter-profiles` (url = baseUrl → omit; exclude as glob, matches post-wget cleanup path):

```xml
<source level="1">
  <include>*.brf</include>
  <include>*.dat</include>
  <exclude>*/dummy.brf</exclude>
</source>
```

`freizeitkarte` (url = baseUrl → omit; excludes for branded variants):

```xml
<source>
  <include>*.zip</include>
  <exclude>freizeitkarte-v5.zip</exclude>
  <exclude>fzk-outdoor-contrast-v5.zip</exclude>
  <exclude>fzk-outdoor-soft-v5.zip</exclude>
</source>
```

`graphhopper` (currently 7 mirror rows — consolidate to one; baseUrl = geofabrik root):

```xml
<source level="1">
  <include>*-latest.osm.pbf</include>
</source>
```
Specific country uris already represented in `<file>` entries.

`routeconverter-maps` (no `<source>` — hosted on our own server, no automation needed):

```xml
<datasource id="routeconverter-maps"
            baseUrl="https://static.routeconverter.com/maps/" …>
  <!-- no <source> element -->
  <map uri="…">…</map>
</datasource>
```

## Tasks (ordered)

### 1. Schema + JAXB

- Edit `datasource/src/main/doc/datasource-catalog.xsd` per schema section above.
- Regenerate JAXB classes: `./mvnw -pl datasource generate-sources`.
- Check emitted classes in `slash.navigation.datasources.binding`.
- Extend `datasource/src/main/java/slash/navigation/datasources/DataSource.java`:
  - Add `Source getSource()`.
  - Create `Source` interface (`getUrl/getLevel/getIncludes/getExcludes`). `getUrl()` returns null when omitted — caller falls back to `datasource.getBaseUrl()`.
- Update `DataSourceImpl` + JAXB adapters in `datasource/src/main/java/slash/navigation/datasources/impl/`.

### 2. Migrate config into XMLs

- Run the one-off migration tool `download-tools/src/main/java/slash/navigation/download/tools/migration/MigrateCatalogSources.java`:
  - Parse `catalog-sources.zsh` (reuse parsing pattern from `MirrorJobParser`).
  - Parse `catalog-mirror-jobs.zsh` (existing `MirrorJobParser` already does this).
  - Snapshot read **must use `?includeSource=true`** (Background → Server backward-compat) so already-migrated ids round-trip without losing `<source>`. Otherwise the migrator would re-overwrite stripped XML and clobber prior work.
  - If the run begins with a fresh server snapshot, that snapshot fetch must also append `?includeSource=true` for every datasource read. Never refresh the migration input from bare `/v1/datasources/<id>.xml` URLs.
  - For each id present in either file, load `~/.routeconverter/snapshot-api.routeconverter.com/datasources/<id>.xml`, merge into a single `<source>` element, insert before existing `<file>/<map>/<theme>` blocks, write XML back preserving formatting.
  - Merge rule: `url`, `level`, `include`, `exclude` come from the catalog-source row, normalized to glob form (`.zip` → `*.zip`; bare extension token → `*<token>`). Mirror-row `accept`/`level`/`cleanup` values fold into the same `<include>`/`<level>`/`<exclude>` — already glob form.
  - Omit `source@url` when it equals `datasource@baseUrl` (most cases). Keep it only when scan index differs from mirror root.
- Run once. Diff. Commit results to server-side XML repo.
- Manual edge cases the migration tool must handle:
  - Multiple mirror rows for same id (`graphhopper` has 7, `freizeitkarte` has 2). Consolidate into one `<source>`. Drop per-row urls.
  - Mirror host differs from baseUrl (`openandromaps-themes` uses gwdg, baseUrl is openandromaps.org). Drop mirror-only host per user direction.
  - Sources in catalog-sources.zsh with no matching mirror row → `<source>` still added; mirror phase will hit it. Operator can `--id`-filter to skip. Sources hosted on `static.routeconverter.com` (`srtm*`, `sonny*`, `routeconverter-maps`, `routeconverter-themes`) → **omit `<source>` entirely** — no automation needed.
  - Sources in catalog-mirror-jobs.zsh with no matching catalog row → `<source>` with mirror url in `url` attr (or omit when equal to `baseUrl`).
  - Scan/mirror `include` value conflicts (e.g. scan uses `latest.osm.pbf` ext, mirror uses `*-latest.osm.pbf` glob): prefer mirror form (`*-latest.osm.pbf`) so wget works directly. Scan filter must accept globs — `ScanWebsite` matches via wildcard suffix logic.

### 3. ScanWebsite refactor

- File: `download-tools/src/main/java/slash/navigation/download/tools/ScanWebsite.java`.
- When `--id` + `--server` provided and no other config flags, load datasource from snapshot, read scan config from `<source>`.
- Snapshot fetch URL: append `?includeSource=true` (Background → Server backward-compat). Without it, server strips `<source>` and ScanWebsite fails with "no scan config" even on migrated ids.
- Keep CLI flags as overrides for one release (deprecation window).
- Fail fast when neither CLI flags nor `<source>` element present.

### 4. Driver refactor

Replace shell loops in `sync-download-catalog.sh`:

- Drop `add_catalog_source` / `add_mirror_job` arrays + sourcing the .zsh files.
- Iterate ids by listing snapshot `datasources/*.xml`.
- Phase `scan`: for each datasource with `<source>` → invoke `ScanWebsite` with `--id` only (it reads XML).
- Phase `mirror`: for each datasource with `<source>` → build wget command from XML (target = `source@url` or `baseUrl` fallback).

Better: introduce `download-tools/src/main/java/slash/navigation/download/tools/CatalogSync.java` orchestrator that drives all phases reading XML directly. `sync-download-catalog.sh` shrinks to ~20 lines invoking the jar.

- All snapshot/fetch requests in this orchestrator append `?includeSource=true` so `<source>` reaches the driver. Centralize this in one place (`DataSourceManager` URL builder or `SnapshotCatalog` invocation) — do not sprinkle the query param across call sites.

### 5. GUI cleanup

Files under `download-tools/src/main/java/slash/navigation/download/tools/gui/`:

- Delete `MirrorJobParser.java`, `MirrorJobSpec.java`, `download-tools/src/test/java/slash/navigation/download/tools/gui/MirrorJobParserTest.java`.
- `MirrorJobRow.java`: wrap `DataSource` + its `Source` directly.
- `CatalogMirrorFrame.java`:
  - Drop "Mirror jobs file" text field, browse button, preference (`PREFERENCE_JOB_FILE`).
  - Source = snapshot only.
  - Filter `dataSourceService.getDataSources()` where `getSource() != null`.
- `MirrorJobCommandBuilder.java`:
  - Signature change: `buildWgetCommand(DataSource ds, Source source, Path mirrorRoot)`.
  - Accept list (wget `--accept`) = `source.getIncludes()`; when empty, derive from distinct uri extensions of `ds.getFiles()/getMaps()/getThemes()` (as `*<ext>` patterns).
  - Exclude list = `source.getExcludes()` — applied as post-wget `rm` of paths relative to mirror root.
  - wget target URL = `source.getUrl()` when set, else `ds.getBaseUrl()`.
- `SnapshotCatalogLoader.java` + `SnapshotJobInfo.java`: extend to expose `Source` from datasource. No more lookup by id from external parser. Loader must request `?includeSource=true` when fetching from server (Background → Server backward-compat).

### 6. Delete shell config

```
git rm download-tools/catalog-sources.zsh
git rm download-tools/catalog-mirror-jobs.zsh
```

Update docs:

- `download-tools/AUTOMATION.md` — rewrite "Configuration" section: describe `<source>` XML instead of shell args.
- `download-tools/README.md` — update GUI section: no more job file picker.

### 7. Tests

- Unit test JAXB round-trip of new `<source>` element in `datasource/src/test/java/`.
- Unit test for accept-pattern auto-derivation in `MirrorJobCommandBuilder`.
- Unit test `CatalogSync` skips datasources without `<source>` for both scan and mirror phases.
- Integration test `MigrateCatalogSources` against committed sample .zsh + sample XML fixtures.

## Open decisions for the next agent

- **Migration write target**: local snapshot then upload via `UpdateCatalog`, or direct edit of server-side XML repo? Confirm with user. Server is source of truth; local snapshot is a copy.
- **Scan target collection**: `<source>` does not carry `type`. `ScanWebsite` must derive whether found URIs land in `<file>`, `<map>`, or `<theme>`. Options: per-id default in code, extension heuristic, or read existing collection contents in the datasource to decide. Confirm strategy before refactor.
- **`level` semantics**: shared between scan (HTML crawl depth) and wget mirror (`-l`). Current shell config has cases where they differ (`brouter-profiles`: scan unset, mirror=1). Collapsed model forces one value — confirm acceptable per id during migration or split into two datasource ids if a real conflict exists.
- **`include` format**: canonical form = wget glob (`*.zip`, `*-latest.osm.pbf`). `ScanWebsite` must accept globs (matches via wildcard suffix logic) since the old separate `<extension>` field is folded into `<include>` as `*<ext>`. Confirm `ScanWebsite` already does suffix match or extend it.
- **One-release backward compat**: keep `ScanWebsite` CLI flags accepted alongside XML for one release, or hard cut? Soft cut reduces risk.

## Risk notes

- JAXB regen may shuffle generated class field order. Diff carefully before committing.
- Server-side datasource XML files likely live in a separate repo (`api.routeconverter.com`). Confirm write access before step 2.
- `SnapshotCatalogLoader.loadAllDataSources` already drives via JAXB — picks up new fields automatically once classes regenerated.
- Migration tool runs against a snapshot fetched from server. If users have unsaved local edits, they get clobbered. Document workflow: snapshot → migrate → upload.
- Single `<source>` ties scan and mirror lifecycles. If a real case emerges where scan and mirror need divergent `level`/`include`/`exclude`, schema must split back into `<scan>` + `<mirror>` — escape hatch documented but not implemented.
- Forgetting `?includeSource=true` in a tool causes silent data loss on round-trip: tool reads stripped XML, writes back via `UpdateCatalog`, server merges (preserves `<source>` if merge semantics intact) **or** replaces (clobbers `<source>` if a tool uses full PUT). Mitigation: centralize URL building, audit all `/v1/datasources/` reads, add a test that snapshot-load → write → reload preserves `<source>`.

## Acceptance criteria

- `catalog-sources.zsh` and `catalog-mirror-jobs.zsh` deleted.
- `sync-download-catalog.sh` runs without sourcing any shell config; reads everything from XML.
- `CatalogMirrorApp` GUI runs without a mirror job file picker.
- All existing scan + mirror behavior reproducible from XML alone.
- All snapshot reads in modified tools append `?includeSource=true`; no call site fetches `/v1/datasources/` without it.
- Test suite green.
