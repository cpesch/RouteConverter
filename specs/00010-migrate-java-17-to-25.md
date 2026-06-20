# 00010 - Migrate the toolchain from Java 17 to Java 25 (LTS)

## Status

Open. Created June 13, 2026 (originally targeting Java 21). Revised June 20, 2026
to target **Java 25** — see "Decision: 25, not 21" below. Tracks GitHub issue
\#110 ("write a spec for bumping the bundled Java runtime").

`RELEASE_NOTES.md` already announces "the build moves to Java 25", but the
version pins below are still on 17. **This spec closes that gap** — it is the
work the release note is promising.

## The three Java knobs (don't conflate them)

A "Java version" in this project is really three independent settings:

1. **Build JDK** — the JDK that compiles the code (CI `actions/setup-java`,
   local sdkman). Currently `17` / `17.0.19` in every workflow.
2. **Bytecode target** — `<java.version>17</java.version>` in root `pom.xml`
   (`--release`). This is the **minimum runtime** a BYO-JDK user (Linux) needs.
3. **Bundled jlink JRE** — `<jre.version>17.0.19</jre.version>` in
   `route-converter-build/pom.xml`. The stripped runtime shipped *inside* the
   Mac `.app` and the Windows bundle/portable. This is the version end users on
   Mac/Windows actually run on.

This migration moves all three to 25.

## Decision: 25, not 21

The original plan (June 13) targeted Java 21. Superseded — go straight to 25:

- **It is the current LTS** (Sep 2025), longest support runway. Targeting 21
  now would force a *second* bump to 25 within a year.
- **The maintainer already committed to it** — `RELEASE_NOTES.md` says the build
  runs on Java 25. This spec makes the pins match.
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

1. `route-converter-build/pom.xml`: `<jre.version>17.0.19</jre.version>` -> a
   25.x build; rebuild + publish the hosted stripped/jlink JREs
   (`jre-<ver>-{x64,aarch64}-stripped.zip` for Mac, `jre-<ver>-x64-stripped.zip`
   for Windows) via `build-mac-jre.yml` / `build-windows-jre.yml`.
2. Root `pom.xml`: `<java.version>17</java.version>` -> `25`.
3. launch4j `<minVersion>` 17 -> 25 in `RouteConverterWindowsOpenSource/pom.xml`
   and `TimeAlbumProWindows/pom.xml`.
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
stripped bundle, watching for `NoClassDefFound`/`ClassNotFoundException`.

## Risks / things to check

- **Removed/deprecated JDK internals.** Java 17->25 removed the Security Manager
  (JEP 486) and is deprecating `sun.misc.Unsafe` memory access (23+). Audit deps
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

## Acceptance criteria

- All modules compile and test green on Java 25 (fallback: 21, if a validated
  blocker forces it — record which dep/module and why in this file).
- jlink `--add-modules` list re-validated against JDK 25; no
  `NoClassDefFound`/`ClassNotFoundException` at startup on any stripped bundle.
- Hosted JREs republished at the new 25.x version for all three platform/arch
  combos (Mac x64, Mac aarch64, Windows x64).
- Every shipped artifact launches: Mac `.app` (x64 + aarch64), Windows unbundled
  `.exe`, Windows bundle `.exe`, Windows portable `.paf.exe`, Linux.
- `java -version` inside each unpacked bundle reports the target 25.x build.
- No new module-access warnings/errors at startup.
- Installer/JRE size delta vs 17 measured and recorded (expected: a few MB; not
  a blocker either way).
- `RELEASE_NOTES.md`'s "build moves to Java 25" now matches the actual pins.
