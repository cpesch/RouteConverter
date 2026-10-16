# Flatpak manifest

Not yet submitted to Flathub. Staged here, build- and run-verified.

## What's here

- `com.routeconverter.RouteConverter.yml` — the manifest. Downloads the
  already-built, signed `RouteConverterLinux.jar` from the pinned
  `previous-releases/<version>/` URL rather than compiling
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

On a GNOME 50 Wayland desktop (Ubuntu 26.04, aarch64, 2026-10-16):

- `--socket=x11`, not `fallback-x11` + `wayland`: AWT has no Wayland
  backend and runs through Xwayland; with `fallback-x11` next to `wayland`
  the sandbox gets no X11 and the app dies with `HeadlessException: No X11
  DISPLAY variable was set`.
- `$HOME` is a tmpfs inside the sandbox, so `routeconverter.sh` points the
  Java preferences at `$XDG_CONFIG_HOME` and the application directory
  (maps, routes, downloads) at `$XDG_DATA_HOME/routeconverter`; a restart
  keeps the settings and answers every catalog request with 304.
- Dark mode follows the desktop through the settings portal. The sandbox
  has no `xrdb`, so Java2D stays at 100 % on a scaled desktop: FlatLaf
  scales the UI and the map follows it, the toolbar icons stay small.

Rebuild/test recipe:
```
flatpak install --user flathub org.freedesktop.Platform//26.08 \
  org.freedesktop.Sdk//26.08 org.freedesktop.Sdk.Extension.openjdk21//26.08
cd flatpak
flatpak-builder --user --install --force-clean build-dir \
  com.routeconverter.RouteConverter.yml
flatpak run com.routeconverter.RouteConverter
```
