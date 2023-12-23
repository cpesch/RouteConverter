#!/bin/sh

MAJOR=17
VERSION=$MAJOR.0.9

mvn install:install-file \
    -Dfile=jlink-routeconverter-opensource-$MAJOR-osx.zip \
    -DgroupId=org.openjdk.java \
    -DartifactId=jlink-routeconverter-opensource \
    -Dversion=$VERSION \
    -Dpackaging=zip \
    -Dclassifier=osx \
    -DcreateChecksum=true
