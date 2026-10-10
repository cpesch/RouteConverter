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

# just want the jar? skip the (flaky, network-dependent) tests with the project's
# OWN properties — plain -DskipTests is IGNORED (surefire binds <skipTests> to
# ${skip.unit.tests}), and -Dmaven.test.skip breaks the build because ~18 modules
# depend on sibling test-jars:
./mvnw --batch-mode -pl RouteConverterLinux -am package \
  -Dskip.unit.tests=true -Dskip.integration.tests=true
```

Integration tests are split by Maven profile: `./mvnw -Phermetic-integration-test verify`
runs only the hermetic ITs (the default coverage set); the live-service ITs
(`*ServiceIT`, `DownloadManagerIT`, `RemoteRouteIT`, …) need network/credentials and
run via `-Pintegration-test` / `-Ptest-all`.

**Excluding a test class: `-Dtest` replaces surefire's includes.** `-Dtest='!FooTest'`
does not subtract from the default set: it becomes the whole selection, so every class
that is not `FooTest` runs in the unit phase, including `*IT`. That pulls the
sample-dependent ITs (`ConvertIT`, `SplitIT`, …) into `mvn test`, and their failures look
like regressions. Exclude the ITs too, and allow modules where nothing matches:
`-Dtest='!FooTest,!*IT' -Dsurefire.failIfNoSpecifiedTests=false`.

**The build is warning-free and enforced.** `<failOnWarning>true</failOnWarning>` on
maven-compiler-plugin (spec 00017 Scope B, PR #268) turns javac warnings into build
failures — a warning you introduce fails CI on all three matrix jobs. It only catches
**default-lint** warnings, though: plain `deprecation` and `unchecked` are javac *notes*
and still pass on their own, so a canary built on those compiles green and proves
nothing — unless the category is named explicitly in `<compilerArgs>`, which is now
the case for all four of `rawtypes`, `deprecation`, `fallthrough` and `unchecked`
(main sources, reactor-wide; test sources stay exempt from `rawtypes`). Spec
`00018-rawtypes-generics-campaign` closed the rawtypes gap (shipped 2026-08-11) and
issues #215/#222 measured the other three clean; `serial` (117) and `this-escape`
(57) are the only categories still deliberately OFF — see tracking issue #256.

**SpotBugs gates `verify` too.** `spotbugs:check` (threshold Medium, effort Max) runs in
the verify phase and fails on any finding not excluded by `config/spotbugs-exclude.xml`.
Fix a finding in the code first; exclude only a deliberate pattern, with a targeted
`<Match>` (class + method) that carries an XML comment giving the reason — no uncommented
`<Match>`. `-Dspotbugs.skip` skips it locally. `mvn package` is unaffected.

To re-measure that backlog: maven-compiler-plugin 3.15 **ignores**
`-Dmaven.compiler.compilerArgument=-Xlint:all`, so the flag has to go into the root pom
as `<compilerArgs><arg>-Xlint:all</arg></compilerArgs>`; and run with
`MAVEN_OPTS="-Duser.language=en -Duser.country=US"` or javac emits localised messages
that cannot be bucketed by category.

CI runs the test matrix on **Java 21, 25** (21 is the minimum) plus a Windows smoke build.
The bundled-JRE version is the single source of truth `<jre.version>` in the root
`pom.xml` — keep it in sync with the CI `setup-java` version on JDK bumps.

Source is **UTF-8** (compiler / surefire / javadoc / `propertiesEncoding`). The one
exception: the **maven-resources-plugin `<encoding>` stays `ISO-8859-1`** — resource
*filtering* also runs over binary resources (e.g. `RouteConverterPortable.exe`), and a
multi-byte encoding throws `MalformedInputException` on binary bytes. Don't "helpfully"
flip it to UTF-8.

**i18n keys.** UI strings live in `RouteConverter_<lang>.properties`; there is **no**
no-suffix base bundle. `CombinedResourceBundle` loads English (`_en`) as a fallback
first, so a key missing in the active locale degrades to English instead of throwing
`MissingResourceException`. Practical rule: **add every new UI key to both `_en` and
`_de`** (German is the maintainer's co-primary locale) — `ResourceBundleTest.everyEnglishKeyIsTranslatedInGerman`
fails the build on an `_en` key with no `_de` translation. Other locales may lag and
fall back to English. (History: keys added to `_en` only once crashed the German Save As
dialog before the fallback existed.)

## Module layout

Reactor modules are in the root `pom.xml`. Roughly:

- **Libraries** — `navigation-formats` (the format engine), `gpx`, `kml`,
  `common`, `common-gui`, `download`, `routing-service`, `elevation-service`,
  `mapsforge-*`/`mapview` (map rendering), `geocoding-service`, … 
- **App bases** — `route-converter-gui` (shared GUI base, the Crowdin target
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
- **Translations go through Crowdin** ([crowdin.com/project/routeconverter](https://crowdin.com/project/routeconverter))
  — don't hand-edit `RouteConverter_*.properties`. Sync runs via Crowdin's
  native GitHub integration (PRs from `l10n_master`, opened as maintainer);
  the old `.github/workflows/crowdin.yml` Action (`l10n` branch) is disabled
  to avoid duplicate/competing PRs — don't re-enable both at once. Root
  `crowdin.yml` still governs file mapping/language codes (e.g. `nb→nb_NO`)
  for whichever path runs.
- **Rewording an English source string drops every locale's translation.**
  Crowdin treats the changed text as a new string: the next sync deletes the
  key from all `RouteConverter_*.properties`, and machine translation refills
  it without context. A missing German key fails
  `ResourceBundleTest.everyEnglishKeyIsTranslatedInGerman` and turns CI red
  on every push to `master`. Short dialog titles are misread most often:
  `feature-locked-title` "Sponsored function" came back as "sponsored event"
  in 15 locales, German included (#419). When you reword a key, re-enter or
  approve the German translation in Crowdin, give the string a context note,
  then **Sync Now**. Check the German value in the `l10n_master` PR before
  merging it. Crowdin's commits don't trigger `build.yml` (`on: push`), so
  the required `Java 21 on Windows` check needs an empty commit pushed to
  `l10n_master`.
- **A translation fixed in the repo must also go into Crowdin, or the next
  sync reverts it.** The GitHub integration only reads the English source
  and writes translations back; it never uploads edited
  `RouteConverter_*.properties`. Push them with the Crowdin CLI (v4) from an
  up-to-date `master`: export `CROWDIN_PROJECT_ID` and
  `CROWDIN_PERSONAL_TOKEN` (`crowdin.yml` reads both from the environment),
  then run, one language per call because `-l` keeps only the last flag:
  `crowdin upload translations -b master -l <code> --auto-approve-imported`.
  `-b master` is required: the integration keeps its files in Crowdin
  branch `master`, so without it the CLI reports the source file "does not
  exist" (`crowdin branch list`, `crowdin file list -b master` show where
  they are). Without `--auto-approve-imported` the upload lands as a mere
  suggestion and the approved machine translation stays. Then **Sync Now**:
  a `l10n_master` PR with no change to the fixed keys proves Crowdin agrees
  (#421 → #422).
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

- **Never publish data that users sent in.** Sample and test files contributed by users
  (the private samples checkout used by the `*FormatIT` tests) are confidential:
  never copy them into this repository, test resources, screenshots, docs or examples,
  not even renamed or trimmed. Invent test data (e.g. a hand-written GPX converted with
  RouteConverter itself) or use RouteConverter catalog data, and hash-check every new
  data file against the samples checkout (`shasum -a 256`) before pushing.
- Continue autonomously when the next step is reversible and strongly implied by
  repo context; stop only for real product/compatibility/architecture decisions.
- Always offer a recommendation when presenting options, and say why.
- Treat maintainer corrections on naming/placement/scope/style as standing
  preferences for the rest of the task.
- Durable notes do not live in this repository: feature and bug work goes into
  GitHub issues; stable agent instructions go here; temporary exploration stays
  in conversation.
- **Parallel agents and Maven.** Several agents running reactor builds (`-am`,
  `install`) at once contend for the shared `~/.m2` and stall for minutes with no
  output. Install the reactor once (`./mvnw -o install -Dskip.unit.tests=true
  -Dskip.integration.tests=true -Djacoco.skip -Dspotbugs.skip`), then let each
  agent build only its modules offline (`./mvnw -o -pl <mod1>,<mod2> verify`),
  without `-am` or `install`. Run at most three Maven-heavy agents concurrently,
  and start anything that may take longer than ~2 minutes in the background with
  output to a log. Those snapshots go stale as soon as `master` moves: rebuild
  them (or use `-am`) before trusting a single-module build after a merge.
