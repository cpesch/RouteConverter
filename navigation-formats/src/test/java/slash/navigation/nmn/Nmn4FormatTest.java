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

public class Nmn4FormatTest extends NavigationTestCase {
    Nmn4Format format = new Nmn4Format();

    public void testIsPosition() {
        assertTrue(format.isPosition("-|-|17|-|-|Gelsenkirchen|45896|Polsumer Straße|-|-|-|7.05143|51.59682|-|-|"));
        assertTrue(format.isPosition("-|-|-|-|-|-|-|-|-|-|7.00905|51.44329|-|"));
        assertTrue(format.isPosition("-|-|-|45128|Südviertel|45128|Hohenzollernstrasse/L451|-|-|-|7.00905|51.44329|-|"));
        assertTrue(format.isPosition("-|-|-|58452|Witten|58452|Schloss Steinhausen|-|-|-|-|-|460|"));

        // GPSBabel creates this
        assertTrue(format.isPosition("-|-|-|-|-|-|-|-|-|-|-|6.42323|51.84617|-|-|"));
        assertTrue(format.isPosition("-|-|16|-|-|bei D 22929,Köthel; Kr Hzgt Lauenburg,,0,|-|-|-|-|-|10.51239|53.61192|-|-|"));
    }

    public void testParsePositionWithStreet() {
        NmnPosition position = format.parsePosition("-|-|-|45128|Südviertel|45128|Hohenzollernstrasse/L451|-|-|-|7.00905|51.44329|-|", null);
        assertEquals(7.00905, position.getLongitude());
        assertEquals(51.44329, position.getLatitude());
        assertEquals("45128 Südviertel, Hohenzollernstrasse/L451", position.getComment());
        assertEquals("45128", position.getZip());
        assertEquals("Südviertel", position.getCity());
        assertEquals("Hohenzollernstrasse/L451", position.getStreet());
        assertNull(position.getNumber());
    }

    public void testParseUppercaseComment() {
        NmnPosition position = format.parsePosition("-|-|-|45128|SÜDVIERTEL|45128|HOHENZOLLERNSTRASSE|-|-|-|7.00905|51.44329|-|", null);
        assertEquals(7.00905, position.getLongitude());
        assertEquals(51.44329, position.getLatitude());
        assertEquals("45128 Südviertel, Hohenzollernstrasse", position.getComment());
        assertEquals("45128", position.getZip());
        assertEquals("Südviertel", position.getCity());
        assertEquals("Hohenzollernstrasse", position.getStreet());
        assertNull(position.getNumber());
    }

    public void testParseNegativePosition() {
        NmnPosition position = format.parsePosition("-|-|-|45128|Südviertel|45128|Hohenzollernstrasse/L451|-|-|-|-7.00905|-51.44329|-|", null);
        assertEquals(-7.00905, position.getLongitude());
        assertEquals(-51.44329, position.getLatitude());
    }

    public void testParseGPSBabelPosition() {
        NmnPosition position = format.parsePosition("-|-|16|-|-|Linau|-|-|-|-|-|10.46348|53.64352|-|-|", null);
        assertEquals(10.46348, position.getLongitude());
        assertEquals(53.64352, position.getLatitude());
        assertEquals("Linau", position.getComment());
        assertNull(position.getZip());
        assertEquals("Linau", position.getCity());
        assertNull(position.getStreet());
        assertNull(position.getNumber());
    }

    public void testParseMN42Position() {
        NmnPosition position = format.parsePosition("-|-|17|-|-|Gelsenkirchen|45896|Polsumer Straße|-|-|-|7.05143|51.59682|-|-|", null);
        assertEquals(7.05143, position.getLongitude());
        assertEquals(51.59682, position.getLatitude());
        assertEquals("45896 Gelsenkirchen, Polsumer Straße", position.getComment());
        assertEquals("45896", position.getZip());
        assertEquals("Gelsenkirchen", position.getCity());
        assertEquals("Polsumer Straße", position.getStreet());
        assertNull(position.getNumber());
    }

}