bin\xjc -verbose -d generated -p slash.navigation.gpx.binding10 c:\Development\RouteConverter\gpx\src\main\doc\gpx10.xsd
bin\xjc -verbose -d generated -p slash.navigation.gpx.binding11 c:\Development\RouteConverter\gpx\src\main\doc\gpx11.xsd
bin\xjc -verbose -d generated -p slash.navigation.gpx.garmin3 c:\Development\RouteConverter\gpx\src\main\doc\GpxExtensionsv3.xsd
bin\xjc -cp lib/jaxb-impl.jar -verbose -d c:\Development\RouteConverter\gpx\src\main\java -p slash.navigation.gpx.trackpoint1 c:\Development\RouteConverter\gpx\src\main\doc\TrackPointExtensionv1.xsd
bin\xjc -cp lib/jaxb-impl.jar -verbose -d c:\Development\RouteConverter\gpx\src\main\java -p slash.navigation.gpx.trackpoint2 c:\Development\RouteConverter\gpx\src\main\doc\TrackPointExtensionv2.xsd
bin\xjc -cp lib/jaxb-impl.jar -verbose -d c:\Development\RouteConverter\gpx\src\main\java -p slash.navigation.gpx.trip1 c:\Development\RouteConverter\gpx\src\main\doc\TripExtensionsv1.xsd
bin\xjc -verbose -d generated -p slash.navigation.gpx.routecatalog10 c:\Development\RouteConverter\gpx\src\main\doc\RouteCatalog10.xsd
bin\xjc -verbose -d generated -p slash.navigation.gpx.trekbuddy c:\Development\RouteConverter\gpx\src\main\doc\trekBuddyExtensions0984.xsd
bin\xjc -verbose -d generated -p slash.navigation.lmx.binding c:\Development\RouteConverter\navigation-formats\src\main\doc\lmx\lmx.xsd
bin\xjc -verbose -d generated -p slash.navigation.tcx.binding1 c:\Development\RouteConverter\navigation-formats\src\main\doc\tcx\TrainingCenterDatabasev1.xsd
bin\xjc -verbose -d generated -p slash.navigation.tcx.binding2 c:\Development\RouteConverter\navigation-formats\src\main\doc\tcx\TrainingCenterDatabasev2.xsd
bin\xjc -verbose -d generated -p slash.navigation.kml.binding20 c:\Development\RouteConverter\kml\src\main\doc\kml20.xsd
bin\xjc -cp lib\jaxb-impl.jar -verbose -d c:\Development\RouteConverter\kml\src\main\java -p slash.navigation.kml.binding21 c:\Development\RouteConverter\kml\src\main\doc\kml21.xsd
bin\xjc -cp lib\jaxb-impl.jar -verbose -d c:\Development\RouteConverter\kml\src\main\java c:\Development\RouteConverter\kml\src\main\doc\ogckml22.xsd -b c:\Development\RouteConverter\kml\src\main\doc\ogckml22.xjb
bin\xjc -cp lib\jaxb-impl.jar -verbose -d c:\Development\RouteConverter\kml\src\main\java c:\Development\RouteConverter\kml\src\main\doc\kml22gx.xsd -b c:\Development\RouteConverter\kml\src\main\doc\kml22gx.xjb
bin\xjc -cp lib\jaxb-impl.jar -verbose -d c:\Development\RouteConverter\kml\src\main\java c:\Development\RouteConverter\kml\src\main\doc\kml22beta.xsd -b c:\Development\RouteConverter\kml\src\main\doc\kml22beta.xjb
bin\xjc -verbose -d generated -p slash.navigation.gopal.binding3 c:\Development\RouteConverter\navigation-formats\src\main\doc\gopal\gopal3.xsd
bin\xjc -verbose -d generated -p slash.navigation.gopal.binding5 c:\Development\RouteConverter\navigation-formats\src\main\doc\gopal\gopal5.xsd
bin\xjc -verbose -dtd -d generated -p slash.navigation.viamichelin.binding c:\Development\RouteConverter\navigation-formats\src\main\doc\viamichelin\export.dtd
bin\xjc -verbose -d generated -p slash.navigation.geonames.binding c:\Development\RouteConverter\geonames\src\main\doc\geonames.xsd
bin\xjc -cp lib\jaxb-impl.jar -verbose -d c:\Development\RouteConverter\nominatim\src\main\java -p slash.navigation.nominatim.search c:\Development\RouteConverter\nominatim\src\main\doc\search.xsd
bin\xjc -cp lib\jaxb-impl.jar -verbose -d c:\Development\RouteConverter\nominatim\src\main\java -p slash.navigation.nominatim.reverse c:\Development\RouteConverter\nominatim\src\main\doc\reverse.xsd
bin\xjc -verbose -d generated -p slash.navigation.nmn.binding7 c:\Development\RouteConverter\navigation-formats\src\main\doc\nmn\nmn7.xsd
bin\xjc -verbose -d generated -p slash.navigation.klicktel.binding c:\Development\RouteConverter\navigation-formats\src\main\doc\klicktel\klicktel.xsd
bin\xjc -verbose -d generated -p slash.navigation.googlemaps.elevation c:\Development\RouteConverter\googlemaps\src\main\doc\elevation.xsd
bin\xjc -verbose -d generated -p slash.navigation.googlemaps.geocode c:\Development\RouteConverter\googlemaps\src\main\doc\geocode.xsd
bin\xjc -cp lib\jaxb-impl.jar -verbose -d c:\Development\RouteConverter\datasource\src\main\java -p slash.navigation.datasources.binding c:\Development\RouteConverter\datasource\src\main\doc\datasource-catalog.xsd
bin\xjc -cp lib\jaxb-impl.jar -verbose -d c:\Development\RouteConverter\download\src\main\java -p slash.navigation.download.queue.binding  c:\Development\RouteConverter\download\src\main\doc\queue.xsd
bin\xjc -cp lib\jaxb-impl.jar -verbose -d c:\Development\RouteConverter\route\src\main\java -p slash.navigation.routes.remote.binding c:\Development\RouteConverter\route\src\main\doc\route-catalog.xsd
bin\xjc -cp lib\jaxb-impl.jar -verbose -d c:\Development\RouteConverter\tileserver-maps\src\main\java -p slash.navigation.maps.tileserver.binding c:\Development\RouteConverter\tileserver-maps\src\main\doc\tileserver-catalog.xsd

xjc -verbose -d kml/src/main/java -p slash.navigation.kml.binding21 kml/src/main/doc/kml21.xsd
