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

package slash.navigation.copilot;

import org.junit.Test;
import slash.navigation.base.Wgs84Position;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;

public class CoPilotFormatTest {
    private CoPilot6Format format = new CoPilot6Format();

    @Test
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

    @Test
    public void testParsePosition() {
        Map<String,String> map = new HashMap<>();
            map.put("Longitude", "11753270");
        map.put("Latitude", "47688350");

        Wgs84Position position1 = format.parsePosition(map);
        assertNotNull(position1);
        assertDoubleEquals(11.75327, position1.getLongitude());
        assertDoubleEquals(47.68835, position1.getLatitude());
        assertNull(position1.getElevation());
        assertNull(position1.getTime());
        assertNull(position1.getDescription());

        map.put("City", "Innsbruck");
        map.put("County","Tirol");

        Wgs84Position position2 = format.parsePosition(map);
        assertNotNull(position2);
        assertEquals("Innsbruck, Tirol", position2.getDescription());

        map.put("State","A");

        Wgs84Position position3 = format.parsePosition(map);
        assertNotNull(position3);
        assertEquals("A Innsbruck, Tirol", position3.getDescription());

        map.put("Address","39 Gumppstrasse");
        map.put("Zip","6020");

        Wgs84Position position4 = format.parsePosition(map);
        assertNotNull(position4);
        assertEquals("A-6020 Innsbruck, Tirol, 39 Gumppstrasse", position4.getDescription());
    }

    @Test
    public void testIsDataVersion() {
        CoPilot6Format coPilot6Format = new CoPilot6Format();
        assertTrue(coPilot6Format.isDataVersion("Data Version=6.0.0.27"));
        assertFalse(coPilot6Format.isDataVersion("Data Version:6.0.0.27"));
        assertFalse(coPilot6Format.isDataVersion("Data Version=7.0.0.27"));
        assertFalse(coPilot6Format.isDataVersion("Data Version=1.0.0.27"));
        assertFalse(coPilot6Format.isDataVersion("Data Version=2.0.0.27"));

        CoPilot7Format coPilot7Format = new CoPilot7Format();
        assertTrue(coPilot7Format.isDataVersion("Data Version=7.0.0.27"));
        assertFalse(coPilot7Format.isDataVersion("Data Version:7.0.0.27"));
        assertFalse(coPilot7Format.isDataVersion("Data Version=6.0.0.27"));
        assertFalse(coPilot7Format.isDataVersion("Data Version=8.0.0.27"));

        CoPilot8Format coPilot8Format = new CoPilot8Format();
        assertTrue(coPilot8Format.isDataVersion("Data Version:1.13.5.2"));
        assertFalse(coPilot8Format.isDataVersion("Data Version=1.13.5.2"));
        assertFalse(coPilot8Format.isDataVersion("Data Version=2.13.5.2"));

        CoPilot9Format coPilot9Format = new CoPilot9Format();
        assertTrue(coPilot9Format.isDataVersion("Data Version:2.14.6.1"));
        assertFalse(coPilot9Format.isDataVersion("Data Version=1.14.6.1"));
        assertFalse(coPilot9Format.isDataVersion("Data Version=2.14.6.1"));
    }
}
