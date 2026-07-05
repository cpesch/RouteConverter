---
name: 00015-show-nonselected-lists-readonly
status: implemented (gray read-only MVP, needs manual UI test)
phases_done: [gray-readonly-mvp]
phases_next: [manual-ui-verification]
last_touched: 2026-07-04
---

# 00015 - Show non-selected position lists read-only (gray)

## Status

Proposed. Created on July 3, 2026, from issue
[#101](https://github.com/cpesch/RouteConverter/issues/101) (Baltasarq,
2026-06-01), which was split off the #91 POI thread. Scope tier decided by the
maintainer on 2026-07-03: **the gray read-only MVP** — draw every non-selected
position list in the file as gray, non-interactive; keep editing exactly as
today (one list, chosen by the existing combo). The more ambitious forum design
(Mogh's panel with per-list colors + visibility checkboxes + multi-edit +
multiple elevation profiles) is recorded below as the future evolution, not this
spec's scope.

This is the **linchpin** for two other specs: the "persistent POIs while editing"
gap in [`00014`](00014-poi-category-overlay.md) and any "show length/duration
context" ambition reuse the read-only secondary-layer machinery introduced here.

## Context — why it's a "huge effort" (code map, 2026-07-03)

RouteConverter is built around **one displayed list**:

- A file's full list collection lives in `FormatAndRoutes.routes`
  (`navigation-formats/.../base/FormatAndRoutes.java:34-62`) — all lists ARE in
  memory.
- But everything downstream binds to a single `PositionsModelImpl` holding one
  `BaseRoute` (`route-converter-gui/.../models/PositionsModelImpl.java:63`).
- `MapsforgeMapView.initialize(positionsModel, …)` (`mapsforge-mapview/.../MapsforgeMapView.java:194`,
  called from `RouteConverter.java:390`) receives **only that single model** and
  drives one "current" updater over one shared layer set (`UpdateDecoupler`,
  `:1358`). Switching the combo (`ConvertPanel.java:393-400` →
  `FormatAndRoutesModelImpl.setSelectedRoute`, `:206` → `positionsModel.setRoute`)
  performs a full **teardown/rebuild** (`replaceRoute`, `:1362`).
- The non-selected lists are reachable via `FormatAndRoutesModel.getRoutes()` but
  **the map view never references it** — they are neither drawn nor discarded.

So "keep the others drawn" is structurally precluded today: there is no
per-list layer bookkeeping and no second render context.

The good news (also from the map): the styling is trivial — line color is an
injected `ColorModel` at draw time (`TrackRenderer.java:48`, `RouteRenderer.java:206`;
waypoint tint at `MapsforgeMapView.createWaypointIcon:483`), and the
`LayerManager` (`:350`, `addLayers` `:794`) accepts arbitrary layers. The blocker
is that **nothing drives a second set**, not the plumbing.

## Design (gray read-only MVP)

Keep the active list's pipeline exactly as today; add a **parallel, read-only,
gray render path** for every other list in the file:

1. **Give the map the full collection.** Pass `FormatAndRoutesModel` (or a
   read-only view of `getRoutes()`) into `MapsforgeMapView.initialize` alongside
   the existing selected `PositionsModel`.
2. **Secondary render path.** For each non-selected `BaseRoute`, render with the
   existing `RouteRenderer`/`TrackRenderer`/waypoint-marker code but: a **gray**
   `ColorModel`, **plain non-draggable markers** (never `DraggableMarker`/
   `selectionUpdater`), and dispatch per the list's own characteristics
   (Route/Track/Waypoints — `getEventMapUpdaterFor`, `:706`). Suppress routing/
   `DistanceAndTimeAggregator` computation for gray Route-type lists (they are
   read-only; don't trigger routing engines for lists the user isn't editing).
3. **Separate layer group / z-order.** Put the gray lists on their own
   `GroupLayer` below the active `overlaysLayer` (`:184`) so the editable list
   draws on top and can be cleared/redrawn independently.
4. **Rebuild on switch.** Hook `FormatAndRoutesModel` selection changes
   (`FormatAndRoutesModelImpl.java:206`): on switch, the newly-selected list
   moves to the editable pipeline and the previously-selected one joins the gray
   set (recompute "all lists except selected").
5. **Strictly non-interactive.** The gray layers must be excluded from ALL
   hit-testing and edit paths — add/move/delete (`movePosition:1085`,
   `AddPositionAction:1201`, `DeletePositionAction:1235`, `getClosestPosition`)
   must only ever see the active `positionsModel`, never snap onto a gray list.

## Behaviour / acceptance criteria

- Loading a file with several lists (e.g. GPX with 1 route + 1 waypoint list)
  shows the combo-selected list editable in color, and every other list drawn in
  gray at the same time.
- Editing (add/move/delete/drag) affects **only** the selected list; gray lists
  never receive edits and are not draggable/selectable.
- Switching the combo makes the newly-selected list editable/colored and the
  previously-selected one gray — both remain visible.
- Gray Route-type lists do not trigger routing-engine computation.
- Single-list files behave exactly as today (no gray set, no regression).
- Hiding is out of scope for the MVP (all non-selected lists show gray); see
  future evolution for per-list visibility toggles.

## Implementation touch points

- `mapsforge-mapview/.../mapsforge/MapsforgeMapView.java` — `initialize`
  signature (`:194`) to also take the route collection; a second `GroupLayer` +
  secondary updater/renderer instances with a gray `ColorModel`; keep gray layers
  out of `getClosestPosition`/hit-testing; companion refresh for the
  `replaceRoute`-triggering listeners (`RepaintPositionListListener:1440`,
  characteristics/routing listeners `:1445/:1452`).
- `route-converter/.../RouteConverter.java:390` — pass the `FormatAndRoutesModel`
  into `initialize`.
- `route-converter-gui/.../models/FormatAndRoutesModelImpl.java:206` — surface
  selection changes so the map can recompute the gray set.
- Reuse `renderer/RouteRenderer`, `renderer/TrackRenderer`, and the waypoint
  marker path with a gray color source (no new renderer logic).

## Risks

- **Edit-target ambiguity** (primary): add-position uses map-click coordinates
  and nearest-position logic; that logic must ignore gray lists entirely.
- **Performance / memory:** N lists ⇒ N× `Polyline`/`Marker` layers on one
  `LayerManager`, rebuilt on each switch (only the active list has the
  incremental fast-path). Large multi-track GPX files need a cap or lazy
  rendering.
- **Per-list characteristics:** each list may be Route/Track/Waypoints — the gray
  path must dispatch per list, not assume one type.
- **Repaint triggers:** existing color/characteristics/routing listeners assume
  one list; each needs a companion that also refreshes the gray set.

## Implementation notes (2026-07-04)

Implemented as designed, with one structural deviation that earlier manual
attempts likely tripped over: `FormatAndRoutesModel` lives in
`route-converter-gui`, which **depends on** `mapview` (where `MapView` and
`PositionsModel` live) — so `MapView.initialize` cannot take
`FormatAndRoutesModel` without a circular module dependency. Solution: new
minimal interface `PositionListsModel` (`mapview/.../converter/gui/models/`,
same package) with `getRoutes()`, `getSelectedRoute()` and list-data listener
registration; `FormatAndRoutesModel` extends it, so no caller changes.

- `MapView.initialize`/`MapsforgeMapView.initialize` take the extra
  `PositionListsModel`; `RouteConverter.setMapView` passes
  `getConvertPanel().getFormatAndRoutesModel()` (the undo wrapper delegates
  listeners to the impl, so events flow).
- Gray render path: one `GroupLayer` (`nonSelectedPositionListsLayer`)
  inserted directly above the map/background tile layers, i.e. below the
  active list's layers and selection markers. Rebuilt wholesale from
  `getRoutes() minus getSelectedRoute()` on every `ListDataEvent` (selection
  switch fires `contentsChanged`, list add/remove fire interval events,
  `setRoutes` fires both), on map/theme switch, and on color/line-width
  changes.
- Per-list dispatch: Waypoints → plain gray `Marker` per position (gray
  variant of waypoint.svg, opacity 0.5); Route/Track → a single gray
  `Polyline` over the stored positions (one layer per list — cheap, no
  per-pair layers, no routing engine, no DistanceAndTimeAggregator).
- Read-only for free: plain `Marker`/`Polyline` layers have no tap/drag
  handling, and all edit paths (`getClosestPosition`, add/move/delete) only
  consult the active `positionsModel`, so gray lists cannot be edited or
  snapped onto.
- Single-list files: gray set is empty, behaviour unchanged.

## Future evolution (not this spec)

Once the read-only secondary pipeline exists, Mogh's forum design is an additive
step: a **left panel listing all lists** with **per-list visibility checkboxes**
and **distinct colors** (instead of flat gray) — this directly answers cpesch's
"how do you know which list a position belongs to?" better than gray. Full
per-list **editing** and **multiple elevation profiles** (Mogh's most ambitious
parts, and the unsolved multi-profile question) remain deliberately out of scope
until the display foundation is proven.

## Out of scope

- Per-list colors, a list panel, and visibility checkboxes (future evolution).
- Editing more than one list at a time.
- Multiple simultaneous elevation profiles.
