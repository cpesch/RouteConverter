#!/bin/sh

jpackage \
  --type app-image \
  --copyright "COPYRIGHT-CP" \
  --description "DESCRIPTION-RC" \
  --icon src/main/app-resources/Resources/RouteConverter.icns \
  --name "NAME-RC" \
  --dest release \
  --vendor "VENDOR-CP" \
  --verbose \
  --input target/input \
  --main-jar RouteConverterMacOpenSource.jar \
  --main-class slash.navigation.converter.gui.RouteConverterOpenSource \
  --java-options '--add-exports java.desktop/com.apple.eawt=ALL-UNNAMED' \
  --java-options '-Xmx1024m' \
  --mac-package-identifier RTCO
