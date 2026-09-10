# Flatpak manifest (spec 00059 P4)

Not yet submitted to Flathub. Staged here, build- and run-verified.

## What's here

- `com.routeconverter.RouteConverter.yml` — the manifest. Downloads the
  already-built, signed `RouteConverterLinux.jar` from the pinned
  `previous-releases/<version>/` URL (rc-meta#146) rather than compiling
  from source in the sandbox — Maven has no network access inside a
  Flatpak build, and vendoring the full dependency tree via a generated
  sources list is a separate, larger effort.
- `routeconverter.sh` — launcher wrapper, invokes the jlinked `/app/jre`
  built at build time from the `org.freedesktop.Sdk.Extension.openjdk21`
  SDK extension, using the same module set as
  `scripts/jre-modules.txt` (the single source of truth the Windows/Mac
  bundled JREs also use).
- `com.routeconverter.RouteConverter.desktop`, `.metainfo.xml`,
  `RouteConverter.png` (256px, extracted from the macOS `.icns` — no
  Linux icon asset existed before).

## Verified

Built and run in a local Ubuntu 26.04 VM with `flatpak-builder`:
`appstreamcli validate` clean (one pedantic-only hint, harmless: CamelCase
app IDs like this are common on Flathub, e.g. org.gnome.Nautilus).
Installed and launched under Xvfb inside the real Flatpak sandbox
(`bwrap`) — log shows `Frame shown 2124 ms after startup`, running on
`/app/jre` (bundled JRE 21.0.12.1), not a host Java install.

Rebuild/test recipe:
```
flatpak install --user flathub org.freedesktop.Platform//26.08 \
  org.freedesktop.Sdk//26.08 org.freedesktop.Sdk.Extension.openjdk21//26.08
cd flatpak
flatpak-builder --user --install --force-clean build-dir \
  com.routeconverter.RouteConverter.yml
flatpak run com.routeconverter.RouteConverter
```

## Before submitting to Flathub

1. At least one real screenshot, hosted at an https URL, added to the
   `<screenshots>` block in the metainfo.
2. Domain ownership verification for `routeconverter.com` — Flathub's
   review checks this automatically once the submission PR is open;
   needs prod web access to place whatever file/record it asks for.
3. `sha256` in the manifest is pinned to release 3.6 — bump alongside
   future releases (not yet automated; the winget/Homebrew bump jobs
   are the pattern to follow here).
4. Submission itself: PR to https://github.com/flathub/flathub proposing
   the new app (manual maintainer review, similar cadence to the winget
   moderator queue).
