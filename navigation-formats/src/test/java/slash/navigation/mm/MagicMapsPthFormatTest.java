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

package slash.navigation.mm;

import slash.navigation.base.GkPosition;
import slash.navigation.base.NavigationTestCase;

public class MagicMapsPthFormatTest extends NavigationTestCase {
    MagicMapsPthFormat format = new MagicMapsPthFormat();

    public void testIsValidLine() {
        assertTrue(format.isNameValue("Pathsize: 98"));
        assertTrue(format.isNameValue(" Pathsize : 98 "));
        assertTrue(format.isNameValue("Pathsize:98"));

        assertTrue(format.isPosition("3811134.00 \t 5818411.00"));
        assertTrue(format.isPosition(" 3811134.00 5818411.00 "));
        assertTrue(format.isPosition("3811134.00 5818411.00"));
        assertTrue(format.isPosition("3799444.14 \t 5829944.10 \tHamburg/Uhlenhorst"));
    }

    public void testParsePosition() {
        GkPosition position = format.parsePosition("3799444.14 \t 5829944.10");
        assertNotNull(position);
        assertNearBy(13.4115129, position.getLongitude());
        assertNearBy(52.5202079, position.getLatitude());
        assertEquals(3799444.14, position.getRight());
        assertEquals(5829944.10, position.getHeight());
        assertNull(position.getElevation());
        assertNull(position.getTime());
        assertNull(position.getDescription());
    }

    public void testParsePositionWithdescription() {
        GkPosition position = format.parsePosition("3799444.14 \t 5829944.10 \tHamburg/Uhlenhorst");
        assertNotNull(position);
        assertNearBy(13.4115129, position.getLongitude());
        assertNearBy(52.5202079, position.getLatitude());
        assertEquals(3799444.14, position.getRight());
        assertEquals(5829944.10, position.getHeight());
        assertNull(position.getElevation());
        assertNull(position.getTime());
        assertEquals("Hamburg/Uhlenhorst", position.getDescription());
    }
}
