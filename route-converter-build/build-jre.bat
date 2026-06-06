@echo off

REM Build a stripped Windows JRE for the two Windows bundle modules.
REM
REM Lives in route-converter-build/ so it sits next to pom.xml (its
REM source of truth for jre.version). Reads JDK + JRE filenames from the
REM pom and writes the JRE into the sibling RouteConverterWindowsBundle/
REM dir where both NSI scripts (RouteConverter.nsi + TimeAlbumPro.nsi)
REM resolve "..\jre-${JRE}" at NSIS compile time.
REM
REM Maintainer setup on the Windows VM:
REM   - Place the extracted Temurin JDK at:
REM       ..\RouteConverterWindowsBundle\jdk-<jre.version>\
REM   - Run this script from route-converter-build\ (or via cd).
REM
REM CI (build-windows-jre.yml / prerelease.yml / release.yml) does not
REM call this file — it inlines the jlink/trim recipe — but the module
REM list + trim list MUST stay in sync.

setlocal

REM Read jre.version from the adjacent pom.xml. The findstr+for trick
REM splits "<jre.version>X.Y.Z</jre.version>" on <> and grabs token 3.
for /f "tokens=3 delims=<>" %%a in ('findstr "jre.version" pom.xml') do set JREVER=%%a
if "%JREVER%"=="" ( echo Failed to read jre.version from pom.xml & exit /b 1 )

set BUNDLE_DIR=..\RouteConverterWindowsBundle
set JDK=%BUNDLE_DIR%\jdk-%JREVER%
set JRE=%BUNDLE_DIR%\jre-%JREVER%
echo Using JDK=%JDK%
echo Using JRE=%JRE%

if not exist "%JDK%\bin\jlink.exe" (
  echo JDK not found at %JDK% — extract Temurin %JREVER% to that path first.
  exit /b 1
)

echo Cleaning
if exist "%JRE%" rmdir /s /q "%JRE%"

echo Building
"%JDK%\bin\jlink" --module-path "%JDK%\jmods" --add-modules java.base,java.compiler,java.datatransfer,java.desktop,java.logging,java.management,java.naming,java.prefs,java.security.jgss,java.sql,java.xml,java.xml.crypto,jdk.crypto.ec,jdk.unsupported --output "%JRE%" --strip-debug --compress 2 --no-header-files --no-man-pages

echo Reducing
rmdir /s /q "%JRE%\legal"
del /q "%JRE%\release"
del /q "%JRE%\bin\javaw.exe"
del /q "%JRE%\bin\keytool.exe"
del /q "%JRE%\bin\kinit.exe"
del /q "%JRE%\bin\klist.exe"
del /q "%JRE%\bin\ktab.exe"
rmdir /s /q "%JRE%\bin\client"
rmdir /s /q "%JRE%\lib\client"
rmdir /s /q "%JRE%\lib\server"
del /q "%JRE%\conf\security\policy\README.txt"

echo Ready

endlocal
