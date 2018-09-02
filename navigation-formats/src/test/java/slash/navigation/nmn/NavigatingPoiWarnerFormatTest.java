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

import org.junit.Test;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.Wgs84Position;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;

public class NavigatingPoiWarnerFormatTest {
    private NavigatingPoiWarnerFormat format = new NavigatingPoiWarnerFormat();

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("; Overnight stay for motorhomes in Europe with/without service"));
        assertTrue(format.isValidLine(";"));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("8.6180900,50.2175100,\"[61352] AH Kreissl GmbH; Benzstrasse 7 [Bad Homburg]\""));
        assertTrue(format.isPosition(" 9.3900000 , 51.5037800 , \"[34369] Donig; Max-Eyth-Str. [Hofgeismar]\" "));
        assertTrue(format.isPosition("11.107167,49.375783,\"HOLSTEINBRUCH BEI WORZELDORF B - GC13VV5\""));

        assertFalse(format.isPosition("; Overnight stay for motorhomes in Europe with/without service"));
    }

    @Test
    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("8.6180901,50.2175101,\"[61352] AH Kreissl GmbH; Benzstrasse 7 [Bad Homburg]\"", new ParserContextImpl());
        assertDoubleEquals(8.6180901, position.getLongitude());
        assertDoubleEquals(50.2175101, position.getLatitude());
        assertEquals("[61352] AH Kreissl GmbH; Benzstrasse 7 [Bad Homburg]", position.getDescription());
    }

    @Test
    public void testParseNegativePosition() {
        Wgs84Position position = format.parsePosition("-8.6180901,-50.2175101,\"ABC\"", new ParserContextImpl());
        assertDoubleEquals(-8.6180901, position.getLongitude());
        assertDoubleEquals(-50.2175101, position.getLatitude());
        assertEquals("ABC", position.getDescription());
    }

    @Test
    public void testParseControlCharacters() {
        char[] chars = new char[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 23, 24, 25, 26, 27, 28, 29, 30, 31};
        for (char c : chars) {
            Wgs84Position position = format.parsePosition("1.2,3.4,\"äöüßA" + c + "Z+*$%\"", new ParserContextImpl());
            assertDoubleEquals(1.2, position.getLongitude());
            assertDoubleEquals(3.4, position.getLatitude());
            assertEquals("äöüßAZ+*$%", position.getDescription());
        }
    }
}
