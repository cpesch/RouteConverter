# 00008 - Review code coverage tooling and baseline

## Summary

JaCoCo is still the best primary coverage tool for this repository.

Reasons:

- It is the de-facto standard for Java/Maven projects and works well with Java 17.
- The repository already uses `org.jacoco:jacoco-maven-plugin` `0.8.14`, which is current enough for the present Java baseline.
- It already supports the existing multi-module aggregation flow through `coverage-report`.
- It produces both HTML and XML output, which keeps the project compatible with IDE review and future CI/reporting integrations.
- Replacing it would create migration work without solving the real bottleneck here, which is missing test coverage in key modules rather than missing tooling features.

## Recommendation

Keep JaCoCo as the main coverage engine.

Use it together with:

- the existing aggregate report in `coverage-report`
- module-focused unit test additions in the most important low-coverage areas
- later, optional Maven quality gates once the baseline is high enough to avoid noisy failures

## Current repository setup

### Root build

The root `pom.xml` already:

- configures JaCoCo `prepare-agent` for unit tests
- configures a second JaCoCo agent property for integration tests via Failsafe
- excludes `*IntegrationTest.java` from Surefire
- runs `*IntegrationTest.java` through Failsafe

### Aggregate coverage module

`coverage-report/pom.xml` already:

- depends on the modules that should be analyzed together
- runs `jacoco:report-aggregate`
- merges module execution data into `coverage-report/target/aggregate.exec`

This means the repository already has a valid coverage architecture; the main work now is to raise meaningful coverage, not to replace the toolchain.

## Verified command

The current aggregate coverage flow was verified with Java 17 from sdkman as requested in `AGENTS.md`:

```sh
source ~/.sdkman/bin/sdkman-init.sh && sdk use java 17.0.19-tem
./mvnw -U -pl coverage-report -am -Dskip.integration.tests=true verify
```

Verified outputs:

- `coverage-report/target/aggregate.exec`
- `coverage-report/target/site/jacoco-aggregate/index.html`
- `coverage-report/target/site/jacoco-aggregate/jacoco.xml`

## Current aggregate baseline

Baseline taken from `coverage-report/target/site/jacoco-aggregate/jacoco.xml` after the verified build:

| Metric | Covered | Missed | Coverage |
|---|---:|---:|---:|
| Instruction | 33,355 | 186,936 | 15.14% |
| Branch | 2,606 | 12,612 | 17.12% |
| Line | 7,438 | 41,483 | 15.20% |
| Complexity | 3,049 | 20,993 | 12.68% |
| Method | 2,178 | 14,202 | 13.30% |
| Class | 467 | 1,547 | 23.19% |

## Module observations

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

These are the modules where incremental work is likely to pay off quickly because test structure already exists.

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
| `route-converter-cmdline` | 0.00% | wrapper/entrypoint module |
| `route-converter-opensource` | 0.00% | packaging/open-source wrapper module |
| `route-converter-tools` | 0.00% | tooling wrapper module |
| `time-album-pro` | 0.00% | tiny module |
| `mapview` | 0.65% | shared abstraction layer with little direct exercise |

These should not all be treated equally. Some are good candidates for explicit exclusion from future gates rather than immediate test investment.

## Test distribution snapshot

A quick source count shows the main imbalance is not the absence of all tests, but concentration of tests in only a few modules.

Examples:

- `navigation-formats`: 279 main Java files, 120 test Java files
- `route-converter`: 241 main Java files, 11 test Java files
- `kml`: 398 main Java files, 0 test Java files
- `gpx`: 71 main Java files, 0 test Java files
- `download`: 21 main Java files, 2 test Java files
- `datasource`: 19 main Java files, 2 test Java files

## Important current-state caveats

### 1. The aggregate report works, but not every root module is equally represented

The build verified the aggregate reporting path, but coverage decisions should distinguish between:

- core logic modules
- shared service/interface modules
- packaging or launcher modules

Those groups should not all share the same future threshold.

During the verified run, `download-gui` was not part of the `coverage-report` reactor path. If `download-gui` becomes part of the "essential parts" scope later, first add it deliberately to the aggregate coverage flow instead of assuming it is already counted.

### 2. Generated code can distort the picture

Some modules include JAXB-generated sources or format/binding-heavy code. Before enforcing thresholds, decide whether generated code should:

- remain counted, because it is shipped and exercised through real flows, or
- be excluded from quality gates while still staying visible in the HTML report

### 3. GUI-heavy modules need targeted tests, not brute-force UI automation first

Modules such as `route-converter`, `common-gui`, and `mapsforge-mapview` are unlikely to improve efficiently through end-to-end UI automation alone. Better returns will come from testing models, helpers, mediators, and extracted non-UI logic.

### 4. Existing `*IT.java` tests were under-counted by the earlier Maven convention

At the time this baseline was first reviewed, the root `pom.xml` excluded `**/*IntegrationTest.java` from Surefire and included only `**/*IntegrationTest.java` in Failsafe.

However, the repository already contains at least `61` test classes named `*IT.java`, including:

- `navigation-formats`: 44
- `route-catalog`: 5
- `download`: 2
- plus additional `*IT.java` tests in `brouter`, `download-tools`, `feedback`, `geonames`, `googlemaps`, `graphhopper`, `hgt`, `nominatim`, and `photon`

This mattered because the baseline likely mixed two situations:

- some useful integration-style coverage is simply not being executed because the naming convention does not match the Maven include pattern
- other `*IT.java` tests are live-service or network-dependent and should probably remain opt-in instead of becoming mandatory coverage inputs

Examples observed during review:

- `download/src/test/java/.../QueuePersisterIT.java` is hermetic and likely a good candidate to execute regularly
- `route-catalog/src/test/java/.../RemoteCategoryIT.java` talks to the live RouteConverter API and should remain opt-in unless a hermetic test seam is introduced
- `download/src/test/java/.../DownloadManagerIT.java` uses `https://static.routeconverter.com/test/` by default, so it should not be treated the same as fully local tests

The Maven naming mismatch has since been aligned so that Failsafe recognizes both `*IT.java` and `*IntegrationTest.java` while Surefire excludes both patterns.

The remaining task is now narrower: decide which `*IT.java` tests should:

- run in normal coverage builds
- run only in an opt-in external integration profile
- or be renamed/reclassified for clarity

## Conclusion

JaCoCo remains the right tool.

The repository already has a functioning coverage pipeline; the real opportunity is to improve coverage selectively in high-value modules, starting with core logic and service layers before adding any hard coverage gates.

