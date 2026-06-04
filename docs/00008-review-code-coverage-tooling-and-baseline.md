# 00008 - Coverage tooling, baseline, plan, and integration-test classification

## Status

Implemented on June 4, 2026.

Planned on June 4, 2026.

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

The main remaining blocker is still `navigation-formats` integration coverage, especially fixture-path handling during module-local hermetic runs.

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

### Open

- A broad hermetic whole-reactor run is still not a reliable green baseline.
- `navigation-formats` still needs stabilization before the hermetic path can be treated as the routine repository-wide coverage job.
- In particular, `KmlFormatIT` currently fails in module-local hermetic execution because fixture paths resolve to `navigation-formats-samples/src/...` relative to the module working directory and those files are not found there.
- The hermetic vs external classification is good enough to guide work now, but more `*IT` classes can still be split or renamed gradually.

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

### Verified on June 4, 2026: current `KmlFormatIT` blocker

Verified command:

```sh
./mvnw -U -pl navigation-formats -am -P hermetic-integration-test -Dit.test=slash.navigation.kml.KmlFormatIT -Dfailsafe.failIfNoSpecifiedTests=false clean verify
```

Observed result:

- build failed
- `slash.navigation.kml.KmlFormatIT` produced fixture lookup errors
- representative failures include missing files under:
  - `navigation-formats-samples/src/test/...`
  - `navigation-formats-samples/src/samples/...`

This is the clearest currently verified reason that `navigation-formats` still blocks a reliable module-local hermetic baseline.

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
| `navigation-formats` | 44 |
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
- `navigation-formats`: broad sample-data and round-trip suites reviewed so far, subject to current path-fix work

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

### Additional conservative candidates identified in the current review

#### `navigation-formats/src/test/java/slash/navigation/url/UrlFormatIT.java`

This is now one of the clearest remaining mixed classes.

- `readRouteCatalogUrl()` should stay external or opt-in because it reads a live RouteConverter URL.
- `readGoogleMapsUrl()` looks unit-style:
  - parses a literal Google Maps URL string
  - no live service contract is asserted
  - no sample-file sweep or round-trip is involved
- `readURLReference()` is a weaker candidate:
  - still narrow and parser-focused
  - but it depends on the `.url` fixture behavior and needs confirmation of the intended local fixture setup

Recommendation:

- strongest next extraction candidate from this class is `readGoogleMapsUrl()`
- if the `.url` fixture remains intentionally local and stable, `readURLReference()` may belong in the same smaller `*Test` class later

#### `navigation-formats/src/test/java/slash/navigation/url/GoogleMapsUrlFormatIT.java`

All four current methods are plausible unit-style candidates:

- `testOriginalBookmark()`
- `testBookmarkWrittenByFirefox()`
- `testBookmarkWrittenByIE()`
- `testBookmarkWrittenByOpera()`

Reason:

- each method exercises one narrow parsing behavior around browser bookmark URL variations
- no network calls are visible in the test
- the assertions are format-specific rather than broad integration-flow assertions

Why this is still only a candidate:

- the class still relies on real sample bookmark files and the full parser entry point
- it is closer to a parser contract test than a pure in-memory unit test

Recommendation:

- this is a good candidate for a future `GoogleMapsUrlFormatTest` if the parser entry can remain stable enough for Surefire use

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

#### Existing narrower candidates still worth keeping on the shortlist

- `navigation-formats/src/test/java/slash/navigation/base/NavigationFormatParserIT.java`
  - `testNavigationFileParserListener()`
  - `testReadWithFormatList()`

These remain narrow API-contract candidates, but they still depend on real parser and fixture behavior.

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
3. fix `navigation-formats` fixture-path handling before treating the hermetic path as a broad baseline
4. continue the conservative rename or extraction strategy:
   - whole-class rename only for clearly unit-style tests
   - method-level extraction for mixed classes
   - leave broad fixture-driven coverage in `*IT`
5. after the path issue is fixed, revisit the next strongest candidates:
   - `UrlFormatIT.readGoogleMapsUrl()`
   - `GoogleMapsUrlFormatIT`
   - `TourFormatIT.testPositionInListOrder()`
   - selected `GpxExtensionsIT` methods

## Conclusion

The repository already has a functioning coverage pipeline.

The important progress is now clear and real:

- the build understands both integration-test naming conventions
- hermetic profiles exist
- one clear unit-style reclassification is complete and verified
- one useful method-level extraction is complete and verified
- the next real blocker is no longer abstract tooling work, but stabilizing `navigation-formats` fixture handling for hermetic integration runs

That makes the next coverage steps much more concrete than before.
