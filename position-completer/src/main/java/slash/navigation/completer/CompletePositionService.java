package slash.navigation.completer;

import slash.navigation.earthtools.EarthToolsService;
import slash.navigation.geonames.GeoNamesService;
import slash.navigation.googlemaps.GoogleMapsService;
import slash.navigation.hgt.HgtFiles;

import java.io.IOException;

/**
 * Helps to complete positions with elevation, postal address and populated place information.
 *
 * @author Christian Pesch
 */

public class CompletePositionService {
    private HgtFiles hgtFiles = new HgtFiles();
    private GeoNamesService geoNamesService = new GeoNamesService();
    private EarthToolsService earthToolsService = new EarthToolsService();

    private GoogleMapsService googleMapsService = new GoogleMapsService();
    private GeoNamesService geonamesService = new GeoNamesService();

    public void dispose() {
        hgtFiles.dispose();
    }

    public Integer getElevationFor(double longitude, double latitude) throws IOException {
        Integer elevation = hgtFiles.getElevationFor(longitude, latitude);
        if (elevation == null)
            elevation = geoNamesService.getElevationFor(longitude, latitude);
        if (elevation == null)
            elevation = earthToolsService.getElevationFor(longitude, latitude);
        return elevation;
    }

    public String getCommentFor(double longitude, double latitude) throws IOException {
        String comment = googleMapsService.getLocationFor(longitude, latitude);
        if (comment == null)
            comment = geonamesService.getNearByFor(longitude, latitude);
        return comment;
    }
}
