---
name: 00008-review-code-coverage-tooling-and-baseline
status: shipped
phases_done: []
phases_next: []
last_touched: 2026-06-04
---

# 00008 - Coverage tooling, baseline, plan, and integration-test classification

## Status

Implemented on June 4, 2026.

Updated on June 5, 2026 (second update).

Updated on June 6, 2026 (Phase 1 complete ? 137 new unit tests across 5 modules; aggregate instruction coverage measured at 35.01%).

Updated on June 6, 2026 (Phase 5 complete ? JaCoCo `check` gates added to 8 modules with per-module thresholds).

Updated on June 6, 2026 (Phase 5 measurement ? aggregate coverage re-measured after all phases; `photon/pom.xml` invalid JaCoCo check execution removed).

Updated on June 7, 2026 (Phase 6 complete + coverage re-measured; JAXB binding classes now excluded from both JaCoCo agent and report; `datasource` threshold corrected to 0.30 to match actual non-binding coverage).

Updated on June 7, 2026 (Phase 6 ? 39 new unit tests across `kml` and `route-catalog`; JaCoCo gates confirmed green for `photon`, `kml`, and `route-catalog`; aggregate coverage re-measured at **40.54% line** after excluding JAXB binding classes from scope).

Updated on June 7, 2026 (Phase 7 ? 130 new unit tests across 7 modules: `routing-service`, `geocoding-service`, `common`, `common-navigation`, `common-gui`, `rest`; all green in Surefire; JaCoCo gate added to `routing-service` at 50% line threshold).

Updated on June 7, 2026 (Phase 8 ? 50 new unit tests across `navigation-formats`: `GoPalUtil` (binding3 round-trip), `ViaMichelinUtil`, `Nmn7Util`, `NavigonCruiserUtil`, `GkPosition`, `MercatorPosition`; all green in Surefire).

Updated on July 4, 2026 (Phase 9 - `common-gui` headless model/helper logic: 18 new unit tests across `FilteringTableModel` (7), `UndoManager` (6), `CombinedResourceBundle` (5); all hermetic, all green in Surefire; module suite 42 -> 60 tests).

Updated on July 4, 2026 (Phase 10 - `mapsforge-mapview` model/updater value classes: 21 new unit tests across `PairWithLayer` (6), `PositionWithLayer` (7), `ThemeStyleImpl` (8); Mockito for `Layer`/`XmlRenderThemeStyle*`, no rendering or live tiles; all green in Surefire; module suite 38 -> 59 tests).

Updated on July 4, 2026 (Phase 11 - `route-converter-gui` document/dnd/helper logic: 32 new unit tests across `DoubleDocument` (9), `IntegerDocument` (8), `UrlDocument` (5), `NavigationFormatFileFilter` (4), `PositionSelection` (6); Swing text/Transferable/FileFilter run headless, Mockito only for `NavigationFormat`; all green in Surefire).

Updated on July 4, 2026 (Phase 12 - `route-converter-gui` dnd/model delegation: 25 new unit tests across `RouteSelection` (4), `CategorySelection` (4), `StringDocument` (4), `TimeZoneModel` (4), `FilteringPositionsModel` (9); Mockito for `PositionsModel`/`BaseRoute`/`RouteModel`/`CategoryTreeNode`, isolated Preferences keys per test; all green in Surefire).

Updated on July 4, 2026 (Phase 13 - `common` value/utility surface + one `mapview` model: 37 new unit tests filling uncovered methods of already-tested classes - `CompactCalendar` (12: equals/hashCode, `hasDateDefined`, `parseDate`, factory zone propagation, cross-zone before/after, `asUTCTimeInTimeZone`), `Transfer` (13: trim, parseInteger/Int/Long/Short, isEmpty overloads, toDouble/toArray, encodeUmlauts, toMixedCase, trimLineFeeds), `Files` (6: getExtension/removeExtension/setExtension/extractFileName/compare/asDialogString) - plus `CharacteristicsModel` (6, Mockito `BaseRoute`) in `mapview`; all green in Surefire).

Updated on July 4, 2026 (Phase 14 - `common` utility surface, part 2: 12 more mock-free unit tests across `Transfer` (7: ceilFraction, roundMeterToMillimeterPrecision, roundMillisecondsToSecondPrecision, escape, isIsoLatin1ButReadWithUtf8, stripNonValidXMLCharacters, formatLong/ShortAsString) and `Files` (5: getExtension `File`/`URL`/`List<URL>` overloads, toUrls url-or-file fallback, reverse); all green in Surefire).

Updated on July 4, 2026 (Phase 15 - `navigation-formats` format helpers/position types: 19 new mock-free unit tests across `GarminFlightPlanFormat` (8: hasValid/createValid identifier+description, createValidWaypointType airport rule, createValidCountryCode US/prefix rules, ctor derivation), `TomTomPosition` (6: x100000 integer scaling, truncation-toward-zero, Integer ctor, null coords, equals), `NmnPosition` (5: description reconstruction, DESCRIPTION_PATTERN parse - number group needs >= 2 chars, isUnstructured); all green in Surefire).

Updated on July 4, 2026 (Phase 16 - `navigation-formats` GoPal/Tour position types: 15 new mock-free unit tests across `TourPosition` (8: description reconstruction incl. name, setDescription clear, asGoPalRoutePosition house-number parse with 0 fallback, asTourPosition identity, equals) and `GoPalPosition` (7: description reconstruction, setDescription clear, asTourPosition address copy, equals; pins the NPE when asTourPosition unboxes a null houseNumber); all green in Surefire).

Updated on July 4, 2026 (Phase 17 - `navigation-formats` NMEA + Garmin FPL positions: 9 new mock-free unit tests across `NmeaPosition` (5: degrees<->NMEA ddmm.mmmm round-trip, value+orientation storage, raw-NMEA constructor, S/W negative degrees, null coordinates) and `GarminFlightPlanPosition` (4: identifier ctor derivation, description fallback identifier,waypointType,country, explicit description precedence, identifier derived from description); all green in Surefire).

Updated on July 4, 2026 (Phase 18 - `navigation-formats` CSV position: 6 new mock-free unit tests for `CsvPosition` (map-backed column lookup via primary and alternative German column names, missing-column null, setter round-trips through the backing map, extended-sensor temperature/heading round-trip, backing-row exposure); all green in Surefire).

Updated on July 4, 2026 (Phase 19 - `navigation-formats` GPX position: 6 new mock-free unit tests for `GpxPosition` (plain constructor accessors, GPX 1.0 BigDecimal constructor populating heading + hdop/pdop/vdop/satellites, plain setDescription city/no-reason, heading/speed round-trip without a position extension, asGpxPosition identity, equals); no JAXB bindings touched; all green in Surefire).

Updated on July 4, 2026 (Phase 20 - `navigation-formats` Excel position: 4 new mock-free unit tests for `ExcelPosition` via its in-memory HSSFWorkbook constructor (coordinate/speed/description storage, coordinate setter round-trips, extended-sensor temperature round-trip, backing-row exposure); no files, no POI mocks; all green in Surefire).

Updated on July 5, 2026 (Phase 21 - `navigation-formats` core route domain: 10 new mock-free unit tests for `BaseRoute`'s shared position-manipulation logic via the concrete `Wgs84Route` (top/move/bottom reordering, remove, removeDuplicates by adjacent distance, getContainedPositions, getPositionsWithinDistanceToPredecessor, getClosestPosition by coordinates and by time, getSuccessor/getIndex/getPosition). Highest refactoring-robustness target - `BaseRoute` is the spine every format's route extends. All green in Surefire).

Updated on July 5, 2026 (Phase 22 - `navigation-formats` position geo-math: 9 new mock-free unit tests for `BaseNavigationPosition` via the concrete `Wgs84Position` (hasCoordinates/hasTime, calculateDistance ~111 km per degree + null without both coordinates, calculateAngle due-east 90, calculateElevation other-minus-this, calculateTime millis delta, calculateSpeed km/h + null without both times, calculateOrthogonalDistance ~0 on the line). The position-level counterpart to Phase 21's `BaseRoute`; pure geo/units math with high refactor blast-radius. All green in Surefire).

Updated on July 5, 2026 (Phase 23 - `navigation-formats` cross-format conversion matrix: 5 new mock-free unit tests for `SimpleRoute` via the concrete `Wgs84Route` (accessors + add, generated-name fallback, equals by name/characteristics/positions, and the `asXxxFormat` conversion matrix - Csv/Gpx/Kml/Nmea/Nmn/Photo/Simple/Tcx preserve position count and longitude, Bcr/GoPal/TomTom preserve count; a null format is safe since it is only stored on the target route). asExcelFormat omitted - it needs a real ExcelFormat to create a sheet, and its position path is covered by Phase 20. All green in Surefire).

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

As of June 6, 2026 (Phase 1 complete), 137 new unit tests have been added across all 5 Phase 1 modules. All tests are hermetic (no network, no live services) and run in Surefire. Phase 1 is closed.

### Phase 1 test inventory (June 6, 2026)

| Module | New tests | Commits | Key classes covered |
|---|---:|---|---|
| `download` | 55 | `5571cc85b`, `ae70a698b` | `Checksum`, `Download`, `DownloadExecutorComparator`, `Validator` |
| `datasource` | 24 | `ae70a698b` | `DataSourceImpl`, `DownloadableImpl`, `FragmentImpl` |
| `route-catalog` | 20 | `afdf4f209` | `DirectoryFileFilter`, `LocalRoute`, `RouteComparator` |
| `download-tools` | 14 | `e2f17392a` | `DownloadableType`, `AnchorFilter` branches, `WgetCommandBuilder` edge cases |
| `route-converter` | 24 | `91181e1a2` | `DateTimeComparator`, `DescriptionComparator`, `TautologyPredicate`, `PointOfInterestPositionPredicate`, `TagStatePhotoPredicate` |
| **Total** | **137** | | |

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
- Phase 1 is **complete** ? all 5 planned modules have new unit tests (137 tests total, all green).
- Phase 2 is the next target: format and conversion logic in `navigation-formats`, `gpx`, `kml`, `common-navigation`.

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

### Verified on July 4, 2026: `common-gui` Phase 9 unit tests

Verified command:

```sh
./mvnw -pl common-gui test -Dskip.integration.tests=true
```

Observed result:

- build succeeded
- 18 new tests ran in Surefire: `FilteringTableModelTest` (7 - row mapping, re-filter on delegate change and on `setFilterPredicate`, `mapRows`, mapped write-through, `getColumnCount` contract), `UndoManagerTest` (6 - undo/redo stack transitions, `discardAllEdits`, `ChangeListener` firing), `CombinedResourceBundleTest` (5 - key merge, later-bundle override, `getKeys`, missing-key `MissingResourceException`, empty list)
- `CombinedResourceBundleTest` uses two tiny fixture bundles under `common-gui/src/test/resources/slash/navigation/gui/helpers/`
- full module suite: `Tests run: 60, Failures: 0, Errors: 0, Skipped: 0` (was 42)

### Verified on July 4, 2026: `mapsforge-mapview` Phase 10 unit tests

Verified command:

```sh
./mvnw -pl mapsforge-mapview test -Dskip.integration.tests=true
```

Observed result:

- build succeeded
- 21 new tests ran in Surefire: `PairWithLayerTest` (6 - getters/row mutation, `hasCoordinates` both-ends rule, mutable layer/distanceAndTime, equals-by-first+second, null/other-type, `toString`), `PositionWithLayerTest` (7 - position/layer accessors, `hasCoordinates`, equals-by-position+layer incl. both-null, null/other-type, `toString`), `ThemeStyleImplTest` (8 - `description()` default-language -> menu-language -> id fallback chain, `getUrl`, `getCategories` mapping + empty, equals/hashCode by url)
- `Layer`, `XmlRenderThemeStyleLayer` and `XmlRenderThemeStyleMenu` are Mockito mocks; no map rendering, no live tiles
- full module suite: `Tests run: 59, Failures: 0, Errors: 0, Skipped: 0` (was 38)

### Verified on July 4, 2026: `route-converter-gui` Phase 11 unit tests

Verified command (`-am` rebuilds the refactored `common-gui`/`common` from source; without it the reactor resolves a stale `common-gui` from the local `.m2` and `OptionsDialog` fails to compile against the older `UIHelper.chooseDirectory` signature - a local artifact-staleness quirk, not a source break):

```sh
./mvnw -pl route-converter-gui -am test \
  -Dtest=DoubleDocumentTest,IntegerDocumentTest,UrlDocumentTest,NavigationFormatFileFilterTest,PositionSelectionTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- build succeeded
- 32 new tests ran in Surefire: `DoubleDocumentTest` (9) and `IntegerDocumentTest` (8 - insert/remove validation state machine, whole-number fraction stripping, `getDouble`/`getInt` zero-on-failure, lone-minus and letter/decimal rejection), `UrlDocumentTest` (5 - `getShortUrl` trim/last-fragment/ellipsis-truncation, empty/blank -> null), `NavigationFormatFileFilterTest` (4 - extension match, directory accept, description/format passthrough), `PositionSelectionTest` (6 - deep-copy of input, flavor array, `isDataFlavorSupported`, POSITION/STRING `getTransferData`, unsupported-flavor throw)
- Swing text `Document`, `Transferable`/`DataFlavor` and `FileFilter` instantiate headlessly; Mockito used only for the `NavigationFormat` collaborator
- all green: `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`

### Verified on July 4, 2026: `route-converter-gui` Phase 12 unit tests

Verified command (`-am` for the same stale-`.m2` reason as Phase 11):

```sh
./mvnw -pl route-converter-gui -am test \
  -Dtest=RouteSelectionTest,CategorySelectionTest,StringDocumentTest,TimeZoneModelTest,FilteringPositionsModelTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- build succeeded
- 25 new tests ran in Surefire: `RouteSelectionTest` (4) and `CategorySelectionTest` (4 - single-flavor Transferable contract: flavor array, `isDataFlavorSupported`, list returned for own flavor, unsupported throws), `StringDocumentTest` (4 - get/set/replace/clear), `TimeZoneModelTest` (4 - default, persist, change-listener fire + remove; isolated per-test Preferences keys), `FilteringPositionsModelTest` (9 - delegation of getRoute/setRoute/getPosition/getIndex/edit/remove/flags/fireTableRowsUpdated through the mapped delegate row, plus UnsupportedOperationException guards for structural ops)
- Mockito mocks `PositionsModel`, `BaseRoute`, `RouteModel`, `CategoryTreeNode`, `PositionColumnValues`
- all green: `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`

### Verified on July 4, 2026: `common` + `mapview` Phase 13 unit tests

Verified commands:

```sh
./mvnw -pl common test \
  -Dtest=CompactCalendarValueTest,TransferStringNumberTest,FilesExtensionTest \
  -Dsurefire.failIfNoSpecifiedTests=false
./mvnw -pl mapview -am test -Dtest=CharacteristicsModelTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- both builds succeeded
- `common`: 31 new tests - `CompactCalendarValueTest` (12), `TransferStringNumberTest` (13), `FilesExtensionTest` (6); no mocks, all pure static/value logic that the existing `CompactCalendarTest`/`TransferTest`/`FilesTest` did not reach
- `mapview`: 6 new tests - `CharacteristicsModelTest` (headless `ComboBoxModel`; Mockito `BaseRoute`; selection reflect, change-updates-route, no-op-when-unchanged, from-null-selection)
- all green: `Tests run: 31 ...` and `Tests run: 6 ...`, Failures 0, Errors 0

### Verified on July 4, 2026: `common` Phase 14 unit tests

Verified command:

```sh
./mvnw -pl common test \
  -Dtest=TransferRoundingEscapeTest,FilesUrlTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- build succeeded
- 12 new tests: `TransferRoundingEscapeTest` (7) and `FilesUrlTest` (5), no mocks
- control/replacement characters are built via `(char) 0x00` / `(char) 0xFFFD` concatenation rather than embedded literals, so the source stays plain ASCII
- all green: `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`

### Verified on July 4, 2026: `navigation-formats` Phase 15 unit tests

Verified command:

```sh
./mvnw -pl navigation-formats test \
  -Dtest=GarminFlightPlanFormatTest,TomTomPositionTest,NmnPositionTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- build succeeded
- 19 new tests, no mocks: `GarminFlightPlanFormatTest` (8), `TomTomPositionTest` (6), `NmnPositionTest` (5)
- discovered while writing: `NmnFormat.DESCRIPTION_PATTERN`'s house-number group `( .[^,;]+)?` needs >= 2 characters after the space, so a one-digit number like `"5"` does not parse (test uses `"55"`)
- all green: `Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`

### Verified on July 4, 2026: `navigation-formats` Phase 16 unit tests

Verified command:

```sh
./mvnw -pl navigation-formats test \
  -Dtest=TourPositionTest,GoPalPositionTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- build succeeded
- 15 new tests, no mocks: `TourPositionTest` (8), `GoPalPositionTest` (7)
- `GoPalPositionTest.asTourPositionThrowsOnNullHouseNumber` pins a real rough edge: `asTourPosition()` calls `Short.toString(getHouseNumber())`, which unboxes a null `Short` to a NullPointerException
- all green: `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`

### Verified on July 4, 2026: `navigation-formats` Phase 17 unit tests

Verified command:

```sh
./mvnw -pl navigation-formats test \
  -Dtest=NmeaPositionTest,GarminFlightPlanPositionTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- build succeeded
- 9 new tests, no mocks: `NmeaPositionTest` (5), `GarminFlightPlanPositionTest` (4)
- NMEA conversion asserted as degrees<->ddmm.mmmm round-trips within 1e-6, with `13.5` degrees stored as value `1330.0`/`East` and South/West orientations negating the decimal degrees
- all green: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`

### Verified on July 4, 2026: `navigation-formats` Phase 18 unit tests

Verified command:

```sh
./mvnw -pl navigation-formats test -Dtest=CsvPositionTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- build succeeded
- 6 new tests, no mocks: `CsvPositionTest` exercises the `Map<String,String>` column lookup - primary name (`Longitude`), alternative name (`Laengengrad`), missing-column null, setter round-trips, extended-sensor temperature/heading
- all green: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`

### Verified on July 4, 2026: `navigation-formats` Phase 19 unit tests

Verified command:

```sh
./mvnw -pl navigation-formats test -Dtest=GpxPositionTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- build succeeded
- 6 new tests, no mocks, no JAXB: `GpxPositionTest` uses the plain and GPX-1.0 BigDecimal constructors (heading/hdop/pdop/vdop/satellites are inherited Wgs84Position fields), plain-text setDescription, extension-free heading/speed round-trip, asGpxPosition identity, equals
- all green: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`

### Verified on July 4, 2026: `navigation-formats` Phase 20 unit tests

Verified command:

```sh
./mvnw -pl navigation-formats test -Dtest=ExcelPositionTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- build succeeded
- 4 new tests, no mocks, no files: `ExcelPositionTest` drives the `ExcelPosition(Double,...)` constructor, which builds a real in-memory `HSSFWorkbook`/`Row`, then round-trips coordinates, speed, description and an extended-sensor temperature through that row
- note: `ExcelPosition` does not override `equals` (identity-based), so no value-equality test was written
- all green: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`

### Verified on July 5, 2026: `navigation-formats` Phase 21 unit tests

Verified command:

```sh
./mvnw -pl navigation-formats test -Dtest=BaseRouteTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- build succeeded
- 10 new tests, no mocks: `BaseRouteTest` drives `BaseRoute`'s shared editing logic through a concrete `Wgs84Route` (null format is safe - these methods only touch positions), with positions along the zero meridian at latitudes 0..3
- distance-based cases use generous metre thresholds (1 degree of latitude is ~111 km) so they stay robust to the exact haversine constants
- all green: `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`

### Verified on July 5, 2026: `navigation-formats` Phase 22 unit tests

Verified command:

```sh
./mvnw -pl navigation-formats test -Dtest=BaseNavigationPositionTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- build succeeded
- 9 new tests, no mocks: `BaseNavigationPositionTest` drives the shared geo/units math through `Wgs84Position`; distance/speed assertions use generous deltas (1 degree latitude ~= 111 km) to stay robust to the bearing constants
- all green: `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`

### Verified on July 5, 2026: `navigation-formats` Phase 23 unit tests

Verified command:

```sh
./mvnw -pl navigation-formats test -Dtest=SimpleRouteTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Observed result:

- build succeeded
- 5 new tests, no mocks: `SimpleRouteTest` exercises accessors/add/equals plus 11 of the 12 `asXxxFormat` conversions through `Wgs84Route` with a null format
- `asExcelFormat` is excluded: it calls `ExcelFormat.createSheet(...)` so it NPEs on a null format; the Excel position path is already covered by Phase 20's `ExcelPositionTest`
- all green: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`

### Verified on July 5, 2026: `datasource` Phase 24 unit tests (`DataSourcesUtil`)

Verified command:

```sh
./mvnw -pl datasource -am test -Dtest=DataSourcesUtilTest \
  -Dsurefire.failIfNoSpecifiedTests=false -P '!local-with-samples'
```

Observed result:

- build succeeded; `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`
- extended the existing `DataSourcesUtilTest` (which only round-tripped the JAXB catalog) with 13 hermetic tests for the pure DTO↔domain mapper/factory methods: `asChecksum`, `asChecksums`, `asBoundingBox`/`asBoundingBoxType` (incl. null-corner branches), `asDatasourceType` (Mockito `DataSource`), `createFileType`/`createMapType`/`createThemeType` (incl. null checksums/bounding box), all three `createFragmentType` overloads (key+sizes, `ZipEntry`+stream checksum, `Fragment`+`FileAndChecksum`), `createChecksumType`, and `asMetaDataComparablePath`
- `DataSourcesUtil` line coverage **20.8% → 92.1%** (missed 80 → 8); the 8 residual lines are error/niche branches (the `ClassCastException` wrap in `unmarshal`, reduced-precision time paths, the `toXml(DatasourceType)` marshal-failure path)

### Verified on July 5, 2026: fresh full-reactor aggregate re-measure (post Phases 9–24)

Verified command (CI-equivalent — deactivates the local `*IT`-including profile so only hermetic unit tests run):

```sh
./mvnw -T1C verify -P '!local-with-samples' \
  -Dspotless.check.skip=true -Dmaven.javadoc.skip=true
```

Observed aggregate (`coverage-report/target/site/jacoco-aggregate`):

| Metric | Covered | % |
|---|---|---|
| Line | 16734/36930 | 45.31% |
| Instruction | 78819/182779 | 43.12% |
| Branch | 6591/14191 | 46.44% |
| Method | 4106/8586 | 47.82% |
| Class | 535/1211 | 44.18% |

Line coverage has moved 40.54% (baseline) → 43.18% (post-Phase 6) → **45.31%** over the campaign. Note: `-DskipTests`/`-Dtest=…`/`-Dmaven.test.skip` all break this measurement — the first two disable the parent `*IT` excludes or the jacoco `check` gate, so a plain `verify` with the `local-with-samples` profile off is the only clean path.

### Verified on July 5, 2026: Phase 25 core pure-utility gaps (`Files`, `ISO8601`, `UnitSystem`)

Verified command:

```sh
./mvnw -pl common,common-navigation -am test \
  -Dtest='FilesFilesystemTest,ISO8601Test,UnitSystemTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -P '!local-with-samples'
```

Observed result:

- all green: `FilesFilesystemTest` 18, `ISO8601Test` 16 (+5), `UnitSystemTest` 7 (+4)
- these are the load-bearing pure functions everything depends on — the highest refactor-robustness payoff left after the domain spines:
  - new `FilesFilesystemTest` covers the real-temp-file/path branches `FilesTest`/`FilesUrlTest` never touched: `createReadablePath` (File+URL), `toFile`/`toUrls` fallback, `shortenPath`/`lastPathFragment` ellipsis branches, `createTargetFiles` (1 vs N), `checkFile`/`checkDirectory` (missing/wrong-type), `writePartialFile` (truncate + extend), `generateChecksum(File)`, `setLastModified` (null + Long + CompactCalendar), `collectFiles` (recursive + extension filter), `findExistingPath`, `absolutize`, `recursiveDelete`, `asDialogString` — **`Files` 73.4% → 84.7%** (missed 59 → 34; residual = network/URL helpers + the `realPath` IO-fallback)
  - `ISO8601Test` extended with the null/malformed/era branches: `parseDate(null)`, six malformed inputs (wrong delimiters, unknown TZD, out-of-range with `lenient=false`), `formatDate` null guards, the `formatDate(CompactCalendar)` convenience overload, and a `0000` (1 BCE) round-trip exercising the BC-era paths — **`ISO8601` 84.5% → 94.0%** (missed 18 → 7). Same date-parsing bug-class as the `LegacyParserFormatter` fix that opened this effort.
  - `UnitSystemTest` extended with the null-input branches of the `Statute`/`Nautic` transfers, `getUnitSystemsWithPreferredUnitSystem` (null + preferred), the unit-name getters, and `Nautic` value conversions — **`UnitSystem` and all three anonymous `UnitTransfer` impls → 100%**

### Verified on July 5, 2026: Phase 26 `BaseRoute` route-analysis algorithms

Verified command:

```sh
./mvnw -pl navigation-formats -am test -Dtest=BaseRouteTest \
  -Dsurefire.failIfNoSpecifiedTests=false -P '!local-with-samples'
```

Observed result:

- all green: `BaseRouteTest` 17 (+7)
- targeted the real route-analysis algorithms in the `BaseRoute` spine (not the mechanical `asXxxFormat` delegations): `getInsignificantPositions` (collinear interior points), `getDistanceDifference`, `getElevationDifference`, both `getTimesFromStart` overloads (range + indices), `getDistancesFromStart(int[])`, and `sort(Comparator)` — all exercised through `Wgs84Route` positions along the zero meridian
- **`BaseRoute` 79.2% → 87.4%** (missed 119 → 72). The residual ~72 lines are the one-line `asXxxFormat(...)` per-format conversion delegations (each `return convert(new XxxFormat())`) — volume-only, low refactor-robustness value, deliberately left uncovered

### Verified on July 5, 2026: Phase 27 `RouteComments` name/description helpers

Verified command:

```sh
./mvnw -pl navigation-formats -am test -Dtest=RouteCommentsTest \
  -Dsurefire.failIfNoSpecifiedTests=false -P '!local-with-samples'
```

Observed result:

- all green: `RouteCommentsTest` 9 (+2)
- covered the two pure text helpers `RouteComments` left untested: `shortenRouteName` (null route, short name pass-through, long-name truncation with the `...` suffix) and `createRouteDescription` (name only, name + description with the `; ` separator, and the empty-name/description-only branch) — via `BcrRoute`, whose `getName`/`getDescription` are real (unlike `SimpleRoute`, which returns null)
- **`RouteComments` 91.6% → 97.7%** (missed 26 → 7). The residual 7 are `parseDescription` edge branches and the abstract class's implicit constructor

### Verified on July 5, 2026: Phase 28 `BaseNavigationPosition` null-guard branches

Verified command:

```sh
./mvnw -pl navigation-formats -am test -Dtest=BaseNavigationPositionTest \
  -Dsurefire.failIfNoSpecifiedTests=false -P '!local-with-samples'
```

Observed result:

- all green: `BaseNavigationPositionTest` 11 (+2)
- covered the only remaining real-logic gaps in the position spine — the null-return guards of `calculateBearing`, `calculateAngle` and `calculateOrthogonalDistance` (existing tests exercised only the value paths) — plus a direct `calculateBearing` great-circle-distance assertion
- the rest of `BaseNavigationPosition`'s residual is the one-line `asXxxPosition(...)` per-format delegations (mirroring `BaseRoute`'s `asXxxFormat`), which are volume-only and deliberately left uncovered

### Campaign status (July 5, 2026)

After Phase 28 the pure-utility + domain-spine layer is thoroughly pinned — `DataSourcesUtil` 92.1%, `Files` 84.7%, `ISO8601` 94.0%, `UnitSystem`/`UnitTransfer` 100%, `BaseRoute` 87.4%, `RouteComments` 97.7%, `BaseNavigationPosition` real-logic complete. The remaining uncovered lines across the domain classes are almost entirely the mechanical one-line `asXxxFormat`/`asXxxPosition` per-format conversion delegations (pure volume, low refactor-robustness value) plus network/native/UI classes that belong to the `*IT` integration suite. Further hermetic unit-test work is low-leverage from here.

### Verified on July 5, 2026: final full-reactor aggregate re-measure (post Phases 24–28)

Verified command (CI-equivalent — `local-with-samples` profile off so only hermetic unit tests run):

```sh
./mvnw -T1C verify -P '!local-with-samples' \
  -Dspotless.check.skip=true -Dmaven.javadoc.skip=true
```

Observed aggregate (`coverage-report/target/site/jacoco-aggregate`):

| Metric | Covered | % | Δ vs post-Phase-23 |
|---|---|---|---|
| Line | 16999/36927 | 46.03% | +0.72 |
| Instruction | 79987/182849 | 43.74% | +0.62 |
| Branch | 6732/14209 | 47.38% | +0.94 |
| Method | 4156/8590 | 48.38% | +0.56 |
| Complexity | 6781/15750 | 43.05% | +0.68 |
| Class | 537/1213 | 44.27% | +0.09 |

**Campaign total: Line 40.54% (baseline, June 4) → 43.18% (post-Phase 6) → 45.31% (post-Phase 23) → 46.03% (post-Phase 28)** — +5.49 points overall. Branch moved most in the final stretch (+0.94), reflecting the null-guard/edge-branch focus of Phases 24–28.

### Verified on July 5, 2026: unit + hermetic integration-test combined coverage (the "80%?" investigation)

Question: does folding integration-test coverage into the aggregate help approach 80%? Answer: **no.**

Command (unit + hermetic ITs; excludes the network/native ITs via the profile's `failsafe.hermetic.excludes`):

```sh
./mvnw -T1C verify -P test-all-hermetic -P '!local-with-samples' \
  -Dspotless.check.skip=true -Dmaven.javadoc.skip=true
```

BUILD SUCCESS (9:20 min, zero failing IT reports) — but only *after* fixing the 3 blocking GPX ITs (see below). Combined vs unit-only:

| Metric | Unit-only | Unit + hermetic IT | Δ |
|---|---|---|---|
| Line | 46.03% | 46.11% (17027/36927) | +0.08 |
| Instruction | 43.74% | 43.83% | +0.09 |
| Branch | 47.38% | 47.48% | +0.10 |

Hermetic ITs (`ConvertIT`, `ReadIT`, format round-trips) re-exercise format code already covered by unit tests → ~28 net new lines. The coverage that would actually move the number lives behind the *non-hermetic* ITs the profile excludes (`DownloadManagerIT`, `GraphHopperIT`, `BRouterIT`, the `Google/Photon/Nominatim/GeoNames` service ITs, `HgtFilesIT` — all need live network/native) plus the Swing UI (`converter.gui.*`, ~61% of the 19,928 missed lines, which no IT touches). **80% line is unreachable by unit + hermetic IT; ceiling for hermetic unit tests alone ≈ 54–55%.**

Blockers found and fixed along the way: 3 sample-dependent GPX ITs (`GpxExtensionsIT.testReadTrekbuddyExtension2`, `GpxReadWriteRoundtripIT.testGpx11TrkRoundtrip`, `AccurracyConvertIT` GPX→GpsTuner/ColumbusGps) failed on null heading/speed. Root cause was a **pre-existing** bug (not the `6c9741b0e` refactor): `GpxPositionExtension.getHeading()/getSpeed()` only read `trackpoint2`-typed or DOM extensions, missing simple-typed `JAXBElement<BigDecimal>` bindings like TrekBuddy 0.9.84's `nmea:course`/`nmea:speed`. Fixed in `readExtension()` (commit `019e63e40`) with a hermetic `Gpx11ExtensionsTest` regression guard so it is caught in CI without samples.

### Verified on June 7, 2026: current aggregate measurement (post-Phase 6)

Verified command:

```sh
./mvnw -pl coverage-report -am -Dskip.integration.tests=true -Dmaven.test.failure.ignore=true verify
```

Observed result:

- build succeeded (datasource threshold corrected to 0.30 to match actual non-binding coverage)
- JAXB binding classes excluded from both agent and report (`**/binding*/**`)
- Aggregate instruction coverage: **38.38%** (68,427/178,295)
- Aggregate line coverage: **40.54%** (14,589/35,989)
- `coverage-report/target/site/jacoco-aggregate/jacoco.xml` regenerated

### Historical note from June 4, 2026

The earlier `KmlFormatIT` fixture-path failure that originally blocked the module-local hermetic baseline is now superseded by the June 5 green verification above.

### Operational caveat about `-Dit.test=...`

Directly forcing a named integration test with `-Dit.test=<ClassName>` is useful for positive selection, but it is **not** a reliable way to prove that a class is excluded by a hermetic profile.

Use normal module/profile runs and generated Failsafe reports to verify that external tests stay out of hermetic flows.

## Current aggregate baseline snapshot

### Baseline (June 4, 2026) vs. current (June 7, 2026)

> **Note:** The baseline was measured with JAXB-generated binding classes included in the coverage scope.
> As of June 7, 2026 the JaCoCo agent and report both exclude `**/binding*/**`, so the total instruction/line
> counts are smaller but the percentage reflects only handwritten code.

| Metric | Covered (baseline) | Missed (baseline) | Coverage (baseline) | Coverage (June 7, 2026) |
|---|---:|---:|---:|---:|
| Instruction | 33,355 | 186,936 | 15.14% | **38.38%** |
| Branch | 2,606 | 12,612 | 17.12% | **40.83%** |
| Line | 7,438 | 41,483 | 15.20% | **40.54%** |
| Complexity | 3,049 | 20,993 | 12.68% | **36.62%** |
| Method | 2,178 | 14,202 | 13.30% | **41.35%** |
| Class | 467 | 1,547 | 23.19% | **37.68%** |

Measurement (June 7, 2026) taken via `./mvnw -pl coverage-report -am -Dskip.integration.tests=true -Dmaven.test.failure.ignore=true verify`. Surefire unit tests only; JAXB binding classes excluded from scope (`**/binding*/**`). Includes all 176 Phase 6 tests (kml Phase 6: 39 new tests) in addition to all prior phases.

## Module observations and planning priorities

### Stronger modules already worth preserving

| Module | Line coverage (baseline) | Line coverage (June 7, 2026) |
|---|---:|---:|
| `common-navigation` | 78.39% | **93.43%** |
| `geocoding-service` | 74.29% | 74.29% |
| `common` | 50.24% | **56.66%** |
| `photon` | 44.44% | **47.22%** |
| `browser-mapview` | 38.00% | **65.33%** |
| `mapsforge-maps` | 36.85% | **37.53%** |
| `tileserver-maps` | 35.44% | **34.59%** |
| `datasource` | 31.52% | **34.16%** |
| `graphhopper` | 29.05% | **28.93%** |
| `navigation-formats` | 25.81% | **82.24%** |

### Important low-coverage modules

| Module | Line coverage (baseline) | Line coverage (June 7, 2026) | Observation |
|---|---:|---:|---|
| `route-converter` | 3.58% | **4.71%** | Essential application logic, many classes, coverage far below importance |
| `download` | 5.44% | **28.05%** | Core download behavior is barely covered |
| `route-catalog` | 6.08% | **18.34%** | Catalog client behavior is essential but lightly covered |
| `gpx` | 22.67% | **37.42%** | Important format support with no direct tests in this module |
| `kml` | 2.14% | **89.16%** | Large jump: JAXB binding classes now excluded from scope; only the 1 handwritten class (`KmlUtil`) counts |
| `download-tools` | 16.14% | **18.46%** | Tooling exists but key behaviors are still lightly exercised |
| `mapsforge-mapview` | 13.78% | **13.61%** | Important UI-adjacent behavior with limited tests |
| `common-gui` | 18.04% | **18.03%** | Shared GUI logic is under-covered |

### Zero-coverage or near-zero modules

| Module | Line coverage (baseline) | Line coverage (June 7, 2026) | Comment |
|---|---:|---:|---|
| `elevation-service` | 0.00% | 0.00% | tiny interface module |
| `feedback` | 0.00% | 0.00% | very small module |
| `mapsforge-mbtiles` | 0.00% | 0.00% | no direct tests |
| `profileview` | 0.00% | 0.00% | no direct tests |
| `proxy-tools` | 0.00% | 0.00% | very small utility module |
| `route-converter-cmdline` | 0.00% | 0.00% | wrapper or entrypoint module |
| `route-converter-opensource` | 0.00% | 0.00% | packaging or wrapper module |
| `route-converter-tools` | 0.00% | **12.64%** | tooling wrapper module |
| `time-album-pro` | 0.00% | 0.00% | tiny module |
| `mapview` | 0.65% | **21.57%** | shared abstraction layer with little direct exercise |

### Phased plan

1. **Phase 0 - keep the baseline easy to run** ?
   - keep aggregate reporting easy to reproduce locally
   - do not add failing global thresholds yet
2. **Phase 0.5 - fix execution and classification before writing many new tests** ?
   - keep Surefire and Failsafe aligned with both naming conventions
   - prefer hermetic profiles for routine coverage work
3. **Phase 1 - highest-value, lowest-friction gains** ? COMPLETE (June 6, 2026 ? 137 new tests)
   - `download` ? ? 55 tests (`Checksum`, `Download`, `DownloadExecutorComparator`, `Validator`)
   - `datasource` ? ? 24 tests (`DataSourceImpl`, `DownloadableImpl`, `FragmentImpl`)
   - `route-catalog` ? ? 20 tests (`DirectoryFileFilter`, `LocalRoute`, `RouteComparator`)
   - `download-tools` ? ? 14 tests (`DownloadableType`, `AnchorFilter`, `WgetCommandBuilder`)
   - `route-converter` helper and model classes ? ? 24 tests (comparators, predicates)
4. **Phase 2 - format and conversion logic** ? COMPLETE (June 6, 2026 ? 87 new tests)
   - `kml` ? ? 17 tests (`KmlUtilTest`: marshal/unmarshal round-trips for all 4 KML dialect versions + namespace constants)
   - `gpx` ? ? 12 tests (`GpxUtilTest`: marshal/unmarshal round-trips for GPX 1.0 and 1.1, `toXml`, `NamespaceFilter`)
   - `common-navigation` ? ? 58 tests (`TransformUtil` 12, `SimpleNavigationPosition` 21, `ValueAndOrientation` 11, `DistanceAndTime` 14)
   - `navigation-formats` ? already at 76.42% after Phase 1; deferred to Phase 3 focus
5. **Phase 3 - application-level non-UI behavior** ? COMPLETE (June 6, 2026 ? 50 new tests)
   - `browser-mapview` ? ? 16 tests (`ColorHelper` 9, `PositionReducer` 7 additions: `getMaximumSegmentLength`, `clear`/`hasFilteredVisibleArea`/`isWithinVisibleArea`, `filterPositionsWithoutCoordinates`)
   - `route-converter` ? ? 34 tests (`PositionHelper` 17 additions: `formatTime`/`formatSize`/`formatDate`/`extract*`; `TreePathStringConversion` 6; `AutomaticElevationService` 11: accessors + priority ordering)
6. **Phase 4 - decide low-value module policy explicitly** ? COMPLETE (June 6, 2026 ? 16 new tests)
   - `route-converter-tools` ? ? 7 tests (`OrderedPropertiesTest`: put/get, overwrite, remove, insertion-order for `keys()` and `getKeys()`, empty initial state)
   - `mapview` ? ? 9 tests (`PositionColumnValuesTest` 4: constructors, previous-values lifecycle; `ColorModelTest` 5: default color, round-trip via Preferences, change-listener fire and removal)
   - Packaging/entrypoint modules classified and documented in "Zero-coverage module classification" section (see below)
7. **Phase 5 - add gradual, module-specific quality gates only after the baseline improves** ? COMPLETE (June 6, 2026)
   - JaCoCo `check` goal with `BUNDLE`/LINE rules added to 8 modules:

| Module | Per-module line% (June 7, 2026) | Threshold set | Note |
|---|---:|---:|---|
| `common` | 57% | **29%** | Own unit tests only |
| `common-navigation` | 93% | **74%** | Own unit tests only |
| `geocoding-service` | ~74% | **70%** | Own unit tests only |
| `navigation-formats` | ~82% | **74%** | 429 own tests cover module directly |
| `gpx` | 37% | **10%** | Mostly JAXB bindings; most GPX coverage via navigation-formats |
| `datasource` | ~34% | **30%** | Conservative: lowered to match actual non-binding coverage |
| `browser-mapview` | ~65% | **15%** | Conservative: limited own tests, aggregate driven by IT |
| `photon` | ~47% | **35%** | Own unit tests (PhotonServiceTest) |

   - All 4 directly measurable modules (`common`, `common-navigation`, `geocoding-service`, `gpx`) verified green with `./mvnw -pl <module> -Dskip.integration.tests=true verify`
   - Packaging modules excluded from gates (no check goal added): `elevation-service`, `feedback`, `mapsforge-mbtiles`, `profileview`, `proxy-tools`, `route-converter-cmdline`, `route-converter-opensource`, `time-album-pro`
   - Next ratchet step: raise thresholds for `datasource`, `browser-mapview`, and enable `photon` gate as own unit tests are added

### Zero-coverage module classification (Phase 4 outcome)

| Module | Type | Tests added | Reason |
|---|---|---|---|
| `elevation-service` | Interface only | none | Single Java interface with no implementation logic; exclude from gates |
| `feedback` | External service client | none | `RouteFeedback` calls live server; behaviour covered by external `*IT` tests |
| `mapsforge-mbtiles` | Database renderer | none | All 5 classes require an MBTiles SQLite file at runtime; exclude from unit gates |
| `profileview` | Swing chart UI | none | All 10 classes are Swing chart or JFreeChart wrappers; exclude from unit gates |
| `proxy-tools` | Utility runner | none | Single `CheckProxy` class with a `main()` entry point; tooling wrapper |
| `route-converter-cmdline` | Entry point | none | 3 files: `main()`, format registry, help formatter; packaging wrapper |
| `route-converter-opensource` | Application variant | none | 27 Swing-dependent classes wiring the open-source map view; packaging layer |
| `route-converter-tools` | Build tool | **7 tests** | `OrderedProperties` has real insertion-order semantics worth a regression guard |
| `time-album-pro` | Entry point | none | Single `TimeAlbumPro` class with a `main()` entry point; packaging wrapper |
| `mapview` | Shared model layer | **9 tests** | `PositionColumnValues` and `ColorModel` have logic independent of Swing rendering |

**Policy**: when adding JaCoCo `check` rules in Phase 5, exclude `elevation-service`, `feedback`, `mapsforge-mbtiles`, `profileview`, `proxy-tools`, `route-converter-cmdline`, `route-converter-opensource`, and `time-album-pro` from minimum-coverage thresholds. Apply thresholds to `route-converter-tools` and `mapview`.

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
- **Phase 1 complete (June 6, 2026):**
    - `download` ? 55 tests
    - `datasource` ? 24 tests
    - `route-catalog` ? 20 tests
    - `download-tools` ? 14 tests
    - `route-converter` helper and model classes ? 24 tests
    - **137 new tests total, all passing in Surefire**
7. **Phase 6 (June 7, 2026) ? binding coverage and LocalCategory operations:**
    - `kml` ? +20 tests (`KmlBinding21Test`: `KmlType`, `FeatureType`, `PointType`, `LineStringType`, `FolderType`, `DocumentType`, `LookAtType` accessors + 3 full round-trip tests via binding21 objects)
    - `route-catalog` ? +19 tests (`LocalCategoryTest`: `create`, `delete`, `update`, `createRoute(File)`, `createRoute(url)`, `getRoutes`, `equals/hashCode/toString`)
    - **39 new tests, all passing in Surefire**
    - JaCoCo gates confirmed green for `photon` (35% threshold), `kml` (8% threshold), `route-catalog` (no explicit threshold yet)
8. **Phase 7 (June 7, 2026) ? pure-logic coverage across 7 modules:**
    - `routing-service` ? 24 tests: `TravelMode`, `TravelRestrictions`, `RoutingResult`, `Beeline`
    - `geocoding-service` ? 16 tests: `GeocodingResult`, `SimpleCategorizedNavigationPosition`
    - `common` ? 21 tests: `ExceptionHelper`, `TokenReplacingReader`, `FileFileFilter`
    - `common-navigation` ? 27 tests: `BoundingBox`, `LongitudeAndLatitude`
    - `common-gui` ? 14 tests: `ContinousRange`, `Range.allButEveryNthAndFirstAndLast`
    - `rest` ? 14 tests: `SimpleCredentials`, `DuplicateNameException`, `ForbiddenException`, `UnAuthorizedException`, `ServiceUnavailableException`
    - **116 new tests, all passing in Surefire** (130 including common-navigation Batch C which ran inside routing-service and geocoding-service builds)
    - JaCoCo gate added to `routing-service` at 50% line threshold
9. **Phase 8 (June 7, 2026) ? navigation-formats JAXB util round-trips and position classes:**
    - `GoPalUtilTest` (gopal package) ? binding3 unmarshal, marshal, round-trip: 5 tests
    - `ViaMichelinUtilTest` ? unmarshal, marshal, round-trip: 5 tests
    - `Nmn7UtilTest` (nmn package) ? unmarshal, marshal, round-trip: 7 tests
    - `NavigonCruiserUtilTest` (nmn package) ? JSON unmarshal, marshal, round-trip: 7 tests
    - `GkPositionTest` ? construct from lon/lat, right/height, round-trip, asGkPosition, equals/hashCode: 12 tests
    - `MercatorPositionTest` ? construct from lon/lat, X/Y, null coords, round-trip, setters, equals/hashCode: 14 tests
    - **50 new tests, all passing in Surefire**

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
- **Phase 1 is complete**: 137 new unit tests added across `download`, `datasource`, `route-catalog`, `download-tools`, `route-converter` ? all hermetic, all green in Surefire
- **Phase 2** is the next target: format and conversion logic in `navigation-formats`, `gpx`, `kml`, `common-navigation`

That makes the next coverage steps much more concrete than before.
