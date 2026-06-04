# 00009 - Plan to increase coverage of essential parts

## Goal

Increase coverage where it protects the most important behavior first:

1. core shared logic
2. datasource/catalog/download flows
3. format parsing and serialization
4. application-level non-UI behavior
5. only then packaging and thin launcher modules

This plan assumes JaCoCo remains the primary reporting tool.

## Guiding principles

- Prefer tests around stable domain behavior over UI mechanics.
- Raise coverage in modules that are both important and realistic to test.
- Do not spend early effort chasing wrapper modules that mainly delegate or package artifacts.
- Keep generated-code decisions explicit before adding hard thresholds.
- Favor focused unit tests and small integration-style tests that use real repository objects.
- Recover already-written but currently under-executed tests before writing large volumes of new ones.

## Test taxonomy to use throughout the plan

To keep the campaign efficient and avoid flaky coverage numbers, split tests into three buckets:

1. **Unit tests**
   - no network
   - no live services
   - small, fast, deterministic

2. **Hermetic integration tests**
   - may use temporary files, sample data, JAXB, or multiple in-repo modules
   - still deterministic and safe for regular coverage runs

3. **External integration tests**
   - talk to live HTTP services or externally hosted test data
   - useful for compatibility checks, but not ideal as required coverage inputs

### Recommendation

Include only unit tests and hermetic integration tests in normal coverage improvement work.

Keep live-service tests opt-in until they are either stabilized behind local fixtures or explicitly separated into a distinct profile.

## Phase 0 - Stabilize the baseline and reporting workflow

### Outcome

A repeatable baseline that everyone can run locally before making coverage decisions.

### Actions

- Keep using the verified command:

```sh
source ~/.sdkman/bin/sdkman-init.sh && sdk use java 17.0.19-tem
./mvnw -U -pl coverage-report -am -Dskip.integration.tests=true verify
```

- Review the HTML report in `coverage-report/target/site/jacoco-aggregate/index.html` after each focused test batch.
- Decide which modules are considered:
  - essential product logic
  - supporting libraries
  - packaging/wrapper modules
- Decide whether JAXB-generated code stays inside future gate calculations.

### Recommendation

Do **not** add a failing minimum threshold yet.

Current line coverage is only `15.20%`, and the repository still has many zero-coverage packaging-style modules. A gate introduced now would create noise and encourage gaming the metric instead of improving the right code.

## Phase 0.5 - Fix test execution conventions before writing too many new tests

### Why this phase is needed

The repository currently contains many `*IT.java` tests, but the root Maven configuration only routes `*IntegrationTest.java` through Failsafe.

That means some existing tests are probably not contributing to coverage at all.

Part of this phase has now been completed: the root Maven configuration has been aligned so Failsafe recognizes both `*IT.java` and `*IntegrationTest.java`, while Surefire excludes both patterns.

### Outcome

A clear distinction between:

- tests that should run in normal coverage builds
- tests that should stay opt-in because they use live services
- tests that need renaming or build configuration changes to execute consistently

### Actions

- inventory existing `*IT.java` tests module by module
- classify each `*IT.java` test as:
  - hermetic integration
  - external integration
  - obsolete or redundant
- keep Maven test includes aligned with both supported naming conventions
- standardize the naming convention for future tests

### Recommendation

Prefer **keeping Maven aligned with both `*IT.java` and `*IntegrationTest.java`**, then gradually standardize naming later.

Reason:

- the repository already contains `61` `*IT.java` tests
- mass-renaming first would add churn without increasing confidence immediately
- some hermetic tests can likely increase coverage quickly now that they can be executed intentionally

### Immediate module-specific opportunities

- `download`: `QueuePersisterIT` looks hermetic and should be considered for regular execution
- `navigation-formats`: many `*IT.java` round-trip tests appear sample-data driven and are likely strong candidates for regular execution
- `route-catalog`: classes such as `RemoteCategoryIT` hit the live API and should remain opt-in unless replaced by hermetic tests around parsing/mapping layers
- `download`: `DownloadManagerIT` currently defaults to `https://static.routeconverter.com/test/`, so it should not become a mandatory coverage source without local test fixtures

## Phase 1 - Highest-value, lowest-friction coverage gains

### Priority modules

| Module | Current line coverage | Why first |
|---|---:|---|
| `download` | 5.44% | Core behavior, moderate size, thin enough for quick improvement |
| `datasource` | 31.52% | Central data model/service area, easy to exercise with real JAXB objects |
| `route-catalog` | 6.08% | Important remote catalog logic with existing test footholds |
| `download-tools` | 16.14% | Already test-oriented and easy to grow with deterministic inputs |
| `common` | 50.24% | Existing test structure can likely lift coverage quickly |

### Work items

#### 1. `download`

Focus on:

- download queue behavior
- retry/failure transitions
- checksum and state handling
- URL/file naming edge cases

Recommended test style:

- deterministic unit tests around queue/state classes
- small tests with temporary files instead of mocked file abstractions where practical

#### 2. `datasource`

Focus on:

- include/source/file/map/theme traversal helpers
- filtering/lookup behavior
- error handling for incomplete catalog structures

Recommended test style:

- use real JAXB binding objects and wrappers
- follow the repository convention from `AGENTS.md`: build real `DataSource` / `Source` / `File` / `Map` / `Theme` inputs instead of mocks

#### 3. `route-catalog`

Focus on:

- remote catalog parsing helpers
- URL composition and normalization
- route list handling
- fallback/error paths

Recommended test style:

- keep network calls mocked at the boundary
- concentrate on parsing, mapping, and domain decisions

#### 4. `download-tools`

Focus on:

- HTML parsing corner cases
- URI extraction and normalization
- command-building edge cases
- wildcard/anchor filter variants

Recommended test style:

- add table-driven parser tests using tiny HTML fixtures
- extend the existing `WgetCommandBuilderTest` style with real binding objects

#### 5. `common`

Focus on:

- date/time and formatting edge cases
- file/path helper boundary cases
- platform-dependent parsing/formatting helpers

Recommended test style:

- small, fast unit tests
- emphasize malformed input and locale/time-zone edge cases

### Expected benefit

This phase should improve real product safety quickly without heavy refactoring.

### Concrete first-batch class targets

To make the first campaign more actionable, start with these classes before expanding module-wide:

- `download`
  - `QueuePersister`
  - `DownloadManager`
  - `DownloadTableModel`
  - `QueueUtil`
- `datasource`
  - `DataSourceService`
  - `DataSourcesUtil`
  - selected wrapper implementations only where branch behavior exists
- `route-catalog`
  - `RemoteCatalog`
  - `RoutesUtil`
  - `LocalCatalog`
  - `DirectoryFileFilter`
- `download-tools`
  - command builder and parser helpers first

### Working progress targets for the first campaign

These are **planning targets**, not gates:

- increase aggregate line coverage by roughly `2` to `3` percentage points
- raise `download` by at least `10` line-coverage points
- raise `datasource` by at least `10` line-coverage points
- raise `route-catalog` by at least `8` line-coverage points
- add at least one new hermetic test seam in `route-converter` helper/model code even if that module is not yet the main focus

## Phase 2 - Strengthen core format and conversion logic

### Priority modules

| Module | Current line coverage | Why now |
|---|---:|---|
| `navigation-formats` | 25.81% | Already has many tests; broad format behavior makes added tests valuable |
| `gpx` | 22.67% | Important format support with no direct tests in the module |
| `kml` | 2.14% | Huge shipped surface area and large blind spot |
| `common-navigation` | 78.39% | Already healthy; use targeted additions to protect edge cases |

### Work items

#### 1. `navigation-formats`

Focus on:

- registries and discovery logic
- conversions shared across multiple formats
- negative parsing cases
- serialization round-trips for representative formats

Recommended approach:

- prioritize high-traffic formats and shared base classes before obscure formats
- add regression tests when a specific format bug is fixed instead of trying to blanket every format equally

#### 2. `gpx`

Focus on:

- parser/serializer behavior not already covered transitively
- extension handling
- route/track/waypoint combinations
- malformed but recoverable input

#### 3. `kml`

Focus on:

- KML document parsing and writing helpers
- shared KML utility code
- representative variants instead of exhaustive duplication

Recommendation:

Treat `kml` as a multi-iteration effort. Its size is too large for a single coverage push.

Additional recommendation:

Do not start by trying to cover all `kml` variants equally. Begin with shared utility code and round-trip behavior already exercised by representative sample files, then add bug-driven regressions.

#### 4. `common-navigation`

Focus on:

- edge-case arithmetic
- locale/format handling
- null/empty input behavior

This module is already healthy, so only targeted additions are needed.

## Phase 3 - Improve application-level behavior without depending on GUI automation

### Priority modules

| Module | Current line coverage | Why this matters |
|---|---:|---|
| `route-converter` | 3.58% | Main application logic, very high business importance |
| `mapsforge-mapview` | 13.78% | Important behavior around map synchronization and updates |
| `common-gui` | 18.04% | Shared UI logic used across the app |
| `browser-mapview` | 38.00% | Already testable, worth incremental hardening |
| `tileserver-maps` | 35.44% | Shared mapping infrastructure |

### Work items

#### 1. `route-converter`

Primary targets:

- models
- helpers
- service facades
- command/state transformation logic
- non-visual controller behavior

Recommendation:

Avoid starting with Swing form tests. Instead, increase coverage around existing testable seams such as helper and model classes, then refactor tightly coupled logic out of panels where needed.

Improvement to the plan:

Treat `route-converter` as two sub-streams:

- **Phase 3A**: add tests around already testable helpers, models, and service facades
- **Phase 3B**: only after that, refactor panel/controller logic to extract testable collaborators

This reduces the chance that the campaign stalls on GUI coupling.

#### 2. `mapsforge-mapview` and `common-gui`

Focus on:

- updater logic
- selection and synchronization helpers
- event/model transformations
- runtime visibility behavior instead of designer-generated layout code

Important note:

Per `AGENTS.md`, do not hand-edit `$$$setupUI$$$`; layout remains controlled by `.form` files. Tests should target runtime logic, not generated UI setup blocks.

#### 3. `browser-mapview` and `tileserver-maps`

Focus on:

- filtering/reduction heuristics
- manager/helper decision logic
- boundary cases around map and overlay selection

## Phase 4 - Make deliberate decisions about low-value modules

### Modules to review separately

- `route-converter-cmdline`
- `route-converter-opensource`
- `route-converter-tools`
- `time-album-pro`
- `feedback`
- `proxy-tools`
- `elevation-service`
- `mapsforge-mbtiles`

### Recommendation

For these modules, choose one of two strategies explicitly:

1. add lightweight smoke tests if the module contains meaningful behavior, or
2. exclude the module from future quality gates if it is mainly packaging, bootstrapping, or thin delegation

Do not let these modules block progress on the essential parts.

Improvement to the plan:

Decide this module policy early enough that future gates are computed against the intended scope rather than the full reactor by accident.

## Phase 5 - Add gradual quality gates

Only after Phases 1 to 3 have raised the baseline:

- add module-specific expectations instead of a single repo-wide number
- start with non-failing reporting or a very low floor on selected essential modules
- tighten thresholds gradually

### Recommended order for gating

1. `common`
2. `common-navigation`
3. `datasource`
4. `download`
5. `route-catalog`
6. `navigation-formats`
7. `route-converter`

### Why module-specific gates

The repository mixes:

- mature shared libraries
- format-heavy parsing modules
- application logic
- wrapper/packaging modules

A single global threshold would reward the wrong optimizations.

## Suggested first implementation batch

If the goal is to start immediately with a practical first increment, the best first batch is:

1. `download`
2. `datasource`
3. `route-catalog`
4. `download-tools`
5. `route-converter` helper/model classes

This batch balances:

- importance
- feasible testability
- likely visible coverage improvement
- low risk of broad refactoring

Improvement to the plan:

Before writing new tests in this batch, first try to unlock any already-written hermetic `*IT.java` tests that belong to these modules. That is likely the fastest way to gain trustworthy coverage.

## Exit criteria for the first campaign

Consider the first campaign successful when all of the following are true:

- the aggregate JaCoCo report remains easy to run locally
- at least the Phase 1 modules each gain clearly measurable coverage
- `route-converter` coverage improves in helper/model/service areas without GUI test fragility
- generated-code and wrapper-module policy is documented before thresholds are enforced
- the intended hermetic `*IT.java` tests are actually being executed by the build, or are explicitly deferred with a documented reason

## Recommended next step

Start with Phase 1 and treat `download`, `datasource`, and `route-catalog` as the first concrete implementation targets.

That gives the best balance of essential behavior, manageable test seams, and visible improvement per unit of effort.

