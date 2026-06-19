# RouteConverter for macOS bundled with JRE 17

The stripped Mac JRE is built in CI by
[`.github/workflows/build-mac-jre.yml`](../.github/workflows/build-mac-jre.yml)
for both `x64` (`macos-13`) and `aarch64` (`macos-latest`) and published to:

    https://static.routeconverter.com/build/mac/jre-<version>-<arch>-stripped.zip
    https://static.routeconverter.com/build/mac/jre-<version>-<arch>-stripped.zip.sha256

`<version>` is the `<jre.version>` property from
[`route-converter-build/pom.xml`](../route-converter-build/pom.xml).

`_build-linux-mac.yml` downloads + sha256-verifies both zips before
`mvn package` and unzips them into `target/Runtime-x64` and
`target/Runtime-aarch64`. `maven-assembly-plugin` then produces two
arch-specific app bundles:

- `target/RouteConverterMacOpenSource-x64-app.zip`
- `target/RouteConverterMacOpenSource-aarch64-app.zip`

To rebuild the hosted JRE (e.g. after bumping `<jre.version>`):
trigger `Build Mac JRE` via `workflow_dispatch`, or push a change to
`route-converter-build/pom.xml` on `master`.

Phase 5.4e/f in [rc-meta/specs/00018-release-automation-3.4.md](
https://github.com/cpesch/rc-meta/blob/main/specs/00018-release-automation-3.4.md).
