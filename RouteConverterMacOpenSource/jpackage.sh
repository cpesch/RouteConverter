#!/bin/sh

jpackage \
  --type app-image \
  --verbose \
  --input target \
  --main-jar RouteConverterMacOpenSource.jar \
  --main-class slash.navigation.converter.gui.RouteConverterOpenSource \
  --dest release \
  --java-options '--add-exports java.desktop/com.apple.eawt=ALL-UNNAMED' \
  --java-options '-Xmx1024m'
