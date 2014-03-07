java -jar download-tools\target\CreateHgtDataSourcesXml.jar "test" "testUrl" "testDirectory" "C:\Temp\test-hgt" "test-hgt-datasources.xml"
java -jar download-tools\target\CreateBrouterDataSourcesXml.jar "test" "testUrl" "testDirectory" "C:\Temp\test-brouter" "test-brouter-datasources.xml"
java -jar download-tools\target\CreateMapDataSourcesXml.jar "test" "testUrl" "testDirectory" "C:\Temp\test-map" "test-map-datasources.xml"

java -jar download-tools\target\CreateHgtDataSourcesXml.jar "NASA SRTM 3" "http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/" "srtm3" "W:\Mirrors\dds.cr.usgs.gov\srtm\version2_1\SRTM3" "hgt\src\main\resources\slash\navigation\hgt\srtm3-datasources.xml"
java -jar download-tools\target\CreateHgtDataSourcesXml.jar "NASA SRTM 1" "http://dds.cr.usgs.gov/srtm/version2_1/SRTM1/" "srtm1" "W:\Mirrors\dds.cr.usgs.gov\srtm\version2_1\SRTM1" "hgt\src\main\resources\slash\navigation\hgt\srtm1-datasources.xml"
java -jar download-tools\target\CreateHgtDataSourcesXml.jar "Jonathan de Ferranti DEM 3" "http://www.viewfinderpanoramas.org/dem3/" "ferranti3" "W:\Mirrors\www.viewfinderpanoramas.org\dem3" "hgt\src\main\resources\slash\navigation\hgt\ferranti3-datasources.xml"
java -jar download-tools\target\CreateHgtDataSourcesXml.jar "Jonathan de Ferranti DEM 1" "http://www.viewfinderpanoramas.org/dem1/" "ferranti1" "W:\Mirrors\www.viewfinderpanoramas.org\dem1" "hgt\src\main\resources\slash\navigation\hgt\ferranti1-datasources.xml"

java -jar download-tools\target\CreateBrouterDataSourcesXml.jar "BRouter" "http://h2096617.stratoserver.net/brouter/segments2/" "brouter" "W:\Mirrors\h2096617.stratoserver.net\brouter\segments2" "brouter\src\main\resources\slash\navigation\brouter\brouter-datasources.xml"

java -jar download-tools\target\CreateMapDataSourcesXml.jar "Freizeitkarte Maps" "http://download.freizeitkarte-osm.de/Experimental/MapsForge/Beta6/" "maps/freizeitkarte" "W:\Mirrors\download.freizeitkarte-osm.de\Experimental\MapsForge\Beta6" "mapsforge-maps\src\main\resources\slash\navigation\maps\freizeitkarte-maps-datasources.xml"
java -jar download-tools\target\CreateMapDataSourcesXml.jar "Mapsforge Maps" "http://download.mapsforge.org/maps/" "maps/mapsforge" "W:\Mirrors\download.mapsforge.org\maps" "mapsforge-maps\src\main\resources\slash\navigation\maps\mapsforge-maps-datasources.xml"
java -jar download-tools\target\CreateMapDataSourcesXml.jar "Openandromaps Maps" "http://ftp5.gwdg.de/pub/misc/openstreetmap/openandromaps/maps/" "maps/openandromaps" "W:\Mirrors\ftp5.gwdg.de\pub\misc\openstreetmap\openandromaps\maps" "mapsforge-maps\src\main\resources\slash\navigation\maps\openandromaps-maps-datasources.xml"

