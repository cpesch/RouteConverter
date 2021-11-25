@echo off

set JDK=jdk-11.0.13.8-1
set JRE=jre-11.0.13.8-1

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
