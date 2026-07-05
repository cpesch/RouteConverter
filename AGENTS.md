# AGENTS.md — RouteConverter

RouteConverter is a free GPS tool to display, edit, and convert routes, tracks,
and waypoints across more than 110 formats — a Java Swing desktop app (plus a
command-line build). This file orients contributors (human or AI) working in this
repository.

Maintainer: **cpesch** (GitHub). Licensed under the **GNU GPL v2** — contributions
ship under the GPL (hence the per-file header below).

## Build & test

Java 21, Maven via the bundled wrapper:

```sh
# macOS/Linux: java is often not on PATH — activate a JDK 21 first, e.g. sdkman:
source ~/.sdkman/bin/sdkman-init.sh && sdk use java 21   # pin may vary; `ls ~/.sdkman/candidates/java/`

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

CI runs the test matrix on **Java 21, 25** (21 is the minimum) plus a Windows smoke build.
The bundled-JRE version is the single source of truth `<jre.version>` in the root
`pom.xml` — keep it in sync with the CI `setup-java` version on JDK bumps.

Source is **UTF-8** (compiler / surefire / javadoc / `propertiesEncoding`). The one
exception: the **maven-resources-plugin `<encoding>` stays `ISO-8859-1`** — resource
*filtering* also runs over binary resources (e.g. `RouteConverterPortable.exe`), and a
multi-byte encoding throws `MalformedInputException` on binary bytes. Don't "helpfully"
flip it to UTF-8.

## Module layout

Reactor modules are in the root `pom.xml`. Roughly:

- **Libraries** — `navigation-formats` (the format engine), `gpx`, `kml`,
  `common`, `common-gui`, `download`, `routing-service`, `elevation-service`,
  `mapsforge-*`/`mapview` (map rendering), `geocoding-service`, … 
- **App bases** — `route-converter-gui` (shared GUI base, the Weblate target
  holding `RouteConverter_*.properties`) and `route-converter` (the app).
- **Platform builds** — `RouteConverter{Windows,Mac,Linux,Portable,CmdLine}`
  (produce the installers/jars).

**Layering.** Non-GUI utilities live in `common` (e.g. `Transfer` — the
byte-size/time formatters `formatSize`/`formatTime`); GUI helpers live in
`common-gui` (e.g. `UIHelper` — `chooseDirectory`, look-and-feel). Routing,
format and service modules depend on `common`, **not** `common-gui` — don't pull
GUI dependencies down into them (a shared formatter needed by a non-GUI module
belongs in `common`, not `UIHelper`).

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

GitHub Actions builds + tests on push/PR. Windows installers are Authenticode-signed
and the standalone jars jar-signed via SignPath Foundation, in CI — contributors need
no signing credentials.

**Publishing.** Tagged releases (`release.yml`) and the rolling prerelease
(`prerelease.yml`) each (a) `rsync` the artefacts over SSH to the download host and
(b) create/refresh a GitHub Release; javadoc is rsynced by `javadoc.yml`. Deploy auth
is the `rc-release-deploy@$RC_RELEASE_DEPLOY_HOST` account via the ed25519 key in
secret `RC_RELEASE_DEPLOY_SSH_KEY` (host in `RC_RELEASE_DEPLOY_HOST`). Targets under
`/var/www/routeconverter.com/static/`: prerelease → `downloads/prereleases/`, release
→ `downloads/release/` + `downloads/previous-releases/<version>/`, javadoc →
`javadoc/`. The canonical download host is **`releases.routeconverter.com`**
(`/latest`, `/prerelease`, `/previous-releases/<v>/`) — an Apache vhost serving that
static tree; javadoc lives at `static.routeconverter.com/javadoc/`.

An rsync `Permission denied (publickey)` is an **infra-side** break of the deploy key
(rotated `RC_RELEASE_DEPLOY_SSH_KEY`, or the server's `authorized_keys` for
`rc-release-deploy`), not a repo bug — fix the key, then re-run the failed job; the
builds need not repeat.

**Build-time secret injection** (e.g. the crash-telemetry HMAC key, spec 00011).
A secret is baked into a Maven-filtered resource at build, mirroring
`apikey.properties`: the resource holds `key=${prop}`, the module's pom enables
`<filtering>true`, and CI passes `-Dprop=${{ secrets.X }}` on the `mvn package`
line in the reusable build workflows (`_build-linux-mac.yml`, `_build-windows.yml`).
Two traps:
- **Declare the secret in the reusable workflow's `on.workflow_call.secrets`** even
  though callers use `secrets: inherit` — otherwise `secrets.X` reads as undefined
  and `actionlint` fails the PR. `inherit` passes values; it does not declare them.
- **A test that asserts the unresolved-`${prop}`/placeholder state must use a
  test-scoped copy of the resource** (`src/test/resources/...`) keeping the literal
  token. Test resources are unfiltered and `target/test-classes` precedes
  `target/classes`, so it shadows the injected main resource — otherwise the test
  passes on a bare `mvn test` but fails in CI, where `mvn package -Dprop=X` filters
  the real value in. Reproduce locally with `mvn -pl <mod> -Dprop=X test`.

## Notes for AI agents

- Continue autonomously when the next step is reversible and strongly implied by
  repo context; stop only for real product/compatibility/architecture decisions.
- Always offer a recommendation when presenting options, and say why.
- Treat maintainer corrections on naming/placement/scope/style as standing
  preferences for the rest of the task.
- Durable notes go in `specs/` (numbered, issue-like filenames, e.g.
  `00010-migrate-java-17-to-25.md`); stable agent instructions go here;
  temporary exploration stays in conversation.
- **Spec lifecycle (frontmatter + status dirs).** Every `specs/000NN-*.md`
  carries a YAML frontmatter block at the top: `name / status / phases_done /
  phases_next / last_touched`. Status enum: `proposed | planned | in-flight |
  shipped | live | retired`. **Active** specs (`proposed`/`planned`/`in-flight`)
  live in `specs/`; **done** ones (`shipped`/`live`/`retired`) in `specs/done/`.
  When a spec's status crosses that line — flip only at zero open tasks, and
  verify ship state from git, not the `status` field alone — `git mv` it to the
  matching dir. Then rerun the aggregator: bare `python3 scripts/gen-status.py`
  (it writes `specs/STATUS.md` itself — do NOT redirect `> specs/STATUS.md`, that
  races its own write). It warns on stale-dated active specs + status/dir
  mismatch. New-spec frontmatter follows `specs/_templates/project-overview-prompt.md`.
