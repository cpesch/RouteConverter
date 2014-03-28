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
package slash.navigation.common;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.common.PositionParser.isPosition;
import static slash.navigation.common.PositionParser.parseExtensionPositions;
import static slash.navigation.common.PositionParser.parsePosition;
import static slash.navigation.common.PositionParser.parsePositions;

public class PositionParserTest {

    @Test
    public void testIsPosition() {
        assertTrue(isPosition("5.783245,50.28655,512"));
        assertTrue(isPosition("-0.203733,51.47185,512"));
        assertTrue(isPosition("151.777,-32.88815,512"));
        assertTrue(isPosition("-0.203733,-32.88815,512"));
        assertTrue(isPosition("-0.203733,-32.88815,-0.1"));
        assertTrue(isPosition("+0.203733,+32.88815,+0.1"));
        assertTrue(isPosition("10.032004,53.569488,64296162722.124001"));
        assertTrue(isPosition("10.244109,53.571977,0.000000"));
        assertTrue(isPosition(" 10.244109,53.571977,0.000000"));
        assertTrue(isPosition("10.244109,53.571977,0.000000 "));
        assertTrue(isPosition("10.244109, 53.571977, 0.000000"));
        assertTrue(isPosition(" 10.244109 , 53.571977 , 0.000000 "));
        assertTrue(isPosition("\n132.927856\n,\n34.44434\n,\n332.75\n"));
        assertTrue(isPosition("\t\t\t\t8.33687,46.90742,1436"));
        assertTrue(isPosition(",,0"));
        assertTrue(isPosition("11.5709833333333,49.9467027777778"));
        assertTrue(isPosition("0.2E-4,-0.2E-5,0.3E-6"));
    }

    @Test
    public void testParseNullPosition() {
        NavigationPosition position = parsePosition(",,0", null);
        assertNull(position.getLongitude());
        assertNull(position.getLatitude());
        assertNull(position.getDescription());
        assertDoubleEquals(0.0, position.getElevation());
    }

    @Test
    public void testParseNoElevationPosition() {
        NavigationPosition position = parsePosition("11.5709833333333,49.9467027777778", null);
        assertDoubleEquals(11.5709833333333, position.getLongitude());
        assertDoubleEquals(49.9467027777778, position.getLatitude());
        assertNull(position.getElevation());
        assertNull(position.getDescription());
    }

    @Test
    public void testParseFloatElevationPosition() {
        NavigationPosition position = parsePosition("13.383570,54.096930,0.000000", "description");
        assertDoubleEquals(13.383570, position.getLongitude());
        assertDoubleEquals(54.096930, position.getLatitude());
        assertDoubleEquals(0.0, position.getElevation());
        Assert.assertEquals("description", position.getDescription());
    }

    @Test
    public void testParseScientificNumberPosition() {
        NavigationPosition position = parsePosition("0.1E-4,-0.2E-5,0.3E-6", null);
        assertDoubleEquals(0.00001, position.getLongitude());
        assertDoubleEquals(-0.000002, position.getLatitude());
        assertDoubleEquals(0.0000003, position.getElevation());
        assertNull(position.getDescription());
    }

    @Test
    public void testParsePositions() {
        List<NavigationPosition> expected = new ArrayList<NavigationPosition>(asList(new SimpleNavigationPosition(1.1, 2.0, 3.0, null), new SimpleNavigationPosition(4.0, 5.0, 6.6, null), new SimpleNavigationPosition(7.0, 8.8, 9.0, null)));
        assertEquals(expected, parsePositions("1.1,2,3 4,5,6.6 7,8.8,9"));
        assertEquals(expected, parsePositions("1.1,2,3\t4,5,6.6\t7,8.8,9"));
        assertEquals(expected, parsePositions("1.1,2,3\n4,5,6.6\n7,8.8,9"));
    }

    @Test
    public void testParseGoogleExtensionPositions() {
        List<NavigationPosition> expected = new ArrayList<NavigationPosition>(asList(new SimpleNavigationPosition(1.1, 2.2, 3.3, null), new SimpleNavigationPosition(4.4, 5.5, 6.6, null), new SimpleNavigationPosition(7.7, 8.8, 9.9, null)));
        assertEquals(expected, parseExtensionPositions("1.1 2.2 3.3 4.4 5.5 6.6 7.7 8.8 9.9"));
    }

    @Test
    public void testParseGoogleExtensionPositionsWithColons() {
        List<NavigationPosition> expected = new ArrayList<NavigationPosition>(asList(new SimpleNavigationPosition(1.1, 2.2, 3.3, null), new SimpleNavigationPosition(4.4, 5.5, 6.6, null), new SimpleNavigationPosition(7.7, 8.8, 9.9, null)));
        assertEquals(expected, parseExtensionPositions("1.1,2.2,3.3 4.4,5.5,6.6,7.7 8.8,9.9"));
    }

    @Test
    public void testParsePositionsWithoutElevation() {
        List<NavigationPosition> expected = new ArrayList<NavigationPosition>(asList(new SimpleNavigationPosition(1.1, 2.0, null, null), new SimpleNavigationPosition(4.0, 5.0, null, null), new SimpleNavigationPosition(7.0, 8.8, null, null)));
        assertEquals(expected, parsePositions("1.1,2 4,5 7,8.8"));
        assertEquals(expected, parsePositions("1.1,2\t4,5\t7,8.8"));
        assertEquals(expected, parsePositions("1.1,2\n4,5\n7,8.8"));
    }
}                                                                          