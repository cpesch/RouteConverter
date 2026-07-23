# mac-launcher

`JavaAppLauncher.m` — the native macOS launcher stub used as
`Contents/MacOS/<App>` in the RouteConverter and TimeAlbumPro `.app` bundles.

On a cold "Open with" macOS delivers the file via a `kAEOpenDocuments` (odoc)
Apple Event, **not** as `argv`. This stub is an `NSApplication` delegate that
catches the launch odoc in `application:openFiles:` (delivered before
`applicationDidFinishLaunching:`), then `execv`s the bundled JRE with the
file(s) appended as `argv` — the path RouteConverter already handles
(`BaseRouteConverter.parseInitialArgs`). After exec, `java` is the app process,
so a subsequent *warm* "Open with" is handled by its own `Desktop`
open-file handler. Fixes forum #4139 (issue #206 / PR #208).

It is a universal (`x86_64` + `arm64`) binary and is byte-identical for both
products — it resolves its jar, icon, and dock name from its own bundle at
runtime, so one build serves both.

## How it is built and installed

- **CI (release/prerelease):** compiled on the **macOS** runner in
  `build-mac-jre.yml` and hosted at
  `https://static.routeconverter.com/build/mac/macos-launcher-stub` (+ `.sha256`).
  The **Linux** assembly runner (`_build-linux-mac.yml`) fetches it into
  `RouteConverterMac/target/RouteConverter` and
  `TimeAlbumProMac/target/TimeAlbumPro`, then the `mac-app` Maven profile
  (`-DmacApp`) assembles the `.app` zips.
- **Local macOS build:** the `mac-native-launcher` profile (auto-active on
  `os.family=mac`) compiles the stub with `clang`; pass `-DmacApp` to also
  assemble the `.app`.

## Checklist: is a hand-rolled Maven/CI build change runner-safe?

The Mac `.app` is **assembled on a Linux runner**, and different workflows run
different profiles. Before merging a change to a Mac `*/pom.xml` or a build
workflow, confirm all three axes — each has burned us:

1. **Profile activation × runner OS.** `os.family=mac` profiles do **not**
   activate on the Linux assembly runner; a step gated that way silently never
   runs there. Anything that must run during the release assembly belongs in the
   main `<build>` or a property-gated profile (`-DmacApp`), **not** an `os=mac`
   gate. `clang` is the only thing that must stay `os=mac` (it can't run on Linux).
2. **Which workflow runs it.** The generic `mvn verify` PR matrix (Java
   21/25/Windows) does **not** fetch the hosted stub and does **not** pass
   `-DmacApp` — so a release-only step failing there would be a false alarm, and
   a release-only step *missing* passes PR CI while breaking the release build.
   Keep `.app` assembly behind `-DmacApp` so PR CI only builds the shaded jar.
3. **Phase.** `RouteConverterMac`/`TimeAlbumProMac` have no compilable sources,
   so `target/` does not exist until `package`; a step writing under `target/`
   must create the dir itself (`mkdir -p`) rather than assume an earlier phase made it.

**Verify against the runner, not a local partial run.** `mvn prepare-package` on
a Mac does not exercise the Linux `package`/assembly path. Simulate it:

```sh
# CI mvn-verify path (must succeed, must NOT produce a .app zip):
mvn -pl RouteConverterMac -am -Dmaven.test.skip=true -P '!mac-native-launcher' package

# release path (must produce both -x64-app.zip and -aarch64-app.zip):
#   seed target/RouteConverter (the stub) + target/Runtime-{x64,aarch64} first, then:
mvn -pl RouteConverterMac -am -Dmaven.test.skip=true -P '!mac-native-launcher' -DmacApp package
```
