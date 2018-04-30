/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.photon;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.geocoding.GeocodingService;
import slash.navigation.rest.Get;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static slash.common.io.Transfer.encodeUri;
import static slash.common.io.Transfer.trim;

/**
 * Encapsulates REST access to the OSM Photon service.
 *
 * @author Christian Pesch
 */

public class PhotonService implements GeocodingService {
    private static final Preferences preferences = Preferences.userNodeForPackage(PhotonService.class);
    private static final String PHOTON_URL_PREFERENCE = "photonUrl";

    public String getName() {
        return "Photon";
    }

    public boolean isDownload() {
        return false;
    }

    public boolean isOverQueryLimit() {
        return false;
    }

    private String getPhotonUrl() {
        return preferences.get(PHOTON_URL_PREFERENCE, "http://photon.komoot.de");
    }

    private String execute(String uri) throws IOException {
        String url = getPhotonUrl() + uri;
        Get get = new Get(url);
        String result = get.executeAsString();
        if (get.isSuccessful())
            return result;
        return null;
    }

    private FeatureCollection getResultFor(String uri) throws IOException {
        String result = execute(uri);
        if (result != null) {
            try {
                return new ObjectMapper().readValue(result, FeatureCollection.class);
            } catch (JsonParseException | JsonMappingException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        }
        return null;
    }

    private List<NavigationPosition> extractPositions(List<Feature> features) {
        List<NavigationPosition> result = new ArrayList<>(features.size());
        for (Feature feature : features) {
            GeoJsonObject geometry = feature.getGeometry();
            if (!(geometry instanceof Point))
                continue;

            Point point = Point.class.cast(geometry);
            LngLatAlt lngLatAlt = point.getCoordinates();
            String type = feature.getProperty("osm_key");
            result.add(new SimpleNavigationPosition(lngLatAlt.getLongitude(), lngLatAlt.getLatitude(), null,
                    getDisplayName(feature) + " (" + type + ")"));
        }
        return result;
    }

    public List<NavigationPosition> getPositionsFor(String address) throws IOException {
        FeatureCollection collection = getResultFor("/api/?q=" + encodeUri(address) + "&limit=10");
        if (collection == null)
            return null;
        return extractPositions(collection.getFeatures());
    }

    public String getAddressFor(NavigationPosition position) throws IOException {
        FeatureCollection collection = getResultFor("/reverse/?lon=" + position.getLongitude() + "&lat=" + position.getLatitude());
        if (collection == null)
            return null;
        List<Feature> features = collection.getFeatures();
        if (features.size() == 0)
            return null;
        Feature feature = features.get(0);
        GeoJsonObject geometry = feature.getGeometry();
        if (!(geometry instanceof Point))
            return null;

        return getDisplayName(feature);
    }

    private String getProperty(Feature feature, String propertyName) {
        Object property = feature.getProperty(propertyName);
        return property != null ? trim(property.toString()) : "";
    }

    private String getDisplayName(Feature feature) {
        String result = getProperty(feature, "name");
        if(result.length() > 0)
            result += ", ";
        String postcode = getProperty(feature, "postcode");
        if(postcode.length() > 0)
            result += postcode;
        String city = getProperty(feature, "city");
        if(city.length() > 0)
            result += " " + city;
        String state = getProperty(feature, "state");
        if(state.length() > 0)
            result += ", " + state;
        String country = getProperty(feature, "country");
        if(country.length() > 0)
            result += ", " + country;

        result = result.replaceAll(" {2}", " ");
        if(result.endsWith(", "))
            result = result.substring(0, result.length() - 2);
        return result;
    }
}
