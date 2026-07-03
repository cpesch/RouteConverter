# 00014 - Category-based POI overlay (show all POIs of a category)

## Status

Proposed. Created on July 3, 2026, as the remaining delta of issue
[#91](https://github.com/cpesch/RouteConverter/issues/91) (asakura42, +WilfriedBl,
+Baltasarq). The **term-search** half of #91 shipped in 3.4 (2026-06-07): the
Find Place dialog searches POIs from Mapsforge/OpenAndroMaps `.poi` databases and
`.map` files and jumps to a hit. This spec covers the half that is **not** done —
the OsmAnd-style ability to **browse a category and see all its POIs on the map
at once** (e.g. every `amenity=bank` in view) while planning a route. #91 is
being closed as delivered; this spec tracks the overlay feature separately.

The "see POIs persistently alongside the route while editing" aspect overlaps
issue #101 (multiple position lists on one map, split off from this same thread);
this spec reuses that display machinery where it lands and does not re-solve it.

## Context — what exists vs the gap

Shipped (code map, 2026-07-03):

- POI reads: `mapsforge-poi/.../MapsforgePoiLookup.java` opens `.poi` SQLite DBs
  (`poi_index`/`poi_data`/`poi_categories`); `MapsforgeMapLookup.java` extracts
  named items from `.map` files. Both wired behind `MapsforgePoiGeocodingService`
  / `AutomaticGeocodingService` and the `GeocodingServiceFacade`.
- Search: `FindPlaceDialog` — a **single free-text term**, bounding-box scoped,
  capped at `MAX_RESULTS=50`, shown as transient **magnifier markers** cleared
  when the dialog closes (`FindPlaceDialog.java:216,263`).
- Category data is present but hidden: `MapsforgeTagMatcher.CATEGORY_TAGS`
  (aeroway, amenity, building, craft, emergency, historic, highway, landuse,
  leisure, man_made, natural, office, place, railway, shop, sport, tourism,
  waterway) and the `.poi` `poi_categories` table — never enumerated for the
  user; category only echoed in a result column.

The gap: no category picker, no structured `key=value` query, no persistent
per-category map layer. You cannot pick "amenity=bank" and see all banks in view.

## Design

Add a **POI overlay** independent of the (unchanged) Find Place term search:

1. **Category enumeration.** Surface the available categories from the loaded
   `.poi` DB `poi_categories` table (fall back to the `CATEGORY_TAGS` allow-list
   for `.map`-extracted POIs). Present as a checkable list grouped by top-level
   key (amenity, shop, tourism, …).
2. **Category query.** Extend `MapsforgePoiLookup` with a **term-optional,
   category-filtered** query path: given selected category key(s)/value(s) and
   the visible bounding box, return matching POIs — reusing the existing SQL
   join on `poi_categories` but filtering by category instead of a `%term%`
   `LIKE`. Same bbox scoping and `.map` fallback (`MapsforgeMapLookup`).
3. **Persistent overlay layer.** Render the selected categories' POIs as a
   **toggleable map layer that stays while editing the route** — not the
   transient magnifier markers. Per-category marker (colour, and optionally a
   distinct icon; see decisions). The layer refreshes on pan/zoom within the
   visible bounds.
4. **Density handling.** A category overlay can return far more than 50 hits.
   Gate rendering below a zoom threshold, apply a per-category cap with a
   "zoom in to see more" hint, and/or cluster; never flood the map or block.
5. **Interaction.** Hovering/clicking a POI shows its name/category; a POI can
   be inserted into the route (reuse the Find Place insert path,
   `FindPlaceDialog.java:222-239`).

## Decisions (recommended defaults — confirm before build)

1. **Where the picker lives — a dedicated "Show POIs" control** (a Map-menu
   entry / small dockable panel with the category checklist + a master
   on/off), separate from Find Place (which stays single-term search).
   *(Alt: extend Find Place — rejected; it conflates jump-to-one with show-many.)*
2. **Marker style — colour-coded markers per top-level category** for the MVP
   (reuse the tokenised `waypoint.svg` `${color}` machinery). Distinct
   per-category icons can follow later (and could reuse an icon set from
   [spec 00013](00013-waypoint-icons-from-style.md) tier b if that ships).
3. **Density — zoom-gated + per-category cap + "zoom in" hint** (no clustering
   in the MVP; add later if needed).

## Behaviour / acceptance criteria

- A control lists the POI categories available for the current map/POI data;
  the user checks one or more.
- Checked categories render all their POIs within the visible map bounds as a
  persistent layer that **remains while editing the route** and updates on
  pan/zoom.
- Unchecking a category (or the master toggle) removes its POIs.
- With too many POIs for the current zoom, the overlay shows a bounded subset
  plus a clear "zoom in to see all" indication — the UI never freezes or floods.
- Works offline from a local `.poi` DB or `.map` extraction; online services are
  not used for the overlay.
- The existing Find Place term search is unchanged.

## Implementation touch points

- `mapsforge-poi/.../MapsforgePoiLookup.java` — add category enumeration
  (`poi_categories`) and a category-filtered, term-optional query
  (`search`/`searchMatches` `:153-227`); raise/parameterise `MAX_RESULTS` for the
  overlay path with bbox+zoom guards.
- `mapsforge-poi/.../MapsforgeMapLookup.java` — category-filtered extraction for
  `.map`-only maps.
- `.../MapsforgeTagMatcher.java` — expose `CATEGORY_TAGS` as the fallback
  category source.
- `mapsforge-mapview/.../mapsforge/MapsforgeMapView.java` — a **new persistent
  POI overlay** (a layer of markers, per-category styled), distinct from the
  transient `MagnifierPainter` (`:1331-1356`) and refreshed on map-bounds change.
- **New** category-picker UI (Map menu + panel) in `route-converter-gui`,
  separate from `FindPlaceDialog`.
- Reuse the DataSource download infra for `.poi` (already wired,
  `MapsforgePoiLookup.java:76-82`).

## Out of scope

- Term search + jump-to-POI (already shipped in 3.4).
- Showing multiple **route/waypoint** position lists at once — that is issue
  #101; this overlay reuses whatever persistent-layer machinery #101 introduces
  rather than duplicating it.
- Online-service category overlays (Nominatim/Photon/Google) — offline `.poi`/
  `.map` only.
