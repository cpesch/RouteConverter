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

package slash.navigation.url;

import org.junit.Test;
import slash.navigation.base.Wgs84Position;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.base.RouteCalculations.asWgs84Position;

public class MotoPlanerUrlFormatTest {
    private static final String INPUT1 = "http://www.motoplaner.de/#32.64013,-16.85148;1,32.70498,-16.8338;1,32.70624,-16.88255;1,32.66431,-16.86777;1,32.68353,-16.90509;1,32.67702,-16.94549;1,32.65959,-16.96608;1,32.74285,-17.02466;1,32.72115,-17.11077;1,32.72183,-17.15622;1,32.78202,-17.17759;1,32.86656,-17.17075;1,32.774,-16.87225;1,32.76274,-16.86161;1,32.74713,-16.82939;1,32.69222,-16.78324;1,32.64031,-16.85144;1&&1";

    private MotoPlanerUrlFormat format = new MotoPlanerUrlFormat();

    @Test
    public void testFindURL() {
        String url = format.findURL(INPUT1);
        assertNotNull(url);
        assertTrue(url.startsWith("32.64013"));
        assertNull(format.findURL("don't care"));
    }

    @Test
    public void testParsePositions() {
        String url = format.findURL(INPUT1);
        List<Wgs84Position> positions = format.parsePositions(url);
        assertNotNull(positions);
        assertEquals(17, positions.size());
        Wgs84Position position1 = positions.get(1);
        assertDoubleEquals(-16.8338, position1.getLongitude());
        assertDoubleEquals(32.70498, position1.getLatitude());
        Wgs84Position position3 = positions.get(3);
        assertDoubleEquals(-16.86777, position3.getLongitude());
        assertDoubleEquals(32.66431, position3.getLatitude());
    }

    @Test
    public void testCreateURL() {
        List<Wgs84Position> positions = new ArrayList<>();
        positions.add(asWgs84Position(10.02571156, 53.57497745));
        positions.add(asWgs84Position(10.20026067, 53.57662034));
        positions.add(asWgs84Position(10.35735078, 53.59171021));
        positions.add(asWgs84Position(10.45696089, 53.64781001));
        String expected = "http://www.motoplaner.de/#53.574977,10.025711;1,53.576620,10.200260;1,53.591710,10.357350;1,53.647810,10.456960;1";
        String actual = format.createURL(positions, 0, positions.size());
        assertEquals(expected, actual);
    }
}
