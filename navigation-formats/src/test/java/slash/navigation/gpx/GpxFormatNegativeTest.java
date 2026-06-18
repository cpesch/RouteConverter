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
package slash.navigation.gpx;

import org.junit.Test;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

/**
 * Negative / malformed-input tests for the GPX readers. GPX is the most-used format, yet was
 * covered almost entirely by happy-path roundtrips (and the broad roundtrip suite is an *IT*,
 * excluded from the gated unit build). These assert that bad input fails cleanly with an
 * {@link IOException} rather than an NPE or a hang, and that the core happy path still parses --
 * so a parsing regression fails the GATED unit build.
 *
 * @author Christian Pesch
 */
public class GpxFormatNegativeTest {

    private List<GpxRoute> read(GpxFormat format, String source) throws IOException {
        ParserContext<GpxRoute> context = new ParserContextImpl<>(null, null);
        format.read(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), context);
        return context.getRoutes();
    }

    @Test
    public void testEmptyInputGpx10() {
        assertThrows(IOException.class, () -> read(new Gpx10Format(), ""));
    }

    @Test
    public void testEmptyInputGpx11() {
        assertThrows(IOException.class, () -> read(new Gpx11Format(), ""));
    }

    @Test
    public void testBlankInputGpx11() {
        assertThrows(IOException.class, () -> read(new Gpx11Format(), "   \n\t  "));
    }

    @Test
    public void testTruncatedGpx11() {
        assertThrows(IOException.class, () -> read(new Gpx11Format(),
                "<?xml version=\"1.0\"?><gpx version=\"1.1\"><wpt lat=\"49.0\" lon=\"8."));
    }

    @Test
    public void testTruncatedGpx10() {
        assertThrows(IOException.class, () -> read(new Gpx10Format(),
                "<?xml version=\"1.0\"?><gpx version=\"1.0\"><wpt lat=\"49"));
    }

    @Test
    public void testNonGpxWellFormedXmlGpx11() {
        assertThrows(IOException.class, () -> read(new Gpx11Format(),
                "<?xml version=\"1.0\"?><html><body>not gpx</body></html>"));
    }

    @Test
    public void testNonGpxWellFormedXmlGpx10() {
        assertThrows(IOException.class, () -> read(new Gpx10Format(),
                "<?xml version=\"1.0\"?><kml><Document/></kml>"));
    }

    @Test
    public void testMalformedCoordinateGpx11() {
        assertThrows(IOException.class, () -> read(new Gpx11Format(),
                "<?xml version=\"1.0\"?><gpx version=\"1.1\"><wpt lat=\"not-a-number\" lon=\"8.0\"/></gpx>"));
    }

    @Test
    public void testValidButEmptyGpx11ReturnsNoRoutesWithoutThrowing() throws IOException {
        List<GpxRoute> routes = read(new Gpx11Format(),
                "<?xml version=\"1.0\"?><gpx version=\"1.1\" creator=\"test\" xmlns=\"http://www.topografix.com/GPX/1/1\"></gpx>");
        // no tracks/routes/waypoints -> no usable route, but no exception
        if (routes != null)
            for (GpxRoute route : routes)
                assertEquals(0, route.getPositionCount());
    }

    @Test
    public void testReadTinyGpx11Waypoint() throws IOException {
        List<GpxRoute> routes = read(new Gpx11Format(),
                "<?xml version=\"1.0\"?>" +
                        "<gpx version=\"1.1\" creator=\"test\" xmlns=\"http://www.topografix.com/GPX/1/1\">" +
                        "<wpt lat=\"49.123456\" lon=\"8.654321\"><ele>250.5</ele><name>home</name></wpt>" +
                        "</gpx>");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(1, route.getPositionCount());
        assertEquals(49.123456, route.getPosition(0).getLatitude(), 1e-9);
        assertEquals(8.654321, route.getPosition(0).getLongitude(), 1e-9);
        assertEquals(250.5, route.getPosition(0).getElevation(), 1e-9);
    }
}
