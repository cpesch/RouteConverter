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

import org.junit.Test;
import slash.navigation.base.Wgs84Position;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;

public class GoRiderGpsFormatTest {
    private GoRiderGpsFormat format = new GoRiderGpsFormat();

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("#CREATED=\"75\" MODIFIED=\"76\" NAME=\"Groningen - Noorderrondrit\""));
        assertTrue(format.isValidLine("#CREATED=\"21\"  SOMETHINGELSE=\"42\"  NAME=\"Groningen - Noorderrondrit\" "));
        assertTrue(format.isValidLine("STREET=\"Tjardaweg\" PT=\"6.53616 53.24917\""));
        assertTrue(format.isValidLine(" STREET=\"Tjardaweg\"  PT=\"6.53616 53.24917\" "));
        assertTrue(format.isValidLine("SOMETHINGELSE=\"42\" STREET=\"Tjardaweg\" SOMETHINGELSE=\"42\" PT=\"6.53616 53.24917\" SOMETHINGELSE=\"42\""));

        assertFalse(format.isValidLine(" CITY=\"Groningen\"  PT=\"6.53616 53.24917\"  SOMETHINGELSE=\"42\" "));
        assertFalse(format.isValidLine(" #CREATED=\"21\"  SOMETHINGELSE=\"42\"  NAME=\"Groningen - Noorderrondrit\" "));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("STREET=\"Tjardaweg\" PT=\"6.53616 53.24917\""));
        assertTrue(format.isPosition(" STREET=\"Groningen\"  PT=\"6.53616 53.24917\"  "));
        assertTrue(format.isPosition("SOMETHINGELSE=\"42\" STREET=\"Groningen\" SOMETHINGELSE=\"42\" PT=\"6.53616 53.24917\" SOMETHINGELSE=\"42\""));

        assertFalse(format.isPosition(" CITY=\"Groningen\"  PT=\"6.53616 53.24917\"  SOMETHINGELSE=\"42\" "));
        assertFalse(format.isPosition("#CREATED=\"75\" MODIFIED=\"76\" NAME=\"Groningen - Noorderrondrit\""));
        assertFalse(format.isPosition("STREET=\"Tjardaweg\""));
    }

    @Test
    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("STREET=\"Tjardaweg\" PT=\"6.53616 53.24917\"", null);
        assertDoubleEquals(6.53616, position.getLongitude());
        assertDoubleEquals(53.24917, position.getLatitude());
        assertEquals("Tjardaweg", position.getDescription());
        assertNull(position.getElevation());
    }
}