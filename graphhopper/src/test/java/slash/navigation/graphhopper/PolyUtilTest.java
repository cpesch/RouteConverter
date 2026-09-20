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
package slash.navigation.graphhopper;

import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.Polygon;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class PolyUtilTest {
    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(UTF_8));
    }

    private static String ring(String id, double west, double south, double east, double north) {
        return id + "\n" +
                "   " + west + "   " + south + "\n" +
                "   " + east + "   " + south + "\n" +
                "   " + east + "   " + north + "\n" +
                "   " + west + "   " + north + "\n" +
                "   " + west + "   " + south + "\n" +
                "END\n";
    }

    @Test
    public void parsesASingleRing() {
        Polygon polygon = PolyUtil.parse(stream("none\n" + ring("1", 0, 0, 1, 1) + "END\n"));

        assertNotNull(polygon);
        assertEquals(1, polygon.rings().size());
        assertEquals(5, polygon.rings().get(0).size());
        assertEquals(polygon.rings().get(0).get(0), polygon.rings().get(0).get(4));
        assertFalse(polygon.holes().get(0));
    }

    @Test
    public void parsesMultipleRings() {
        Polygon polygon = PolyUtil.parse(stream("none\n" + ring("1", 0, 0, 1, 1) + ring("2", 5, 5, 6, 6) + "END\n"));

        assertNotNull(polygon);
        assertEquals(2, polygon.rings().size());
        assertFalse(polygon.holes().get(1));
        assertEquals(new BoundingBox(6.0, 6.0, 0.0, 0.0), polygon.boundingBox());
    }

    @Test
    public void parsesAHoleRing() {
        Polygon polygon = PolyUtil.parse(stream("none\n" + ring("1", 0, 0, 10, 10) + ring("!2", 4, 4, 6, 6) + "END\n"));

        assertNotNull(polygon);
        assertFalse(polygon.holes().get(0));
        assertTrue(polygon.holes().get(1));
    }

    @Test
    public void parsesScientificNotation() {
        Polygon polygon = PolyUtil.parse(stream("none\n1\n" +
                "   9.326076E-01   5.057032E+01\n" +
                "   1.0E+00   5.0E+01\n" +
                "   1.0E+00   5.1E+01\n" +
                "END\nEND\n"));

        assertNotNull(polygon);
        NavigationPosition first = polygon.rings().get(0).get(0);
        // longitude first, then latitude
        assertEquals(0.9326076, first.getLongitude(), 0.0000001);
        assertEquals(50.57032, first.getLatitude(), 0.0000001);
    }

    @Test
    public void returnsNullForAnEmptyFile() {
        assertNull(PolyUtil.parse(stream("")));
        // asia/mongolia.poly is a HTTP 200 with a body of one byte
        assertNull(PolyUtil.parse(stream("\n")));
    }

    @Test
    public void returnsNullForATruncatedFile() {
        assertNull(PolyUtil.parse(stream("none\n1\n0 0\n1 0\n1 1\n")));
        assertNull(PolyUtil.parse(stream("none\n" + ring("1", 0, 0, 1, 1))));
    }

    @Test
    public void returnsNullForGarbage() {
        assertNull(PolyUtil.parse(stream("<html>\n<head><title>404 Not Found</title></head>\n<body>\n<h1>Not Found</h1>\n</body>\n</html>\n")));
        assertNull(PolyUtil.parse(stream("none\n1\nabc def\nEND\nEND\n")));
        assertNull(PolyUtil.parse(stream("none\n1\n0 0 0\n1 0 0\n1 1 0\nEND\nEND\n")));
    }

    @Test
    public void realWorldDenmarkPolygonExcludesTheBalticTile() throws Exception {
        Polygon polygon;
        try (InputStream inputStream = getClass().getResourceAsStream("denmark.poly")) {
            polygon = PolyUtil.parse(inputStream);
        }
        assertNotNull(polygon);

        // the extract of Denmark spans 7.7..15.65 E and 54.44..58.06 N, a rectangle full of Baltic and Sweden
        BoundingBox baltic = new BoundingBox(15.5, 57.5, 14.5, 56.5);
        BoundingBox jutland = new BoundingBox(10.0, 57.0, 9.0, 56.0);
        assertNotNull(polygon.boundingBox().intersect(baltic));
        assertFalse(polygon.intersects(baltic));
        assertTrue(polygon.intersects(jutland));
    }
}
