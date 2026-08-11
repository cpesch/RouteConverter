---
name: 00018-rawtypes-generics-campaign
status: shipped
phases_done: [measure, spike-navigation-formats, decide-approach, close-navigation-formats, measure-route-converter-gui, spike-route-converter-gui, measure-tail-modules, route-converter-gui, tail-modules, measure-gate-readiness, pre-gate-cleanup, gate]
phases_next: []
last_touched: 2026-08-11
---

# 00018 - Retire the raw types in the route/format hierarchy

## Status

`shipped`. All 4 phases landed 2026-08-10/11. Approach decided by the maintainer 2026-08-08 — see
[Decision](#decision-2026-08-08). Phase 1 (`close-navigation-formats`) shipped
2026-08-10 in [PR #286](https://github.com/cpesch/RouteConverter/pull/286) —
`navigation-formats/src/main` is `-Xlint:rawtypes` clean (~~100~~ **293 real,
see [Phase 1 baseline correction](#phase-1-baseline-correction-2026-08-10)**
→ 0, verified with `-Xmaxwarns` raised; 16 residual hits are pre-existing
JAXB-generated `jakarta.xml.bind.JAXBElement` usage, out of scope). The factory builder aborted the issue for this phase (#282,
55 files / 245 sites, over its 30-file PR cap, module can't compile
partially) — landed instead via a direct `mvn compile` loop. That also forced
the same mechanical widening in downstream modules (`mapview`,
`mapsforge-mapview`, `route-converter-gui`, `route-converter-cmdline`) to keep
the full reactor compiling.

Phase 2 (`route-converter-gui`) shipped 2026-08-10, commit
[`f41d209ea`](https://github.com/cpesch/RouteConverter/commit/f41d209ea) —
`route-converter-gui/src/main` is `-Xlint:rawtypes` clean (**120** → 0, the
true count once javac's silent `-Xmaxwarns 100` default was accounted for —
see [Phase 2 measurement + spike](#phase-2-measurement--spike-route-converter-gui-2026-08-10)).
Issue [#287](https://github.com/cpesch/RouteConverter/issues/287) was filed,
grilled, and `agent:approved`; the builder aborted exactly as decision 5
predicted (42 files/100 flagged sites, same 30-file cap as #282) — landed
directly instead, same as phase 1. Also widened `mapview`'s
`PositionsModel.getRoute()`/`setRoute()` (the 2-line prerequisite) and its one
downstream caller in `mapsforge-mapview`.

Phase 3 (`tail-modules`) shipped as two PRs, both merged 2026-08-11:
[PR #291](https://github.com/cpesch/RouteConverter/pull/291) (issue #288,
route-family + JAXB/misc residual, `route-converter-cmdline`/
`mapsforge-mapview`/`download`/`tileserver-maps`/`datasource`) and
[PR #290](https://github.com/cpesch/RouteConverter/pull/290) (issue #289, the
4 Swing hits in `mapsforge-mapview`).

Phase 4 (`gate`) shipped 2026-08-11 in two steps: [PR #298](https://github.com/cpesch/RouteConverter/pull/298)
(issue #295, the 22 pre-gate hits nobody had scoped — `navigation-formats` +
`route-catalog` JAXB residue, `profileview`, `common-gui`, `route-converter`),
then commit [`186ff0c7b`](https://github.com/cpesch/RouteConverter/commit/186ff0c7b)
(issue #296, the actual `-Xlint:rawtypes` gate flip — the builder's dry-run
produced a malformed diff hunk header and aborted, landed directly, same as
phases 1 and 2). `-Xlint:rawtypes` is now a permanent, enforced build gate
for main sources reactor-wide, with test sources exempt per the Decision
section. **Campaign complete** — see the [Phased plan](#phased-plan) for the
per-phase acceptance detail.

Successor to spec 00017. Scope A (deprecated APIs, hand-written unchecked)
shipped in PR #139; Scope B (`failOnWarning` + the build-helper locale noise)
shipped in PR #268. This spec covers the largest remaining bucket from tracking
issue #256: `rawtypes`.

## Problem

~~`-Xlint:rawtypes` reports **318** warnings (184 main, 134 test) — 59% of the
whole `-Xlint:all` backlog.~~ **Corrected 2026-08-10, fully re-measured with
javac's silent `-Xmaxwarns 100` raised (details:
[Phase 1 baseline correction](#phase-1-baseline-correction-2026-08-10)):
`-Xlint:rawtypes` reports **746** warnings (509 main, 237 test) — 57% of the
whole `-Xlint:all` backlog, which is itself **1307**, not the ~539 the
original 59% figure implied.** The proportion barely moved; every absolute
number under it was wrong by 2–4×. The qualitative diagnosis is unaffected —
it never depended on the exact count. They are not scattered: they are one
structural defect in `navigation-formats/base`, radiating outward.

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
`<compilerArgs><arg>-Xlint:all</arg></compilerArgs>` in the root pom — **not
re-verified with `-Xmaxwarns`, known wrong for the two navigation-formats
rows (see correction below), unverified for every other row**:

| raw type name | hits | | module / source root | hits |
|---|---|---|---|---|
| `NavigationFormat` | 94 | | navigation-formats / test | ~~98~~ **201** |
| `BaseRoute` | 86 | | navigation-formats / main | ~~82~~ **293** |
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

## Phase 1 baseline correction (2026-08-10)

Triggered by the same discovery that corrected phase 2's count (see
[Phase 2 measurement + spike](#phase-2-measurement--spike-route-converter-gui-2026-08-10)):
javac's default `-Xmaxwarns` is 100 and it drops anything past that cutoff
with **zero indication** it did so. Both of phase 1's own numbers for
`navigation-formats/main` — the baseline table's **82** (measured via
`-Xlint:all`, 2026-08-08) and the Status/close-out text's **100** (measured
via a dedicated `-Xlint:rawtypes`-only pass) — were capped. Re-measured
2026-08-10, throwaway worktree, checked out to the pre-phase-1 commit
(`699833a41`), same recipe as phase 2's correction (`-Xlint:rawtypes` +
`-Xmaxwarns 100000` in `compilerArgs`, `failOnWarning` temporarily `false`,
`mvn -pl navigation-formats -am clean test-compile`):

> **Main: 293. Test: 201.** Not 82/100 and not 98. `navigation-formats` alone
> (494 combined) exceeds the entire reactor-wide `-Xlint:all` total this spec
> opened with (318) — which means that headline number was always impossible,
> not just imprecise, and nobody caught it because a `BUILD SUCCESS` with a
> capped count looks identical to one with a real count.

**The shipped fix is unaffected — this is a historical-record correction, not
a code defect.** Re-measured the *current* (post-phase-1) state the same way,
`-Xmaxwarns` raised: **0** real `rawtypes` warnings, 16 pre-existing JAXB
residual, exactly as documented. Whatever the true pre-fix count actually
was, PR #286's mechanical propagation (compiler-driven, not a fixed list — see
Behaviour in issue #282's body) reached every site regardless, because it
worked by recompiling until clean rather than by counting down from a number.
The wrong number never gated the work; it only misinformed the written record
of how big the work was.

**Reactor-wide total re-verified too (2026-08-10, same pre-phase-1 commit,
`mvn clean test-compile`, `-Xlint:all` + `-Xmaxwarns 1000000` in root pom):**

> **Grand total: 1307**, not 318. Broken down: `rawtypes` **746** (509 main +
> 237 test — 1 off the dedicated rawtypes-only pass above by a duplicate log
> line, not a real discrepancy), `serial` **410**, `this-escape` **94**,
> `unchecked` **54**, `deprecation`/misc **3**.

Every one of the four category totals quoted in [Out of scope](#out-of-scope)
was wrong, not just the two already corrected above: `serial` **410** (not
117 — 3.5×), `this-escape` **94** (not 57), `unchecked` **54** (not 31).
`rawtypes` matches the two already-corrected module figures (293 + 201 for
`navigation-formats`, the rest small and already accurate — see the per-module
table below).

**Root cause is broader than "the reported category's own count exceeded
100" — it's a *shared* budget across every enabled category in one compile
execution.** `route-converter-gui/main` alone carries **308** of the 410
`serial` hits (undeclared `serialVersionUID` on every `AbstractAction`/dialog
subclass — a Swing idiom, not a design defect) plus its own 171 `rawtypes`
hits (pre-phase-1) — combined, one module's compile blew past the 100-warning
cap before printing even half its `rawtypes` warnings, which is exactly why
the original baseline reported only **56** for that row: not `rawtypes`
hitting its own cap, `serial` (an unrelated, out-of-scope category) crowding
it out. Any module with heavy `serial`/`this-escape` noise has this failure
mode regardless of how small its *own* category count looks.

Per-module `rawtypes` breakdown, uncapped, pre-phase-1 (main + test, `–`
where a module wasn't in the original table):

| module | main | test |
|---|---|---|
| `navigation-formats` | 293 | 201 |
| `route-converter-gui` | 171 (was reported 56) | 26 |
| `route-converter-cmdline` | 12 | – |
| `mapsforge-mapview` | 11 | 5 |
| `mapview` | 8 | 5 |
| `download` | 3 | – |
| `tileserver-maps` | 3 | – |
| `datasource` | 2 | – |
| `profileview` | 2 | – |
| `route-catalog` | 2 | – |
| `common-gui` | 1 | – |
| `route-converter` (root aggregator) | 1 | – |

None of this changes anything already shipped or in flight: phase 1 and
phase 2's fixes are both independently re-verified clean (above), `serial` is
permanently WONTFIX regardless of its true count, `this-escape` stays
deferred, and `unchecked` is scheduled for re-measurement at phase 4 anyway
(now with the right recipe). The only actionable fallout is `route-converter-gui`'s
own **171** pre-phase-1 figure — since phase 2 already re-measured and fixed
the module's *current* state directly (120 real, not derived from this
number), this doesn't change phase 2's outcome either. Recorded here so the
next person measuring anything in this repo with `-Xlint` doesn't have to
rediscover it.

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

## Phase 2 measurement + spike: `route-converter-gui` (2026-08-10)

Open question 2 said phase 2's true size was unknown because the spike never
compiled `route-converter-gui` against the tightened bounds. Re-measured now
that phase 1 (PR #286) has landed, throwaway worktree off `origin/master`,
same recipe as the baseline (`-Xlint:rawtypes` only — not `-Xlint:all` — added
to root pom `compilerArgs`, `failOnWarning` temporarily set `false` so the
reactor completes instead of halting on the first module, `MAVEN_OPTS=
"-Duser.language=en -Duser.country=US"`, `mvn -pl route-converter-gui -am
clean compile`). Reverted before removing the worktree; nothing committed.

**`route-converter-gui/src/main`: 120 warnings, not 56, and not 100 either.**
The original 56 was measured 2026-08-08, before phase 1 shipped. Phase 1's
mechanical `<?, ?>`/`<?>` widening of gui call sites kept the module
*compiling* against the tightened `navigation-formats` API, but it did not
reduce gui's own rawtypes count — count went up, because widening the
signatures at the module boundary exposed raw usage one layer further in that
hadn't been visible before.

**And the first re-measurement (100) was itself wrong — silently capped by
javac's default `-Xmaxwarns 100`.** Discovered only once the fix was
implemented: after fixing the reported 100, a clean rebuild with the same
`-Xlint:rawtypes` flag still showed 0 errors but the module wasn't actually
warning-free — a *second* clean measurement pass (this time adding
`-Xmaxwarns 100000` to the same `compilerArgs`) surfaced **20 more** hits in 6
files that had reported zero in every prior pass, simply because they were
warnings 101–120 and javac drops anything past its default cap without any
indication that it did so. **Any `-Xlint:rawtypes` measurement in this
campaign that reported a count ≥100 must be re-verified with `-Xmaxwarns`
added** — this includes re-checking whether phase 1's original 318-warning
`-Xlint:all` baseline (2026-08-08) undercounted for the same reason (unlike
per-category counts, which mostly stayed under 100 individually and are
probably fine, but the reactor-wide `-Xlint:all` total warning volume per
module compile could easily have crossed 100 and silently dropped some). Not
re-verified here — flagging it rather than re-litigating a already-shipped
phase 1's numbers.

True per-file breakdown, `mvn -pl route-converter-gui -am clean compile` with
`<compilerArgs><arg>-Xlint:rawtypes</arg><arg>-Xmaxwarns</arg><arg>100000</arg></compilerArgs>`,
`failOnWarning` temporarily `false`:

| raw type | hits | | file | hits |
|---|---|---|---|---|
| `BaseRoute` | 43 | | `ConvertPanel.java` | 17 |
| `NavigationFormat` | 25 | | `OptionsDialog.java` | 13 |
| `JList` | 21 | | `UndoFormatAndRoutesModel.java` | 9 (0 in the first, capped pass) |
| `JComboBox` | 19 | | `FormatAndRoutesModelImpl.java` | 8 |
| `FormatAndRoutes` | 5 | | `FileOperations.java` | 5 |
| `FilteringPositionsModel` / `FilterPredicate` | 2 each | | `ChangeRoute.java` | 4 (0 in the first pass) |
| `ComboBoxModel` / `AbstractListModel` / `BaseNavigationFormat` | 1 each | | `PhotoPanel.java` / `PositionsModelImpl.java` | 4 each |

`route-converter-gui/src/test`: **21**, close to the original 26 estimate —
test sources are out of scope per the Decision above, approach C (not
re-verified against `-Xmaxwarns` since it's under the cap and out of scope
either way).

**42 of the 120 are Swing (`JList` 21, `JComboBox` 19, `ComboBoxModel` 1,
`AbstractListModel` 1), not route-hierarchy.** The original phased plan filed
"the 12 Swing raw types" under phase 3 (tail-modules), on the assumption they
lived in `cmdline`/`mapview`/`mapsforge-mapview`. They don't — these 42 live in
`route-converter-gui` itself (21 near-identical `ListCellRenderer` subclasses
each declaring `getListCellRendererComponent(JList list, …)` instead of
`JList<?> list`), so phase 3 cannot fix them without a second pass over this
same module. They are trivially mechanical: widen the renderer parameter to
`<?>`, or use diamond `<>` on `new JComboBox<?>()`-style field
initialisers (raw `<?>` in a `new` expression is a compile error — see below).
Zero design risk, no correlation with the route hierarchy.

### Spike: same wildcard-widening approach on the remaining 59

Mechanically parameterised all 100 flagged main-source occurrences with the
same recipe phase 1 used (`BaseRoute`→`<?, ?>`, `NavigationFormat`/
`BaseNavigationFormat`/`FilteringPositionsModel`/`FilterPredicate`→`<?>`,
`FormatAndRoutes`→`<?, ?, ?>`, Swing types→`<?>`), one substitution per
flagged line, nothing else touched.

**Round 1 — 130 compile errors across 15 files.** Four distinct root causes,
none of them new to Java, all avoidable once known:

1. **A bare wildcard cannot be a direct supertype.** `FormatAndRoutesModel
   extends ComboBoxModel<?>` and `FormatAndRoutesModelImpl extends
   AbstractListModel<?>` — `error: unexpected type, required: class or
   interface without bounds, found: ?` — 2 sites, but the cascade is large:
   once those two types stop resolving, every consumer of
   `getSize()`/`setSelectedItem()` (`ConvertPanel`, `AddPositionListAction`,
   `MergePositionListMenu`, `FileOperations`, `PasteAction`, …) loses the
   method and reads as unrelated "cannot find symbol" noise. Fix: a *concrete*
   type argument one level down is legal even though the bare wildcard isn't —
   `ComboBoxModel<BaseRoute<?, ?>>` / `AbstractListModel<BaseRoute<?, ?>>`.
2. **A bare wildcard cannot appear in a `new` expression either** — 17 sites,
   same error shape (`new JComboBox<?>()` × 15 Swing widget fields, `new
   FormatAndRoutes<?, ?, ?>(…)` × 4 in `FileOperations`). Fix: diamond `<>` —
   except one of the four `FormatAndRoutes` sites, which the next point
   explains.
3. **Cross-module erasure clash.** `mapview`'s `PositionsModel.setRoute(BaseRoute
   route)`/`getRoute()` are *still raw* (2 of mapview's own residual hits,
   independently re-measured at 2, not the baseline's stale 8 — see the tail-
   modules re-measurement below). `route-converter-gui` classes overriding them
   with a widened `setRoute(BaseRoute<?, ?>)` no longer override anything —
   `name clash: … have the same erasure, yet neither overrides the other` —
   `FilteringPositionsModel`, `OverlayPositionsModel`, `PositionsModelImpl` all
   hit this. **`route-converter-gui` cannot compile rawtypes-clean without
   these two `mapview` methods widened first.** This is the concrete answer to
   open question 2 — the true size includes 2 lines outside the module.
4. **Wildcard-capture wall, same shape as phase 1's `NavigationFormatParser`.**
   `FileOperations.setRoutes(new FormatAndRoutes<>(format, result.getAllRoutes()))`
   — diamond inference fails because `format` (`NavigationFormat<?>`) and the
   route list are independent captures with nothing left to correlate them
   through. Same wall, different call site.

After fixing 1–3 by hand (2 supertype signatures, 17 diamonds, 2 lines in
`mapview`) — 8 sites, all mechanical, no design judgment required — **Round 2:
30 raw errors, 15 unique sites in 6 files.** Same order of magnitude as phase
1's Round 2 (78 → 14).

### A real defect, same shape as phase 1's unsound insert

`PositionsModelImpl` — `getIndex(NavigationPosition)` (line 96), `add(int,
List<BaseNavigationPosition>)` (205), `sort(Comparator<NavigationPosition>)`
(239), `order(List<NavigationPosition>)` (246) — each calls `getRoute()` (a
`BaseRoute<?, ?>` whose real position-type parameter is an *independent*,
unrelated capture at every call) with an argument typed only as the common
supertype (`BaseNavigationPosition`/`NavigationPosition`), not the route's
actual position type. Every one of these methods already carries a
pre-existing `@SuppressWarnings("unchecked")` — under raw types that
suppression was quietly papering over exactly this gap; wildcards turn the
same gap into a hard capture error instead of a silent unchecked warning.
It's sound today only because callers happen to always pass the
route's own position type by convention (`createPositions` converts through
`NavigationFormatConverter` first) — never verified by the compiler. This is
the identical pattern to phase 1's `getDuplicateFirstPosition` finding: correct
by convention, not by the type system, on a path (position add/sort/order) that
runs on ordinary editing, not just write. Phase 1's fix was to make the helper
generic in the position type rather than cast; the same move — or a
`@SuppressWarnings`-documented cast at each of these 4 sites, the smaller-diff
option phase 1 also uses in `NmnFormat`/`CoPilotFormat` — is the candidate fix.
Not a crash today; a latent hole the type system currently can't see.

One more, minor, not a runtime bug: `ConvertPanel`'s existing phase-1-authored
workaround (`// FormatAndRoutesModel extends the raw ComboBoxModel; narrow the
unchecked conversion…` then `@SuppressWarnings("unchecked") ComboBoxModel<
FormatAndRoutesModel> comboBoxModel = formatAndRoutesModel;`) casts to a type
argument — `FormatAndRoutesModel` — that isn't the combo box's real element
type (its elements are routes; the field itself is declared `JComboBox<
FormatAndRoutesModel>`, likewise a placeholder). Harmless under erasure, but
exactly the kind of misleading type witness this campaign exists to remove;
phase 2 should retype both to `<BaseRoute<?, ?>>` to match what the model and
renderer actually deal in.

### Tail-modules re-measured too (throwaway, same recipe)

The baseline table's per-module counts are 2026-08-08, pre-phase-1, like gui's
was. Re-measured 2026-08-10 for the record (`mvn -pl mapview,mapsforge-mapview,
route-converter-cmdline,download,tileserver-maps,datasource -am clean
compile`, full reactor, no test-compile):

| module | baseline (2026-08-08) | now (2026-08-10) |
|---|---|---|
| `mapview` | 8 | **2** (exactly the `getRoute`/`setRoute` pair above) |
| `mapsforge-mapview` | 11 | 10 |
| `route-converter-cmdline` | 12 | 6 |
| `download` | 3 | 3 |
| `tileserver-maps` | 3 | 3 |
| `datasource` | 2 | 2 |
| `common-gui` | (not itemised) | 1 |
| `navigation-formats` | 82 (pre-phase-1) | 16 (post-phase-1, JAXB, out of scope) |

Phase 1's fallout moved every downstream module's count, not just gui's — up
in one case (gui, 56→100), down in most others (cmdline 12→6, navigation-formats
82→16). None of the tail-module deltas are surprising; `mapview`'s is the one
that matters for phase 2, because 2 of its 2 remaining hits are the
prerequisite this spike found.

### Decision (2026-08-10) — phase 2 scope, grilled with the maintainer

Four forks, each grilled one at a time with a recommended default; the
maintainer took the recommendation on all four:

1. **The 41 Swing hits ship in phase 2, not a separate issue.** Same module,
   already touched, zero design risk — 20 `ListCellRenderer` subclasses widen
   `JList list` → `JList<?> list`; the `new JComboBox<?>()`-shaped field
   initialisers take diamond `<>`. Finishes `route-converter-gui` completely
   in one PR rather than leaving a residue for a phase 3 that wouldn't
   otherwise touch this module.
2. **`mapsforge-mapview` (10 hits) and `route-converter-cmdline` (6 hits)
   defer to phase 3.** Not required for `route-converter-gui` to compile
   rawtypes-clean — only `mapview`'s 2-line `getRoute()`/`setRoute()`
   prerequisite is, and that's already in scope (item 3 below). Keeps phase
   2's PR to one module plus the unavoidable 2-line touch outside it, instead
   of a 3-module change.
3. **`mapview`'s `PositionsModel.getRoute()`/`setRoute(BaseRoute route)`
   widen to `BaseRoute<?, ?>` as part of phase 2**, not a separate prerequisite
   issue — 2 lines, strictly required (§ Round 1 cause 3 above), and it also
   zeroes out `mapview`'s own remaining tail-modules count for free.
4. **The `PositionsModelImpl` defect** (`getIndex`/`add`/`sort`/`order` passing
   base-typed positions/comparators/lists into a route whose real position
   type is an unrelated wildcard capture) **is fixed with a documented
   `@SuppressWarnings("unchecked")` cast at each of the 4 call sites**, not by
   making the class generic in the position type. Matches phase 1's
   `NmnFormat`/`CoPilotFormat.getDuplicateFirstPosition` precedent — smaller
   diff, no interface change in `mapview`.
5. **Dispatch is a direct `mvn compile` loop from the start, not the factory
   builder.** 42 files / 100 sites in `route-converter-gui` alone sits in the
   same range that made the builder abort phase 1 (55 files/245 sites, 30-file
   cap, module can't compile partially). File the issue for tracking + review,
   but land the PR the same way phase 1 was actually landed.

## Phase 3 measurement + scope (2026-08-10)

Re-measured `tail-modules` for real, throwaway worktree off `origin/master`
(same commit the phase-2 spike used, `da9cafc0c`), same recipe: root pom
`compilerArgs` → `-Xlint:rawtypes`, `failOnWarning` temporarily `false`,
`MAVEN_OPTS="-Duser.language=en -Duser.country=US"`, full reactor `clean
test-compile` (not just `compile` — this run also caught the one test-source
hit in `mapsforge-mapview`). Reverted before removing the worktree; nothing
committed. Numbers agree with the phase-2 session's own tail-module spot-check
above; this pass goes one level deeper and breaks each module's count down by
*why* it's raw, which changes the scope in one real way (below).

| module | main | test | total | route-family | Swing | JAXB residual | other single-site |
|---|---|---|---|---|---|---|---|
| `route-converter-cmdline` | 6 | 0 | 6 | 6 | 0 | 0 | 0 |
| `mapsforge-mapview` | 10 | 1 | 11 | 5 | 4 | 0 | 2 (`org.mapsforge.map.layer.TileLayer`) |
| `mapview` | 2 | 0 | 2 | 2 | 0 | 0 | 0 |
| `download` | 3 | 0 | 3 | 0 | 0 | 1 | 2 (`java.util.concurrent.Future`) |
| `tileserver-maps` | 3 | 0 | 3 | 0 | 0 | 2 | 1 (own `ItemPreferencesMediator`) |
| `datasource` | 2 | 0 | 2 | 0 | 0 | 1 | 1 (own `Fragment`) |
| **total** | | | **27** | **13** | **4** | **4** | **6** |

**Correction to the phase-2 section's "this phase's Swing count is 0":** that's
right for the *originally-claimed* 12, which do live in `route-converter-gui`
(41, phase 2 §Decision item 1) — but 4 real, distinct Swing hits exist in
`mapsforge-mapview` on their own account, unrelated to that miscount: 3
`ListCellRenderer` subclasses declaring `getListCellRendererComponent(JList
list, …)` instead of `JList<?> list` (`LocalMapListCellRenderer.java:40`,
`LocalThemeListCellRenderer.java:40`, `ThemeStyleListCellRenderer.java:34`),
plus one designer-generated hit inside `MapSelector.java`'s `$$$setupUI$$$()`
(`comboBoxZoom = new JComboBox();` at line 219) — the field itself is already
`JComboBox<Integer>`, but the IntelliJ form format has no generics concept, so
the generated initialiser is permanently raw. Confirmed no `ComboBoxModel` hit
anywhere in the tail modules (the earlier "JComboBox, JList, ComboBoxModel"
framing was describing the shape of the bucket, not a literal 1:1 list).

**`mapview`'s 2 hits are phase 2's, not phase 3's.** Phase 2 §Decision item 3
already claims `PositionsModel.getRoute()`/`setRoute(BaseRoute route)` →
`BaseRoute<?, ?>` as part of that phase (it's the 2-line prerequisite phase 2
needs to compile against). Zero design risk either way, but doing it twice in
two issues would just be a merge conflict waiting to happen — `mapview` drops
out of phase 3's scope entirely, count 0.

No structural knot anywhere in the tail modules: `ItemPreferencesMediator`
(`tileserver-maps`) and `Fragment` (`datasource`) are both already correctly
generic at their own declaration — the raw hit is a single unparameterized
call/field site each, not a raw bound needing redesign. Same for the mapsforge
`TileLayer` hits (third-party generic class, single-site `instanceof` pattern
match, `<?>` fixes both).

**Decision (2026-08-10) — phase 3 scope, grilled with the maintainer:**

1. **One issue for the whole route-family + residual bucket** (`route-converter-cmdline`
   6, `mapsforge-mapview` 5 route-family + 2 `TileLayer`, `download` 1 JAXB + 2
   `Future`, `tileserver-maps` 2 JAXB + 1 `ItemPreferencesMediator`, `datasource`
   1 JAXB + 1 `Fragment` — **21 hits, ~14 files**), not one per module. Total
   scope is small and every fix is the same mechanical `<?, ?>`/`<?>`/concrete-
   type-argument shape phase 1 already established; splitting by module would
   just add tracking overhead for no review-risk reduction.
2. **The JAXB residual (4: `download`, `tileserver-maps`, `datasource`) and the
   misc single-site hits (6: `TileLayer`×2, `Future`×2, `ItemPreferencesMediator`,
   `Fragment`) fold into that same issue**, rather than being carved out like
   navigation-formats' 16 JAXBElement residue. Each is a one-line fix with no
   generated-code entanglement (unlike navigation-formats' JAXB residue, these
   aren't inside checked-in xjc output), and `download`/`tileserver-maps`/
   `datasource` would otherwise get no `-Xlint:rawtypes` cleanup this phase at
   all — they have zero route-family hits of their own.
3. **The 4 `mapsforge-mapview` Swing hits ship as their own issue**, not folded
   into #1 above and not deferred to some future pass — same rationale spec
   used for the original "12 Swing raw types" framing: unrelated to the route
   hierarchy, independently fixable, and mixing Swing-widget generics into the
   route-family issue would blur what the PR is actually about.
4. **`MapSelector.java:219`'s designer-generated raw `new JComboBox()` gets a
   documented `@SuppressWarnings("rawtypes")` on `$$$setupUI$$$()`, not a
   hand-edit.** The `.form` format has no generics concept, so writing
   `new JComboBox<Integer>()` by hand would just get silently reverted (or
   drift out of round-trip sync) the next time anyone opens the form in
   IntelliJ. Same shape as leaving the JAXB/kml `ObjectFactory` generated code
   alone — suppress at the generated site, fix the other 3 (hand-written
   `ListCellRenderer` overrides) for real.

Filed as [issue #288](https://github.com/cpesch/RouteConverter/issues/288)
(route-family + JAXB/misc residual, 21 hits) → [PR #291](https://github.com/cpesch/RouteConverter/pull/291),
merged 2026-08-11, and [issue #289](https://github.com/cpesch/RouteConverter/issues/289)
(the 4 `mapsforge-mapview` Swing hits) → PR #290, merged 2026-08-10.

## Phase 4 gate-readiness check (2026-08-11)

Re-verified reactor-wide with the same `-Xmaxwarns`-raised recipe, this time
against current `master` (`c93e51af8`, phases 1–3 all merged) —
`-Xlint:rawtypes,unchecked` in root `compilerArgs`, `-Xmaxwarns 1000000`,
`mvn clean test-compile`, full reactor:

> **`rawtypes`: 22 main, 149 test. `unchecked`: 18 main, 11 test.** The gate's
> stated acceptance criterion — "main-source rawtypes warnings are 0
> reactor-wide" — **is not met yet.** Phase 4 cannot be filed as-is.

The 22 main hits, by module:

| module | hits | nature |
|---|---|---|
| `navigation-formats` | 16 | pre-existing JAXB `JAXBElement`, already documented out of scope |
| `route-catalog` | 2 | pre-existing JAXB `JAXBElement`, same class, never itemised in any phase table |
| `profileview` | 2 | **new** — `ProfileModel.getRoute()`'s local `BaseRoute` (route-hierarchy, not JAXB) + `PatchedXYSeries`'s constructor param `Comparable` (JFreeChart's own ctor takes raw `Comparable`, single-site) |
| `common-gui` | 1 | **new** — `WindowHelper.handleThrowable`'s `Class` parameter |
| `route-converter` | 1 | **new** — `ThemeStyleDialog`'s designer-generated `$$$setupUI$$$()` raw `new JComboBox()`, structurally identical to `mapsforge-mapview`'s `MapSelector.java` case phase 3 already fixed by suppression |

**Three whole modules — `profileview`, `common-gui`, `route-converter` — were
never in any phase's scope table.** They surfaced in the
[reactor-wide re-verification](#phase-1-baseline-correction-2026-08-10) done
for phase 1's correction (that table already listed `profileview`/`route-catalog`/
`common-gui`/`route-converter` with counts, but nobody had connected those
rows back to "does the gate's zero-hit criterion actually hold" until now).
None of the four new hits are structurally hard — same mechanical shapes
already established (`BaseRoute<?, ?>`, `Comparable<?>`, `Class<?>`, and the
same documented-suppression precedent for the designer-generated `JComboBox`)
— but they need their own decision (fold into a small phase-3.5 issue, or
have the gate's `-Xlint:rawtypes` scoping explicitly exempt the 18 JAXB sites
by file/package rather than requiring literal zero). Not resolved here —
flagging so phase 4 isn't drafted against a false "already at zero" premise.

`unchecked`'s 18 main hits: 16 are `kml`'s three generated `ObjectFactory`
classes (`binding21`/`binding22`/`binding22beta` — the same JAXB RI 2.1
codegen the Out of scope section already names), 1 is `mapsforge-mapview`'s
`MapSelector.java:222` (the `@SuppressWarnings("rawtypes")` phase 3 added
there suppresses only that category — the sibling `unchecked` warning on the
same raw-`JComboBox` construction was never addressed, and needs the same
suppression extended or left as-is pending a decision), 1 is the new
`route-converter/ThemeStyleDialog` hit above (same shape, same fix). Down
from the corrected 54 pre-campaign — consistent with "should disappear during
phases 1–3" for the hand-written fallout, not literally to zero.

### Decision (2026-08-11) — resolving the gate-readiness gap

Grilled with the maintainer the same day this gap was flagged. All 22 hits
were fixed and the gate config validated end-to-end in a throwaway worktree
(applied the exact `pom.xml` change from item 4 below on top of the 22 fixes;
full reactor `BUILD SUCCESS`, test sources — 149 `rawtypes` hits, untouched —
did not fail the build) *before* filing, so neither issue asks the builder to
implement something unverified.

1. **Split into two issues, not one.** [Issue #295](https://github.com/cpesch/RouteConverter/issues/295)
   fixes the 22 hits (16 `navigation-formats` + 2 `route-catalog` JAXB
   residue, fixed for real — `<?>` widening, not exemption, since they're
   hand-written code that *uses* `JAXBElement` raw, not generated code
   itself; 2 `profileview` + 1 `common-gui` + 1 `route-converter`, same
   mechanical shapes as every prior phase, the `route-converter` one a
   documented suppression like `MapSelector`'s). **Shipped 2026-08-11, merged
   via [PR #298](https://github.com/cpesch/RouteConverter/pull/298), issue
   closed.** [Issue #296](https://github.com/cpesch/RouteConverter/issues/296)
   is the pom-only gate flip, explicitly blocked on #295 merging first — now
   unblocked and `agent:approved`. Rejected folding both into one PR: the
   gate flip is a one-block, easily-reviewed config change, and keeping it
   separate from a 9-file source PR means the gate can be reverted (or
   re-landed) without touching the source fixes, and vice versa.

   **Live-fire test of the sequencing decision:** the builder correctly
   *aborted* a premature dispatch on #296 (dry-run branch
   `agent/builder/dryrun-19566` on `rc/rc-meta`) because the spec it fetched
   still described #295 as "just filed" — it has no live GitHub access to
   check #295's real merge state itself, so it treated the spec's own text as
   the source of truth and refused to gate a build it couldn't verify was
   safe. That is exactly the intended failure mode: the two-issue split meant
   the worst outcome of a stale doc was a no-op abort, not a broken gate
   landing on `master`. Lesson for next time: update the spec **before**
   re-applying `agent:approved` to a downstream-blocked issue, not after —
   the gap between "I confirmed the merge in chat" and "the spec file says
   so" is exactly what the builder has no way to bridge.
2. **`-Xmaxwarns` gets raised permanently, in #296.** javac's silent 100-cap
   caused two wrong headline numbers in this campaign already (phase 1: 100 vs
   293 real; phase 2: 100 vs 120 real). Doesn't change gate behaviour
   (`failOnWarning` fires on the first warning regardless of the print cap) —
   only ensures a future CI log shows the true count.
3. **`unchecked` stays fully out of scope for both issues.** It needs its own
   suppress-vs-regenerate decision on the kml `ObjectFactory` codegen (16 of
   its 18 main hits) that has nothing to do with flipping the rawtypes gate —
   same reasoning phase 3 used to split Swing out of tail-modules rather than
   fold it in. The `MapSelector.java:222` / `ThemeStyleDialog` sibling
   `unchecked` warnings noted above are not addressed by either issue.

## Phased plan

Each phase is one PR, each ends green on the full reactor.

1. ✅ **`close-navigation-formats`** — shipped 2026-08-10, [PR #286](https://github.com/cpesch/RouteConverter/pull/286).
   The 5 bounds, the raw references in `navigation-formats/src/main`, and the
   two real defects from the Decision section (the `commentRoutes` loop
   variable, and `getDuplicateFirstPosition` made generic in the position type
   across `NmnFormat` + `CoPilotFormat` + their callers). Correction to this
   plan: the two **pre-existing** `@SuppressWarnings("unchecked")` in
   `NavigationFormatParser` were **removed**, not left in place — once
   `commentRoutes`/`preprocessRoute` were properly genericized end to end, both
   suppressions were vacuous (nothing left to suppress); the one *new*,
   documented cast per format lives in `NmnFormat`/`CoPilotFormat.getDuplicateFirstPosition`
   instead, exactly where the Decision section says the approach-A cost shows
   up. `-Xlint:rawtypes` for `navigation-formats/main`: ~~100~~ **293 real**
   (see [Phase 1 baseline correction](#phase-1-baseline-correction-2026-08-10))
   → 0, re-verified 2026-08-10 with `-Xmaxwarns` raised (16 residual hits are
   pre-existing `jakarta.xml.bind.JAXBElement` in generated gpx/kml/tcx code,
   out of scope). Full reactor compiles and tests green — required the
   same mechanical `<?, ?>`/`<?>` widening in `mapview`, `mapsforge-mapview`,
   `route-converter-gui`, `route-converter-cmdline` to keep those modules
   compiling against the tightened API (not a rawtypes-elimination pass on
   those modules themselves — that's still phases 2–3 below).
2. ✅ **`route-converter-gui`** — shipped 2026-08-10, commit [`f41d209ea`](https://github.com/cpesch/RouteConverter/commit/f41d209ea).
   Re-measured true count **120 main hits** (not 56, and not the first
   re-measurement's capped 100 either — see
   [Phase 2 measurement + spike](#phase-2-measurement--spike-route-converter-gui-2026-08-10)),
   42 Swing (unrelated to the route hierarchy, mechanical) + 78 route-hierarchy
   (approach A, same as phase 1, plus the 2-line prerequisite widening of
   `mapview`'s `PositionsModel.getRoute()`/`setRoute()` — and its one
   downstream caller in `mapsforge-mapview` — and a documented-cast fix for
   the real defect the spike found in `PositionsModelImpl`, mirrored in
   `UndoPositionsModel`). Grilled and scoped 2026-08-10, filed as
   [issue #287](https://github.com/cpesch/RouteConverter/issues/287); the
   builder aborted exactly as decision 5 predicted (42 files/100 flagged
   sites, same 30-file cap as phase 1's #282) — landed directly instead.
   `-Xlint:rawtypes` for `route-converter-gui/main`: 120 → 0 (verified with
   `-Xmaxwarns` raised past javac's silent default cap — see the correction
   note above). Full reactor `mvn clean test` green.
3. ✅ **`tail-modules`** — shipped as two PRs, both merged 2026-08-11: (a)
   [issue #288](https://github.com/cpesch/RouteConverter/issues/288) →
   [PR #291](https://github.com/cpesch/RouteConverter/pull/291), route-family +
   JAXB/misc residual across `route-converter-cmdline`, `mapsforge-mapview`,
   `download`, `tileserver-maps`, `datasource` (21 hits); (b)
   [issue #289](https://github.com/cpesch/RouteConverter/issues/289) → PR #290,
   the 4 Swing hits in `mapsforge-mapview` alone. #291's builder couldn't run
   Maven in its environment and asked for local confirmation before merge —
   re-verified 2026-08-11 with `-Xmaxwarns` raised: **0** rawtypes remaining in
   all 5 target modules, full reactor `mvn clean test` green. `mapview`
   dropped to 0 and out of this phase's scope — its 2 hits
   (`PositionsModel.getRoute()`/`setRoute()`) were phase 2's prerequisite, not
   phase 3's. See [Phase 3 measurement + scope](#phase-3-measurement--scope-2026-08-10)
   for the full breakdown — its numbers were single-category and small enough
   (27 total) to never risk the `-Xmaxwarns` cap that hit phases 1–2.
4. ✅ **`gate`** — re-scoped 2026-08-11 into two issues once the readiness gap
   above was resolved (see [Decision](#decision-2026-08-11--resolving-the-gate-readiness-gap)),
   both shipped 2026-08-11: [issue #295](https://github.com/cpesch/RouteConverter/issues/295)
   (the 22 pre-gate hits: 16 `navigation-formats` + 2 `route-catalog` JAXB
   residue, 2 `profileview`, 1 `common-gui`, 1 `route-converter`) →
   [PR #298](https://github.com/cpesch/RouteConverter/pull/298); [issue #296](https://github.com/cpesch/RouteConverter/issues/296)
   (the pom-only gate flip) → commit [`186ff0c7b`](https://github.com/cpesch/RouteConverter/commit/186ff0c7b) —
   the builder's dry-run (`agent/builder/noop-19574`) produced the correct
   content but a malformed diff hunk header (`@@ -1,3 +1,3 @@` against an
   ~80-line body) that failed to apply, so it landed directly, same as
   phases 1 and 2. Added `-Xlint:rawtypes` and a permanently-raised
   `-Xmaxwarns` to the root pom's `<compilerArgs>`, with the
   `default-testCompile` override from the Decision section so test sources
   stay exempt. `failOnWarning` was already true from #268. Acceptance
   verified on the actual commit, not just the pre-flight worktree: full
   reactor `mvn clean test-compile` and `mvn test` both green, and a
   deliberately reintroduced raw declaration in `common-gui/main` failed the
   build (reverted before the real commit) while test sources (149 rawtypes
   hits, untouched) did not.

**Campaign closed.** `-Xlint:rawtypes` is now a permanent, enforced gate for
main sources reactor-wide across the whole `RouteConverter` reactor.

## Out of scope

- `serial` (~~117~~ **410 real** — see
  [Reactor-wide total re-verified](#phase-1-baseline-correction-2026-08-10),
  308 of it in `route-converter-gui/main` alone) — WONTFIX per #256; never
  enable `-Xlint:serial`. The corrected count doesn't change the WONTFIX.
- `this-escape` (~~57~~ **94 real**) — deferred per #256. Doesn't change the deferral.
- `unchecked` (~~31~~ **54 real**) — the 15 hand-written hits are fallout of these same raw
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
2. ~~**Phase 2's true size is unknown.**~~ **Resolved 2026-08-10:** 100 main
   hits (not 56), 41 Swing + 59 route-hierarchy, plus a 2-line prerequisite in
   `mapview`. Full findings in [Phase 2 measurement + spike](#phase-2-measurement--spike-route-converter-gui-2026-08-10).
3. **Does the 82-format parse suite actually cover the write path?** Phase 1's
   only safety net is `ReadWriteBase`/`ConvertBase` staying green, and
   `preprocessRoute` — where the unsound insert lives — runs on write. Worth
   confirming that path is exercised before changing it, per the "one green run
   is not verification" rule.
