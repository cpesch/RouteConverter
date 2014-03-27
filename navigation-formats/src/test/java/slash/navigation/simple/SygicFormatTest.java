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

public class SygicFormatTest extends NavigationTestCase {
    SygicUnicodeFormat format = new SygicUnicodeFormat();

    public void testIsValidLine() {
        assertTrue(format.isValidLine("; something"));
        assertTrue(format.isValidLine("2.324360\t48.826760\tRue Antoine Chantin(14ème Arrondissement Paris), Paris"));
        assertTrue(format.isValidLine("17.556660\t54.758830\tLeba Lebska\t+48598662069"));
        assertTrue(format.isValidLine(""));
    }

    public void testIsPosition() {
        assertTrue(format.isPosition("2.324360\t48.826760\tRue Antoine Chantin(14ème Arrondissement Paris), Paris"));
        assertTrue(format.isPosition("17.556660\t54.758830\tLeba Lebska\t+48598662069"));

        assertFalse(format.isPosition("17.556660    54.758830    Leba Lebska"));
        assertFalse(format.isPosition("17.556660    54.758830    Leba Lebska     +48598662069"));
    }

    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("17.556660\t54.758830\tLeba Lebska\t+48598662069", null);
        assertEquals(17.556660, position.getLongitude());
        assertEquals(54.758830, position.getLatitude());
        assertNull(position.getElevation());
        assertEquals("Leba Lebska +48598662069", position.getDescription());
    }

    public void testParsePositionWithoutElevation() {
        Wgs84Position position = format.parsePosition("2.324360\t48.826760\tRue Antoine Chantin(14ème Arrondissement Paris), Paris", null);
        assertEquals(2.324360, position.getLongitude());
        assertEquals(48.82676, position.getLatitude());
        assertNull(position.getElevation());
        assertEquals("Rue Antoine Chantin(14ème Arrondissement Paris), Paris", position.getDescription());
    }
}