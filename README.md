# RouteConverter

[![Test Coverage](https://codecov.io/gh/cpesch/RouteConverter/branch/master/graph/badge.svg)](https://codecov.io/gh/cpesch/RouteConverter)
[![Translation status](https://hosted.weblate.org/widgets/routeconverter/-/svg-badge.svg)](https://hosted.weblate.org/engage/routeconverter/)
[![License: GPL v2](https://img.shields.io/badge/license-GPL--2.0-blue.svg)](LICENSE-GPL.txt)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)

**RouteConverter** is a popular, free open-source tool to **display, edit, enrich and
convert** GPS routes, tracks and waypoints across **80+ formats** (GPX, KML, NMEA,
TomTom, Garmin, and many more). It's a cross-platform Java/Swing desktop app with a
map view, plus a command-line tool.

🌐 **[routeconverter.com](https://www.routeconverter.com/)** — features, screenshots,
supported formats, FAQ, and downloads.

## Download

- **Stable releases:** https://www.routeconverter.com/releases/
- **Prereleases (frequent):** https://www.routeconverter.com/prereleases/

The **Windows** (`.exe`) and **macOS** (`.app`) downloads bundle a Java runtime —
nothing else to install. The **Linux** build ships as a runnable `.jar` that needs
**Java 17 or later** installed on your system.

## Build & run from source

You need **JDK 17** (e.g. from [Adoptium](https://adoptium.net/)). Maven comes
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

> On macOS/Linux, if `java` isn't on your `PATH`, activate a JDK 17 first
> (e.g. via [sdkman](https://sdkman.io/): `sdk use java 17`).

CI builds and tests on **Java 17, 21 and 25** plus a Windows smoke build.

## Contributing

Contributions are very welcome — bug fixes, new formats, UI improvements,
translations. 🎉

- **Pull requests:** fork, branch, and open a PR against `master`. Keep your diff
  small and focused, match the surrounding code style, and add tests for your
  change. A maintainer reviews and merges every PR.
- **Conventions:** see [`AGENTS.md`](AGENTS.md) for the project layout, module
  layering, build/test commands, and code conventions (GPL header, JAXB test
  objects, IntelliJ GUI Designer `.form` files, …).
- **Translations** go through Weblate, not direct edits:
  [hosted.weblate.org/projects/routeconverter](https://hosted.weblate.org/projects/routeconverter/).
- **Issues:** https://github.com/cpesch/RouteConverter/issues

Contributors are listed in [`CONTRIBUTORS.txt`](CONTRIBUTORS.txt).

### IDE setup (IntelliJ IDEA)

1. **File → Open…** the root `pom.xml`.
2. **Settings → Editor → GUI Designer:** choose *"Generate GUI into: Java source
   code"* and disable *"Automatically copy form runtime classes…"*. Layout is
   edited in the `.form` files, not the generated `$$$setupUI$$$` blocks.

Eclipse (m2e) and NetBeans work too — import the root `pom.xml` as a Maven project.

## License

RouteConverter is licensed under the **GNU General Public License v2** — see
[`LICENSE-GPL.txt`](LICENSE-GPL.txt). By contributing, you agree your code ships
under the GPL.

Have fun! — Christian Pesch ([@cpesch](https://github.com/cpesch))
