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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.copilot;

import slash.navigation.NavigationTestCase;
import slash.navigation.Wgs84Position;

import java.util.HashMap;
import java.util.Map;

public class CoPilotFormatTest extends NavigationTestCase {
    CoPilot6Format format = new CoPilot6Format();

    public void testIsValidLine() {
        assertTrue(format.isNameValue("Data Version=6.0.0.27"));
        assertTrue(format.isNameValue("Start Stop=Stop 0"));
        assertTrue(format.isNameValue("Start StopOpt=Stop 3"));
        assertTrue(format.isNameValue("Memo="));

        assertFalse(format.isNameValue("End Trip"));
        assertFalse(format.isNameValue("End Stop"));
        assertFalse(format.isNameValue("End StopOpt"));
        assertFalse(format.isNameValue("Egal"));
    }

    public void testParsePosition() {
        Map<String,String> map = new HashMap<String,String>();
            map.put("Longitude", "11753270");
        map.put("Latitude", "47688350");

        Wgs84Position position1 = format.parsePosition(map);
        assertNotNull(position1);
        assertEquals(11.75327, position1.getLongitude());
        assertEquals(47.68835, position1.getLatitude());
        assertNull(position1.getElevation());
        assertNull(position1.getTime());
        assertNull(position1.getComment());

        map.put("City", "Innsbruck");
        map.put("County","Tirol");

        Wgs84Position position2 = format.parsePosition(map);
        assertNotNull(position2);
        assertEquals("Innsbruck, Tirol", position2.getComment());

        map.put("State","A");

        Wgs84Position position3 = format.parsePosition(map);
        assertNotNull(position3);
        assertEquals("A Innsbruck, Tirol", position3.getComment());

        map.put("Address","39 Gumppstrasse");
        map.put("Zip","6020");

        Wgs84Position position4 = format.parsePosition(map);
        assertNotNull(position4);
        assertEquals("A-6020 Innsbruck, Tirol, 39 Gumppstrasse", position4.getComment());
    }
}
