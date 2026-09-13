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

## The deployment target is not optional

Every `clang` invocation that builds the stub **must** pass
`-mmacosx-version-min=11.0`. Without it clang stamps `LC_BUILD_VERSION.minos`
with the host SDK version, so the minimum macOS the app supports silently
follows whatever the build machine happens to be. macOS refuses to `exec` a
Mach-O whose `minos` is higher than the running OS, and the Finder shows such a
bundle with a prohibition badge on a greyed icon — no dialog, no log, no hint.

That is exactly what shipped: once the `macos-latest` runner image moved to
macOS 26, the hosted stub carried `minos 26.0`, and RouteConverter 3.6 through
3.6.5 could not be started by anyone still on macOS 15 or earlier (#393,
support report 1348).

`11.0` is the floor for a reason — it is what the bundled Adoptium JRE itself
declares (`Contents/bin/java` and `lib/server/libjvm.dylib` are `minos 11.0`),
and `arm64` has no lower target. `LSMinimumSystemVersion` in both
`src/main/app-resources/Info.plist` files says the same, and the three clang
sites (`build-mac-jre.yml`, `RouteConverterMac/pom.xml`,
`TimeAlbumProMac/pom.xml`) must stay in step.

Two gates enforce it, because the runner image will move again:

- `build-mac-jre.yml` checks the freshly compiled stub with `vtool
  -show-build-version` per slice, before it is hashed and hosted.
- the `.app` smoke-check in `_build-linux-mac.yml` checks the stub inside each
  assembled zip with `scripts/check-macho-minos.py` — a stdlib-only Mach-O
  parser, because the assembly runner is Linux and has neither `vtool` nor
  `otool`. `scripts/test-check-macho-minos.sh` exercises that parser on macOS
  and skips elsewhere.

Note that *building* at a low target does not by itself prove the app runs on
that macOS; it only removes the kernel-level refusal. Verify a real launch on
the oldest supported macOS before a release that touches the stub.

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
