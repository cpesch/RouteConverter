# `analyze` JSON contract

`RouteConverterCmdLine analyze <file> [--brouter-segments <dir>]` reads a file
with the full RouteConverter parser and writes a **single line** of JSON to
stdout describing the file's aggregated metadata. This is the authoritative
contract consumed by the rc-site `analyze_files` Django management command
(specs/00055). Field names here match the `FileMetadata` columns (camelCase on
the wire, snake_case in Django).

## Invocation

```
java -Xmx1g -jar RouteConverterCmdLine.jar analyze /path/to/file.gpx
```

- Exit `0`: one line of JSON on **stdout**, nothing else on stdout.
- Non-zero exit: a human-readable message on **stderr**, no JSON on stdout.
  - `5`  usage (no file argument)
  - `10` file does not exist
  - `25` parse failure / unreadable / analysis error (message on stderr)

All diagnostic logging goes to stderr (`java.util.logging` ConsoleHandler), so
stdout carries only the JSON payload.

## Fields

| JSON field        | Type            | Meaning |
|-------------------|-----------------|---------|
| `size`            | integer         | bytes on disk |
| `format`          | string          | RouteConverter format display name, e.g. `"GPS Exchange Format 1.1 (*.gpx)"` |
| `positionLists`   | integer         | number of position lists (routes/tracks/waypoint lists) in the file |
| `positions`       | integer         | total number of positions across all lists |
| `bbox`            | object or null  | bounding box over all positions with coordinates; `null` if none |
| `bbox.north`      | number          | maximum latitude |
| `bbox.south`      | number          | minimum latitude |
| `bbox.east`       | number          | maximum longitude |
| `bbox.west`       | number          | minimum longitude |
| `lengthM`         | integer or null | total length in metres, summed over all lists; `null` if not computable |
| `lengthKind`      | string or null  | `"track"` \| `"straight-line"` \| `"routed"` (see below); `null` exactly when `lengthM` is `null` (no length computable) |
| `durationS`       | integer or null | total duration in seconds derived from position timestamps only; `null` if no timestamps |
| `elevationGainM`  | integer or null | cumulative ascent in metres; `null` if no elevation data |
| `elevationLossM`  | integer or null | cumulative descent in metres; `null` if no elevation data |
| `startTime`       | string or null  | earliest position timestamp, ISO-8601 UTC (`2020-05-01T08:15:00Z`); `null` if none |
| `firstName`       | string or null  | first non-empty route/track/waypoint-list name in the file; `null` if none. Used as the title for rescued routes (specs/00056) |
| `extension`       | string or null  | the detected format's default file extension including the dot (e.g. `".gpx"`). Used to name rescued orphan-blob File rows (specs/00056) |

`country`/`continent` are **not** produced here — the Django command derives
them from `bbox` centre via point-in-polygon (keeps geo data out of Java).

## `lengthKind` semantics

- **Track** characteristic → `track` (recorded GPS track length, measured
  point-to-point along the recorded geometry).
- **Waypoints** characteristic → `straight-line` (the direct distance between the
  points; never routed).
- **Route** characteristic (planned routes) → depends on `--brouter-segments`:
  - **routed** when `--brouter-segments <dir>` is given, the list falls inside
    the BRouter `.rd5` segment coverage in that directory, and every leg
    routes successfully. The reported length is the sum of the on-road leg
    distances between consecutive route points.
  - **straight-line** otherwise — no `--brouter-segments` given, the directory does
    not exist, the list is outside coverage, the route fails to route (routing
    error, timeout, no segment), or the routed length comes back materially
    shorter than the straight-line distance through the same points (impossible
    for an on-road route, so the result is distrusted). A partially-covered
    route is reported wholly as `straight-line`, never as a mix, so the label never
    over-promises.

Routing is best-effort and never aborts the run: any BRouter failure degrades
that list to `straight-line` and the JSON is still emitted.

### BRouter profile

Routed lengths use the **`trekking`** profile (BRouter's general-purpose
bike/foot profile), bundled with the command-line tool. Planned catalog routes
are predominantly cycling and hiking tours; trekking follows both roads and
paths, giving a plausible on-road length across the widest range of routes,
whereas a car-only profile would refuse footpaths and fall back to straight-line on
exactly those tours. The profile and its `lookups.dat` must match the lookup
version of the `.rd5` segments on disk; a mismatch makes routing fail and the
list falls back to `straight-line`.

### Routing vs. the client

The number is a BRouter estimate, not a promise of equality with the client's
Convert tab (which may use map-rendered routing such as GraphHopper or Google
Maps for Route lists). `lengthKind=routed` labels the method, not a specific
router.

File-level aggregation of a mixed file reports the least-certain kind present:
`straight-line` if any list is straight-line, else `routed` if any list is routed, else
`track`. If no list has a computable length (`lengthM` is `null`) then
`lengthKind` is `null` too.
