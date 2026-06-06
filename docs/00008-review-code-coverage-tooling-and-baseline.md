# 00008 - Coverage tooling, baseline, plan, and integration-test classification

## Status

Implemented on June 4, 2026.

Updated on June 5, 2026 (second update).

Updated on June 6, 2026 (Phase 1 start ? `download` module unit tests).

## Summary

JaCoCo remains the right coverage tool for this repository.

The repository now has a clearer and more usable coverage path:

- aggregate coverage reporting already works through `coverage-report`
- Surefire and Failsafe are aligned for both `*IT.java` and `*IntegrationTest.java`
- hermetic integration-test profiles exist for routine coverage work
- the first clear unit-style reclassification has been completed:
  - `route-catalog/src/test/java/slash/navigation/routes/remote/TempFileIT.java`
  - now `route-catalog/src/test/java/slash/navigation/routes/remote/TempFileTest.java`
- the first method-level extraction from a broader `*IT` class has also been completed:
  - unmarshalling-only KML helper tests now live in `navigation-formats/src/test/java/slash/navigation/kml/KmlUtilTest.java`
- a second conservative extraction has now also been completed:
  - the literal Google Maps parser case from `navigation-formats/src/test/java/slash/navigation/url/UrlFormatIT.java`
  - now runs in Surefire via `navigation-formats/src/test/java/slash/navigation/url/UrlFormatTest.java`
- a third conservative extraction has now also been completed:
  - the local `.url` fixture parser case from `navigation-formats/src/test/java/slash/navigation/url/UrlFormatIT.java`
  - now runs in Surefire via `navigation-formats/src/test/java/slash/navigation/url/UrlFormatTest.java`
- a fourth conservative extraction has now also been completed:
  - `TourFormatIT.testPositionInListOrder()` extracted from the broader tour round-trip class
  - now runs in Surefire via `navigation-formats/src/test/java/slash/navigation/tour/TourFormatTest.java`
- a fifth conservative extraction has now also been completed:
  - selected fixture-file extension-mapping tests from the GPX area
  - now run in Surefire via `navigation-formats/src/test/java/slash/navigation/gpx/GpxExtensionsTest.java`
- a sixth conservative extraction has now also been completed:
  - `testNavigationFileParserListener()` and `testReadWithFormatList()` from `NavigationFormatParserIT`
  - now run in Surefire via `navigation-formats/src/test/java/slash/navigation/base/NavigationFormatParserTest.java`
  - `NavigationFormatParserIT` now has 19 remaining tests, all verified green

As of June 5, 2026 (second update), the `NavigationFormatParserIT` listener and format-list API contract tests have been extracted and verified in Surefire, and `NavigationFormatParserIT` remains green with 19 tests.

As of June 6, 2026, Phase 1 has started. Three new unit test classes have been added to the `download` module covering pure-logic behavior:

- `ChecksumTest` ? 20 tests: `sameDay`, `laterThan`, `getLatestChecksum`, `equals/hashCode`, getters, `toString`
- `DownloadTest` ? 19 tests: `getPercentage`, `getChecksum` (state × action matrix), `setETag` (gzip strip), `equals/hashCode`, `getSize`/`getLastModified`, fragments
- `DownloadExecutorComparatorTest` ? 5 tests: ordering by timestamp, null-checksum, non-`DownloadExecutor` runnables

All 44 tests pass in Surefire (`Tests run: 44, Failures: 0, Errors: 0, Skipped: 0`).

## Current status

### Done

- JaCoCo remains the chosen primary coverage tool.
- The aggregate coverage workflow through `coverage-report` is still valid.
- Maven test naming is aligned:
  - Surefire excludes both `*IT.java` and `*IntegrationTest.java`
  - Failsafe includes both `*IT.java` and `*IntegrationTest.java`
- Hermetic profiles now exist for routine coverage work:
  - `hermetic-integration-test`
  - `test-all-hermetic`
- One clear unit-style `*IT` class has been reclassified and verified:
  - `TempFileTest` now runs in Surefire
- One mixed `*IT` class has already had narrow helper tests extracted and verified:
  - `KmlUtilTest`
- A second mixed `*IT` class has had its clearest narrow method extracted and verified:
  - `UrlFormatTest.readGoogleMapsUrl()`
- That same mixed `*IT` class has now had a second hermetic local-fixture method extracted and verified:
  - `UrlFormatTest.readURLReference()`
- `TourFormatIT.testPositionInListOrder()` has been extracted and verified in Surefire:
  - `TourFormatTest.testPositionInListOrder()`
- Selected GPX extension-mapping tests have been extracted and verified in Surefire:
  - `GpxExtensionsTest`
- Two narrow API-contract methods from `NavigationFormatParserIT` have been extracted and verified in Surefire:
  - `NavigationFormatParserTest.testNavigationFileParserListener()`
  - `NavigationFormatParserTest.testReadWithFormatList()`

### Open

- A broad hermetic whole-reactor run is still not a reliable green baseline.
- The hermetic vs external classification is good enough to guide work now, but more `*IT` classes can still be split or renamed gradually.
- The next `navigation-formats` work is now about conservative extraction choices, not emergency fixture-path repair.

## Coverage architecture and recommendation

### Tooling decision

Keep JaCoCo as the main coverage engine.

Reasons:

- it is the de-facto standard for Java/Maven projects
- it already works with Java 17 in this repository
- the repository already uses `org.jacoco:jacoco-maven-plugin` `0.8.14`
- the existing aggregate report flow through `coverage-report` is already valid
- replacing the tool would create migration work without solving the real bottleneck, which is missing coverage in key modules rather than missing reporting features

### Current build setup

The root `pom.xml` already:

- configures JaCoCo `prepare-agent` for unit tests
- configures a second JaCoCo agent property for integration tests via Failsafe
- excludes both `*IT.java` and `*IntegrationTest.java` from Surefire
- runs both naming conventions through Failsafe
- provides dedicated hermetic profiles for routine coverage work without live-service tests

`coverage-report/pom.xml` already:

- depends on the modules that should be analyzed together
- runs `jacoco:report-aggregate`
- merges module execution data into `coverage-report/target/aggregate.exec`

## Verified commands and observed outcomes

Before running Maven commands in a shell session, initialize Java once:

```sh
source ~/.sdkman/bin/sdkman-init.sh && sdk use java 17.0.19-tem
```

### Aggregate coverage flow

Previously verified command:

```sh
./mvnw -U -pl coverage-report -am -Dskip.integration.tests=true verify
```

Previously verified outputs:

- `coverage-report/target/aggregate.exec`
- `coverage-report/target/site/jacoco-aggregate/index.html`
- `coverage-report/target/site/jacoco-aggregate/jacoco.xml`

### Verified on June 4, 2026: `KmlUtilTest`

Verified command:

```sh
./mvnw -U -pl navigation-formats -am -Dtest=slash.navigation.kml.KmlUtilTest -Dsurefire.failIfNoSpecifiedTests=false clean test
```

Observed result:

- build succeeded
- `slash.navigation.kml.KmlUtilTest` ran in Surefire
- `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`

### Verified on June 4, 2026: `TempFileTest`

Verified command:

```sh
./mvnw -U -pl route-catalog -am -Dtest=slash.navigation.routes.remote.TempFileTest -Dsurefire.failIfNoSpecifiedTests=false clean test
```

Observed result:

- build succeeded
- `slash.navigation.routes.remote.TempFileTest` ran in Surefire
- `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`

### Verified on June 5, 2026: `UrlFormatTest` extraction follow-up

Verified command:

```sh
./mvnw -U -pl navigation-formats -am -Dtest=slash.navigation.url.UrlFormatTest,slash.navigation.url.GoogleMapsUrlFormatTest -Dsurefire.failIfNoSpecifiedTests=false clean test
```

Observed result:

- build succeeded
- `slash.navigation.url.UrlFormatTest` ran in Surefire with the extracted `readGoogleMapsUrl()` coverage
- `slash.navigation.url.GoogleMapsUrlFormatTest` remained green
- `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`

### Verified on June 5, 2026: second `UrlFormatTest` extraction

Verified command:

```sh
./mvnw -U -pl navigation-formats -am -Dtest=slash.navigation.url.UrlFormatTest -Dsurefire.failIfNoSpecifiedTests=false clean test
```

Observed result:

- build succeeded
- `slash.navigation.url.UrlFormatTest` now also runs the extracted `readURLReference()` coverage in Surefire
- `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`

### Verified on June 5, 2026: remaining `UrlFormatIT` after second extraction

Verified command:

```sh
./mvnw -U -pl navigation-formats -am -P hermetic-integration-test -Dit.test=slash.navigation.url.UrlFormatIT -Dfailsafe.failIfNoSpecifiedTests=false clean verify
```

Observed result:

- build succeeded
- `slash.navigation.url.UrlFormatIT` now contains only the live RouteConverter URL case
- `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`

### Verified on June 5, 2026: narrowed `UrlFormatIT`

Verified command:

```sh
./mvnw -U -pl navigation-formats -am -P hermetic-integration-test -Dit.test=slash.navigation.url.UrlFormatIT -Dfailsafe.failIfNoSpecifiedTests=false clean verify
```

Observed result:

- build succeeded
- `slash.navigation.url.UrlFormatIT` now contains only the remaining broader cases
- `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`

### Verified on June 5, 2026: module-local hermetic `navigation-formats`

Verified command:

```sh
./mvnw -U -pl navigation-formats -am -P hermetic-integration-test verify
```

Observed result:

- build succeeded
- `navigation-formats` Failsafe suite stayed green in module-local hermetic execution
- `Tests run: 526, Failures: 0, Errors: 0, Skipped: 0`
- `navigation-formats/target/failsafe-reports` contained 44 XML reports at that point, before the later Google Maps bookmark reclassification reduced the current `navigation-formats` `*IT` inventory by one

### Verified on June 5, 2026 (second update): `NavigationFormatParserTest` extraction

Verified command:

```sh
./mvnw -U -pl navigation-formats -am -Dtest=slash.navigation.base.NavigationFormatParserTest -Dsurefire.failIfNoSpecifiedTests=false clean test
```

Observed result:

- build succeeded
- `slash.navigation.base.NavigationFormatParserTest` ran in Surefire
- `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`

Verified command:

```sh
./mvnw -pl navigation-formats -am -P hermetic-integration-test -Dit.test=slash.navigation.base.NavigationFormatParserIT -Dfailsafe.failIfNoSpecifiedTests=false clean verify
```

Observed result:

- build succeeded
- `slash.navigation.base.NavigationFormatParserIT` ran in Failsafe with 19 remaining tests
- `Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`

### Verified on June 6, 2026: `download` module Phase 1 unit tests

Verified command:

```sh
./mvnw -pl download -am -Dtest=slash.navigation.download.ChecksumTest,slash.navigation.download.DownloadTest,slash.navigation.download.executor.DownloadExecutorComparatorTest -Dsurefire.failIfNoSpecifiedTests=false clean test
```

Observed result:

- build succeeded
- all three new classes ran in Surefire
- `Tests run: 44, Failures: 0, Errors: 0, Skipped: 0`

### Historical note from June 4, 2026

The earlier `KmlFormatIT` fixture-path failure that originally blocked the module-local hermetic baseline is now superseded by the June 5 green verification above.

### Operational caveat about `-Dit.test=...`

Directly forcing a named integration test with `-Dit.test=<ClassName>` is useful for positive selection, but it is **not** a reliable way to prove that a class is excluded by a hermetic profile.

Use normal module/profile runs and generated Failsafe reports to verify that external tests stay out of hermetic flows.

## Current aggregate baseline snapshot

The current baseline snapshot retained from the earlier aggregate coverage run is:

| Metric | Covered | Missed | Coverage |
|---|---:|---:|---:|
| Instruction | 33,355 | 186,936 | 15.14% |
| Branch | 2,606 | 12,612 | 17.12% |
| Line | 7,438 | 41,483 | 15.20% |
| Complexity | 3,049 | 20,993 | 12.68% |
| Method | 2,178 | 14,202 | 13.30% |
| Class | 467 | 1,547 | 23.19% |

## Module observations and planning priorities

### Stronger modules already worth preserving

| Module | Line coverage |
|---|---:|
| `common-navigation` | 78.39% |
| `geocoding-service` | 74.29% |
| `common` | 50.24% |
| `photon` | 44.44% |
| `browser-mapview` | 38.00% |
| `mapsforge-maps` | 36.85% |
| `tileserver-maps` | 35.44% |
| `datasource` | 31.52% |
| `graphhopper` | 29.05% |
| `navigation-formats` | 25.81% |

### Important low-coverage modules

| Module | Line coverage | Observation |
|---|---:|---|
| `route-converter` | 3.58% | Essential application logic, many classes, coverage far below importance |
| `download` | 5.44% | Core download behavior is barely covered |
| `route-catalog` | 6.08% | Catalog client behavior is essential but lightly covered |
| `gpx` | 22.67% | Important format support with no direct tests in this module |
| `kml` | 2.14% | Very large surface area with almost no direct module coverage |
| `download-tools` | 16.14% | Tooling exists but key behaviors are still lightly exercised |
| `mapsforge-mapview` | 13.78% | Important UI-adjacent behavior with limited tests |
| `common-gui` | 18.04% | Shared GUI logic is under-covered |

### Zero-coverage or near-zero modules

| Module | Line coverage | Comment |
|---|---:|---|
| `elevation-service` | 0.00% | tiny interface module |
| `feedback` | 0.00% | very small module |
| `mapsforge-mbtiles` | 0.00% | no direct tests |
| `profileview` | 0.00% | no direct tests |
| `proxy-tools` | 0.00% | very small utility module |
| `route-converter-cmdline` | 0.00% | wrapper or entrypoint module |
| `route-converter-opensource` | 0.00% | packaging or wrapper module |
| `route-converter-tools` | 0.00% | tooling wrapper module |
| `time-album-pro` | 0.00% | tiny module |
| `mapview` | 0.65% | shared abstraction layer with little direct exercise |

### Phased plan

1. **Phase 0 - keep the baseline easy to run**
   - keep aggregate reporting easy to reproduce locally
   - do not add failing global thresholds yet
2. **Phase 0.5 - fix execution and classification before writing many new tests**
   - keep Surefire and Failsafe aligned with both naming conventions
   - prefer hermetic profiles for routine coverage work
3. **Phase 1 - highest-value, lowest-friction gains**
   - `download`
   - `datasource`
   - `route-catalog`
   - `download-tools`
   - `common`
4. **Phase 2 - format and conversion logic**
   - `navigation-formats`
   - `gpx`
   - `kml`
   - `common-navigation`
5. **Phase 3 - application-level non-UI behavior**
   - `route-converter`
   - `mapsforge-mapview`
   - `common-gui`
   - `browser-mapview`
   - `tileserver-maps`
6. **Phase 4 - decide low-value module policy explicitly**
   - add lightweight smoke tests where behavior matters
   - otherwise exclude packaging-style modules from future gates deliberately
7. **Phase 5 - add gradual, module-specific quality gates only after the baseline improves**

### Recommended first implementation batch

If the goal is the next practical coverage increment, the best first batch is still:

1. `download`
2. `datasource`
3. `route-catalog`
4. `download-tools`
5. `route-converter` helper and model classes

## `*IT` inventory and current classification

### Inventory by module

| Module | `*IT` count |
|---|---:|
| `navigation-formats` | 43 |
| `route-catalog` | 5 |
| `download` | 2 |
| `feedback` | 2 |
| `brouter` | 1 |
| `download-tools` | 1 |
| `geonames` | 1 |
| `googlemaps` | 1 |
| `graphhopper` | 1 |
| `hgt` | 1 |
| `nominatim` | 1 |
| `photon` | 1 |

### Clearly hermetic `*IT` tests

- `download`: `QueuePersisterIT`
- `route-catalog`: `LocalCategoryIT`
- `navigation-formats`: broad sample-data and round-trip suites reviewed so far; module-local hermetic verification is green as of June 5

### Clearly external `*IT` tests

- `download`: `DownloadManagerIT`
- `download-tools`: `SnapshotCatalogIT`
- `route-catalog`: `RemoteCategoryIT`, `RemoteFileIT`, `RemoteRouteIT`
- `feedback`: `RouteFeedbackIT`, `SendChecksumsIT`
- `geonames`: `GeoNamesServiceIT`
- `googlemaps`: `GoogleServiceIT`
- `nominatim`: `NominatimServiceIT`
- `photon`: `PhotonServiceIT`
- `hgt`: `HgtFilesIT`
- `graphhopper`: `GraphHopperIT`
- `brouter`: `BRouterIT`

### Hermetic profile intent

For routine coverage work, prefer:

```sh
./mvnw -U -pl <module> -am -P test-all-hermetic clean verify
```

For deliberate live-service checks, use the broader opt-in profile instead:

```sh
./mvnw -U -pl <module> -am -P integration-test clean verify
```

## Unit-style reclassification and extraction review

### Already completed

#### `route-catalog/src/test/java/slash/navigation/routes/remote/TempFileTest.java`

This was the clearest whole-class rename candidate and has now been completed.

#### `navigation-formats/src/test/java/slash/navigation/kml/KmlUtilTest.java`

This is now the clearest completed method-level extraction from a broader `*IT` class:

- `testReader()`
- `testInputStream()`
- `testUnmarshal20()`
- `testUnmarshal20TypeError()`
- `testUnmarshal21()`
- `testUnmarshal21TypeError()`
- `testUnmarshal22Beta()`
- `testUnmarshal22()`

These tests now use in-memory XML documents and fit naturally in a Surefire unit-test class.

#### `navigation-formats/src/test/java/slash/navigation/url/UrlFormatTest.java`

The clearest narrow method from `UrlFormatIT` has now been extracted into Surefire:

- `readGoogleMapsUrl()`

This keeps the literal Google Maps URL parser-registry assertion in a small `*Test` class while leaving the live RouteConverter URL and local `.url` fixture coverage in `UrlFormatIT`.

#### `navigation-formats/src/test/java/slash/navigation/url/GoogleMapsUrlFormatBookmarkTest.java`

The former bookmark-fixture parser contract coverage from `GoogleMapsUrlFormatIT` has now been reclassified into Surefire as a dedicated `*Test` class.

- `testOriginalBookmark()`
- `testBookmarkWrittenByFirefox()`
- `testBookmarkWrittenByIE()`
- `testBookmarkWrittenByOpera()`

This keeps the local `.url` fixture variants together in one narrow parser-contract class without leaving them in Failsafe.

### Additional conservative candidates identified in the current review

#### `navigation-formats/src/test/java/slash/navigation/url/UrlFormatIT.java`

This class is no longer mixed in the same way as before; its hermetic local-fixture case has now been extracted.

- `readRouteCatalogUrl()` should stay external or opt-in because it reads a live RouteConverter URL.

Recommendation:

- `readGoogleMapsUrl()` is complete in `UrlFormatTest`
- `readURLReference()` is also complete in `UrlFormatTest`
- keep the remaining live RouteConverter URL coverage in `UrlFormatIT`

#### `navigation-formats/src/test/java/slash/navigation/tour/TourFormatIT.java`

Candidate method:

- `testPositionInListOrder()`

Reason:

- it uses `TourFormat` directly rather than the full format registry
- it asserts a narrow ordering contract instead of a broad format sweep

Why this is still only a candidate:

- it still reads a real fixture file
- it is better described as a small parser-contract test than a pure in-memory unit test

#### `navigation-formats/src/test/java/slash/navigation/gpx/GpxExtensionsIT.java`

These methods are narrower than the surrounding format suites and are worth tracking as method-level candidates:

- `testReadGarminGpxExtensionv3()`
- `testReadGarminTrackPointExtensionv1()`
- `testReadGarminTrackPointExtensionv2()`
- `testReadTrekbuddyExtension1()`
- `testReadTrekbuddyExtension2()`
- `testWriteGarminGpxExtensionv3Temperature()`

Reason:

- they focus on extension-field mapping rather than broad route conversion behavior
- the write test checks one concrete serialized extension fragment

Why these are weaker candidates than `KmlUtilTest`:

- they still depend on real GPX fixtures and parser or writer behavior
- the class still behaves more like a focused integration contract than a pure unit test

#### `navigation-formats/src/test/java/slash/navigation/tour/TourFormatTest.java`

`testPositionInListOrder()` has been extracted from the tour round-trip IT class. It uses `TourFormat` directly (not the full registry) and asserts a narrow position-order contract from a local fixture.

#### `navigation-formats/src/test/java/slash/navigation/gpx/GpxExtensionsTest.java`

Extension-field mapping tests have been extracted from the GPX area:

- `testReadGarminGpxExtensionv3()`
- `testReadGarminTrackPointExtensionv1()`
- `testReadGarminTrackPointExtensionv2()`
- `testReadTrekbuddyExtension1()`
- `testReadTrekbuddyExtension2()`
- `testWriteGarminGpxExtensionv3Temperature()`

These tests use local fixture GPX files but focus on extension-field mapping rather than broad route conversion.

#### `navigation-formats/src/test/java/slash/navigation/base/NavigationFormatParserTest.java`

Two narrow API-contract methods have been extracted from `NavigationFormatParserIT`:

- `testNavigationFileParserListener()` ? tests that adding and removing a listener correctly receives and stops receiving format-detection events
- `testReadWithFormatList()` ? tests that reading with an explicit format list gates the parser correctly

Both use only local fixture files and no external services. `NavigationFormatParserIT` now has 19 remaining tests.

### Tests that should probably stay `*IT`

The following reviewed classes should still remain integration-style, even if hermetic:

- `download/.../QueuePersisterIT`
- `route-catalog/.../LocalCategoryIT`
- `navigation-formats/.../ReadIT`
- `navigation-formats/.../ReadWriteRoundtripIT`
- `navigation-formats/.../SplitIT`
- `navigation-formats/.../AppendIT`
- `navigation-formats/.../ConvertFailsIT`
- `navigation-formats/.../StartDateIT`
- `navigation-formats/.../OziExplorerConvertIT`
- `navigation-formats/.../AccurracyConvertIT`

These still exercise real file, parser, writer, round-trip, or multi-format integration behavior rather than small isolated unit seams.

## Practical recommendation from here

1. keep `00008` as the single coverage planning note
2. use `test-all-hermetic` for routine module-level coverage work where possible
3. keep the now-green `navigation-formats` hermetic module run as a verified baseline and re-check it after each extraction batch
4. continue the conservative rename or extraction strategy:
   - whole-class rename only for clearly unit-style tests
   - method-level extraction for mixed classes
   - leave broad fixture-driven coverage in `*IT`
5. the `navigation-formats` conservative extraction shortlist is now largely exhausted:
   - remaining `NavigationFormatParserIT` tests are broader format-validation sweeps, not narrow API contracts
   - remaining `TourReadWriteRoundtripIT` tests are full round-trip integration tests
   - next meaningful work is Phase 1: writing new tests for under-covered modules
6. **next recommended batch (Phase 1 ? lowest friction, highest value):**
   - `download`
   - `datasource`
   - `route-catalog`
   - `download-tools`
   - `route-converter` helper and model classes

## Conclusion

The repository already has a functioning coverage pipeline.

The important progress is now clear and real:

- the build understands both integration-test naming conventions
- hermetic profiles exist
- two clear unit-style reclassifications are complete and verified
- six useful method-level extractions are complete and verified:
  - `KmlUtilTest` (8 methods)
  - `UrlFormatTest.readGoogleMapsUrl()` and `readURLReference()`
  - `GoogleMapsUrlFormatBookmarkTest` (4 bookmark-fixture methods)
  - `TourFormatTest.testPositionInListOrder()`
  - `GpxExtensionsTest` (6 extension-field mapping methods)
  - `NavigationFormatParserTest.testNavigationFileParserListener()` and `testReadWithFormatList()`
- `navigation-formats` `*IT` extraction shortlist is now largely exhausted
- the next practical step is Phase 1: writing new tests for under-covered modules (`download`, `datasource`, `route-catalog`, `download-tools`)

That makes the next coverage steps much more concrete than before.
