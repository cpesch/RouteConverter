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
import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.Wgs84Position;

import java.text.DateFormat;

import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;

public class KienzleGpsFormatTest {
    private KienzleGpsFormat format = new KienzleGpsFormat();

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("101;7.0894440000;50.7405550000;;;53111;Bonn;Dorotheenstr.;103;16:10;"));
        assertTrue(format.isValidLine("113;7.0475000000;50.7500000000;PHE I;;53119;Bonn;Oppelner Str.;126;16:49;"));
        assertTrue(format.isValidLine("Position;X;Y;Empfänger;Land;PLZ;Ort;Strasse;Hausnummer;Planankunft;Zusatzinfos"));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("113;7.0475000000;50.7500000000;PHE I;;53119;Bonn;Oppelner Str.;126;16:49;"));
        assertTrue(format.isPosition("103;7.0997220000;50.7494440000;;;53117;Bonn;Am Jesuitenhof/Am Römerlager;;16:16;"));

        assertFalse(format.isPosition("Position;X;Y;Empfänger;Land;PLZ;Ort;Strasse;Hausnummer;Planankunft;Zusatzinfos"));
    }

    @Test
    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("113;7.0475000000;50.7500000000;PHE I;;53119;Bonn;Oppelner Str.;126;16:49;", new ParserContextImpl());
        assertDoubleEquals(7.0475000000, position.getLongitude());
        assertDoubleEquals(50.7500000000, position.getLatitude());
        assertNull(position.getElevation());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(1970, 1, 1, 16, 49, 0);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected,  actual);
        assertEquals(expectedCal, position.getTime());
        assertEquals("PHE I: 53119 Bonn, Oppelner Str. 126", position.getDescription());
    }

    @Test
    public void testParseNegativePosition() {
        Wgs84Position position = format.parsePosition("113;-7.0475000000;-50.7500000000;PHE I;;53119;Bonn;Oppelner Str.;126;16:49;", new ParserContextImpl());
        assertDoubleEquals(-7.0475000000, position.getLongitude());
        assertDoubleEquals(-50.7500000000, position.getLatitude());
    }
}