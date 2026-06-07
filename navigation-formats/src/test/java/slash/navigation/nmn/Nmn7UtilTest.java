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

package slash.navigation.nmn;

import org.junit.Test;
import slash.navigation.nmn.binding7.Route;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link Nmn7Util} unmarshal/marshal round-trips.
 */
public class Nmn7UtilTest {

    private static final String NMN7_XML =
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +
            "<Route>" +
            "<Name>Hamburg to Berlin</Name>" +
            "<Point><Name>Hamburg</Name><x>10.050</x><y>53.570</y></Point>" +
            "<Point><Name>Berlin</Name><x>13.404</x><y>52.520</y></Point>" +
            "</Route>";

    // --- unmarshal ---

    @Test
    public void testUnmarshalReturnsNonNull() throws IOException {
        Route route = Nmn7Util.unmarshal(
                new ByteArrayInputStream(NMN7_XML.getBytes(Charset.forName("ISO-8859-1"))));
        assertNotNull(route);
    }

    @Test
    public void testUnmarshalRouteName() throws IOException {
        Route route = Nmn7Util.unmarshal(
                new ByteArrayInputStream(NMN7_XML.getBytes(Charset.forName("ISO-8859-1"))));
        assertEquals("Hamburg to Berlin", route.getName());
    }

    @Test
    public void testUnmarshalPointCount() throws IOException {
        Route route = Nmn7Util.unmarshal(
                new ByteArrayInputStream(NMN7_XML.getBytes(Charset.forName("ISO-8859-1"))));
        assertEquals(2, route.getPoint().size());
    }

    @Test
    public void testUnmarshalFirstPointName() throws IOException {
        Route route = Nmn7Util.unmarshal(
                new ByteArrayInputStream(NMN7_XML.getBytes(Charset.forName("ISO-8859-1"))));
        assertEquals("Hamburg", route.getPoint().get(0).getName());
    }

    @Test
    public void testUnmarshalFirstPointCoordinates() throws IOException {
        Route route = Nmn7Util.unmarshal(
                new ByteArrayInputStream(NMN7_XML.getBytes(Charset.forName("ISO-8859-1"))));
        Route.Point p = route.getPoint().get(0);
        assertEquals(new BigDecimal("10.050"), p.getX());
        assertEquals(new BigDecimal("53.570"), p.getY());
    }

    // --- marshal ---

    @Test
    public void testMarshalProducesNonEmptyOutput() throws Exception {
        Route route = buildMinimalRoute("Test Route");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Nmn7Util.marshal(route, out);
        String xml = out.toString("ISO-8859-1");
        assertNotNull(xml);
        assert xml.contains("Test Route") : "Output should contain route name";
    }

    // --- round-trip ---

    @Test
    public void testRoundTrip() throws Exception {
        Route original = buildMinimalRoute("My Route");
        Route.Point point = new Route.Point();
        point.setName("Munich");
        point.setX(new BigDecimal("11.576"));
        point.setY(new BigDecimal("48.137"));
        original.getPoint().add(point);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Nmn7Util.marshal(original, out);

        Route roundtripped = Nmn7Util.unmarshal(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("My Route", roundtripped.getName());
        assertEquals("Munich", roundtripped.getPoint().get(0).getName());
    }

    private static Route buildMinimalRoute(String name) {
        Route route = new Route();
        route.setName(name);
        return route;
    }
}

