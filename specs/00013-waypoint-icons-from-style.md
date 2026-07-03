# 00013 - Per-waypoint icons from KML/KMZ (fetch + render real icons)

## Status

Proposed. Created on July 3, 2026, from issue
[#89](https://github.com/cpesch/RouteConverter/issues/89) (Smig0l, 2024-08-06).
The issue thread stalled unanswered — the maintainer asked what fidelity was
wanted ("match Google Earth exactly?") and got no reply. Fidelity tier decided
by the maintainer on 2026-07-03: **tier (c) — fetch and render the actual
referenced icons** (pixel-faithful to Google Earth). Round-trip: **preserve** the
icon on save. Tiers (a)/(b) are recorded below as the lighter alternatives that
were not taken.

## Context

KML/KMZ files exported from Google Earth carry a per-placemark icon via
`<styleUrl>` → `<Style>`/`<StyleMap>` → `<IconStyle>` → `<Icon><href>` (a PNG
URL) plus an `<IconStyle><color>` tint. RouteConverter renders **every** waypoint
as an identical **black circle**, so a Google-Earth map of distinct icons becomes
a wall of identical dots.

Root cause (code map, 2026-07-03):

- Waypoint markers are one **cached** bitmap: `MapsforgeMapView.createWaypointIcon()`
  renders `waypoint.svg` once, colored by the single app-wide
  `MapPreferencesModel.waypointColorModel` (default `FF000000`). Every marker
  reuses that instance — no per-position variation.
- `KmlFormat.parseCharacteristics()` uses `<styleUrl>` **only** to classify a
  placemark as Waypoints/Route/Track (`KmlFormat.java:145-167`). The
  `<IconStyle>` `<color>` and `<Icon><href>` are **never captured** on read.
- `Wgs84Position` (base of `KmlPosition`/`GpxPosition`) has **no** per-point
  icon/href/color field.
- Icon data is also **dropped on save**: `Kml22Format.createWayPoints()`
  (`:242-263`) builds a fresh placemark and hardcodes `setStyleUrl("#waypoint")`
  — so editing a KML in RouteConverter silently strips its icons today (a
  data-loss bug this spec also fixes).

## Chosen approach — tier (c): fetch + render the real icon

Capture each waypoint's real icon reference, resolve it to image bytes (remote
URL, KMZ-embedded file, or Google icon-palette URL), decode it to a marker
bitmap, and render it — with an async, fail-safe path that never blocks the UI
and falls back to the default dot. Preserve the reference on save.

Because this is the heaviest tier, it is **phased** so a useful subset ships
first and the fragile parts are additive:

- **Phase 1 — parse + display (embedded + absolute URLs).** Capture the icon
  href (+ color); render KMZ-**embedded** images and absolute `http(s)` hrefs.
  Async fetch off the EDT, in-memory cache, fallback to the default dot on any
  failure. Display-only.
- **Phase 2 — round-trip preserve.** Write the `<IconStyle><Icon><href>` (+color)
  back on KML save; re-embed images on KMZ save so an edit doesn't strip icons.
- **Phase 3 — Google icon-palette resolution.** Resolve palette-style refs like
  `#icon-1899-0288D1-nodesc` (icon id `1899` + tint `0288D1`) whose `<Style>`
  lacks a direct `<href>`, via a Google icon-CDN URL template, applying the color
  as a tint. Optional; skip if the `<Style>` already carries a concrete href.

## Design

- **Model:** add nullable `String iconHref` and `Integer iconColor` (ARGB tint)
  to `Wgs84Position`, inherited by `KmlPosition`.
- **KML/KMZ read:** for a waypoint placemark, resolve `<styleUrl>` →
  `<Style>` (or `<StyleMap>` "normal" pair) → `<IconStyle>` → `<Icon><href>` and
  `<color>`; store on the position. KML color order is `aabbggrr`.
- **Icon resolution (`IconResolver`, new):**
  - KMZ-embedded relative href (e.g. `files/foo.png`) → read bytes from the KMZ
    archive.
  - absolute `http(s)` href → download, cache; bound memory (LRU keyed by href),
    optional on-disk cache with TTL.
  - Phase 3: palette id with no direct href → Google icon-CDN URL template.
  - decode bytes → `AwtBitmap`; apply `iconColor` tint if present.
  - all resolution is **asynchronous**; the render shows the default dot as a
    placeholder and swaps in the real icon when ready; **any** failure (404,
    offline, decode error, unsupported) leaves the default dot.
- **Render:** in `createWaypointIcon()`, drop the single-instance cache; key a
  bitmap cache per resolved icon; the marker reads `iconHref`/`iconColor` off the
  position via `PositionWithLayer.getPosition()`.
- **KML/KMZ write (Phase 2):** emit `<Style><IconStyle><Icon><href>` (+color)
  per waypoint in `createWayPoints`; re-embed image bytes for KMZ.

## Behaviour / acceptance criteria

- Loading a Google-Earth KMZ with embedded icons renders each waypoint's actual
  icon; a KML with absolute icon URLs renders them once fetched.
- Until an icon resolves (or if it fails), the waypoint shows the default dot —
  the map never blanks, blocks, or crashes on a dead/slow link.
- Two placemarks with different icons render as visibly different markers.
- Fetching happens off the EDT; a slow/unreachable host does not freeze the UI.
- (Phase 2) Saving a KML/KMZ loaded with icons preserves them — reload shows the
  same icons; no silent normalization to black.
- Waypoints without any icon reference are unchanged (default dot).

## Implementation touch points

- `navigation-formats/.../base/Wgs84Position.java` — add `iconHref` +
  `iconColor` fields + accessors.
- `navigation-formats/.../kml/KmlFormat.java` (+ `Kml22Format`,
  `Kml22BetaFormat`, `Kml21Format`) — read `styleUrl`→`Style`/`StyleMap`→
  `IconStyle`→`Icon` href + color for waypoint placemarks (new; today only
  `styleUrl` for characteristics at `:145-167`, `:209-223`); write them back in
  `createWayPoints` (`Kml22Format.java:242-263`).
- `navigation-formats/.../kml/KmzFormat.java` / `Kmz22Format.java` — extract
  embedded image bytes on read; re-embed on write (Phase 2).
- **New** `IconResolver` (async fetch + decode + bounded cache + fallback) — the
  network/archive/palette logic, isolated from the render loop.
- `mapsforge-mapview/.../mapsforge/MapsforgeMapView.java` —
  `createWaypointIcon()` (`:479-504`): per-icon bitmap cache, read
  `iconHref`/`iconColor`; `WaypointOperation.add` (`:277-288`): kick async
  resolution + default-dot placeholder + swap on completion.

## Risks / notes

- **Network I/O must be fully asynchronous / off the EDT.** The render path and
  file-open must never block on a fetch. This is the main engineering risk.
- **Dead links are common** — old Google My Maps / Earth icon URLs frequently
  404. A robust fallback to the default dot is mandatory, not optional.
- **Remote fetch on file open is a privacy/SSRF consideration** — opening a KML
  would contact a remote server. Consider gating behind the same trust posture
  as existing online map/tile fetching, or a preference; note it in the UI.
- **KMZ archive handling** — relative hrefs resolve to entries inside the KMZ;
  needs archive read (and re-write for round-trip).
- **Memory** — many distinct icons per file; bound the cache (LRU), consider a
  disk cache with TTL, and downscale to marker size.
- **Google icon palette (Phase 3)** — `#icon-NNNN-RRGGBB` refs may lack a direct
  href; resolving them depends on a Google icon-CDN URL shape that can change.
  Keep it optional and behind the same fallback.

## Alternatives not taken (recorded so the grounding isn't lost)

- **(a) color the marker** — give each dot its KML `<IconStyle>` color only
  (`0288D1`→blue), reusing the existing `${color}`/`${opacity}` tokens in
  `waypoint.svg`; no network, no assets, KML-only. Cheapest; fixes only the
  "all identical" complaint, not the icon shapes.
- **(b) bundled icon set** — add `String symbol` to `Wgs84Position`, read GPX
  `<sym>` (`Gpx11Format.java:178`, currently ignored) + KML icon id, ship a
  curated icon set + name→asset lookup, select bitmap by symbol. Reproducible +
  offline + also surfaces GPX `<sym>`, but not pixel-faithful to Google Earth.

## Out of scope

- An in-app icon editor / picker (this spec renders + preserves parsed icons, it
  does not author new ones).
- GPX `<sym>` rendering (belongs to alternative (b); tier (c) is KML/KMZ-focused).
