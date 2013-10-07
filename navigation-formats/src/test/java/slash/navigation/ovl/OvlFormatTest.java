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

package slash.navigation.ovl;

import slash.navigation.base.NavigationTestCase;
import slash.navigation.base.Wgs84Position;

public class OvlFormatTest extends NavigationTestCase {
    OvlFormat format = new OvlFormat();

    public void testIsSectionTitle() {
        assertTrue(format.isSectionTitle("[Symbol 1]"));
        assertTrue(format.isSectionTitle("[Symbol 12]"));
        assertTrue(format.isSectionTitle("[Symbol 123]"));
        assertTrue(format.isSectionTitle("[Symbol 1234]"));
        assertTrue(format.isSectionTitle("[Overlay]"));
        assertTrue(format.isSectionTitle("[MapLage]"));

        assertFalse(format.isSectionTitle(" [MapLage] "));
        assertFalse(format.isSectionTitle("[Symbol1]"));
        assertFalse(format.isSectionTitle("[Symbol A]"));

        assertFalse(format.isSectionTitle("[Egal]"));
        assertFalse(format.isSectionTitle("[CLIENT]"));
        assertFalse(format.isSectionTitle("[COORDINATES]"));
        assertFalse(format.isSectionTitle("[DESCRIPTION]"));
        assertFalse(format.isSectionTitle("[ROUTE]"));
    }

    public void testOvlSectionWithOnePositionFromGpsBabel() {
        OvlSection section = new OvlSection("Symbol 5");
        section.put("Typ","1");
        section.put("Group", "1");
        section.put("Width", "100");
        section.put("Height", "100");
        section.put("Dir", "100");
        section.put("Zoom", "2");
        section.put("Trans", "2");
        section.put("TransByte", "5");
        section.put("Path", "waypoint.bmp");
        section.put("XKoord", "13.41151290");
        section.put("YKoord", "52.52020790");
        assertEquals("waypoint.bmp", section.get("Path"));
        assertEquals(1, section.getPositionCount());
        Wgs84Position position = section.getPosition(0);
        assertEquals(13.41151290, position.getLongitude());
        assertEquals(52.52020790, position.getLatitude());
        assertNull(position.getDescription());
        assertNull(position.getElevation());
        assertNull(position.getTime());
    }

    public void testOvlSectionWithOnePositionFromEasyGps() {
        OvlSection section = new OvlSection("Symbol 78");
        section.put("Typ","3");
        section.put("Group", "1");
        section.put("Width", "40");
        section.put("Height", "40");
        section.put("Dir", "100");
        section.put("Col", "1");
        section.put("Zoom", "2");
        section.put("Size", "102");
        section.put("Area", "2");
        section.put("TransByte", "5");
        section.put("Path", "waypoint.bmp");
        section.put("XKoord", "13.4119419\" lat=\"52.520688");
        section.put("YKoord", "");
        assertEquals("1", section.get("Col"));
        assertEquals(1, section.getPositionCount());
        Wgs84Position position = section.getPosition(0);
        assertEquals(13.4119419, position.getLongitude());
        assertEquals(52.520688, position.getLatitude());
        assertNull(position.getDescription());
        assertNull(position.getElevation());
        assertNull(position.getTime());
    }

    public void testOvlSectionWithTwoPositionsFromGpsDings() {
        OvlSection section = new OvlSection("Symbol 1");
        section.put("Typ","3");
        section.put("Group", "2");
        section.put("Col", "1");
        section.put("Zoom", "1");
        section.put("Size", "105");
        section.put("Art", "1");
        section.put("Punkte", "2");
        section.put("XKoord0", "13.4115129");
        section.put("YKoord0", "52.5202079");
        section.put("XKoord1", "13.4119689");
        section.put("YKoord1", "52.5206319");
        assertEquals("1", section.get("Art"));
        assertEquals(2, section.getPositionCount());
        Wgs84Position position1 = section.getPosition(0);
        assertEquals(13.4115129, position1.getLongitude());
        assertEquals(52.5202079, position1.getLatitude());
        Wgs84Position position2 = section.getPosition(1);
        assertEquals(13.4119689, position2.getLongitude());
        assertEquals(52.5206319, position2.getLatitude());
    }
}
