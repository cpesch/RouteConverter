#!/bin/sh

VERSION=11.0.12
JDK=jdk-$VERSION.jdk
JRE=Runtime/Contents/Home

echo Cleaning
rm -rf $JRE

echo Building
/Library/Java/JavaVirtualMachines/$JDK/Contents/Home/bin/jlink --module-path "%JDK%/jmods" --add-modules java.base,java.compiler,java.datatransfer,java.desktop,java.logging,java.management,java.naming,java.prefs,java.security.jgss,java.sql,java.xml,java.xml.crypto,jdk.crypto.ec,jdk.unsupported --output $JRE --strip-debug --compress 2 --no-header-files --no-man-pages

echo Reducing
rm -rf $JRE/legal
rm -f $JRE/release
rm -f $JRE/bin/keytool
rm -f $JRE/conf/security/policy/README.txt
rm -f $JRE/lib/server/Xusage.txt

echo Zipping
zip -9 -r jlink-routeconverter-opensource-$VERSION-osx.zip Runtime

echo Ready
