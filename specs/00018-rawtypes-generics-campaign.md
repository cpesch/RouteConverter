---
name: 00018-rawtypes-generics-campaign
status: planned
phases_done: [measure, spike-navigation-formats, decide-approach]
phases_next: [close-navigation-formats, route-converter-gui, tail-modules, gate]
last_touched: 2026-08-08
---

# 00018 - Retire the raw types in the route/format hierarchy

## Status

`planned`. Approach decided by the maintainer 2026-08-08 — see
[Decision](#decision-2026-08-08). Phases 1–4 below are now buildable; each still
wants its own issue with the usual locked decisions before approval.

Successor to spec 00017. Scope A (deprecated APIs, hand-written unchecked)
shipped in PR #139; Scope B (`failOnWarning` + the build-helper locale noise)
shipped in PR #268. This spec covers the largest remaining bucket from tracking
issue #256: `rawtypes`.

## Problem

`-Xlint:rawtypes` reports **318** warnings (184 main, 134 test) — 59% of the
whole `-Xlint:all` backlog. They are not scattered: they are one structural
defect in `navigation-formats/base`, radiating outward.

The core hierarchy declares its type parameters with **raw bounds**:

```java
public interface NavigationFormat<R extends BaseRoute> { … }                  // BaseRoute takes 2 params
public abstract class BaseNavigationFormat<R extends BaseRoute>
        implements NavigationFormat<R> { … }
public abstract class BaseRoute<P extends BaseNavigationPosition,
                                F extends BaseNavigationFormat> { … }         // BaseNavigationFormat takes 1
public abstract class SimpleRoute<P extends BaseNavigationPosition,
                                  F extends BaseNavigationFormat>
        extends BaseRoute<P, F> { … }
public abstract class SimpleFormat<R extends SimpleRoute>
        extends TextNavigationFormat<R> { … }                                 // SimpleRoute takes 2
```

Route and format reference each other, so each bound wants the other's
parameters. Every raw bound is one warning at the declaration, and every use of
those types inherits the rawness — which is why the two names `NavigationFormat`
(94 hits) and `BaseRoute` (86) account for over half the total.

The concrete leaves are already fully typed — `GpxRoute extends
BaseRoute<GpxPosition, GpxFormat>`, `TomTomRoute extends BaseRoute<TomTomPosition,
TomTomRouteFormat>`. **Only the bounds and the intermediate plumbing are raw.**
That is what makes this tractable at all, and it is the fact that should stop
anyone from budgeting a rewrite of all 82 formats.

## Measured baseline

2026-08-08, JDK 21.0.11, Maven 3.9.9, full reactor `clean test-compile` with
`<compilerArgs><arg>-Xlint:all</arg></compilerArgs>` in the root pom:

| raw type name | hits | | module / source root | hits |
|---|---|---|---|---|
| `NavigationFormat` | 94 | | navigation-formats / test | 98 |
| `BaseRoute` | 86 | | navigation-formats / main | 82 |
| `BaseNavigationFormat` | 40 | | route-converter-gui / main | 56 |
| `SimpleRoute` | 31 | | route-converter-gui / test | 26 |
| `GoPalRouteFormat` | 16 | | route-converter-cmdline / main | 12 |
| `SimpleFormat` | 15 | | mapsforge-mapview / main | 11 |
| `JAXBElement` | 9 | | mapview / main | 8 |
| Swing (`JComboBox`, `JList`, …) | 12 | | remaining 5 modules | 25 |
| `FormatAndRoutes` | 4 | | | |
| long tail (11 names, ≤2 each) | 11 | | | |

Worst single file is the test helper
`navigation-formats/src/test/java/slash/navigation/base/NavigationTestCase.java`
at **49** hits — one file is 15% of the whole campaign.

Reproducing the measurement: maven-compiler-plugin 3.15 **ignores**
`-Dmaven.compiler.compilerArgument=-Xlint:all`; the flag has to go in the root
pom as `<compilerArgs>`. Run with `MAVEN_OPTS="-Duser.language=en
-Duser.country=US"` or javac localises the messages and they cannot be bucketed
by category.

## Spike: how far do wildcard bounds get? (throwaway, not committed)

The cheap hypothesis is that the bounds only need *parameterising*, not new type
parameters — `F extends BaseNavigationFormat<?>` instead of a third parameter
threaded through everything. Tested on a scratch copy of master:

**Round 1** — parameterised the five bounds above (`BaseRoute<?, ?>`,
`BaseNavigationFormat<?>`, `SimpleRoute<?, ?>`) and nothing else:

> **78 compile errors in 12 files.** Every one was `type argument X is not within
> bounds of type-variable X` — declaration-site propagation, not call-site
> breakage. Nothing complained about `getFormat()`, `write()` or any consumer.

**Round 2** — mechanically parameterised every remaining raw reference to those
four types across `navigation-formats/src/main` (245 references, 55 files):

> **14 unique compile errors in 5 files**, and `-Xlint:rawtypes` for
> `navigation-formats/main` drops **82 → 0**.

The residue, and this is the part that matters:

| file | errors | nature |
|---|---|---|
| `base/NavigationFormatParser.java` | 6 | **wildcard capture** — `capture#1 of ? extends BaseRoute<…>` cannot be passed back to a format expecting that exact route type |
| `base/BaseRoute.java` | 3 | `BaseRoute<P,F>` not convertible where an exact type is wanted |
| `base/RouteComments.java` | 3 | `BaseNavigationPosition` not convertible |
| `gopal/GoPalRoute.java` | 1 | `GoPalRouteFormat` not within bounds |
| `base/SimpleLineBasedFormat.java` | 1 | list-of-capture conversion |

The capture errors are the wall. `<?>` deliberately forgets *which* route type a
format is bound to, and `NavigationFormatParser` is exactly the code that needs
to remember. Wildcards cannot express that; only a real F-bounded signature
(`NavigationFormat<R extends BaseRoute<P, F>, …>`) or a cast can.

Two caveats on the spike, stated plainly because they bound what it proves:

- It compiled `-pl navigation-formats -am` only. The **56 route-converter-gui
  main** and **124 test-source** hits were never compiled against the changed
  bounds. Those are unmeasured, and `RouteComments.commentRouteName(BaseRoute<
  BaseNavigationPosition, BaseNavigationFormat>)` is the kind of signature whose
  change is visible to callers in other modules.
- The 245-reference rewrite was a regex, and it mangled 5 lines (constructor
  names and a `.class` literal) before it compiled. A real implementation is
  hand-work or a much more careful transform.

## Decision (2026-08-08)

**Chosen: A for main sources, C for test sources, plus a hand-fix of the two
real defects the spike exposed in `NavigationFormatParser`.** Rationale and the
rejected alternatives are recorded below so this is not re-litigated.

### Correction to the spike's framing

The earlier draft said this approach "trades 318 warnings for ~5 documented
unchecked casts". **That was wrong, and it was the sentence the decision hung
on.** `NavigationFormatParser.commentRoutes` and `.preprocessRoute` already
carry `@SuppressWarnings("unchecked")` on master. A adds no type hole; it leaves
the two existing suppressions exactly where they are while making the other
~179 main-source sites genuinely checked.

### Two of the 14 residual errors are real defects, not type-system friction

- `commentRoutes` iterates a `List<BaseRoute<?, ?>>` with a loop variable
  declared `BaseRoute<BaseNavigationPosition, BaseNavigationFormat<?>>`.
  Generics are invariant, so that is not a supertype of
  `BaseRoute<GpxPosition, GpxFormat>` — the raw type was hiding a straightforward
  invariance bug. Fixing the loop variable to `BaseRoute<?, ?>` is a strict
  improvement and needs no cast.
- `preprocessRoute` inserts the result of
  `NmnFormat`/`CoPilotFormat.getDuplicateFirstPosition(…)` — declared to return a
  bare `BaseNavigationPosition` — into a route whose position type is unknown.
  That is genuinely unsound today, on a path that runs on every write. **Fix it
  by making the helper generic in the position type**
  (`<P extends BaseNavigationPosition> P getDuplicateFirstPosition(BaseRoute<P, ?> route)`),
  not by casting. This is the one place approach B's safety win actually
  matters, and it can be had at A's cost.

### Rejected

**B — full F-bounded generics.** Correct, and the only option that removes the
capture problem wholesale, but unmeasured (the spike covered A, and those numbers
do not transfer), and a 3-parameter mutually recursive signature
(`NavigationFormat<R extends BaseRoute<P,F>, P …, F extends BaseNavigationFormat<R,P,F>>`)
lands on ~80 classes plus every consumer declaration. `MultipleRoutesFormat` and
`GoPalRouteFormat` already surfaced as bound violations in the spike, so a format
producing more than one route type may not satisfy the recursion at all — and
discovering that at 60% done is expensive. B is the right answer for a new
codebase; the payoff here would come from years of further format evolution.

**C for main sources — leave `-Xlint:rawtypes` off entirely.** Rejected because,
unlike `serial` in #256, this bucket is not cosmetic: it is hiding the two live
defects above on the write path. C **is** adopted for test sources (below).

### Test sources stay raw — and this does NOT forfeit the gate

The draft assumed a reactor-wide `failOnWarning` meant the 124 test-source hits
(49 in `NavigationTestCase` alone) had to be fixed or the gate abandoned.
**Measured 2026-08-08: false.** `maven-compiler-plugin` accepts per-execution
configuration, so `-Xlint:rawtypes` can be scoped to `default-compile` and
overridden off for `default-testCompile`:

```xml
<configuration>
    <failOnWarning>true</failOnWarning>
    <compilerArgs><arg>-Xlint:rawtypes</arg></compilerArgs>
</configuration>
<executions>
    <execution>
        <id>default-testCompile</id>
        <configuration>
            <compilerArgs combine.self="override"/>
        </configuration>
    </execution>
</executions>
```

Verified on a scratch copy of master, `-pl navigation-formats -am`: **100
rawtypes warnings from main sources, 0 from test sources.** The
`combine.self="override"` attribute is load-bearing — without it Maven merges the
parent's `compilerArgs` into the execution rather than replacing them.

Consequence: main-source cleanliness is enforceable while the test helpers keep
their raw signatures. Phase 4 survives, scoped to `default-compile`.

## Phased plan

Each phase is one PR, each ends green on the full reactor.

1. **`close-navigation-formats`** — the spike, done properly by hand: the 5
   bounds, the raw references in `navigation-formats/src/main`, the two real
   defects from the Decision section (the `commentRoutes` loop variable, and
   `getDuplicateFirstPosition` made generic in the position type across
   `NmnFormat` + `CoPilotFormat` + their callers), and the two **pre-existing**
   `@SuppressWarnings("unchecked")` in `NavigationFormatParser` left in place
   with a comment naming what they cover. Acceptance: `-Xlint:rawtypes` for `navigation-formats/main` is 0, full
   reactor still compiles, all existing tests pass. No behaviour change — this
   is the phase where a green `ReadWriteBase`/`ConvertBase` suite is the whole
   safety net.
2. **`route-converter-gui`** — the 56 main hits, most of which are consumers of
   the signatures phase 1 changed. Size unknown until phase 1 lands; re-measure
   before scoping.
3. **`tail-modules`** — cmdline 12, mapsforge-mapview 11, mapview 8, download 3,
   tileserver-maps 3, datasource 2. Includes the 12 Swing raw types
   (`JComboBox`, `JList`, `ComboBoxModel`), which are unrelated to the route
   hierarchy and independently fixable at any time.
4. **`gate`** — add `-Xlint:rawtypes` to the root pom's `<compilerArgs>`
   alongside the `deprecation,try,lossy-conversions` set from #269, with the
   `default-testCompile` override from the Decision section so test sources stay
   exempt. `failOnWarning` is already true from #268. Acceptance: main-source
   rawtypes warnings are 0 reactor-wide and a deliberately reintroduced raw
   declaration in a main source fails the build, while the same declaration in a
   test source does not.

## Out of scope

- `serial` (117) — WONTFIX per #256; never enable `-Xlint:serial`.
- `this-escape` (57) — deferred per #256.
- `unchecked` (31) — the 15 hand-written hits are fallout of these same raw
  types and should disappear during phases 1–3; re-measure at phase 4 rather
  than fixing them separately. The 16 in kml `ObjectFactory` are checked-in JAXB
  RI 2.1 codegen with no regenerating plugin, and need their own decision
  (suppress vs regenerate against a current xjc).
- `JAXBElement` (9) and `Fragment`/`Future`/`Class` raw hits — inside generated
  or third-party-shaped code; fold into phase 3 only if free.

## Open questions

Questions 1 and 2 of the draft are resolved in [Decision](#decision-2026-08-08):
approach A for main sources, C for tests, and the gate is achievable main-only
via the per-execution override. What remains:

1. **Is any of this API consumed outside the repo?** `RouteComments.commentRouteName`
   and `getDuplicateFirstPosition` take
   `BaseRoute<BaseNavigationPosition, BaseNavigationFormat>` today; phase 1
   widens both. RouteConverter is used as a library by third parties, so this is
   a source-compatibility question, not just an internal one. If the answer is
   yes, phase 1 should keep a deprecated raw-signature overload for one release.
2. **Phase 2's true size is unknown.** The spike compiled
   `-pl navigation-formats -am` only, so route-converter-gui's 56 main hits were
   never compiled against the changed bounds. Re-measure after phase 1 lands
   rather than scoping phase 2 now.
3. **Does the 82-format parse suite actually cover the write path?** Phase 1's
   only safety net is `ReadWriteBase`/`ConvertBase` staying green, and
   `preprocessRoute` — where the unsound insert lives — runs on write. Worth
   confirming that path is exercised before changing it, per the "one green run
   is not verification" rule.
