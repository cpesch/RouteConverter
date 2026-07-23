# RouteConverter

[![Test Coverage](https://codecov.io/gh/cpesch/RouteConverter/branch/master/graph/badge.svg)](https://codecov.io/gh/cpesch/RouteConverter)
[![Translation status](https://hosted.weblate.org/widgets/routeconverter/-/svg-badge.svg)](https://hosted.weblate.org/engage/routeconverter/)
[![License: GPL v2](https://img.shields.io/badge/license-GPL--2.0-blue.svg)](LICENSE-GPL.txt)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)

**RouteConverter** is a popular, free open-source tool to **display, edit, enrich and
convert** GPS routes, tracks and waypoints across **80+ formats** (GPX, KML, NMEA,
TomTom, Garmin, and many more). It's a cross-platform Java/Swing desktop app with a
map view, plus a command-line tool.

🌐 **[routeconverter.com](https://www.routeconverter.com/)** — features, screenshots,
supported formats, FAQ, and downloads.

## Download

- **Stable releases:** https://releases.routeconverter.com/latest/
- **Prereleases (frequent):** https://releases.routeconverter.com/prerelease/

The **Windows** (`.exe`) and **macOS** (`.app`) downloads bundle a Java runtime —
nothing else to install. The **Linux** build ships as a runnable `.jar` that needs
**Java 21 or later** installed on your system.

## Build & run from source

You need **JDK 21** (e.g. from [Adoptium](https://adoptium.net/)). Maven comes
bundled via the wrapper — no separate install.

```sh
git clone git@github.com:cpesch/RouteConverter.git
cd RouteConverter

./mvnw clean package          # full build + tests

# run the desktop app (build that module first):
./mvnw -pl RouteConverterLinux -am package
java -jar RouteConverterLinux/target/RouteConverterLinux.jar

# …or the command-line converter:
java -jar RouteConverterCmdLine/target/RouteConverterCmdLine.jar
```

> On macOS/Linux, if `java` isn't on your `PATH`, activate a JDK 21 first
> (e.g. via [sdkman](https://sdkman.io/): `sdk use java 21`).

CI builds and tests on **Java 21 and 25** plus a Windows smoke build.

## Contributing

Contributions are very welcome — bug fixes, new formats, UI improvements,
translations. 🎉 See [`CONTRIBUTING.md`](CONTRIBUTING.md) for how to report
bugs, open pull requests, and translate the app. Contributors are listed in
[`CONTRIBUTORS.txt`](CONTRIBUTORS.txt).

### IDE setup (IntelliJ IDEA)

1. **File → Open…** the root `pom.xml`.
2. **Settings → Editor → GUI Designer:** choose *"Generate GUI into: Java source
   code"* and disable *"Automatically copy form runtime classes…"*. Layout is
   edited in the `.form` files, not the generated `$$$setupUI$$$` blocks.

Eclipse (m2e) and NetBeans work too — import the root `pom.xml` as a Maven project.

## Code signing

The Windows installers and the standalone Java artifacts are code-signed
(Authenticode / jar signature) so Windows SmartScreen and Defender recognise
them. Free code signing for open-source projects is provided by
[SignPath.org](https://about.signpath.io/), with a certificate issued by the
[SignPath Foundation](https://signpath.org/). The certificate is held in
SignPath's HSM and never leaves it — CI holds only a submitter token that can
request a signature, not extract the key.

## License

RouteConverter is licensed under the **GNU General Public License v2** — see
[`LICENSE-GPL.txt`](LICENSE-GPL.txt). By contributing, you agree your code ships
under the GPL.

Have fun! — Christian Pesch ([@cpesch](https://github.com/cpesch))
