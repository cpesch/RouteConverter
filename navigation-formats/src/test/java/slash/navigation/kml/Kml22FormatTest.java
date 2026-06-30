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

package slash.navigation.kml;

import org.junit.Test;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;

public class Kml22FormatTest {
    private final Kml22Format format = new Kml22Format();

    @Test
    public void testAsPositions() {
        List<String> strings = asList("151.2393322528181,-33.59862693992532,0",
                "151.2274390264927,-33.59631160091919,1.5");
        List<KmlPosition> positions = format.asKmlPositions(strings);
        assertEquals(2, positions.size());
        KmlPosition position1 = positions.get(0);
        assertDoubleEquals(151.2393322528181, position1.getLongitude());
        assertDoubleEquals(-33.59862693992532, position1.getLatitude());
        assertDoubleEquals(0.0, position1.getElevation());
        KmlPosition position2 = positions.get(1);
        assertDoubleEquals(151.2274390264927, position2.getLongitude());
        assertDoubleEquals(-33.59631160091919, position2.getLatitude());
        assertDoubleEquals(1.5, position2.getElevation());
    }

    @Test
    public void testPointCoordinates() throws Exception {
        String string = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "<Document><Placemark><Point>\n" +
                "<coordinates>151.2393322528181, -33.59862693992532, 0 \n" +
                "</coordinates>\n" +
                "</Point></Placemark></Document></kml>";
        ParserContext<KmlRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(string.getBytes()), context);
        List<KmlRoute> routes = context.getRoutes();
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(1, route.getPositionCount());
        KmlPosition position = route.getPositions().get(0);
        assertDoubleEquals(151.2393322528181, position.getLongitude());
        assertDoubleEquals(-33.59862693992532, position.getLatitude());
        assertNull(position.getSpeed());
        assertDoubleEquals(0.0, position.getElevation());
    }

    @Test
    public void testPointCoordinatesWithoutSpaces() throws Exception {
        String string = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "<Document><Placemark><Point>\n" +
                "<coordinates>151.2393322528181,-33.59862693992532,0\n" +
                "</coordinates>\n" +
                "</Point></Placemark></Document></kml>";
        ParserContext<KmlRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(string.getBytes()), context);
        List<KmlRoute> routes = context.getRoutes();
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(1, route.getPositionCount());
        KmlPosition position = route.getPositions().get(0);
        assertDoubleEquals(151.2393322528181, position.getLongitude());
        assertDoubleEquals(-33.59862693992532, position.getLatitude());
        assertNull(position.getSpeed());
        assertDoubleEquals(0.0, position.getElevation());
    }

    @Test
    public void testLineStringCoordinates() throws Exception {
        String string = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "<Document><Placemark><LineString>\n" +
                "<coordinates>151.2393322528181, -33.59862693992532, 0 \n" +
                "151.2274390264927, -33.59631160091919, 0 \n\n" +
                "151.2179531903903, -33.59844652615273, 0 \n\n" +
                "</coordinates>\n" +
                "</LineString></Placemark></Document></kml>";
        ParserContext<KmlRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(string.getBytes()), context);
        List<KmlRoute> routes = context.getRoutes();
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(3, route.getPositionCount());
        KmlPosition position = route.getPositions().get(1);
        assertDoubleEquals(151.2274390264927, position.getLongitude());
        assertDoubleEquals(-33.59631160091919, position.getLatitude());
        assertNull(position.getSpeed());
        assertDoubleEquals(0.0, position.getElevation());
    }

    @Test
    public void testLineStringCoordinatesWithoutSpaces() throws Exception {
        String string = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "<Document><Placemark><LineString>\n" +
                "<coordinates>151.2393322528181,-33.59862693992532,0\n" +
                "151.2274390264927,-33.59631160091919,0\n" +
                "151.2179531903903,-33.59844652615273,0\n" +
                "</coordinates>\n" +
                "</LineString></Placemark></Document></kml>";
        ParserContext<KmlRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(string.getBytes()), context);
        List<KmlRoute> routes = context.getRoutes();
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(3, route.getPositionCount());
        KmlPosition position = route.getPositions().get(1);
        assertDoubleEquals(151.2274390264927, position.getLongitude());
        assertDoubleEquals(-33.59631160091919, position.getLatitude());
        assertNull(position.getSpeed());
        assertDoubleEquals(0.0, position.getElevation());
    }

    // ---- write/read round-trips (exercise the write path) ----

    private List<KmlRoute> readKml(String kml) throws Exception {
        ParserContext<KmlRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(kml.getBytes()), context);
        return context.getRoutes();
    }

    private String writeKml(List<KmlRoute> routes) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        format.write(routes, outputStream);
        return outputStream.toString();
    }

    @Test
    public void testWriteReadLineStringRoundTrip() throws Exception {
        String kml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "<Document><name>round-trip</name><Placemark><name>track</name><LineString>\n" +
                "<coordinates>11.1,48.1,100\n11.2,48.2,200\n11.3,48.3,300\n</coordinates>\n" +
                "</LineString></Placemark></Document></kml>";
        List<KmlRoute> first = readKml(kml);
        assertEquals(1, first.size());
        assertEquals(3, first.get(0).getPositionCount());

        String written = writeKml(first);
        assertTrue("writes KML 2.2 namespace", written.contains("http://www.opengis.net/kml/2.2"));

        List<KmlRoute> second = readKml(written);
        assertEquals(1, second.size());
        KmlRoute route = second.get(0);
        assertEquals(3, route.getPositionCount());
        KmlPosition position = route.getPositions().get(1);
        assertDoubleEquals(11.2, position.getLongitude());
        assertDoubleEquals(48.2, position.getLatitude());
        assertDoubleEquals(200.0, position.getElevation());
    }

    @Test
    public void testWriteReadWaypointsRoundTrip() throws Exception {
        String kml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "<Document>\n" +
                "<Placemark><name>first</name><Point><coordinates>11.1,48.1,10</coordinates></Point></Placemark>\n" +
                "<Placemark><name>second</name><Point><coordinates>11.2,48.2,20</coordinates></Point></Placemark>\n" +
                "</Document></kml>";
        List<KmlRoute> written = readKml(writeKml(readKml(kml)));
        assertEquals(1, written.size());
        KmlRoute route = written.get(0);
        assertEquals(2, route.getPositionCount());
        assertDoubleEquals(11.1, route.getPositions().get(0).getLongitude());
        assertDoubleEquals(48.2, route.getPositions().get(1).getLatitude());
        assertDoubleEquals(20.0, route.getPositions().get(1).getElevation());
    }

    @Test
    public void testWriteReadPreservesRouteName() throws Exception {
        String kml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "<Document><name>my route</name><Placemark><LineString>\n" +
                "<coordinates>11.1,48.1,0\n11.2,48.2,0\n</coordinates>\n" +
                "</LineString></Placemark></Document></kml>";
        List<KmlRoute> second = readKml(writeKml(readKml(kml)));
        assertEquals(1, second.size());
        assertEquals("my route", second.get(0).getName());
    }

    // ---- nested Folder/Document recursion (shared via KmlFormat.extractTracksFromContainers) ----

    @Test
    public void testNestedFolderIsRecursedInto() throws Exception {
        String kml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "<Document><Folder><name>outer</name><Folder><name>inner</name>\n" +
                "<Placemark><LineString><coordinates>11.1,48.1,0\n11.2,48.2,0\n</coordinates></LineString></Placemark>\n" +
                "</Folder></Folder></Document></kml>";
        List<KmlRoute> routes = readKml(kml);
        assertEquals(1, routes.size());
        assertTrue("nested folder path must be in the route name", routes.get(0).getName().contains("outer") && routes.get(0).getName().contains("inner"));
        assertEquals(2, routes.get(0).getPositionCount());
    }

    @Test
    public void testSpeedAndMarksFoldersAreSkipped() throws Exception {
        // KML 2.2 ignores its own internally-generated "Speed [Km/h]" and "Marks [Km]" folders
        String kml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "<Document>\n" +
                "<Folder><name>Speed [Km/h]</name><Placemark><Point><coordinates>1.0,1.0,0</coordinates></Point></Placemark></Folder>\n" +
                "<Folder><name>Marks [Km]</name><Placemark><Point><coordinates>2.0,2.0,0</coordinates></Point></Placemark></Folder>\n" +
                "<Placemark><LineString><coordinates>11.1,48.1,0\n11.2,48.2,0\n</coordinates></LineString></Placemark>\n" +
                "</Document></kml>";
        List<KmlRoute> routes = readKml(kml);
        assertEquals("only the real track, the Speed/Marks folders are ignored", 1, routes.size());
        assertEquals(2, routes.get(0).getPositionCount());
    }
}
