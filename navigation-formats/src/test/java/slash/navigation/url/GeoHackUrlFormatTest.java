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
import java.util.Map;

import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.base.RouteCalculations.asWgs84Position;

public class GeoHackUrlFormatTest {
    private static final String INPUT1 = "https://geohack.toolforge.org/geohack.php?params=57.265278055556_N_13.604166944444_E_region:SE-F_type:landmark&amp;pagename=Scandinavian_Raceway&amp;language=de";
    private static final String INPUT2 = "https://geohack.toolforge.org/geohack.php?params=12.34_N_56.78_E_region:yes_type:a&amp;pagename=Position_A&params=12.34_S_56.78_W_region:yes_type:a&amp;pagename=Position+B&amp;language=de";

    private final GeoHackUrlFormat format = new GeoHackUrlFormat();

    @Test
    public void testFindURL() {
        String url = format.findURL(INPUT1);
        assertNotNull(url);
        assertTrue(url.startsWith("params=57.26527"));
        assertNull(format.findURL("don't care"));
    }

    private List<Wgs84Position> parsePositions(String text) {
        String url = format.findURL(text);
        Map<String, List<String>> parameters = format.parseURLParameters(url, "UTF-8");
        return format.parsePositions(parameters);
    }

    @Test
    public void testParsePosition() {
        List<Wgs84Position> positions = parsePositions(INPUT1);
        assertNotNull(positions);
        assertEquals(1, positions.size());
        Wgs84Position position = positions.get(0);
        assertDoubleEquals(13.604166944444, position.getLongitude());
        assertDoubleEquals(57.265278055556, position.getLatitude());
        assertEquals("Scandinavian Raceway", position.getDescription());
    }

    @Test
    public void testParsePositions() {
        List<Wgs84Position> positions = parsePositions(INPUT2);
        assertNotNull(positions);
        assertEquals(2, positions.size());
        Wgs84Position position0 = positions.get(0);
        assertDoubleEquals(56.78, position0.getLongitude());
        assertDoubleEquals(12.34, position0.getLatitude());
        assertEquals("Position A", position0.getDescription());
        Wgs84Position position1 = positions.get(1);
        assertDoubleEquals(-56.78, position1.getLongitude());
        assertDoubleEquals(-12.34, position1.getLatitude());
        assertEquals("Position B", position1.getDescription());
    }

    @Test
    public void testParseLegacyUrl() {
        List<Wgs84Position> positions = parsePositions("https://tools.wmflabs.org/geohack/geohack.php?language=it&pagename=Spoleto&params=42.733333_N_12.733333_E_type:adm3rd_scale:1000000&title=Spoleto");
        assertNotNull(positions);
        assertEquals(1, positions.size());
        Wgs84Position position = positions.get(0);
        assertDoubleEquals(12.733333, position.getLongitude());
        assertDoubleEquals(42.733333, position.getLatitude());
        assertEquals("Spoleto", position.getDescription());
    }

    @Test
    public void testCreateURL() {
        List<Wgs84Position> positions = new ArrayList<>();
        positions.add(asWgs84Position(13.604166944444, 57.265278055556, "Scandinavian Raceway"));
        String expected = "http://geohack.toolforge.org/geohack.php?params=57.265278055556_N_13.604166944444_E_&pagename=Scandinavian_Raceway";
        String actual = format.createURL(positions, 0, positions.size());
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateURLWithMoreThanOnePosition() {
        List<Wgs84Position> positions = new ArrayList<>();
        positions.add(asWgs84Position(56.78, 12.34, "Position A"));
        positions.add(asWgs84Position(-56.78, -12.34, "Position B"));
        String expected = "http://geohack.toolforge.org/geohack.php?params=12.34_N_56.78_E_&pagename=Position_A&params=12.34_S_56.78_W_&pagename=Position_B";
        String actual = format.createURL(positions, 0, positions.size());
        assertEquals(expected, actual);
    }
}
