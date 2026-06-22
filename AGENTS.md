# AGENTS.md — RouteConverter

RouteConverter is a free GPS tool to display, edit, and convert routes, tracks,
and waypoints across more than 110 formats — a Java Swing desktop app (plus a
command-line build). This file orients contributors (human or AI) working in this
repository.

Maintainer: **cpesch** (GitHub). Licensed under the **GNU GPL v2** — contributions
ship under the GPL (hence the per-file header below).

## Build & test

Java 17, Maven via the bundled wrapper:

```sh
# macOS/Linux: java is often not on PATH — activate a JDK 17 first, e.g. sdkman:
source ~/.sdkman/bin/sdkman-init.sh && sdk use java 17   # pin may vary; `ls ~/.sdkman/candidates/java/`

./mvnw --batch-mode verify                 # full build + tests + coverage
./mvnw --batch-mode -pl <module> -am test  # one module (-am pulls sibling deps; add -U after dep changes)

# run the app (build the runnable Linux jar, then launch it):
./mvnw --batch-mode -pl RouteConverterLinux -am package
java -jar RouteConverterLinux/target/RouteConverterLinux.jar
```

Integration tests are split by Maven profile: `./mvnw -Phermetic-integration-test verify`
runs only the hermetic ITs (the default coverage set); the live-service ITs
(`*ServiceIT`, `DownloadManagerIT`, `RemoteRouteIT`, …) need network/credentials and
run via `-Pintegration-test` / `-Ptest-all`.

CI runs the test matrix on **Java 17, 21, 25** plus a Windows smoke build.
The bundled-JRE version is the single source of truth `<jre.version>` in the root
`pom.xml` — keep it in sync with the CI `setup-java` version on JDK bumps.

## Module layout

Reactor modules are in the root `pom.xml`. Roughly:

- **Libraries** — `navigation-formats` (the format engine), `gpx`, `kml`,
  `common`, `common-gui`, `download`, `routing-service`, `elevation-service`,
  `mapsforge-*`/`mapview` (map rendering), `geocoding-service`, … 
- **App bases** — `route-converter-gui` (shared GUI base, the Weblate target
  holding `RouteConverter_*.properties`) and `route-converter` (the app).
- **Platform builds** — `RouteConverter{Windows,Mac,Linux,Portable,CmdLine}`
  (produce the installers/jars).

The catalog **server** (`https://api.routeconverter.com/`) is a separate codebase;
this repo defines the wire schema + client tools. Server-side changes need
coordination with that codebase.

## Code conventions

- **GPL header on every `.java`** under `slash.navigation.*` — copy verbatim from
  a sibling; the `@author Christian Pesch` line is convention.
- **Plural getters for repeated XML elements.** A repeated singular element
  `<include>` (JAXB field `include` → `List<String>`) gets getter `getIncludes()`.
  Cf. `Source.getIncludes()` vs `SourceType.getInclude()`.
- **IntelliJ GUI Designer: edit the `.form`, not `$$$setupUI$$$`.** The `.form` is
  the canonical layout; the generated Java block is overwritten on regen. Hide a
  widget at runtime with `setVisible(false)`.
- **Tests use real JAXB binding objects, not mocked interfaces.** Construct
  `new ObjectFactory().create…()` + `new DataSourceImpl(...)` — see
  `WgetCommandBuilderTest`. Mocking the interfaces explodes into stub sprawl.
- **Integration tests: classify hermetic vs external.** Hermetic ITs (temp files,
  sample data, no live services) run in normal coverage; live-service ITs (real
  HTTP, credentials, remote state) stay opt-in. Keep filenames aligned with the
  Failsafe convention; change Maven includes deliberately, don't add a second
  naming scheme.
- **Small, focused diffs**; match the surrounding style.
- **Translations go through Weblate** ([hosted.weblate.org/projects/routeconverter](https://hosted.weblate.org/projects/routeconverter/))
  — don't hand-edit `RouteConverter_*.properties`; `weblate*` / `translations*`
  branches are bot-managed.
- **Release tags are plain `MAJOR.MINOR[.PATCH]`**, no `v` prefix.

## Contributing

Bugs and feature requests: [github.com/cpesch/RouteConverter/issues](https://github.com/cpesch/RouteConverter/issues)
(the desktop app also feeds error reports back to the maintainer). To send a change:

1. Fork, branch, open a PR against `master`.
2. **A human reviews and merges every PR** — no auto-merge. Automated review bots
   may comment; that's advisory.
3. Keep the CI matrix green; never commit secrets.

## CI & releases

GitHub Actions builds + tests on push/PR. Tagged releases (and the rolling
prerelease) publish installers + jars to `static.routeconverter.com`; Windows
installers are Authenticode-signed and the standalone jars jar-signed via SignPath
Foundation, in CI — contributors need no signing credentials.

## Notes for AI agents

- Continue autonomously when the next step is reversible and strongly implied by
  repo context; stop only for real product/compatibility/architecture decisions.
- Always offer a recommendation when presenting options, and say why.
- Treat maintainer corrections on naming/placement/scope/style as standing
  preferences for the rest of the task.
- Durable notes go in `specs/` (numbered, issue-like filenames, e.g.
  `00010-migrate-java-17-to-25.md`); stable agent instructions go here;
  temporary exploration stays in conversation.
