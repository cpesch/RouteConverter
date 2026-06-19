# 00009 - Reduce bundle download size with a generic approach

## Status

Open. Created on June 13, 2026.

Background task to come back to after the `stripped`-classifier removal
(June 13, 2026).

## Context

Several third-party dependencies were consumed as custom `stripped`
artifacts hosted on `mvn.routeconverter.com` (classifier `stripped`):

- `com.garmin:fit`
- `com.intellij:forms-rt`
- `commons-cli:commons-cli`
- `commons-codec:commons-codec`
- `commons-io:commons-io`
- `commons-logging:commons-logging`
- `javax.help:javahelp`
- `org.apache.poi:poi`, `poi-ooxml`, `poi-ooxml-lite`
- `org.apache.xmlbeans:xmlbeans`
- `org.jfree:jfreechart`

Each `stripped` jar was a manually pre-shrunk copy (dead-code / unused
classes removed) whose only purpose was to shrink the shipped bundle. The
downside: every upgrade was blocked until someone rebuilt and re-published
the `stripped` variant for the new version, so dependencies silently went
stale (e.g. `com.garmin:fit` stuck at 21.176.00 while 21.205.0 was out).

On June 13, 2026 the `classifier>stripped` was removed from all poms and the
dependencies now resolve as their plain upstream artifacts from Maven
Central. This unblocks normal version bumps but increases the shipped
artifact size again (the full jars are larger than the stripped copies).

## Problem

Recover the size savings **without** the per-dependency, manually
maintained `stripped` artifacts. The shrinking must be a build step that
runs automatically against whatever upstream version is currently declared,
so upgrades stay friction-free.

## Options to evaluate

1. **jlink custom runtime + jdeps** (best fit; partly already in use).
   The Windows/Mac bundles already ship a jlink runtime. Extend the same
   idea to the application classpath: run `jdeps --print-module-deps` /
   `jdeps --ignore-missing-deps` to compute the real module/package set and
   drop what is unused. Pairs well with the existing `jlink-*` runtimes in
   `route-converter-mac-*` / `route-converter-windows-*`.

2. **maven-shade-plugin `minimizeJar`** on the final shaded application jar
   (the Mac/Windows bundles already shade via `maven-shade-plugin`). Turns
   on tree-shaking of unused classes at assembly time. Cheap to try; needs
   `<filters>`/`<keep>` rules for reflection-loaded classes (JAXB binding
   classes, POI, jfreechart, help set) to avoid stripping classes only
   referenced reflectively.

3. **ProGuard / R8 as a single assembly-time step** over the shaded jar.
   Most aggressive shrink, but needs keep-rules for reflection, JAXB,
   service loaders, and resource bundles. Highest maintenance.

4. **Per-dependency exclusions** (already done partially for POI in
   `navigation-formats/pom.xml`). Lowest effort, but only removes whole
   transitive jars, not unused classes within a needed jar.

## Recommendation (starting point)

Try option 2 (`minimizeJar` with explicit keep filters) first on the
opensource desktop jar and measure the size delta; it is the smallest
change and reuses the shade plugin already configured. If the keep-rule
surface gets unmanageable or savings are insufficient, escalate to option 1
(jdeps-driven trimming) which is the most robust and aligns with the
existing jlink packaging.

Whatever is chosen, it must be a generic, version-agnostic build step — no
hand-built artifacts pinned to a specific dependency version.

## Acceptance criteria

- No `classifier>stripped` reappears in any pom.
- Bundle size is measured before/after; record the delta here.
- The shrink step runs in CI against the declared upstream versions.
- All formats still load (POI/Excel, jfreechart profile view, JavaHelp,
  JAXB-bound catalog/datasource XML, Garmin FIT) — verify with the existing
  integration tests, since these are the reflection-heavy areas most at risk
  from aggressive shrinking.
