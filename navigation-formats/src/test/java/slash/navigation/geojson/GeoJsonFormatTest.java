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
package slash.navigation.geojson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static slash.common.io.Transfer.UTF8_ENCODING;

/**
 * Tests {@link GeoJsonFormat}.
 *
 * @author Christian Pesch
 */
public class GeoJsonFormatTest {

    @Test
    public void readsZeroZeroFeatureWithLocationNameAsCoordinateLess() throws IOException {
        String json =
                """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {"type": "Point", "coordinates": [0, 0]},
                      "properties": {"location": {"name": "Some Place"}}
                    }
                  ]
                }
                """;

        List<Wgs84Position> positions = read(json);

        assertEquals(1, positions.size());
        Wgs84Position position = positions.get(0);
        assertFalse(position.hasCoordinates());
        assertEquals("Some Place", position.getDescription());
    }

    @Test
    public void readsNullGeometryFeatureAsCoordinateLess() throws IOException {
        String json =
                """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": null,
                      "properties": {"name": "Some Place"}
                    }
                  ]
                }
                """;

        List<Wgs84Position> positions = read(json);

        assertEquals(1, positions.size());
        Wgs84Position position = positions.get(0);
        assertFalse(position.hasCoordinates());
        assertEquals("Some Place", position.getDescription());
    }

    @Test
    public void skipsZeroZeroFeatureWithoutNameOrAddress() throws IOException {
        String json =
                """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {"type": "Point", "coordinates": [0, 0]},
                      "properties": {}
                    }
                  ]
                }
                """;

        List<Wgs84Position> positions = read(json);

        assertEquals(0, positions.size());
    }

    @Test
    public void reviewWithRatingAndMultiLineTextHasNoLineFeeds() throws IOException {
        String json =
                """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {"type": "Point", "coordinates": [9.973844, 53.5493424]},
                      "properties": {
                        "location": {"name": "Name", "address": "Address"},
                        "five_star_rating_published": 5,
                        "review_text_published": "Line one\\nLine two"
                      }
                    }
                  ]
                }
                """;

        List<Wgs84Position> positions = read(json);

        assertEquals(1, positions.size());
        assertEquals("Name; Address; 5/5 Line one Line two", positions.get(0).getDescription());
    }

    @Test
    public void reviewWithRatingWithoutTextOmitsText() throws IOException {
        String json =
                """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {"type": "Point", "coordinates": [9.973844, 53.5493424]},
                      "properties": {
                        "location": {"name": "Name", "address": "Address"},
                        "five_star_rating_published": 5
                      }
                    }
                  ]
                }
                """;

        List<Wgs84Position> positions = read(json);

        assertEquals(1, positions.size());
        assertEquals("Name; Address; 5/5", positions.get(0).getDescription());
    }

    @Test
    public void reviewWithTextWithoutRatingOmitsRating() throws IOException {
        String json =
                """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {"type": "Point", "coordinates": [9.973844, 53.5493424]},
                      "properties": {
                        "location": {"name": "Name", "address": "Address"},
                        "review_text_published": "text"
                      }
                    }
                  ]
                }
                """;

        List<Wgs84Position> positions = read(json);

        assertEquals(1, positions.size());
        assertEquals("Name; Address; text", positions.get(0).getDescription());
    }

    @Test
    public void writingACoordinateLessPositionDoesNotWriteAnEmptyCoordinatesArray() throws IOException {
        GeoJsonFormat format = new GeoJsonFormat();

        List<Wgs84Position> positions = new ArrayList<>();
        Wgs84Position coordinateLess = new Wgs84Position(null, null, null, null, null, "Some Place");
        Wgs84Position normal = new Wgs84Position(13.4050, 52.5200, null, null, null, "Berlin");
        positions.add(coordinateLess);
        positions.add(normal);
        Wgs84Route route = new Wgs84Route(format, RouteCharacteristics.Waypoints, "test", positions);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(route, out, 0, positions.size());
        String written = out.toString(UTF8_ENCODING);

        JsonNode root = new ObjectMapper().readTree(written);
        JsonNode firstGeometry = root.path("features").get(0).path("geometry");
        assertTrue(firstGeometry.isNull());

        List<Wgs84Position> reread = read(written);
        assertEquals(2, reread.size());

        Wgs84Position rereadCoordinateLess = reread.get(0);
        assertFalse(rereadCoordinateLess.hasCoordinates());
        assertEquals("Some Place", rereadCoordinateLess.getDescription());

        Wgs84Position rereadNormal = reread.get(1);
        assertTrue(rereadNormal.hasCoordinates());
        assertEquals(13.4050, rereadNormal.getLongitude(), 0.000001);
        assertEquals(52.5200, rereadNormal.getLatitude(), 0.000001);
        assertEquals("Berlin", rereadNormal.getDescription());
    }

    private List<Wgs84Position> read(String json) throws IOException {
        GeoJsonFormat format = new GeoJsonFormat();
        ParserContext<Wgs84Route> context = new ParserContextImpl<>();
        format.read(new BufferedReader(new StringReader(json)), UTF8_ENCODING, context);
        List<Wgs84Route> routes = context.getRoutes();
        return routes.isEmpty() ? List.of() : routes.get(0).getPositions();
    }
}
