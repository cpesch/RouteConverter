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

package slash.navigation.loc;

import org.junit.Test;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class GeoCachingFormatTest {
    private final GeoCachingFormat format = new GeoCachingFormat();

    private List<GpxRoute> read(String xml) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(xml.getBytes(UTF_8)), context);
        return context.getRoutes();
    }

    @Test
    public void readsTwoWaypoints() throws IOException {
        List<GpxRoute> routes = read("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<loc version=\"1.0\" src=\"EasyGPS\">\n" +
                "<waypoint><name id=\"GC1234\"><![CDATA[Cache one]]></name>" +
                "<coord lat=\"47.123456\" lon=\"11.654321\"/><type>Geocache</type>" +
                "<link text=\"Cache Details\">http://example.org</link>" +
                "<difficulty>2.5</difficulty><terrain>1.5</terrain><container>3</container></waypoint>\n" +
                "<waypoint><name id=\"GC5678\"><![CDATA[Cache two]]></name>" +
                "<coord lat=\"-1.5\" lon=\"-2.25\"/><unknown/></waypoint>\n" +
                "</loc>");
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(Waypoints, route.getCharacteristics());
        assertEquals(2, route.getPositionCount());
        GpxPosition first = route.getPosition(0);
        assertEquals("GC1234; Cache one", first.getDescription());
        assertEquals(47.123456, first.getLatitude(), 0.0000001);
        assertEquals(11.654321, first.getLongitude(), 0.0000001);
        assertEquals(0.0, first.getElevation(), 0.0);
        GpxPosition second = route.getPosition(1);
        assertEquals("GC5678; Cache two", second.getDescription());
        assertEquals(-1.5, second.getLatitude(), 0.0000001);
        assertEquals(-2.25, second.getLongitude(), 0.0000001);
    }

    @Test
    public void writesTwoPositions() throws IOException {
        List<GpxPosition> positions = asList(
                new GpxPosition(11.5, 47.25, null, null, null, "GC1; First cache"),
                new GpxPosition(-2.0, -1.0, 5.0, null, null, "Only name"));
        GpxRoute route = new GpxRoute(new Gpx10Format(), Waypoints, "name", null, positions);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, 2);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<loc version=\"1.0\" src=\"EasyGPS\">\n" +
                "<waypoint>\n" +
                "<name id=\"GC1\"><![CDATA[First cache]]></name>\n" +
                "<coord lat=\"47.250000\" lon=\"11.500000\"/>\n" +
                "<type></type>\n" +
                "</waypoint>\n" +
                "<waypoint>\n" +
                "<name id=\"Only name\"><![CDATA[]]></name>\n" +
                "<coord lat=\"-1.000000\" lon=\"-2.000000\"/>\n" +
                "<type></type>\n" +
                "</waypoint>\n" +
                "</loc>\n", new String(target.toByteArray(), UTF_8));
    }

    @Test
    public void writtenFileCanBeReadAgain() throws IOException {
        List<GpxPosition> positions = asList(new GpxPosition(11.5, 47.25, null, null, null, "GC1; First & <cache>"));
        GpxRoute route = new GpxRoute(new Gpx10Format(), Waypoints, "name", null, positions);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, 1);
        List<GpxRoute> routes = read(new String(target.toByteArray(), UTF_8));
        assertEquals(1, routes.size());
        assertEquals("GC1; First & <cache>", routes.get(0).getPosition(0).getDescription());
    }

    @Test
    public void nonLocXmlIsNoRoute() throws IOException {
        assertTrue(read("<?xml version=\"1.0\"?><gpx><wpt lat=\"1\" lon=\"2\"/></gpx>").isEmpty());
    }

    @Test
    public void malformedXmlIsNoRoute() throws IOException {
        assertTrue(read("<loc version=\"1.0\"><waypoint><name id=\"a\">x</name><coord lat=\"1\" lon=\"2\"/></waypoint").isEmpty());
        assertTrue(read("this is no xml at all").isEmpty());
        assertTrue(read("").isEmpty());
    }
}
