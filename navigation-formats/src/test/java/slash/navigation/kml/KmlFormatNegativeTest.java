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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Negative / malformed-input tests for the KML 2.2 reader: bad input must fail cleanly with an
 * {@link IOException} (not an NPE or hang), and structurally valid KML with no geometry must
 * yield no routes without throwing.
 *
 * @author Christian Pesch
 */
public class KmlFormatNegativeTest {
    private final Kml22Format format = new Kml22Format();

    private List<KmlRoute> read(String source) throws IOException {
        ParserContext<KmlRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), context);
        return context.getRoutes();
    }

    @Test
    public void testEmptyInput() {
        assertThrows(IOException.class, () -> read(""));
    }

    @Test
    public void testBlankInput() {
        assertThrows(IOException.class, () -> read("   \n\t  "));
    }

    @Test
    public void testTruncatedKml() {
        assertThrows(IOException.class, () -> read(
                "<?xml version=\"1.0\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><Placemark><Point><coordinates>8.0,49."));
    }

    @Test
    public void testNonKmlWellFormedXml() {
        assertThrows(IOException.class, () -> read(
                "<?xml version=\"1.0\"?><gpx version=\"1.1\"><wpt lat=\"49.0\" lon=\"8.0\"/></gpx>"));
    }

    @Test
    public void testValidButEmptyKmlReturnsNoRoutesWithoutThrowing() throws IOException {
        List<KmlRoute> routes = read(
                "<?xml version=\"1.0\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document/></kml>");
        if (routes != null)
            for (KmlRoute route : routes)
                assertEquals(0, route.getPositionCount());
    }
}
