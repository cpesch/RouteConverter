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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.*;
import slash.navigation.common.NavigationPosition;
import slash.navigation.geocoding.BaseGeocodingService;
import slash.navigation.geocoding.CategorizedNavigationPosition;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.geocoding.SimpleCategorizedNavigationPosition;
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

public class PhotonService extends BaseGeocodingService {
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
        return preferences.get(PHOTON_URL_PREFERENCE, "https://photon.komoot.io");
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
                return asFeatureCollection(new ObjectMapper().readTree(result));
            } catch (JsonParseException | JsonMappingException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        }
        return null;
    }

    private FeatureCollection asFeatureCollection(JsonNode root) {
        if (root == null || !"FeatureCollection".equals(root.path("type").asText()))
            return null;

        FeatureCollection result = new FeatureCollection();
        JsonNode features = root.path("features");
        if (!features.isArray())
            return result;

        for (JsonNode featureNode : features) {
            Feature feature = asFeature(featureNode);
            if (feature != null)
                result.add(feature);
        }
        return result;
    }

    private Feature asFeature(JsonNode featureNode) {
        if (!featureNode.isObject())
            return null;

        Feature feature = new Feature();
        GeoJsonObject geometry = asPoint(featureNode.path("geometry"));
        if (geometry != null)
            feature.setGeometry(geometry);

        JsonNode properties = featureNode.path("properties");
        if (properties.isObject()) {
            properties.properties().forEach(entry -> feature.setProperty(entry.getKey(), asProperty(entry.getValue())));
        }
        return feature;
    }

    private GeoJsonObject asPoint(JsonNode geometryNode) {
        if (!geometryNode.isObject() || !"Point".equals(geometryNode.path("type").asText()))
            return null;

        JsonNode coordinates = geometryNode.path("coordinates");
        if (!coordinates.isArray() || coordinates.size() < 2 ||
                !coordinates.get(0).isNumber() || !coordinates.get(1).isNumber())
            return null;

        LngLatAlt lngLatAlt = new LngLatAlt();
        lngLatAlt.setLongitude(coordinates.get(0).doubleValue());
        lngLatAlt.setLatitude(coordinates.get(1).doubleValue());
        if (coordinates.size() > 2 && coordinates.get(2).isNumber())
            lngLatAlt.setAltitude(coordinates.get(2).doubleValue());

        Point point = new Point();
        point.setCoordinates(lngLatAlt);
        return point;
    }

    private Object asProperty(JsonNode value) {
        if (value == null || value.isNull())
            return null;
        if (value.isTextual())
            return value.asText();
        if (value.isIntegralNumber())
            return value.asLong();
        if (value.isFloatingPointNumber())
            return value.asDouble();
        if (value.isBoolean())
            return value.asBoolean();
        if (value.isArray()) {
            List<Object> result = new ArrayList<>();
            for (JsonNode node : value) {
                result.add(asProperty(node));
            }
            return result;
        }
        if (value.isObject()) {
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            value.properties().forEach(entry -> result.put(entry.getKey(), asProperty(entry.getValue())));
            return result;
        }
        return value.asText();
    }

    private List<CategorizedNavigationPosition> extractPositions(List<Feature> features) {
        List<CategorizedNavigationPosition> result = new ArrayList<>(features.size());
        for (Feature feature : features) {
            CategorizedNavigationPosition position = extractPosition(feature);
            if (position != null)
                result.add(position);
        }
        return result;
    }

    CategorizedNavigationPosition extractPosition(Feature feature) {
        GeoJsonObject geometry = feature.getGeometry();
        if (!(geometry instanceof Point point))
            return null;

        LngLatAlt lngLatAlt = point.getCoordinates();
        String type = getProperty(feature, "type");
        if(type.isEmpty())
            type = getProperty(feature, "osm_value");
        if(type.isEmpty())
            type = getProperty(feature, "osm_key");
        return new SimpleCategorizedNavigationPosition(lngLatAlt.getLongitude(), lngLatAlt.getLatitude(), null,
                getDisplayName(feature), type.isEmpty() ? null : type);
    }

    public List<GeocodingResult> getPositionsFor(String address) throws IOException {
        FeatureCollection collection = getResultFor("/api/?q=" + encodeUri(address) + "&limit=10");
        if (collection == null)
            return null;
        return asGeocodingResults(extractPositions(collection.getFeatures()));
    }

    public String getAddressFor(NavigationPosition position) throws IOException {
        FeatureCollection collection = getResultFor("/reverse/?lon=" + position.getLongitude() + "&lat=" + position.getLatitude());
        if (collection == null)
            return null;
        List<Feature> features = collection.getFeatures();
        if (features.isEmpty())
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
        if(!result.isEmpty())
            result += ", ";
        String postcode = getProperty(feature, "postcode");
        if(!postcode.isEmpty())
            result += postcode;
        String city = getProperty(feature, "city");
        if(!city.isEmpty())
            result += " " + city;
        String state = getProperty(feature, "state");
        if(!state.isEmpty())
            result += ", " + state;
        String country = getProperty(feature, "country");
        if(!country.isEmpty())
            result += ", " + country;

        result = result.replaceAll(" {2}", " ");
        if(result.endsWith(", "))
            result = result.substring(0, result.length() - 2);
        return result;
    }
}
