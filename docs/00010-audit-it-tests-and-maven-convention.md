# 00010 - Audit `*IT` tests and align Maven convention

## Summary

The repository contains a substantial existing `*IT.java` suite, but the root `pom.xml` previously routed only `*IntegrationTest.java` through Failsafe.

This has now been aligned so that:

- Surefire excludes both `*IT.java` and `*IntegrationTest.java`
- Failsafe includes both `*IT.java` and `*IntegrationTest.java`

This preserves the current default behavior that integration tests remain opt-in, because `skip.integration.tests` is still `true` unless a dedicated profile enables them.

## Why the change was needed

The repository already contains `61` `*IT.java` classes across multiple modules.

Without the Maven convention update, these tests were easy to miss in the intended integration-test flow.

That created two problems:

1. already-written integration-style tests did not reliably contribute to coverage when integration tests were enabled
2. naming and execution policy drifted apart, which made coverage planning harder to trust

## Maven change made

The root `pom.xml` now treats integration-test naming explicitly:

- unit-test phase (`maven-surefire-plugin`)
  - excludes `**/*IT.java`
  - excludes `**/*IntegrationTest.java`
- integration-test phase (`maven-failsafe-plugin`)
  - includes `**/*IT.java`
  - includes `**/*IntegrationTest.java`

## Current `*IT` inventory by module

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

## Classification rule used in this audit

Tests are classified as:

- **Hermetic**
  - uses temporary files, sample data, or in-repo fixtures only
  - deterministic enough for regular integration-style coverage runs
- **External**
  - depends on live HTTP services, externally hosted test files, credentials, or mutable remote state
  - should remain opt-in

## Classification results

### Clearly hermetic `*IT` tests

| Module | Tests | Classification | Evidence |
|---|---|---|---|
| `download` | `QueuePersisterIT` | Hermetic | Uses only temporary files and queue XML round-trips |
| `route-catalog` | `LocalCategoryIT`, `TempFileIT` | Hermetic | Uses temporary directories or temp-file naming logic only |
| `navigation-formats` | all current `*IT.java` files reviewed in this module | Hermetic | Representative tests such as `ReadWriteRoundtripIT` and `NavigationFormatParserIT` operate on `TEST_PATH` / sample files, not live services |

### Clearly external `*IT` tests

| Module | Tests | Classification | Evidence |
|---|---|---|---|
| `download` | `DownloadManagerIT` | External | Defaults to `https://static.routeconverter.com/test/` and includes live download checks |
| `download-tools` | `SnapshotCatalogIT` | External | Defaults to `https://api.routeconverter.com/` |
| `route-catalog` | `RemoteCategoryIT`, `RemoteFileIT`, `RemoteRouteIT` | External | Extend `BaseRemoteCatalogTest`, which talks to a live API and mutates remote state |
| `feedback` | `RouteFeedbackIT`, `SendChecksumsIT` | External | Use real service endpoints, credentials, and remote state |
| `geonames` | `GeoNamesServiceIT` | External | Exercises the live GeoNames service |
| `googlemaps` | `GoogleServiceIT` | External | Exercises live Google service behavior |
| `nominatim` | `NominatimServiceIT` | External | Exercises live Nominatim responses |
| `photon` | `PhotonServiceIT` | External | Exercises live Photon responses |
| `hgt` | `HgtFilesIT` | External | Uses live remote elevation datasets |
| `graphhopper` | `GraphHopperIT` | External | May require downloading external OSM routing data |
| `brouter` | `BRouterIT` | External | Depends on externally sourced routing segment data when not already present |

### Mixed modules

| Module | Mix | Recommendation |
|---|---|---|
| `download` | 1 hermetic, 1 external | Good candidate for enabling only the hermetic test in regular coverage work |
| `route-catalog` | 2 hermetic, 3 external | Separate local/temp-file tests from remote API tests in future coverage profiles |

## Recommendation

### Immediate recommendation

Keep the current default behavior:

- unit tests run by default
- integration tests remain opt-in via Maven profile

This is still appropriate because many `*IT.java` tests are external.

### Short-term recommendation

Use the new Maven naming alignment to unlock hermetic `*IT` tests deliberately during coverage work, but do **not** assume that all `*IT` tests belong in routine coverage runs.

### Preferred next refinement

Introduce two explicit integration-test scopes in a follow-up change:

1. **hermetic integration tests**
   - safe for routine coverage runs
2. **external integration tests**
   - opt-in only

That split would let the repository benefit from existing `*IT` coverage without making normal coverage builds flaky or dependent on live services.

## Coverage-planning impact

This audit changes the recommended execution order for coverage improvement:

1. first, recover hermetic `*IT` tests that already exist
2. then add new tests in `download`, `datasource`, and `route-catalog`
3. keep external service tests out of normal coverage baselines unless intentionally requested

## Verified consequence of the Maven change

With the root `pom.xml` update, a command such as the following can now target `*IT.java` tests through Failsafe as intended:

```sh
source ~/.sdkman/bin/sdkman-init.sh && sdk use java 17.0.19-tem
./mvnw -U -pl download -am -P integration-test -Dit.test=QueuePersisterIT -Dfailsafe.failIfNoSpecifiedTests=false verify
```

The extra `-Dfailsafe.failIfNoSpecifiedTests=false` is helpful when using `-Dit.test=...` in a reactor build, because aggregator or dependency modules may legitimately have no matching tests.

This does not mean all `*IT.java` tests should run in normal coverage builds. It only means the naming convention is now aligned with the build so intentional execution works reliably.

