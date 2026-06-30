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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;

public class Kml22BetaFormatTest {
    private final Kml22BetaFormat format = new Kml22BetaFormat();

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
    public void testLineStringCoordinates() throws Exception {
        String kml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://earth.google.com/kml/2.2\">\n" +
                "<Document><Placemark><LineString>\n" +
                "<coordinates>11.1,48.1,0\n11.2,48.2,0\n11.3,48.3,0\n</coordinates>\n" +
                "</LineString></Placemark></Document></kml>";
        List<KmlRoute> routes = readKml(kml);
        assertEquals(1, routes.size());
        assertEquals(3, routes.get(0).getPositionCount());
        KmlPosition position = routes.get(0).getPositions().get(1);
        assertDoubleEquals(11.2, position.getLongitude());
        assertDoubleEquals(48.2, position.getLatitude());
        assertNull(position.getSpeed());
    }

    @Test
    public void testWriteReadLineStringRoundTrip() throws Exception {
        String kml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://earth.google.com/kml/2.2\">\n" +
                "<Document><name>round-trip</name><Placemark><name>track</name><LineString>\n" +
                "<coordinates>11.1,48.1,100\n11.2,48.2,200\n11.3,48.3,300\n</coordinates>\n" +
                "</LineString></Placemark></Document></kml>";
        List<KmlRoute> first = readKml(kml);
        assertEquals(1, first.size());
        assertEquals(3, first.get(0).getPositionCount());

        String written = writeKml(first);
        assertTrue("writes KML 2.2-beta namespace", written.contains("earth.google.com/kml/2.2"));

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
    public void testWriteReadPreservesRouteName() throws Exception {
        String kml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://earth.google.com/kml/2.2\">\n" +
                "<Document><name>my route</name><Placemark><LineString>\n" +
                "<coordinates>11.1,48.1,0\n11.2,48.2,0\n</coordinates>\n" +
                "</LineString></Placemark></Document></kml>";
        List<KmlRoute> second = readKml(writeKml(readKml(kml)));
        assertEquals(1, second.size());
        assertTrue(second.get(0).getName().contains("my route"));
    }
}
