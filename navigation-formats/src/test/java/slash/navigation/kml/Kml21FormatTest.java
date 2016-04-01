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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static slash.common.TestCase.assertDoubleEquals;

public class Kml21FormatTest {
    private Kml21Format format = new Kml21Format();

    @Test
    public void testPointCoordinates() throws Exception {
        String string = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://earth.google.com/kml/2.1\">\n" +
                "<Document><Placemark><Point>\n" +
                "<coordinates>151.2393322528181, -33.59862693992532, 0\n" +
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
                "<kml xmlns=\"http://earth.google.com/kml/2.1\">\n" +
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
}