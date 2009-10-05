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

import slash.navigation.NavigationTestCase;
import slash.navigation.Wgs84Position;
import slash.common.io.CompactCalendar;

import java.text.DateFormat;

public class ColumbusV900ProfessionalFormatTest extends NavigationTestCase {
    ColumbusV900ProfessionalFormat format = new ColumbusV900ProfessionalFormat();

    public void testIsValidLine() {
        assertTrue(format.isValidLine("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX"));
        assertTrue(format.isValidLine("1150  ,T,090522,150532,48.206931N,016.372713E,-5   ,0   ,0  ,3D,SPS ,2.3  ,2.1  ,1.0  ,"));
    }

    public void testIsPosition() {
        assertTrue(format.isPosition("2971  ,V,090508,084815,48.132451N,016.321871E,319  ,12  ,207,3D,SPS ,1.6  ,1.3  ,0.9  ,VOX02971"));

        assertFalse(format.isPosition("5     ,T,090421,061057,47.797281N,013.049743E,504  ,0   ,206,         "));
        assertFalse(format.isPosition("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX"));
    }

    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("2971  ,V,090508,084815,48.132451N,016.321871E,319  ,12  ,207,3D,SPS ,1.6  ,1.3  ,0.9  ,VOX02971", null);
        assertEquals(16.321871, position.getLongitude());
        assertEquals(48.132451, position.getLatitude());
        assertEquals(319.0, position.getElevation());
        assertEquals(12.0, position.getSpeed());
        assertEquals(207.0, position.getHeading());
        assertEquals(1.6, position.getPdop());
        assertEquals(1.3, position.getHdop());
        assertEquals(0.9, position.getVdop());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2009, 5, 8, 8, 48, 15);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertEquals("VOX02971", position.getComment());
    }
}