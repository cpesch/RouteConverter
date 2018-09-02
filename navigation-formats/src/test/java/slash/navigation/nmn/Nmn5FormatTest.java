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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;

public class Nmn5FormatTest {
    private Nmn5Format format = new Nmn5Format();

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("-|-|17|4353|72574|BAD URACH|72574|SHELL|-|-|-|9.38107|48.49711|617|-|9.39698|48.49193|"));
        assertTrue(format.isPosition("-|-|-|-|-|STATION1|-|-|-|-|-|10.031982|53.569403|-|-|-|-|"));
        assertTrue(format.isPosition("-|-|-|-|-|STATION1|-|-|-|-|-|-10.031982|-53.569403|-|-|-|-|"));

        // Alpenpaesse Frankreich Sued Alpen.rte
        assertFalse(format.isPosition("Col d Izoard,2361,12|-|-|05100|Cervieres|05100|D902|-|-|-|6.73139|44.82497|-|"));
    }

    @Test
    public void testParsePosition() {
        NmnPosition position = format.parsePosition("-|-|17|4353|89150|LAICHINGEN|-|-|-|-|-|9.60476|48.47282|-|-|9.68246|48.48852|", null);
        assertDoubleEquals(9.60476, position.getLongitude());
        assertDoubleEquals(48.47282, position.getLatitude());
        assertEquals("Laichingen", position.getDescription());
    }

    @Test
    public void testParseNegativePosition() {
        NmnPosition position = format.parsePosition("-|-|17|4353|89150|LAICHINGEN|-|-|-|-|-|-9.60476|-48.47282|-|-|9.68246|48.48852|", null);
        assertDoubleEquals(-9.60476, position.getLongitude());
        assertDoubleEquals(-48.47282, position.getLatitude());
        assertEquals("Laichingen", position.getDescription());
    }

    @Test
    public void testParsePositionWithStreet() {
        NmnPosition position = format.parsePosition("-|-|17|4353|89601|SCHELKLINGEN|89601|Marktstrasse|20|-|-|9.73374|48.37512|-|-|9.73416|48.37679|", null);
        assertDoubleEquals(9.73374, position.getLongitude());
        assertDoubleEquals(48.37512, position.getLatitude());
        assertEquals("Schelklingen, Marktstrasse 20", position.getDescription());
        assertNull(position.getZip());
        assertEquals("Schelklingen", position.getCity());
        assertEquals("Marktstrasse", position.getStreet());
        assertEquals("20", position.getNumber());
    }

    @Test
    public void testSetComment() {
        NmnPosition position = format.parsePosition("-|-|17|4353|89601|-|89601|-|-|-|-|9.73374|48.37512|-|-|9.73416|48.37679|", null);
        assertDoubleEquals(9.73374, position.getLongitude());
        assertDoubleEquals(48.37512, position.getLatitude());
        assertNull(position.getZip());
        assertNull(position.getCity());
        assertNull(position.getStreet());
        assertNull(position.getNumber());
        assertNull(position.getDescription());
        assertTrue(position.isUnstructured());
        position.setDescription(null);
        assertNull(position.getDescription());
        
        position.setDescription("87451 Schelklingen, Marktstrasse 20");
        assertEquals("87451", position.getZip());
        assertEquals("Schelklingen", position.getCity());
        assertEquals("Marktstrasse", position.getStreet());
        assertEquals("20", position.getNumber());
        assertEquals("87451 Schelklingen, Marktstrasse 20", position.getDescription());

        position.setDescription("92845 Bad Urach, Shell");
        assertEquals("92845", position.getZip());
        assertEquals("Bad Urach", position.getCity());
        assertEquals("Shell", position.getStreet());
        assertNull(position.getNumber());
        assertEquals("92845 Bad Urach, Shell", position.getDescription());
    }
}
