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
package slash.navigation.simple;

import slash.navigation.base.NavigationTestCase;
import slash.navigation.base.Wgs84Position;

public class Route66FormatTest extends NavigationTestCase {
    Route66Format format = new Route66Format();

    public void testIsPosition() {
        assertTrue(format.isPosition("11.107167,49.375783,\"HOLSTEINBRUCH BEI WORZELDORF B - GC13VV5\""));

        assertFalse(format.isPosition("8.6180900,50.2175100,\"[61352] AH Kreissl GmbH; Benzstraße 7 [Bad Homburg]\""));
        assertFalse(format.isPosition(" 9.3900000 , 51.5037800 , \"[34369] Donig; Max-Eyth-Str. [Hofgeismar]\" "));
    }

    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("11.107167,49.375783,\"HOLSTEINBRUCH BEI WORZELDORF B - GC13VV5\"", null);
        assertEquals(11.107167, position.getLongitude());
        assertEquals(49.375783, position.getLatitude());
        assertEquals("Holsteinbruch Bei Worzeldorf B - Gc13vv5", position.getDescription());
    }

    public void testParseNegativePosition() {
        Wgs84Position position = format.parsePosition("-8.6180900,-50.2175100,\"ABC\"", null);
        assertEquals(-8.618090, position.getLongitude());
        assertEquals(-50.217510, position.getLatitude());
        assertEquals("Abc", position.getDescription());
    }
}
