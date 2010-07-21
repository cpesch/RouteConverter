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
package slash.navigation.nmn;

import slash.navigation.base.NavigationTestCase;
import slash.navigation.base.Wgs84Position;

public class NavigatingPoiWarnerFormatTest extends NavigationTestCase {
    NavigatingPoiWarnerFormat format = new NavigatingPoiWarnerFormat();

    public void testIsValidLine() {
        assertTrue(format.isValidLine("; Overnight stay for motorhomes in Europe with/without service"));        
        assertTrue(format.isValidLine(";"));
    }

    public void testIsPosition() {
        assertTrue(format.isPosition("8.6180900,50.2175100,\"[61352] AH Kreissl GmbH; Benzstraﬂe 7 [Bad Homburg]\""));
        assertTrue(format.isPosition(" 9.3900000 , 51.5037800 , \"[34369] Donig; Max-Eyth-Str. [Hofgeismar]\" "));
        assertTrue(format.isPosition("11.107167,49.375783,\"HOLSTEINBRUCH BEI WORZELDORF B - GC13VV5\""));

        assertFalse(format.isPosition("; Overnight stay for motorhomes in Europe with/without service"));
    }

    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("8.6180901,50.2175101,\"[61352] AH Kreissl GmbH; Benzstraﬂe 7 [Bad Homburg]\"", null);
        assertEquals(8.6180901, position.getLongitude());
        assertEquals(50.2175101, position.getLatitude());
        assertEquals("[61352] AH Kreissl GmbH; Benzstraﬂe 7 [Bad Homburg]", position.getComment());
    }

    public void testParseNegativePosition() {
        Wgs84Position position = format.parsePosition("-8.6180901,-50.2175101,\"ABC\"", null);
        assertEquals(-8.6180901, position.getLongitude());
        assertEquals(-50.2175101, position.getLatitude());
        assertEquals("ABC", position.getComment());
    }
}
