# 00010 - Migrate the toolchain from Java 17 to Java 25 (LTS)

## Status

In progress. Created June 13, 2026 (originally targeting Java 21). Revised June 20,
2026 to target **Java 25** — see "Decision: 25, not 21" below. Tracks GitHub issue
\#110 ("write a spec for bumping the bundled Java runtime").

**June 28, 2026 — landed the 21 floor (knobs 2+3 + CI + docs).** Maintainer chose
the safe floor (21), not 25, for this round. Done in-repo and validated on a local
JDK 21 build (bytecode major 65 confirmed; `mvn clean test` failure set identical
to the JDK 17 baseline — only pre-existing env-dependent `*IT`/URL tests fail,
none introduced by the bump):

- root `pom.xml`: `<java.version>` 17→21, `<jre.version>` 17.0.19→21.0.11, and the
  compiler switched from `<source>`/`<target>` to `<release>` (the optimization
  below — now material because the matrix still builds on 25 above a 21 floor).
- CI: `build.yml` matrix `[17,21,25]`→`[21,25]` (17 can't javac-target 21) and the
  Windows smoke `[17]`→`[21]`; codecov upload gate `==17`→`==21`;
  `release-prepare.yml`/`javadoc.yml`/`_build-linux-mac.yml` build JDK `'17'`→`'21'`;
  `release.yml` pin `'17.0.19'`→`'21.0.11'`. The `build-{mac,windows}-jre.yml` /
  `_build-windows.yml` derive the version from `<jre.version>` and follow automatically.
- docs: `RELEASE_NOTES.md` (EN+DE) Java 25→21; `AGENTS.md` + `README.md` matrix
  drop 17. (README/AGENTS user-facing 17→21 already shipped in #115.)

**jlink re-validation (JDK 21).** `jdeps --print-module-deps` against 21 matches the
curated `scripts/jre-modules.txt` except it flags `jdk.management`, pulled in by
`UpdateChecker.getTotalMemory()` casting to `com.sun.management.OperatingSystemMXBean`.
That call is wrapped in `catch (Throwable) { return "?"; }`, so on the stripped JRE
(which omits `jdk.management`) it degrades silently to "?" rather than crashing —
a **pre-existing** gap, identical on 17 and 21, not introduced by this bump. No new
module is *required* at startup. Left the module list unchanged; restoring the
total-memory telemetry field by adding `jdk.management` is a separate minor decision.

**Remaining (gated CI/human, not doable locally):**
- Republish the hosted stripped JREs at **21.0.11** via `build-mac-jre.yml` /
  `build-windows-jre.yml` (Mac x64+aarch64, Windows x64). Until then, release/bundle
  builds that fetch `jre-21.0.11-*-stripped.zip` will 404.
- Smoke-launch every bundle (Mac `.app` x64+aarch64, Windows `.exe`/bundle/portable,
  Linux) with `scripts/verify-runtime.sh`; confirm `java -version` reports 21.0.11
  and no startup `NoClassDefFound`.
- **25 is still the eventual goal** — repeat knobs 1–3 against 25 once the 21 floor
  is proven in production.

`RELEASE_NOTES.md` already announces "the build moves to Java 25" — that is the
**build JDK** (knob 1). The bytecode target and bundled JRE (knobs 2–3) are
still 17. This spec extends the move to all three so the build JDK and the
runtime RouteConverter actually ships on are one LTS, instead of a
build-on-25 / ship-on-17 split.

## The three Java knobs (don't conflate them)

A "Java version" in this project is really three independent settings:

1. **Build JDK** — the JDK that compiles the code (CI `actions/setup-java`,
   local sdkman). Currently `17` / `17.0.19` in every workflow.
2. **Bytecode target** — `<java.version>17</java.version>` in root `pom.xml`,
   feeding maven-compiler `<source>`/`<target>` (not `<release>` — see
   Optimizations). This is the **minimum runtime** a BYO-JDK user (Linux) needs.
3. **Bundled jlink JRE** — `<jre.version>17.0.19</jre.version>` in the root
   `pom.xml` `<properties>` (was `route-converter-build/pom.xml` until spec 00045
   folded that module into root). The stripped runtime shipped *inside* the
   Mac `.app` and the Windows bundle/portable. This is the version end users on
   Mac/Windows actually run on.

This migration moves all three to 25.

**Constraint: all three must be the same version.** Because the compiler uses
`<source>`/`<target>` (not `<release>` — see Optimizations), building on a newer
JDK than the target does *not* prevent the compiler from emitting calls to APIs
that exist only in the newer JDK. So a "build on 25, target+ship 17" split is
**not safe** — it can silently produce bytecode/API references that fail on the
17 JRE (`UnsupportedClassVersionError` / `NoSuchMethodError` at runtime, not
caught at compile time). Build JDK, bytecode target, and bundled JRE therefore
move together, atomically, to the same 25.x line. (Adopting `<release>` would
relax this and permit a build-ahead split — but that is the Optimization, not
the default.) This also means there is **no safe mechanical-only sub-slice** to
hand to the factory: the JRE bump forces the hosted-JRE publish + native-bundle
smoke, which are human/CI steps a context-only minion cannot perform.

## Decision: 25, not 21

The original plan (June 13) targeted Java 21. Superseded — go straight to 25:

- **It is the current LTS** (Sep 2025), longest support runway. Targeting 21
  now would force a *second* bump to 25 within a year.
- **The build is already standardizing on it** — `RELEASE_NOTES.md` moves the
  build JDK to Java 25. Shipping a 25 JRE keeps build and runtime aligned;
  shipping 21 would mean a build-on-25 / ship-on-21 split.
- **End-user functionality is identical.** RouteConverter is a mature Swing app;
  it exposes no new *language* features to users. The 17→25 difference is
  runtime-internal (GC, JIT, FFM, security-manager removal). So the choice is
  governed by **risk and support runway**, not features — and 25 wins both
  (do the bump once) provided validation passes.
- **Packaging size is not a factor.** The bundled JRE is jlink-stripped to this
  project's exact module set, so base-JRE growth mostly doesn't apply; expect a
  few MB at most on a ~50 MB JRE. The spec measures it (see Acceptance), but it
  should not drive the version choice.

**Fallback to 21:** if validation (below) finds a hard dependency or jlink-module
blocker on 25, drop to **Java 21** — identical artefact list, one version lower,
lower risk — accepting that a later 25 bump will then be needed. 21 is the safe
floor, 25 is the goal.

## Scope of the change

1. root `pom.xml`: `<jre.version>17.0.19</jre.version>` -> a
   25.x build; rebuild + publish the hosted stripped/jlink JREs
   (`jre-<ver>-{x64,aarch64}-stripped.zip` for Mac, `jre-<ver>-x64-stripped.zip`
   for Windows) via `build-mac-jre.yml` / `build-windows-jre.yml`.
2. Root `pom.xml`: `<java.version>17</java.version>` -> `25`.
3. ~~launch4j `<minVersion>` 17 -> 25~~ — **obsolete:** spec 00044 dropped launch4j
   (bundled-JRE `.exe` only) and renamed the module to `RouteConverterWindows`.
   No needs-Java `.exe`, so no launch4j `<minVersion>` to bump.
4. CI workflows: `actions/setup-java` `java-version` `17` -> `25` in
   `_build-linux-mac.yml`, `javadoc.yml`, `release-prepare.yml`, and the pinned
   `17.0.19` in `release.yml`; confirm `build.yml` `distribution: 'zulu'` ships a
   25 build (or switch distribution if not).
5. Local build pin: `sdk use java <25.x>` (no `.sdkmanrc` in the tree — the pin
   is operational, see AGENTS.md). Keep the chosen 25.x build consistent across
   CI, the hosted JRE, and local.
6. Mac launcher scripts and NSIS bundles reference the runtime via the
   `jre.version` property / extracted dir, so they follow automatically once the
   hosted JRE is republished — but re-verify the bundled `.app` / `.exe` boot.

## jlink module list — the silent-failure risk

The bundled JRE's `jlink --add-modules` list is hand-curated. A JDK bump that
needs a *new* module surfaces only as a runtime `NoClassDefFound` at startup —
**unit/integration tests do not catch it** (they run on the full JDK, not the
stripped JRE). Precedent: httpclient5 5.6 needed `jdk.net`. Before shipping,
re-derive the required module set against JDK 25 (`jdeps --print-module-deps` on
the full dependency set, or `jlink --suggest-providers`) and smoke-launch every
stripped bundle with the existing `scripts/verify-runtime.sh`, watching for
`NoClassDefFound`/`ClassNotFoundException`.

## Risks / things to check

- **Removed/deprecated JDK internals.** Java 17->25 permanently disabled the
  Security Manager (JEP 486, JDK 24) and deprecated `sun.misc.Unsafe` memory
  access for removal (JEP 471, JDK 23). Audit deps
  for use of either: JAXB/`jaxb-impl`, POI, jfreechart, jna, sqlite-jdbc,
  Garmin FIT, mapsforge native bits.
- **Strong encapsulation** tightened further across 17 -> 25. The Mac launcher
  already passes `--add-exports java.desktop/com.apple.eawt=ALL-UNNAMED`; confirm
  no new `--add-opens`/`--add-exports` are needed on 25.
- Run the **full** suite (unit + integration) on JDK 25 and smoke-launch every
  bundle: Mac `.app` (x64 + aarch64), Windows unbundled `.exe`, Windows bundle
  `.exe`, Windows portable `.paf.exe`, Linux.
- Coordinate with [00009](00009-reduce-bundle-size-generically.md): both touch
  the hosted jlink JRE artifacts — do them together if possible.

## Optimizations (optional, fold into this bump)

- The compiler is wired with `<source>`/`<target>` (`${java.version}`). Consider
  switching to `<release>${java.version}</release>`: `--release` validates code
  against the *target* JDK's API, catching accidental use of APIs newer than the
  floor — which `source`/`target` silently allows once the build JDK (25) is
  ahead of the target. One-line change, removes a latent footgun now that build
  JDK and target diverge.

## Acceptance criteria

- All modules compile and test green on Java 25 (fallback: 21, if a validated
  blocker forces it — record which dep/module and why in this file).
- jlink `--add-modules` list re-validated against JDK 25; no
  `NoClassDefFound`/`ClassNotFoundException` at startup on any stripped bundle
  (`scripts/verify-runtime.sh` green on all three platform/arch combos).
- Hosted JREs republished at the new 25.x version for all three platform/arch
  combos (Mac x64, Mac aarch64, Windows x64).
- Every shipped artifact launches: Mac `.app` (x64 + aarch64), Windows unbundled
  `.exe`, Windows bundle `.exe`, Windows portable `.paf.exe`, Linux.
- `java -version` inside each unpacked bundle reports the target 25.x build.
- No new module-access warnings/errors at startup.
- Installer/JRE size delta vs 17 measured and recorded (expected: a few MB; not
  a blocker either way).
- Build JDK, bytecode target, and bundled JRE all on the same 25.x line — no
  build-vs-ship split; `RELEASE_NOTES.md`'s Java 25 claim now holds for the
  shipped runtime, not just the build.
