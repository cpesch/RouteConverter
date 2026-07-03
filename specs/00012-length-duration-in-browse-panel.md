# 00012 - Length and duration columns in the Browse panel

## Status

Proposed. Created on July 3, 2026, from issue
[#85](https://github.com/cpesch/RouteConverter/issues/85) (JimmyS83). The
request thread converged on a design; the remaining ambiguities were resolved
with the maintainer on 2026-07-03 (placement, missing-duration handling,
opt-in). The reporter **reconfirmed the requirement on 2026-07-03**, stressing
that **duration is as crucial as length** (it was missing from his original
text) and that both must be visible in the Browse panel where he compares
stored routes. Ready to implement.

## Context

The Browse panel is a route *library* browser: a category tree plus a table of
routes that today shows only **Name** (description) and **Creator**. The
reporter keeps tens/hundreds of local `.gpx` tracks under `.routeconverter/routes`
organised in folders and uses Browse to pick the next trip. To compare
candidates he needs **length (distance)** and **duration (time)** per route —
today those only appear on the Convert tab, forcing a click-to-Convert-and-back
for every route.

His concrete workflow (reporter, 2026-07-03): while planning an after-work ride
he switches between locally-stored routes and compares **length + time** to
choose the day's route — length hints at how urban/busy it is, and **time**
decides whether he'll "make it home in time". Both numbers must sit in the
Browse/map UI where he does the comparing, not one tab away. This makes
**duration a first-class requirement, not a nice-to-have** — the columns must
carry it, and the `–` fallback (below) only applies when a track genuinely has
no time data.

Two hard constraints from the maintainer rule out the obvious approaches:

1. **No bulk download** — RouteConverter must not fetch every file in a folder
   just to extract a few numbers (network overload).
2. **No server-side parsing** — the catalog server must not parse geometry on
   request (server overload).

### The converged design (issue #85)

Selecting a Browse row *already* downloads, parses and opens that route
(`BrowsePanel.openRoute` → `ConvertPanel.openPositionList`), after which the
Convert tab's length/duration are computed. So the numbers become available for
**exactly the routes the user opens**, at no extra cost. The design is therefore
**lazy and session-scoped**: a route's Length/Duration is blank until it is
opened this session; once opened it is cached in memory and shown. This honours
both constraints — nothing is downloaded or parsed that the user didn't already
open.

## Decisions (resolved 2026-07-03)

1. **Placement — two new Browse table columns** (`Length`, `Duration`), lazily
   filled per row after the route is opened. (Not a map overlay.)
2. **Missing duration — show `–`.** Distance and duration come from map
   rendering, not parsing (see Risks). A **track** distance is beeline; its
   duration comes only from recorded GPS timestamps, so a timestamp-less track
   has no duration → render `–` (exactly as the Convert tab already does).
   Distance still shows. No speed-assumption estimator is added.
3. **Opt-in — a preference toggle** shows/hides the two columns (the reporter
   asked for it "optionally"; other users keep the lean Name/Creator table).

## Behaviour / acceptance criteria

- A preference (e.g. *Browse: show length & duration*) toggles two extra
  columns in the Browse routes table. Default off (keeps current behaviour).
- With the toggle on, every Browse row shows `–` for Length and Duration until
  that route is opened this session.
- Opening a route (selecting its row, which already loads it) populates its
  Length + Duration once the route has been rendered/computed, and the matching
  Browse row updates in place.
- Values are **cached in memory keyed by the route URL** for the lifetime of the
  running application; reopening or revisiting the folder shows the cached
  values without recompute. Cache is not persisted (cleared on restart).
- Length is formatted like the Convert tab (`PositionHelper.formatDistance`,
  respecting the unit preference); Duration like the Convert tab
  (`Transfer.formatDuration`). A route with distance but no time shows the
  distance and `–` for duration.
- Toggling the preference off hides the columns without losing the cache.

## Implementation sketch (touch points)

Grounded in the current code (2026-07-03):

- **Table model** — `route-catalog/.../impl/RoutesTableModel.java`: widen
  `getColumnCount()` from 2 to 4; add a single-row update path
  (`fireTableRowsUpdated`) so one route can refresh without a full reload.
- **Renderers** — `route-converter-gui/.../renderer/RoutesTableCellRenderer.java`
  and `SimpleHeaderRenderer`: render columns 2/3 (Length, Duration) reusing
  `PositionHelper.formatDistance` / `Transfer.formatDuration`; header keys for
  the two new columns.
- **Session cache** — new `Map<String url, DistanceAndTime>` (none exists
  today). Keyed by `RouteModel.getUrl()` / `route.route().getUrl()`.
- **Fill hook** — the length/duration are produced when the open route is
  rendered and land in the single shared `DistanceAndTimeAggregator`
  (`RouteConverter.getDistanceAndTimeAggregator()`), the same source the Convert
  tab's `LengthToJLabelAdapter` reads via `getTotalDistanceAndTime()`. Add a
  `DistancesAndTimesAggregatorListener` (or piggy-back where
  `LengthToJLabelAdapter.updateAdapterFromDelegate` fires); when the aggregator
  settles, correlate the total back to the URL that `BrowsePanel.openRoute()`
  just opened, write it into the cache, and fire the row update.
- **Preference** — a boolean in the existing preferences plumbing + a
  view/menu toggle that adds/removes the two columns.

## Risks / notes

- **Stats come from map rendering, not parsing.** Length/duration only exist
  after the opened route has been drawn; for a routed **Route** (street network)
  they depend on the async routing engine (BRouter/GraphHopper/GoogleMaps)
  finishing — the value may appear beeline-first then refine to the routed
  figure. The fill **must be event-driven** (fire when the aggregator settles
  for the just-opened URL), never read synchronously at selection time.
- **Correlation.** Opening is asynchronous; ensure the settled total is
  attributed to the URL actually opened (guard against a fast folder switch or
  a second open racing the first).
- **Tracks/Waypoints without timestamps** → duration `–` by design (item 2).
- **No new persistence, no new parsing, no server/catalog change** — a
  session-scoped in-memory map is sufficient.

## Out of scope

- Pre-computing stats for un-opened routes (violates the no-bulk-download
  constraint).
- A speed-assumption duration estimator for timestamp-less tracks.
- Persisting the cache across restarts, or a map-corner overlay (considered and
  dropped in favour of columns).
