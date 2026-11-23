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
package slash.navigation.googletakeout;

import org.junit.jupiter.api.Test;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.ParserResult;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Tests for {@link GoogleCommuteRoutesFormat}, with synthetic JSON built in the test.
 *
 * @author Christian Pesch
 */
public class GoogleCommuteRoutesFormatTest {
    private static final double DELTA = 0.0000001;
    private static final String COMMUTES = "{\"trips\": [\n" +
            " {\"place_visit\": [{\"id\": \"\"},\n" +
            "   {\"id\": \"DESTINATION_ID\", \"place\": {\"lat_lng\": {\"latitude\": 50.11, \"longitude\": 8.68}, \"semantic_type\": \"TYPE_WORK\"}}],\n" +
            "  \"transition\": [{\"route\": {\"travel_mode\": \"DRIVE\"}, \"origin\": {\"visit_id\": \"\"}, \"destination\": {\"visit_id\": \"DESTINATION_ID\"}}]},\n" +
            " {\"place_visit\": [{\"id\": \"SOURCE_ID\", \"place\": {\"lat_lng\": {\"latitude\": 50.05, \"longitude\": 8.57}, \"semantic_type\": \"TYPE_HOME\"}},\n" +
            "   {\"id\": \"DESTINATION_ID\", \"place\": {\"lat_lng\": {\"latitude\": 50.11, \"longitude\": 8.68}, \"semantic_type\": \"TYPE_WORK\"}}],\n" +
            "  \"transition\": [{\"route\": {\"travel_mode\": \"TRANSIT\", \"transit\": {\"route\": {\"leg\": [{\"mode\": \"SUBWAY\"}]}}}}]},\n" +
            " {\"place_visit\": [{\"id\": \"DESTINATION_ID\", \"place\": {\"lat_lng\": {\"latitude\": 0, \"longitude\": 0}}}]}\n" +
            "]}";

    private final GoogleCommuteRoutesFormat format = new GoogleCommuteRoutesFormat();

    private List<Wgs84Route> read(String json) throws IOException {
        ParserContext<Wgs84Route> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(json.getBytes(UTF_8)), context);
        return context.getRoutes();
    }

    @Test
    public void readsEachPlaceOnce() throws IOException {
        List<Wgs84Route> routes = read(COMMUTES);

        assertEquals(1, routes.size());
        Wgs84Route route = routes.get(0);
        assertEquals(Waypoints, route.getCharacteristics());
        assertEquals("Commute routes", route.getName());
        assertEquals(2, route.getPositionCount());
        Wgs84Position work = route.getPosition(0);
        assertEquals(50.11, work.getLatitude(), DELTA);
        assertEquals(8.68, work.getLongitude(), DELTA);
        assertEquals("Destination of commute 1 (type work) by drive", work.getDescription());
        Wgs84Position home = route.getPosition(1);
        assertEquals(50.05, home.getLatitude(), DELTA);
        assertEquals(8.57, home.getLongitude(), DELTA);
        assertEquals("Start of commute 2 (type home) by transit", home.getDescription());
    }

    @Test
    public void readsE7Coordinates() throws IOException {
        List<Wgs84Route> routes = read("{\"trips\": [{\"place_visit\": [{\"id\": \"DESTINATION_ID\", " +
                "\"place\": {\"lat_lng\": {\"latitude\": 501100000, \"longitude\": 86800000}}}]}]}");
        Wgs84Position position = routes.get(0).getPosition(0);
        assertEquals(50.11, position.getLatitude(), DELTA);
        assertEquals(8.68, position.getLongitude(), DELTA);
    }

    @Test
    public void tripsWithoutCoordinatesGiveNoRoute() throws IOException {
        assertEquals(0, read("{\"trips\": [{\"place_visit\": [{\"id\": \"\"}], \"transition\": []}]}").size());
    }

    @Test
    public void otherJsonIsDeclined() {
        assertThrows(IllegalArgumentException.class, () -> {
            read("{\"semanticSegments\": []}");
        });
    }

    @Test
    public void parserDetectsCommuteRoutes() throws IOException {
        File file = File.createTempFile("Routen für Pendelstrecke", ".json");
        try {
            Files.writeString(file.toPath(), COMMUTES);
            ParserResult result = new NavigationFormatParser(new NavigationFormatRegistry()).read(file);

            assertTrue(result.isSuccessful());
            assertEquals(GoogleCommuteRoutesFormat.class, result.getFormat().getClass());
            assertEquals(2, result.getTheRoute().getPositionCount());
        } finally {
            assertTrue(file.delete());
        }
    }
}
