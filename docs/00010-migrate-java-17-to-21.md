# 00010 - Migrate the toolchain from Java 17 to Java 21 (LTS)

## Status

Open. Created on June 13, 2026. Deferred — Java 17 is supported until 2029, so
this is planned, not urgent.

## Context

The project targets Java 17 throughout:

- `<java.version>17</java.version>` in the root `pom.xml` (compiler source/target).
- `<jre.version>` in `route-converter-build/pom.xml` pins the bundled/jlink
  runtime (currently a 17.0.x Temurin build).
- launch4j requires a JRE `<minVersion>17</minVersion>` for the unbundled
  Windows `.exe` (`RouteConverterWindowsOpenSource`, `TimeAlbumProWindows`).
- CI (`_build-linux-mac.yml`, `_build-windows.yml`, `build-mac-jre.yml`,
  `build-windows-jre.yml`) resolves the JDK/JRE from these properties.

Java 21 is the current LTS (supported well beyond 17). Moving to it gets newer
GC defaults, performance, and language features, and keeps us on a maintained
LTS line.

## Why defer

- Java 17 has vendor support through ~2029; no security forcing function yet.
- A bundled-runtime migration touches the hosted jlink JRE artifacts on
  `static.routeconverter.com` (both `x64`/`aarch64` Mac and `x64` Windows),
  not just pom properties — see [00009](00009-reduce-bundle-size-generically.md)
  for the related runtime/size work; do them together if possible.

## Scope of the change

1. `route-converter-build/pom.xml`: bump `<jre.version>` to a 21.x build and
   rebuild + publish the hosted stripped/jlink JREs
   (`jre-<ver>-{x64,aarch64}-stripped.zip` for Mac, `jre-<ver>-x64-stripped.zip`
   for Windows) via `build-mac-jre.yml` / `build-windows-jre.yml`.
2. Root `pom.xml`: `<java.version>17</java.version>` -> `21`.
3. launch4j `<minVersion>` 17 -> 21 in `RouteConverterWindowsOpenSource/pom.xml`
   and `TimeAlbumProWindows/pom.xml`.
4. CI workflows: `actions/setup-java` `java-version`, and any hardcoded `17`.
5. Mac launcher scripts and NSIS bundles reference the runtime via the
   `jre.version` property / extracted dir, so they follow automatically once the
   hosted JRE is republished — but re-verify the bundled `.app` / `.exe` boot.

## Risks / things to check

- Dependencies that do bytecode manipulation or reflect into the JDK
  (JAXB/`jaxb-impl`, POI, jfreechart, anything using `--add-opens`/`--add-exports`).
  The Mac launcher already passes `--add-exports java.desktop/com.apple.eawt=ALL-UNNAMED`;
  confirm no new `--add-opens` are needed on 21.
- Strong encapsulation tightened further across 17 -> 21; run the full test
  suite (unit + integration) and smoke-launch every bundle (Mac x64/aarch64,
  Windows unbundled/bundle/portable).
- Garmin FIT, sqlite-jdbc, mapsforge native bits: re-verify on 21.

## Acceptance criteria

- All modules compile and test green on Java 21.
- Hosted JREs republished at the new version for all three platform/arch combos.
- Every shipped artifact launches: Mac `.app` (x64 + aarch64), Windows
  unbundled `.exe`, Windows bundle `.exe`, Windows portable `.paf.exe`, Linux.
- No new module-access warnings/errors at startup.
