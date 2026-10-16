#!/bin/sh
# $HOME is a tmpfs inside the sandbox: keep the preferences and the maps,
# routes and downloads in the app's persistent ~/.var/app directories
exec /app/jre/bin/java -Xmx1024m \
  -Djava.util.prefs.userRoot="$XDG_CONFIG_HOME" \
  -DapplicationDirectory="$XDG_DATA_HOME/routeconverter" \
  -jar /app/share/routeconverter/RouteConverterLinux.jar "$@"
