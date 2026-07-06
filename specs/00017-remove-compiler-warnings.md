---
name: 00017-remove-compiler-warnings
status: in-flight
phases_done: [1, 2]
phases_next: []
last_touched: 2026-07-06
---

# 00017 - Remove compiler warnings (Scope A: deprecated APIs + real unchecked)

## Status

Planned. Created July 6, 2026. Scope and every risk decision fixed by the
maintainer in a grilling session (see Decisions). Delivered on a worktree
branch off `master` (not the active `spec-00016-p4-joptionpane` WIP), one
squash PR, two commits.

## Problem

`mvn clean compile` (Java 21, 45 modules) is not warning-clean. Measured
landscape:

### Default build (no `-Xlint`) — 115 warnings
| bucket | count | where |
|---|---|---|
| deprecated API | 68 | 21 source files |
| "platform locale" (JAXB codegen) | 46 | generated date/time code, not our source |
| missing source dir | 1 | profileview native2ascii execution |

### Under `-Xlint:all` — ~450 source warnings (currently hidden)
| bucket | count |
|---|---|
| rawtypes (`found raw type`) | 181 |
| serial (no `serialVersionUID`) | 83 |
| this-escape | 57 |
| unchecked | 28 (16 generated, 12 real) |
| deprecated API | 68 |
| redundant cast | 2 |

## Decision: Scope A only

Fix everything the **default** build emits from our own source, plus the 12
real `unchecked`. Explicitly **out of scope**: rawtypes / serial / this-escape
(the `-Xlint`-only bulk) and the 46 JAXB-codegen locale warnings — invasive,
low ROI, or not our source. No enforcement gate this pass (`-Werror` /
`failOnWarning` impossible without the excluded bulk; deferred to a future spec).

## Grilled decisions (locked)

- **Q1 Scope** → Scope A. Leave rawtypes/serial/this-escape.
- **Q2 Enforcement** → no gate now.
- **Q3 `new URL(String)` (×9)** → **full conversion** to `new URI(s).toURL()`.
  Guard the two file-path-capable sites (NavigationFormatParser.parse,
  RecentUrlsModel) so a `URISyntaxException` from a space-bearing local path
  falls back to `new File(s).toURI().toURL()` — preserves old leniency.
- **Q4 commons-cli 1.11.0 (×37)** → **do both** swaps: `Option.Builder.build()`
  → `get()`; old `HelpFormatter` → `org.apache.commons.cli.help.HelpFormatter`.
- **Q5 real unchecked (×12)** → **per-site judgment**: fix generics where local
  and clean (PhotoPanel, ConvertPanel, ThemeStyleDialog, MapSelector);
  `@SuppressWarnings("unchecked")` + why-comment on BaseRoute (a real fix there
  cascades into the excluded 181 rawtypes).
- **Q6a profileview** → remove the dead `native2ascii` plugin block entirely
  (no `LocalizationBundle*.properties`, no `resources/` dir there).
- **Q6b JAXB codegen locale (×46)** → out of scope.
- **Q7 delivery** → one spec, one worktree branch off master, one squash PR,
  two commits (mechanical / behavior-touching).

## Work items

### Deprecated API (68)
- `new Locale(String,String)` → `Locale.of(...)` — LocaleHelper (×21 across sites).
- `System.runFinalization()` → delete the call — WindowHelper (deprecated for removal).
- `new URL(String)` → URI conversion (×9) per Q3: NavigationFormatParser (×2),
  RecentUrlsModel, TileServerMapSource, googlemaps profile-URL append, + others.
- commons-cli (×37) per Q4: ScanWebsite, UpdateCatalog, SnapshotCatalog, MirrorCatalog.

### Real unchecked (12) — per-site per Q5
- PhotoPanel (×6), ConvertPanel (×3), ThemeStyleDialog (×1), MapSelector (×1): fix generics.
- BaseRoute (×1): suppress + comment.

### Build hygiene (1)
- profileview/pom.xml: remove dead native2ascii execution.

## Phases

- **Phase 1 — mechanical / zero-risk (commit 1):** Locale.of, delete
  runFinalization, commons-cli both swaps, profileview dead-plugin removal.
- **Phase 2 — behavior-touching (commit 2):** URL→URI full conversion with
  file-path fallback, per-site unchecked fixes.

## Corrections found during execution

- **URL sites: 12, not 9.** Ground-truth `grep 'new URL('` beat the truncated
  log dedup: added NavigationFormatParser (x2) and DeleteRoutes.
- **commons-cli: 6 files, not 4.** CheckProxy (proxy-tools) and
  FilterResourceBundles (route-converter-tools) were missed in the first pass,
  caught by the `-Xlint` verify, fixed in P2.
- **"12 real unchecked" over-counted.** 6 of them live in IntelliJ
  `$$$setupUI$$$`-generated blocks (PhotoPanel x3, ConvertPanel, ThemeStyleDialog,
  MapSelector) — treated as generated (out of scope, like ObjectFactory).
  Genuinely hand-written and fixed: PhotoPanel:108/314, ConvertPanel:385,
  BaseRoute:508.
- **FileOperations:119/131 unchecked are pre-existing on master** (raw
  FormatAndRoutes; method at line 84 already suppresses the sibling site) —
  the initial baseline undercounted them due to inconsistent `-Xlint`
  application. Out of scope (same cascade as ConvertPanel:385).

## Verification

- Full-reactor `-Xlint:all` compile (temp pom arg, reverted): all targeted
  buckets now **0** — Locale, URL(String), Option.Builder, old HelpFormatter,
  runFinalization, missing-source-dir. Remaining unchecked are only generated
  `setupUI` + pre-existing FileOperations + generated ObjectFactory.
- New `Files.toUrl` unit tests cover the http-passthrough and the bare/
  space-bearing-path File fallback (guards the Q3 leniency claim).
- Unit tests green across all touched modules. Note: `IntegerModelTest.
  testSetIntegerNegative` is **pre-existing flaky** (default-Locale pollution
  across tests, execution-order dependent) — reproduced then cleared on both
  master and this branch; unrelated to these changes.

## Out of scope / follow-ups

- Enforcement gate (`-Xlint` + `-Werror`) — future spec, needs the bulk cleared first.
- rawtypes (181) / serial (83) / this-escape (57) — separate invasive campaign.
- JAXB codegen locale (46) — generator config, risks churning generated output.
