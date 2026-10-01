# `convert` — non-interactive conversion (online converter)

```
java -jar RouteConverterCmdLine.jar convert --to <Format> [--from <Format>[,<Format>...]] <source file> <target file>
```

The jar is the shaded `RouteConverterCmdLine/target/RouteConverterCmdLine.jar`
(`./mvnw -pl RouteConverterCmdLine -am package -Dskip.unit.tests=true -Dskip.integration.tests=true`).
It is the same artifact as the `analyze` jar; nothing else is needed on the classpath.

Formats are the simple class names (`Gpx11Format`, `Kml22Format`, `FitFormat`, ...).

- `--from` limits reading to the listed formats. Without it every read format is probed
  (including the GPSBabel-backed ones, which start an external program) — **always pass it for
  untrusted input**.
- The target is written to exactly `<target file>`, which must not exist. A route that does not
  fit one file of the target format is refused (exit 26), not split.
- The sponsor-gated write formats (`SponsorGatedFormats`) are refused (exit 17).
- stdout: one JSON line `{"routesRead":N,"routesWritten":M,"positions":P}`; `routesWritten < routesRead`
  means the target format holds one route/track and the others were left out. stderr: warnings only.
- KMZ input is capped at `-Drc.kmz.max.uncompressed.bytes` inflated bytes (default 256 MiB).
- DOCTYPE and external entities are rejected in every XML format.

| exit | meaning |
|------|---------|
| 0 | converted |
| 5 | usage |
| 10 / 11 | source missing / target exists |
| 15 / 16 | unknown or unwritable `--to` / unknown or unreadable `--from` |
| 17 | `--to` is a sponsor-gated format |
| 20 | source not readable as the `--from` formats |
| 21 | source has no positions |
| 25 / 26 | write failed / target would need several files |

Server use (rc-site `converter` app): `java -Xmx256m -XX:+UseSerialGC -Djava.awt.headless=true
-Djava.util.prefs.userRoot=<tmp> -jar RouteConverterCmdLine.jar convert ...`, cold start about 0.3 s.
