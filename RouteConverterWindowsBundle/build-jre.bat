@echo off

REM Single source of truth = jre.version property in
REM ..\route-converter-build\pom.xml. Parse "<jre.version>X.Y.Z</jre.version>"
REM with findstr+for (tokens=3 delims=<>).
for /f "tokens=3 delims=<>" %%a in ('findstr "jre.version" ..\route-converter-build\pom.xml') do set JREVER=%%a
if "%JREVER%"=="" ( echo Failed to read jre.version from pom & exit /b 1 )
set JDK=jdk-%JREVER%
set JRE=jre-%JREVER%
echo Using JDK=%JDK% JRE=%JRE%

echo Cleaning
rmdir /s /q %JRE%

echo Building
%JDK%\bin\jlink --module-path "%JDK%\jmods" --add-modules java.base,java.compiler,java.datatransfer,java.desktop,java.logging,java.management,java.naming,java.prefs,java.security.jgss,java.sql,java.xml,java.xml.crypto,jdk.crypto.ec,jdk.unsupported --output %JRE% --strip-debug --compress 2 --no-header-files --no-man-pages

echo Reducing
rmdir /s /q %JRE%\legal
del /q %JRE%\release
del /q %JRE%\bin\javaw.exe
del /q %JRE%\bin\keytool.exe
del /q %JRE%\bin\kinit.exe
del /q %JRE%\bin\klist.exe
del /q %JRE%\bin\ktab.exe
rmdir /s /q %JRE%\bin\client
rmdir /s /q %JRE%\lib\client
rmdir /s /q %JRE%\lib\server
del /q %JRE%\conf\security\policy\README.txt

echo Ready
