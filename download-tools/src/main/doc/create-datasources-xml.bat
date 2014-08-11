java -jar download-tools\target\CreateBrouterDataSourcesXml.jar "testId" "testName" "testUrl" "testDirectory" "C:\Temp\test-brouter" "C:\Temp\test-brouter-datasources.xml"
java -jar download-tools\target\CreateGraphHopperDataSourcesXml.jar "testId" "testName" "testUrl" "testDirectory" "C:\Temp\test-graphhopper" "C:\Temp\test-graphhopper-datasources.xml"
java -jar download-tools\target\CreateHgtDataSourcesXml.jar "testId" "testName" "testUrl" "testDirectory" "C:\Temp\test-hgt" "C:\Temp\test-hgt-datasources.xml"
java -jar download-tools\target\CreateMapDataSourcesXml.jar "testId" "testName" "testUrl" "testDirectory" "C:\Temp\test-map" "C:\Temp\test-map-datasources.xml"

java -jar download-tools\target\CreateBrouterDataSourcesXml.jar "brouter" "BRouter" "http://h2096617.stratoserver.net/brouter/segments2/" "brouter" "W:\Mirrors\h2096617.stratoserver.net\brouter\segments2" "C:\Temp\brouter-datasources.xml"

java -jar download-tools\target\CreateGraphHopperDataSourcesXml.jar "graphhopper" "GraphHopper" "http://download.geofabrik.de/" "graphhopper" "W:\Mirrors\download.geofabrik.de" "C:\Temp\graphhopper-datasources.xml"

java -jar download-tools\target\CreateHgtDataSourcesXml.jar "srtm3" "NASA SRTM 3" "http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/" "srtm3" "W:\Mirrors\dds.cr.usgs.gov\srtm\version2_1\SRTM3" "C:\Temp\srtm3-datasources.xml"
java -jar download-tools\target\CreateHgtDataSourcesXml.jar "srtm1" "NASA SRTM 1" "http://dds.cr.usgs.gov/srtm/version2_1/SRTM1/" "srtm1" "W:\Mirrors\dds.cr.usgs.gov\srtm\version2_1\SRTM1" "C:\Temp\srtm1-datasources.xml"
java -jar download-tools\target\CreateHgtDataSourcesXml.jar "ferranti3" "Jonathan de Ferranti DEM 3" "http://www.viewfinderpanoramas.org/dem3/" "ferranti3" "W:\Mirrors\www.viewfinderpanoramas.org\dem3" "C:\Temp\ferranti3-datasources.xml"
java -jar download-tools\target\CreateHgtDataSourcesXml.jar "ferranti1" "Jonathan de Ferranti DEM 1" "http://www.viewfinderpanoramas.org/dem1/" "ferranti1" "W:\Mirrors\www.viewfinderpanoramas.org\dem1" "C:\Temp\ferranti1-datasources.xml"

java -jar download-tools\target\CreateMapDataSourcesXml.jar "Freizeitkarte Maps" "http://download.freizeitkarte-osm.de/Experimental/MapsForge/Beta6/" "maps/freizeitkarte" "W:\Mirrors\download.freizeitkarte-osm.de\Experimental\MapsForge\Beta6" "mapsforge-maps\src\main\resources\slash\navigation\maps\freizeitkarte-maps-datasources.xml"
java -jar download-tools\target\CreateMapDataSourcesXml.jar "Mapsforge Maps" "http://download.mapsforge.org/maps/" "maps/mapsforge" "W:\Mirrors\download.mapsforge.org\maps" "mapsforge-maps\src\main\resources\slash\navigation\maps\mapsforge-maps-datasources.xml"
java -jar download-tools\target\CreateMapDataSourcesXml.jar "Openandromaps Maps" "http://ftp5.gwdg.de/pub/misc/openstreetmap/openandromaps/maps/" "maps/openandromaps" "W:\Mirrors\ftp5.gwdg.de\pub\misc\openstreetmap\openandromaps\maps" "mapsforge-maps\src\main\resources\slash\navigation\maps\openandromaps-maps-datasources.xml"
java -jar download-tools\target\CreateMapDataSourcesXml.jar "Androidmaps" "http://www.androidmaps.co.uk/maps/" "maps/androidmaps" "W:\Mirrors\www.androidmaps.co.uk\maps" "mapsforge-maps\src\main\resources\slash\navigation\maps\android-maps-datasources.xml"

