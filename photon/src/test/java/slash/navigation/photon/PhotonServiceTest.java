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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.Polygon;
import org.junit.Test;
import slash.navigation.geocoding.CategorizedNavigationPosition;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PhotonService}: extractPosition, getDisplayName, getProperty branches.
 *
 * @author Christian Pesch
 */
public class PhotonServiceTest {
    private final PhotonService service = new PhotonService();

    // ---- helpers ----

    private static Feature pointFeature(double lon, double lat) {
        Feature f = new Feature();
        f.setGeometry(point(lon, lat));
        return f;
    }

    private static Point point(double lon, double lat) {
        LngLatAlt coordinates = new LngLatAlt();
        coordinates.setLongitude(lon);
        coordinates.setLatitude(lat);

        Point point = new Point();
        point.setCoordinates(coordinates);
        return point;
    }

    // ---- extractPosition: geometry variants ----

    @Test
    public void returnsNullForNullGeometry() {
        assertNull(service.extractPosition(new Feature()));
    }

    @Test
    public void returnsNullForNonPointGeometry() {
        Feature f = new Feature();
        f.setGeometry(new Polygon());
        assertNull(service.extractPosition(f));
    }

    @Test
    public void returnsPositionForPoint() {
        Feature f = pointFeature(13.405, 52.520);
        CategorizedNavigationPosition pos = service.extractPosition(f);
        assertNotNull(pos);
        assertEquals(13.405, pos.getLongitude(), 0.0001);
        assertEquals(52.520, pos.getLatitude(), 0.0001);
    }

    // ---- extractPosition: type resolution ----

    @Test
    public void usesTypeProperty() {
        Feature f = pointFeature(1.0, 2.0);
        f.setProperty("type", "city");
        assertEquals("city", service.extractPosition(f).getCategory());
    }

    @Test
    public void fallsBackToOsmValueWhenTypeEmpty() {
        Feature f = pointFeature(1.0, 2.0);
        f.setProperty("osm_value", "bus_stop");
        assertEquals("bus_stop", service.extractPosition(f).getCategory());
    }

    @Test
    public void fallsBackToOsmKeyWhenTypeAndOsmValueEmpty() {
        Feature f = pointFeature(1.0, 2.0);
        f.setProperty("osm_key", "highway");
        assertEquals("highway", service.extractPosition(f).getCategory());
    }

    @Test
    public void categoryIsNullWhenAllTypeFieldsEmpty() {
        Feature f = pointFeature(1.0, 2.0);
        assertNull(service.extractPosition(f).getCategory());
    }

    // ---- getDisplayName: various combinations exercised via extractPosition ----

    @Test
    public void displayNameWithAllFields() {
        Feature f = pointFeature(13.405, 52.520);
        f.setProperty("name", "Berlin");
        f.setProperty("postcode", "10115");
        f.setProperty("city", "Berlin");
        f.setProperty("state", "Berlin");
        f.setProperty("country", "Deutschland");
        f.setProperty("type", "city");
        CategorizedNavigationPosition pos = service.extractPosition(f);
        String desc = pos.getDescription();
        assertTrue("should contain name", desc.contains("Berlin"));
        assertTrue("should contain postcode", desc.contains("10115"));
        assertTrue("should contain country", desc.contains("Deutschland"));
    }

    @Test
    public void displayNameNameOnlyTrimsTrailingCommaSpace() {
        // name="Test", all others empty: "Test, " -> trailing ", " -> "Test"
        Feature f = pointFeature(1.0, 2.0);
        f.setProperty("name", "Test");
        assertEquals("Test", service.extractPosition(f).getDescription());
    }

    @Test
    public void displayNamePostcodeAndCity() {
        // no name, postcode + city -> "12345 Hamburg"
        Feature f = pointFeature(1.0, 2.0);
        f.setProperty("postcode", "12345");
        f.setProperty("city", "Hamburg");
        assertEquals("12345 Hamburg", service.extractPosition(f).getDescription());
    }

    @Test
    public void displayNameCityAndCountry() {
        // no name, no postcode, city + country -> " Berlin, Germany" (leading space from city branch)
        Feature f = pointFeature(1.0, 2.0);
        f.setProperty("city", "Berlin");
        f.setProperty("country", "Germany");
        String desc = service.extractPosition(f).getDescription();
        assertTrue("should contain city", desc.contains("Berlin"));
        assertTrue("should contain country", desc.contains("Germany"));
    }

    @Test
    public void displayNameNameAndState() {
        // name + state, no other fields
        Feature f = pointFeature(1.0, 2.0);
        f.setProperty("name", "Town");
        f.setProperty("state", "Bavaria");
        String desc = service.extractPosition(f).getDescription();
        assertTrue("should contain name", desc.contains("Town"));
        assertTrue("should contain state", desc.contains("Bavaria"));
    }

    @Test
    public void displayNameNameAndCityCollapseDoubleSpace() {
        // name="A", city="B" -> "A,  B" -> replaceAll -> "A, B"
        Feature f = pointFeature(1.0, 2.0);
        f.setProperty("name", "A");
        f.setProperty("city", "B");
        String desc = service.extractPosition(f).getDescription();
        assertFalse("double space should be collapsed", desc.contains("  "));
        assertEquals("A, B", desc);
    }

    @Test
    public void displayNameAllEmpty() {
        Feature f = pointFeature(1.0, 2.0);
        assertEquals("", service.extractPosition(f).getDescription());
    }

    @Test
    public void displayNameCountryOnly() {
        Feature f = pointFeature(1.0, 2.0);
        f.setProperty("country", "France");
        String desc = service.extractPosition(f).getDescription();
        assertTrue("should contain country", desc.contains("France"));
    }

    // ---- original tests (preserved) ----

    @Test
    public void extractsCategorizedPositionUsingPhotonType() {
        Feature feature = new Feature();
        feature.setGeometry(point(13.4050, 52.5200));
        feature.setProperty("name", "Berlin");
        feature.setProperty("city", "Berlin");
        feature.setProperty("country", "Deutschland");
        feature.setProperty("type", "city");

        CategorizedNavigationPosition position = service.extractPosition(feature);

        assertEquals(13.4050, position.getLongitude(), 0.0);
        assertEquals(52.5200, position.getLatitude(), 0.0);
        assertEquals("Berlin, Berlin, Deutschland", position.getDescription());
        assertEquals("city", position.getCategory());
    }

    @Test
    public void fallsBackToOsmValueThenOsmKeyForPhotonCategory() {
        Feature feature = new Feature();
        feature.setGeometry(point(13.4050, 52.5200));
        feature.setProperty("name", "Checkpoint");
        feature.setProperty("osm_value", "bus_stop");

        CategorizedNavigationPosition position = service.extractPosition(feature);
        assertEquals("bus_stop", position.getCategory());

        Feature fallbackFeature = new Feature();
        fallbackFeature.setGeometry(point(13.4050, 52.5200));
        fallbackFeature.setProperty("name", "Unnamed");
        fallbackFeature.setProperty("osm_key", "highway");

        CategorizedNavigationPosition fallbackPosition = service.extractPosition(fallbackFeature);
        assertEquals("highway", fallbackPosition.getCategory());
    }

    @Test
    public void returnsNullForNonPointPhotonFeatures() {
        Feature feature = new Feature();
        assertNull(service.extractPosition(feature));
    }

    // ---- asFeatureCollection: JSON parsing (replaces geojson-jackson binding) ----

    private static JsonNode node(String json) throws IOException {
        return new ObjectMapper().readTree(json);
    }

    @Test
    public void asFeatureCollectionNullRoot() {
        assertNull(service.asFeatureCollection(null));
    }

    @Test
    public void asFeatureCollectionWrongType() throws IOException {
        assertNull(service.asFeatureCollection(node("{\"type\":\"Point\"}")));
    }

    @Test
    public void asFeatureCollectionMissingFeaturesArray() throws IOException {
        FeatureCollection collection = service.asFeatureCollection(node("{\"type\":\"FeatureCollection\"}"));
        assertNotNull(collection);
        assertTrue(collection.getFeatures().isEmpty());
    }

    @Test
    public void asFeatureCollectionParsesPointAndProperties() throws IOException {
        String json = "{\"type\":\"FeatureCollection\",\"features\":[" +
                "{\"type\":\"Feature\"," +
                "\"geometry\":{\"type\":\"Point\",\"coordinates\":[13.405,52.520,33.0]}," +
                "\"properties\":{\"name\":\"Berlin\",\"osm_id\":240109189,\"importance\":0.87," +
                "\"isComplete\":true,\"extent\":[13.0,52.0,14.0,53.0]," +
                "\"address\":{\"city\":\"Berlin\"},\"nothing\":null}}" +
                "]}";
        FeatureCollection collection = service.asFeatureCollection(node(json));
        assertNotNull(collection);
        assertEquals(1, collection.getFeatures().size());

        Feature feature = collection.getFeatures().get(0);
        Point point = (Point) feature.getGeometry();
        assertEquals(13.405, point.getCoordinates().getLongitude(), 0.0001);
        assertEquals(52.520, point.getCoordinates().getLatitude(), 0.0001);
        assertEquals(33.0, point.getCoordinates().getAltitude(), 0.0001);

        assertEquals("Berlin", feature.getProperty("name"));
        assertEquals(Long.valueOf(240109189L), feature.getProperty("osm_id"));
        assertEquals(0.87, (Double) feature.getProperty("importance"), 0.0001);
        assertEquals(Boolean.TRUE, feature.getProperty("isComplete"));
        assertNull(feature.getProperty("nothing"));

        List<?> extent = feature.getProperty("extent");
        assertEquals(4, extent.size());
        Map<?, ?> address = feature.getProperty("address");
        assertEquals("Berlin", address.get("city"));
    }

    @Test
    public void asFeatureCollectionSkipsNonObjectFeatureAndNonPointGeometry() throws IOException {
        String json = "{\"type\":\"FeatureCollection\",\"features\":[" +
                "42," +
                "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[]}}," +
                "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[1.0]}}" +
                "]}";
        FeatureCollection collection = service.asFeatureCollection(node(json));
        assertNotNull(collection);
        // non-object (42) skipped; the two malformed-geometry features become geometry-less features
        assertEquals(2, collection.getFeatures().size());
        assertNull(collection.getFeatures().get(0).getGeometry());
        assertNull(collection.getFeatures().get(1).getGeometry());
    }
}

