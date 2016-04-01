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

package slash.navigation.nmea;

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;

import java.io.*;
import java.text.DateFormat;
import java.util.List;

import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;
import static slash.common.io.Transfer.ISO_LATIN1_ENCODING;

public class MagellanExploristFormatTest {
    private MagellanExploristFormat format = new MagellanExploristFormat();

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("$PMGNFMT,%TRK,LAT,HEMI,LON,HEMI,ALT,UNIT,TIME,VALID,NAME,%META,ASCII"));
        assertTrue(format.isValidLine("$PMGNTRK,4914.967,N,00651.208,E,000199,M,152224,A,KLLERTAL-RADWEG,210307*48"));
        assertTrue(format.isValidLine("$PMGNTRK,5159.928,N,00528.243,E,00008,M,093405.33,A,,250408*79"));
        assertTrue(format.isValidLine("$PMGNTRK,4348.1258,N,08735.0978,E,000000,M,,A,???,*E1"));

        assertFalse(format.isValidLine("# Description"));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("$PMGNTRK,4914.967,N,00651.208,E,000199,M,152224,A,KLLERTAL-RADWEG,210307*48"));
        assertTrue(format.isPosition("$PMGNTRK,5159.928,N,00528.243,E,00008,M,093405.33,A,,250408*79"));

        assertFalse(format.isPosition("$GPGGA,134012,4837.4374,N,903.4036,E,1,,,-48.0,M,,M,,*61"));
        assertFalse(format.isPosition("$GPWPL,4837.4374,N,903.4036,E,*4C"));
        assertFalse(format.isPosition("$GPRMC,134012,A,4837.4374,N,903.4036,E,,,260707,,A*5A"));
        assertFalse(format.isPosition("$GPZDA,134012,26,07,07,,*49"));

        assertFalse(format.isPosition("$GPGGA,162611,3554.2367,N,10619.4966,W,1,03,06.7,02300.3,M,-022.4,M,,*7F"));

        assertFalse(format.isPosition("$GPGGA,130441.89,5239.3154,N,00907.7011,E,1,08,1.25,16.76,M,46.79,M,,*6D"));
        assertFalse(format.isPosition("$GPGGA,130441,5239,N,00907.7011,E,1,08,1.25,16.76,M,46.79,M,,*6F"));
        assertFalse(format.isPosition("$GPGGA,140404.000,4837.5339,N,00903.4040,E,1,08,00.0,484.0,M,00.0,M,,*67"));
        assertFalse(format.isPosition("$GPGGA,140404.000,4837.5339,N,00903.4040,E,1,08,00.0,484.0,M,0,M,,*49"));
        assertFalse(format.isPosition("$GPGGA,140404.000,4837.5339,N,00903.4040,E,1,08,00.0,0,M,0,M,,*5F"));
        assertFalse(format.isPosition("$GPGGA,140404.000,4837.5339,N,00903.4040,E,1,08,0,0,M,0,M,,*71"));
        assertFalse(format.isPosition("$GPGGA,,4837.5339,N,00903.4040,E,1,08,0,0,M,0,M,,*6A"));
        assertFalse(format.isPosition("$GPGGA,175947.000,4812.0597,N,01136.4663,E,1,07,1.4,495.3,M,,,,*09"));
        assertFalse(format.isPosition("$GPRMC,140403.000,A,4837.5194,N,00903.4022,E,15.00,0.00,260707,,*3E"));
        assertFalse(format.isPosition("$GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,000.0,160607,,,A*76"));
        assertFalse(format.isPosition("$GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,000.0,160607,,,A*76"));
        assertFalse(format.isPosition("$GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,000.0,,,,A*70"));
        assertFalse(format.isPosition("$GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,,,,,A*5E"));
        assertFalse(format.isPosition("$GPRMC,180114,A,4808.9490,N,00928.9610,E,,,,,,A*70"));
        assertFalse(format.isPosition("$GPRMC,,A,4808.9490,N,00928.9610,E,,,,,,A*7D"));
        assertFalse(format.isPosition("$GPRMC,175947.000,A,4812.0597,N,01136.4663,E,0.0,163.8,010907,,,A*62"));
        assertFalse(format.isPosition("$GPZDA,032910,07,08,2004,00,00*48"));
        assertFalse(format.isPosition("$GPWPL,5334.169,N,01001.920,E,STATN1*22"));
    }

    @Test
    public void testParsePMGNTRK() {
        NmeaPosition position = format.parsePosition("$PMGNTRK,4914.9670,N,00651.2080,E,000199,M,152224,A,KLLERTAL-RADWEG,210307*56");
        assertDoubleEquals(4914.967, position.getLatitudeAsValueAndOrientation().getValue());
        assertDoubleEquals(651.208, position.getLongitudeAsValueAndOrientation().getValue());
        assertEquals("N", position.getLatitudeAsValueAndOrientation().getOrientation().value());
        assertEquals("E", position.getLongitudeAsValueAndOrientation().getOrientation().value());
        assertDoubleEquals(6.8534666667, position.getLongitude());
        assertDoubleEquals(49.24945, position.getLatitude());
        assertDoubleEquals(199.0, position.getElevation());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2007, 3, 21, 15, 22, 24);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertEquals("Kllertal-Radweg", position.getDescription());
    }

    @Test
    public void testWritePMGNTRK() throws IOException {
        StringReader reader = new StringReader(
                "$PMGNTRK,4914.9672,N,00651.2081,E,00199,M,152224,A,KLLERTAL-RADWEG,210307*7B"
        );
        ParserContext<NmeaRoute> context = new ParserContextImpl<>();
        format.read(new BufferedReader(reader), ISO_LATIN1_ENCODING, context);
        List<NmeaRoute> routes = context.getRoutes();
        assertEquals(1, routes.size());
        NmeaRoute route = routes.get(0);
        assertEquals(1, route.getPositionCount());
        NmeaPosition position = route.getPositions().get(0);
        assertDoubleEquals(4914.9672, position.getLatitudeAsValueAndOrientation().getValue());
        assertDoubleEquals(651.2081, position.getLongitudeAsValueAndOrientation().getValue());
        assertEquals("N", position.getLatitudeAsValueAndOrientation().getOrientation().value());
        assertEquals("E", position.getLongitudeAsValueAndOrientation().getOrientation().value());
        assertDoubleEquals(6.8534683333, position.getLongitude());
        assertDoubleEquals(49.2494533333, position.getLatitude());
        assertDoubleEquals(199.0, position.getElevation());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2007, 3, 21, 15, 22, 24);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertEquals("Kllertal-Radweg", position.getDescription());

        StringWriter writer = new StringWriter();
        format.write(route, new PrintWriter(writer), 0, 1);
        String eol = System.getProperty("line.separator");
        String expectedLines = "$PMGNFMT,%TRK,LAT,HEMI,LON,HEMI,ALT,UNIT,TIME,VALID,NAME,%META,ASCII" + eol +
                "$PMGNTRK,4914.9672,N,00651.2081,E,000199,M,152224.000,A,Kllertal-Radweg,210307*55" + eol +
                "$PMGNCMD,END*3D" + eol;
        assertEquals(expectedLines, writer.getBuffer().toString());
    }
}