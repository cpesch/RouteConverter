# RouteConverter for Windows bundled with JRE 17

`jre.version` (the bundled JDK patch level) is the single source of
truth in [`../route-converter-build/pom.xml`](../route-converter-build/pom.xml).
Every reference below reads from it; only edit the pom when bumping
versions.

1. Download and extract the Temurin Java 17 OpenJDK for Windows into
   `./jdk-<jre.version>/` (sibling of this README). Download URL pattern:

       https://github.com/adoptium/temurin17-binaries/releases/

2. Analyze module dependencies and check for errors

       jdk-<jre.version>\bin\jdeps -s target\RouteConverterWindowsOpenSource.jar

3. Build the custom JRE

       cd ..\route-converter-build
       build-jre.bat

   Output lands at `..\RouteConverterWindowsBundle\jre-<jre.version>\`,
   right next to this README — which is where `RouteConverter.nsi` and
   `TimeAlbumPro.nsi` expect to find it (`!define JRE_PATH "..\jre-${JRE}"`,
   evaluated against the filtered `.nsi` in each bundle module's
   `target/`).

4. Install NSIS from https://nsis.sourceforge.io/Download

5. Build installer with NSIS

       build-app.bat

GitHub Actions (`build-windows-jre.yml` / `prerelease.yml` / `release.yml`)
runs steps 1–5 headlessly on `windows-latest`; the stripped JRE is
hosted at `https://static.routeconverter.com/build/windows/`.
