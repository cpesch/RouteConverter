---
name: 00009-reduce-bundle-size-generically
status: retired
phases_done: [evaluate-minimizeJar, prototype-jdeps-jlink, trial-exclude-transitives]
phases_next: []
last_touched: 2026-07-06
---

# 00009 - Reduce bundle download size with a generic approach

## Status

Retired on July 6, 2026. Created June 13, 2026.

Investigated all four options empirically (see findings below). No
low-maintenance, version-agnostic shrink remains: minimizeJar breaks the
app, a jdeps app-jar closure inherits the same reflection keep-rule burden,
and every large transitive is live at runtime so there are no dead whole-jars
to exclude. The JRE runtime is already jlink/jdeps-trimmed. Closing without a
build change; reopen only if a specific feature (e.g. PBF import / offline
routing) is deprecated, which would free its transitives.

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

## Findings — option 2 (`minimizeJar`) measured 2026-07-06

Ran `maven-shade-plugin` with `<minimizeJar>true</minimizeJar>` on
`RouteConverterLinux` (single fat jar, simplest bundle), Java 21, no other
config change.

Size delta:

| | baseline | minimizeJar | delta |
|---|---|---|---|
| jar size | 61.5 MB | 16.4 MB | −73% |
| entries | 32040 | 4467 | −86% |

The size win is large but **unusable as-is** — minimizeJar traces
reachability from the packaging module's own classes, and RouteConverter
loads formats, JAXB bindings and viewers reflectively, so the tracer cannot
see them. Per-package class counts after minimize:

| package | baseline | minimized |
|---|---|---|
| `com/garmin/fit` | 550 | **0** (FIT format wiped) |
| `javax/help` | 202 | 9 (JavaHelp gutted) |
| `org/jfree` | 779 | 63 (profile chart gone) |
| `slash/navigation` (own code) | 2436 | **294** (parsers stripped) |
| `org/apache/poi` | 4356 | 1662 (Excel halved) |
| `org/apache/xmlbeans` | 1738 | 669 |
| `jakarta/xml/bind` | 126 | 18 (JAXB runtime cut) |

`slash/navigation` collapsing to 294 and `com/garmin/fit` to 0 mean the app
would fail at runtime: `NavigationFormatRegistry` instantiates format
classes by name, so a missing class throws `ClassNotFoundException` on load.

Conclusion: a bare `minimizeJar` is not viable. Making it correct requires
`<filter><includes>` keep-rules covering essentially every
reflection-loaded package (all `slash/navigation` formats, POI, xmlbeans,
jfreechart, JavaHelp, Garmin FIT, JAXB `com/sun/xml/bind` +
`jakarta/xml/bind`). Once those are kept whole, the recoverable savings
shrink toward the genuinely-dead transitive code only — a small fraction of
the −73% headline. The keep-rule surface is exactly the maintenance burden
the spec warns about, and it is version-fragile (new POI/JAXB internals need
new keep rules).

Recommendation: **do not pursue option 2.** Escalate to option 1
(jdeps-driven trimming) which computes the real required package set
automatically per build and aligns with the existing jlink packaging.

## Findings — option 1 (jdeps) prototyped 2026-07-06

Two distinct levers hide under "option 1"; they are not the same job.

### 1a. JRE runtime trim — already done

`scripts/jre-modules.txt` + `build-mac-jre.yml` / `build-windows-jre.yml`
already jlink a minimal runtime (`java.base`, `java.desktop`, … 19 modules),
and `scripts/verify-runtime.sh` gates it with `jdeps --print-module-deps`
plus forced-init / smoke / GUI-boot checks on the stripped JRE. The runtime
half of option 1 is in production. Nothing to add there.

### 1b. App fat-jar trim — prototyped, measured

The 61.5 MB `RouteConverterLinux.jar` (41.6 MB of that is `.class` bytes) is
the untrimmed part. Ran `jdeps --multi-release 17 -verbose:class` on it
(252 940 edges), then BFS'd the class-to-class closure from **proper roots**
— the real main class `RouteConverterOpenSource` plus every `ObjectFactory`
/ `package-info` (JAXB binding entry points) — instead of minimizeJar's
empty packaging-module roots.

Result: closure keeps 5306 / 27133 classes (19.6 %); 32.2 MB of 41.6 MB
class bytes are statically unreachable. **But that 77 % is an unsafe upper
bound** — proper roots fix the *own-code* problem (formats survive:
`slash.navigation` 996 kept here vs minimizeJar's 294, because format
classes are `.class` literals in `NavigationFormatRegistry`, statically
visible), yet jdeps is blind to the same reflective edges minimizeJar was:

| reflective lib | drop% by static closure | reality |
|---|---|---|
| `com.garmin.fit` | 94 % | FIT `Factory` reflects mesgNum→class |
| `org.glassfish.jaxb` | 100 % | JAXB runtime, string context path |
| `org.openxmlformats.schemas` | 91 % | POI loads schema types reflectively |
| `org.apache.xmlbeans` | 89 % | xmlbeans type-system reflection |
| `org.apache.poi` | 68 % | partial reflective loading |
| `org.mozilla.javascript` | 100 % | Rhino eval |

Trimming those statically = the same runtime `ClassNotFoundException`
minimizeJar produced. **jdeps buys better default roots, not freedom from
keep-rules.** A safe jdeps-closure trim needs the identical keep-rule
surface as option 2 — so it is not meaningfully lower-maintenance.

### Option 4 (dependency exclusion) — trialled, candidates all live

Hypothesis: whole transitive jars with 0 direct source `import`s in
RouteConverter (protobuf 1.5M, Rhino 1.23M, jts 1.07M, hppc 0.96M,
osmosis 0.22M — ≈ 5 MB, all graphhopper/proxy-vole transitives) are dead
weight and removable via `<exclusion>`.

Trialled by adding an `<exclusion>` for `osmosis-osm-binary` (drags its
`protobuf-java` child) to `graphhopper/pom.xml` and rebuilding. **All five
turned out to be live** — the "0 source imports" heuristic is a false
signal because usage flows through our *own* graphhopper module and through
consumer libraries, not direct RC imports:

| candidate | actual consumer | how caught |
|---|---|---|
| osmosis-osm-binary + protobuf | `PbfUtil.extractBoundingBox` (our `graphhopper` module) called from `GraphDescriptor` runtime path | **compile error** on exclusion |
| Rhino | `proxy-vole` PAC-script evaluation (proxy autoconfig) | dependency tree consumer |
| hppc | graphhopper-core routing primitive collections | dependency tree consumer |
| jts | graphhopper-core geometry | dependency tree consumer |

The osmosis exclusion failed at `mvn compile` (`package
org.openstreetmap.osmosis.osmbinary does not exist`) — caught before any
runtime gate was even needed. Exclusion reverted; tree clean.

Lesson: the gate worked exactly as intended (it stopped every unsafe
removal), but the corollary is that **there are no cheap whole-jar wins
here** — every large transitive the static closure flagged as droppable is
actually reached at runtime.

### Verdict

- Option 2 (minimizeJar): rejected — breaks the app.
- Option 1a (JRE jlink): already shipped; nothing to add.
- Option 1b (jdeps app-jar closure): better-rooted than minimizeJar but
  inherits the full reflection keep-rule burden; not worth reimplementing a
  shade-minimizer.
- Option 4 (exclusions): no dead whole-jars found; all candidates live.

**Conclusion: no low-maintenance generic win remains.** The JRE is already
trimmed; the app fat jar's weight is genuine (reflective libs + live
graphhopper/POI/JAXB transitives). Any further shrink requires per-class
keep-rules (option 2/3) whose maintenance cost the spec explicitly rejects,
OR a targeted feature-level decision (e.g. drop PBF-import/offline-routing
support to shed protobuf+osmosis) — a product call, not a build tweak.
Recommend parking 00009 unless a specific feature is deprecated.

## Acceptance criteria

- No `classifier>stripped` reappears in any pom.
- Bundle size is measured before/after; record the delta here.
- The shrink step runs in CI against the declared upstream versions.
- All formats still load (POI/Excel, jfreechart profile view, JavaHelp,
  JAXB-bound catalog/datasource XML, Garmin FIT) — verify with the existing
  integration tests, since these are the reflection-heavy areas most at risk
  from aggressive shrinking.
