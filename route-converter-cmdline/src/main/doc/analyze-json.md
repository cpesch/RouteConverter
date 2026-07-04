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
| `lengthKind`      | string          | `"track"` \| `"beeline"` \| `"routed"` (see below) |
| `durationS`       | integer or null | total duration in seconds derived from position timestamps only; `null` if no timestamps |
| `elevationGainM`  | integer or null | cumulative ascent in metres; `null` if no elevation data |
| `elevationLossM`  | integer or null | cumulative descent in metres; `null` if no elevation data |
| `startTime`       | string or null  | earliest position timestamp, ISO-8601 UTC (`2020-05-01T08:15:00Z`); `null` if none |
| `firstName`       | string or null  | first non-empty route/track/waypoint-list name in the file; `null` if none. Used as the title for rescued routes (specs/00056) |
| `extension`       | string or null  | the detected format's default file extension including the dot (e.g. `".gpx"`). Used to name rescued orphan-blob File rows (specs/00056) |

`country`/`continent` are **not** produced here — the Django command derives
them from `bbox` centre via point-in-polygon (keeps geo data out of Java).

## `lengthKind` semantics

Each position list is measured point-to-point (recorded geometry):

- **Track** characteristic → `track` (recorded GPS track length).
- **Route** / **Waypoints** characteristic → `beeline` (straight-line length
  between planned points; not a routed distance).
- `routed` is reserved: when a `RouteLengthComputer` backed by BRouter is wired
  in (spec 00055 P3, host infra), Route-type lists inside coverage report
  `routed` with the on-road distance. The seam is `RouteLengthComputer`; the
  default `PointToPointLengthComputer` never returns `routed`.

File-level aggregation of a mixed file reports the least-certain kind present:
`beeline` if any list is beeline, else `routed` if any list is routed, else
`track`.
