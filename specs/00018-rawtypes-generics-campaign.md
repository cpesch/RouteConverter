---
name: 00018-rawtypes-generics-campaign
status: draft
phases_done: [measure, spike-navigation-formats]
phases_next: [decide-approach, close-navigation-formats, route-converter-gui, tail-modules, gate]
last_touched: 2026-08-08
---

# 00018 - Retire the raw types in the route/format hierarchy

## Status

`draft`. The measurement and a throwaway spike are done and reported below; the
approach decision in [Decision needed](#decision-needed) is **not** made. Do not
approve any implementation issue from this document until that section is
resolved.

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

## Decision needed

Three viable approaches. The spike says all three are cheaper than "rewrite the
format hierarchy", but they differ in what they buy.

**A. Wildcard closure + casts at the capture sites.**
Parameterise the bounds and every raw reference; at the ~5 capture sites in
`NavigationFormatParser` and friends, add `@SuppressWarnings("unchecked")` with
a comment naming the lost linkage. Spike-validated for `navigation-formats/main`
(82 → 0, 14 residual errors). Mechanical, reviewable, no signature redesign.
Cost: the casts are exactly the type-safety hole `rawtypes` was warning about —
we trade 318 warnings for ~5 documented unchecked casts.

**B. Real F-bounded generics.**
`NavigationFormat<R extends BaseRoute<P, F>, P extends BaseNavigationPosition, F
extends BaseNavigationFormat<R, P, F>>`, threaded through every route and format
class. Removes the warnings *and* the capture problem, no casts. Cost: a
3-parameter mutually recursive signature on ~80 classes, and every consumer
declaration in route-converter-gui / cmdline / mapview grows with it. This is
the "cascades through BaseRoute" risk spec 00017 Q5 flagged.

**C. Do not enable `-Xlint:rawtypes`.**
Same disposition as `serial` in #256: the category is off by default, so the
decision costs nothing to implement. The hierarchy keeps working as it has for
years, and the 318 warnings stay invisible.

Recommendation: **A, scoped to main sources, and C for the test sources.** A is
spike-validated and its cost is five documented casts. The 124 test-source hits
buy nothing — `NavigationTestCase` alone is 49 of them, and test helpers taking
raw routes is a readability question, not a correctness one. B is the only
option that is actually *correct*, but a 3-parameter recursive signature across
80 classes in a converter with 82 formats and thin test coverage of the parse
paths is a poor trade for zero behaviour change.

If A is chosen, note it cannot fully gate: `-Xlint:rawtypes` is reactor-wide and
test sources compile under the same flag, so either the tests get fixed too or
the gate stays off. **That tension is unresolved and is the main thing to settle
before writing any implementation issue.**

## Phased plan (conditional on A)

Each phase is one PR, each ends green on the full reactor.

1. **`close-navigation-formats`** — the spike, done properly by hand: 5 bounds,
   the raw references in `navigation-formats/src/main`, and the ~5 capture-site
   casts. Acceptance: `-Xlint:rawtypes` for `navigation-formats/main` is 0, full
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
4. **`gate`** — only if the test-source question above is answered: add
   `-Xlint:rawtypes` to the root pom's `<compilerArgs>` alongside the
   `deprecation,try,lossy-conversions` set from #269, with `failOnWarning`
   already true from #268.

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

1. Approach A, B, or C (above). Blocks everything.
2. Test sources: fix, or leave and forgo the gate? Determines whether phase 4
   exists at all.
3. If A: are five `@SuppressWarnings("unchecked")` casts in the parser an
   acceptable price for removing 184 main-source warnings? A cast that is wrong
   fails at runtime where the raw type failed at runtime too — no worse, but no
   better either.
4. `RouteComments.commentRouteName` and friends take
   `BaseRoute<BaseNavigationPosition, BaseNavigationFormat>` today. Parameterising
   that is visible to other modules. Is any of this API consumed outside the
   repo (RouteConverter is used as a library by third parties)?
