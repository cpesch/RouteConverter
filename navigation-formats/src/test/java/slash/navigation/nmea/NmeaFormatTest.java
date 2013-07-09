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
import slash.navigation.base.SimpleRoute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;
import static slash.common.io.Transfer.ISO_LATIN1_ENCODING;
import static slash.navigation.common.UnitConversion.nauticMilesToKilometer;

public class NmeaFormatTest {
    private NmeaFormat format = new NmeaFormat();

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("@Sonygps/ver1.0/wgs-84"));
        assertTrue(format.isValidLine("$ADVER,3080,2.0"));
        assertTrue(format.isValidLine("$PSRFTXT,Version: 2.4.12.09-XMitac-C4PROD2.0 0000003729*76"));

        assertTrue(format.isValidLine("$PTOM101,PocketPC,hp iPAQ h2200,2577,4.20*45"));
        assertTrue(format.isValidLine("$PTOM102,GPS Engine,408,TomTom Wireless GPS,Bluetooth Serial Port COM8:*B"));
        assertTrue(format.isValidLine("$PTOM103,070805,063346.000,070805,083346.000*3A"));
        assertTrue(format.isValidLine("$PTOM104,\\My Documents\\GPS Log\\GPS20070805083346-busy.pgl*8"));

        assertTrue(format.isValidLine("$GPGGA,180114,4808.9490,N,00928.9610,E,1,05,12.6,00616.6,M,048.0,M,,*49"));
        assertTrue(format.isValidLine("$GPGSA,A,3,05,09,12,14,22,,,,,,,,19.9,12.6,15.3*0B"));
        assertTrue(format.isValidLine("$GPGSV,2,1,08,05,40,250,50,09,85,036,51,22,16,285,36,17,,,00*4F"));
        assertTrue(format.isValidLine("$GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,000.0,160607,,,A*76"));
        assertTrue(format.isValidLine("$GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,000.0,160607,,,A*76"));
        assertTrue(format.isValidLine("$GPGGA,132713,5509.7861,N,00140.5854,W,1,07,1.0,98.9,M,,M,,*7d"));
        assertTrue(format.isValidLine("$GPRMC,132713,A,5509.7861,N,00140.5854,W,2.1,278.3,010110,,*e"));
        assertTrue(format.isValidLine("$GPRMC,172103.38,V,4424.5358,N,06812.3754,W,0.000,0.000,101010,0,W,N*3A"));

        assertTrue(format.isValidLine("$GPVTG,000.0,T,,M,000.0,N,000.0,K,A*0D"));
        assertTrue(format.isValidLine("$GPVTG,0.00,T,,M,1.531,N,2.835,K,A*37"));

        assertTrue(format.isValidLine("$GPZDA,032910,07,08,2004,00,00*48"));
        assertTrue(format.isValidLine("$GPWPL,5334.169,N,01001.920,E,STATN1*22"));
        assertTrue(format.isValidLine("$GPGGA,123613.957,,,,,0,00,,,M,0.0,M,,0000*59"));
        assertTrue(format.isValidLine("$GPRMC,123613.957,V,,,,,,,170807,,*29"));

        assertTrue(format.isValidLine("$GPZDA,032910.331,07,08,2004,00,00*48"));
        assertTrue(format.isValidLine("$GPGGA,145524.054,,,,,0,00,,,M,0.0,M,,0000*54"));
        assertTrue(format.isValidLine("$GPRMC,145524.054,V,,,,,,,300807,,*21"));

        assertTrue(format.isValidLine("$GPGGA,175947.000,4812.0597,N,01136.4663,E,1,07,1.4,495.3,M,,,,*09"));
        assertTrue(format.isValidLine("$GPRMC,175947.000,A,4812.0597,N,01136.4663,E,0.0,163.8,010907,,,A*62"));

        assertTrue(format.isValidLine("$PMGNTRK,4914.967,N,00651.208,E,000199,M,152224,A,KLLERTAL-RADWEG,210307*48"));
        assertTrue(format.isValidLine("$PMGNTRK,5159.928,N,00528.243,E,00008,M,093405.33,A,,250408*79"));

        assertFalse(format.isValidLine("# Comment"));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("$GPGGA,134012,4837.4374,N,903.4036,E,1,,,-48.0,M,,M,,*61"));
        assertTrue(format.isPosition("$GPWPL,4837.4374,N,903.4036,E,*4C"));
        assertTrue(format.isPosition("$GPRMC,134012,A,4837.4374,N,903.4036,E,,,260707,,A*5A"));
        assertTrue(format.isPosition("$GPZDA,134012,26,07,07,,*49"));
        assertTrue(format.isPosition("$GPGSA,A,3,,,,15,17,18,23,,,,,,4.7,4.4,1.5*3F"));
        assertTrue(format.isPosition("$GPGSA,A,3,05,09,12,14,22,,,,,,,,19.9,12.6,15.3*0B"));

        assertTrue(format.isPosition("$GPGGA,162611,3554.2367,N,10619.4966,W,1,03,06.7,02300.3,M,-022.4,M,,*7F"));
        assertTrue(format.isPosition("$GPGGA,130441.89,5239.3154,N,00907.7011,E,1,08,1.25,16.76,M,46.79,M,,*6D"));
        assertTrue(format.isPosition("$GPGGA,130441,5239,N,00907.7011,E,1,08,1.25,16.76,M,46.79,M,,*6F"));
        assertTrue(format.isPosition("$GPGGA,140404.000,4837.5339,N,00903.4040,E,1,08,00.0,484.0,M,00.0,M,,*67"));
        assertTrue(format.isPosition("$GPGGA,140404.000,4837.5339,N,00903.4040,E,1,08,00.0,484.0,M,0,M,,*49"));
        assertTrue(format.isPosition("$GPGGA,140404.000,4837.5339,N,00903.4040,E,1,08,00.0,0,M,0,M,,*5F"));
        assertTrue(format.isPosition("$GPGGA,140404.000,4837.5339,N,00903.4040,E,1,08,0,0,M,0,M,,*71"));
        assertTrue(format.isPosition("$GPGGA,,4837.5339,N,00903.4040,E,1,08,0,0,M,0,M,,*6A"));
        assertTrue(format.isPosition("$GPGGA,175947.000,4812.0597,N,01136.4663,E,1,07,1.4,495.3,M,,,,*09"));
        assertTrue(format.isPosition("$GPRMC,140403.000,A,4837.5194,N,00903.4022,E,15.00,0.00,260707,,*3E"));
        assertTrue(format.isPosition("$GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,000.0,160607,,,A*76"));
        assertTrue(format.isPosition("$GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,000.0,160607,,,A*76"));
        assertTrue(format.isPosition("$GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,000.0,,,,A*70"));
        assertTrue(format.isPosition("$GPRMC,180114,A,4808.9490,N,00928.9610,E,000.0,,,,,A*5E"));
        assertTrue(format.isPosition("$GPRMC,180114,A,4808.9490,N,00928.9610,E,,,,,,A*70"));
        assertTrue(format.isPosition("$GPRMC,,A,4808.9490,N,00928.9610,E,,,,,,A*7D"));
        assertTrue(format.isPosition("$GPRMC,175947.000,A,4812.0597,N,01136.4663,E,0.0,163.8,010907,,,A*62"));
        assertTrue(format.isPosition("$GPRMC,172103.38,V,4424.5358,N,06812.3754,W,0.000,0.000,101010,0,W,A*35"));
        assertTrue(format.isPosition("$GPZDA,032910,07,08,2004,00,00*48"));
        assertTrue(format.isPosition("$GPWPL,5334.169,N,01001.920,E,STATN1*22"));
        assertTrue(format.isPosition("$GPVTG,0.00,T,,M,1.531,N,2.835,K,A*37"));
        assertTrue(format.isPosition("$GPVTG,000.0,T,,M,000.0,N,000.0,K,A*0D"));
        assertTrue(format.isPosition("$GPGGA,132713,5509.7861,N,00140.5854,W,1,07,1.0,98.9,M,,M,,*7d"));
        assertTrue(format.isPosition("$GPRMC,171624,A,5341.1395,N, 951.2163,E,1.285115,0.000000,150711,,*34"));
        assertTrue(format.isPosition("$GPGGA,171624,5341.1395,N, 951.2163,E,1,00,0.0,173.773102,M,0.0,M,,*66"));

        assertFalse(format.isPosition("$PMGNTRK,4914.967,N,00651.208,E,000199,M,152224,A,KLLERTAL-RADWEG,210307*48"));
        assertFalse(format.isPosition("$PMGNTRK,5159.928,N,00528.243,E,00008,M,093405.33,A,,250408*79"));
        assertFalse(format.isPosition("$GPRMC,132713,A,5509.7861,N,00140.5854,W,2.1,278.3,010110,,*e"));

        assertFalse(format.isPosition("$GPGSV,2,1,08,05,40,250,50,09,85,036,51,22,16,285,36,17,,,00*4F"));
        assertFalse(format.isPosition("@Sonygps/ver1.0/wgs-84"));
        assertFalse(format.isPosition("$GPGGA,123613.957,,,,,0,00,,,M,0.0,M,,0000*59"));
        assertFalse(format.isPosition("$GPRMC,123613.957,V,,,,,,,170807,,*29"));
        assertFalse(format.isPosition("$GPGGA,145524.054,,,,,0,00,,,M,0.0,M,,0000*54"));
        assertFalse(format.isPosition("$GPRMC,145524.054,V,,,,,,,300807,,*21"));
    }

    @Test
    public void testIsPositionRespectingFixQuality() {
        assertTrue(format.isPosition("$GPRMC,061013.64,A,5119.8979,N,01219.1497,E,0,0,160709,0,W,A*34"));
        assertTrue(format.isPosition("$GPGGA,061014.64,5119.8979,N,01219.1497,E,1,5,1.892,144.426,M,42.396,M,0,*63"));
        assertFalse(format.isPosition("$GPRMC,060900.64,V,0000.0000,N,00000.0000,E,0,0,160709,0,W,N*25"));
        assertFalse(format.isPosition("$GPGGA,060901.64,0000.0000,N,00000.0000,E,,2,60.000,0,M,0,M,0,*55"));
        assertFalse(format.isPosition("$GPGGA,060901.64,0000.0000,N,00000.0000,E,0,2,60.000,0,M,0,M,0,*65"));
        assertTrue(format.isPosition("$GPGGA,060901.64,0000.0000,N,00000.0000,E,1,2,60.000,0,M,0,M,0,*64"));
        assertFalse(format.isPosition("$GPRMC,060914.64,V,4508.3662,N,01543.0320,E,0,0,160709,0,W,N*2A"));
        assertFalse(format.isPosition("$GPGGA,060915.64,4512.4901,N,01541.0840,E,,3,60.000,-0.000,M,0,M,0,*61"));
        assertFalse(format.isPosition("$GPGSA,A,1,05,09,12,14,22,,,,,,,,19.9,12.6,15.3*09"));
        assertTrue(format.isPosition("$GPGSA,A,3,05,09,12,14,22,,,,,,,,19.9,12.6,15.3*0B"));
    }

    @Test
    public void testHaveDifferentLongitudeAndLatitude() {
        NmeaPosition one = new NmeaPosition(1.0, "E", 5159.971, "N", 22.0, 14.0, 12.0, null, null);
        NmeaPosition two = new NmeaPosition(1.1, "E", 5159.971, "N", 22.0, 14.0, 12.0, null, null);
        assertTrue(format.haveDifferentLongitudeAndLatitude(one, two));
        assertTrue(format.haveDifferentLongitudeAndLatitude(two, one));
        assertFalse(format.haveDifferentLongitudeAndLatitude(one, one));
        assertFalse(format.haveDifferentLongitudeAndLatitude(two, two));

        NmeaPosition three = new NmeaPosition(528.81, "E", 1.0, "N", 22.0, 14.0, 12.0, null, null);
        NmeaPosition four = new NmeaPosition(528.9, "E", 1.1, "N", 22.0, 14.0, 12.0, null, null);
        assertTrue(format.haveDifferentLongitudeAndLatitude(three, four));
        assertTrue(format.haveDifferentLongitudeAndLatitude(four, three));
        assertFalse(format.haveDifferentLongitudeAndLatitude(three, three));
        assertFalse(format.haveDifferentLongitudeAndLatitude(four, four));

        NmeaPosition five = new NmeaPosition(528.81, "E", 5159.971, "N", 22.0, 14.0, 12.0, null, null);
        NmeaPosition six = new NmeaPosition(528.82, "E", 5159.971, "N", 22.0, 14.0, 12.0, null, null);
        assertTrue(format.haveDifferentLongitudeAndLatitude(five, six));
        assertTrue(format.haveDifferentLongitudeAndLatitude(six, five));
        assertFalse(format.haveDifferentLongitudeAndLatitude(five, five));
        assertFalse(format.haveDifferentLongitudeAndLatitude(six, six));

        NmeaPosition seven = new NmeaPosition(528.81, "E", 5159.971, "N", 22.0, 14.0, 12.0, null, null);
        NmeaPosition eight = new NmeaPosition(528.81, "E", 5159.972, "N", 22.0, 14.0, 12.0, null, null);
        assertTrue(format.haveDifferentLongitudeAndLatitude(seven, eight));
        assertTrue(format.haveDifferentLongitudeAndLatitude(eight, seven));
        assertFalse(format.haveDifferentLongitudeAndLatitude(seven, seven));
        assertFalse(format.haveDifferentLongitudeAndLatitude(eight, eight));
    }

    @Test
    public void testParseGGA() {
        NmeaPosition position = format.parsePosition("$GPGGA,130441.89,4837.4374,S,00903.4036,E,1,08,1.25,16.76,M,46.79,M,,*6D");
        assertDoubleEquals(903.4036, position.getLongitudeAsValueAndOrientation().getValue());
        assertDoubleEquals(4837.4374, position.getLatitudeAsValueAndOrientation().getValue());
        assertEquals("E", position.getLongitudeAsValueAndOrientation().getOrientation().value());
        assertEquals("S", position.getLatitudeAsValueAndOrientation().getOrientation().value());
        assertDoubleEquals(9.0567266, position.getLongitude());
        assertDoubleEquals(-48.6239566, position.getLatitude());
        assertDoubleEquals(16.76, position.getElevation());
        assertEquals(new Integer(8), position.getSatellites());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(1970, 1, 1, 13, 4, 41, 89);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertNull(position.getComment());
    }

    @Test
    public void testParseGGAFromSonyLogger() {
        NmeaPosition position = format.parsePosition("$GPGGA,162611,3554.2367,N,10619.4966,W,1,03,06.7,02300.3,M,-022.4,M,,*7F");
        assertDoubleEquals(-106.3249433, position.getLongitude());
        assertDoubleEquals(35.9039449, position.getLatitude());
        assertDoubleEquals(2300.3, position.getElevation());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(1970, 1, 1, 16, 26, 11);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertNull(position.getComment());
    }

    @Test
    public void testParseRMC() {
        NmeaPosition position = format.parsePosition("$GPRMC,180114,A,4837.4374,N,00903.4036,E,14.32,000.0,160607,,,A*76");
        assertDoubleEquals(9.0567266, position.getLongitude());
        assertDoubleEquals(48.6239566, position.getLatitude());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2007, 6, 16, 18, 1, 14);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertNull(position.getElevation());
        assertNull(position.getComment());
        assertDoubleEquals(nauticMilesToKilometer(14.32), position.getSpeed());
    }

    @Test
    public void testParseWPL() {
        NmeaPosition position = format.parsePosition("$GPWPL,5334.169,N,01001.920,E,STATN1*22");
        assertDoubleEquals(10.0319999, position.getLongitude());
        assertDoubleEquals(53.5694833, position.getLatitude());
        assertNull(position.getTime());
        assertNull(position.getElevation());
        assertEquals("STATN1", position.getComment());
    }

    @Test
    public void testParseZDA() {
        NmeaPosition position = format.parsePosition("$GPZDA,032910.542,07,08,2004,00,00*48");
        assertNull(position.getLongitude());
        assertNull(position.getLatitude());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2004, 8, 7, 3, 29, 10, 542);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertNull(position.getElevation());
        assertNull(position.getComment());
        assertNull(position.getSpeed());
    }

    @Test
    public void testParseVTG() {
        NmeaPosition position = format.parsePosition("$GPVTG,32.19,T,,M,1.531,N,2.835,K,A*37");
        assertDoubleEquals(2.835, position.getSpeed());
        assertDoubleEquals(32.19, position.getHeading());
        assertNull(position.getTime());
        assertNull(position.getElevation());
        assertNull(position.getComment());
    }

    @Test
    public void testParseGSA() {
        NmeaPosition position = format.parsePosition("$GPGSA,A,3,,,,15,17,18,23,,,,,,4.7,4.4,1.5*3F");
        assertDoubleEquals(4.7, position.getPdop());
        assertDoubleEquals(4.4, position.getHdop());
        assertDoubleEquals(1.5, position.getVdop());
    }

    @Test
    public void testMerging() throws IOException {
        StringReader reader = new StringReader(
                "$GPGGA,130441.89,4837.4374,N,00903.4036,E,1,08,1.25,16.76,M,46.79,M,,*6D\n" +
                        "$GPRMC,180114,A,4837.4374,N,00903.4036,E,000.0,000.0,160600,,,A*7B\n" +
                        "$GPZDA,032910,07,08,2004,00,00*48\n" +
                        "$GPVTG,0.00,T,,M,1.531,N,2.835,K,A*37"
        );
        ParserContext<NmeaRoute> context = new ParserContextImpl<NmeaRoute>();
        format.read(new BufferedReader(reader), null, ISO_LATIN1_ENCODING, context);
        List<NmeaRoute> routes = context.getRoutes();
        assertEquals(1, routes.size());
        SimpleRoute route = routes.get(0);
        assertEquals(1, route.getPositionCount());
        NmeaPosition position = (NmeaPosition) route.getPositions().get(0);
        assertDoubleEquals(9.0567266, position.getLongitude());
        assertDoubleEquals(48.6239566, position.getLatitude());
        assertDoubleEquals(2.835, position.getSpeed());
        assertDoubleEquals(16.76, position.getElevation());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2004, 8, 7, 3, 29, 10);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertNull(position.getComment());
    }

    @Test
    public void testGGAAndRMCDateProblem() throws IOException {
        StringReader reader = new StringReader(
                "$GPGGA,134012.000,4837.4374,N,00903.4036,E,1,08,00.0,-48.7654,M,00.0,M,,*47\n" +
                        "$GPRMC,134012.000,A,4837.4374,N,00903.4036,E,3.00,0.00,260707,,*06"
        );
        ParserContext<NmeaRoute> context = new ParserContextImpl<NmeaRoute>();
        format.read(new BufferedReader(reader), null, ISO_LATIN1_ENCODING, context);
        List<NmeaRoute> routes = context.getRoutes();
        assertEquals(1, routes.size());
        NmeaRoute route = routes.get(0);
        assertEquals(1, route.getPositionCount());
        NmeaPosition position = route.getPositions().get(0);
        assertDoubleEquals(9.0567266, position.getLongitude());
        assertDoubleEquals(48.6239566, position.getLatitude());
        assertDoubleEquals(5.5560129, position.getSpeed());
        assertDoubleEquals(-48.7654, position.getElevation());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2007, 7, 26, 13, 40, 12);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertNull(position.getComment());

        StringWriter writer = new StringWriter();
        format.write(route, new PrintWriter(writer), 0, 1);
        String eol = System.getProperty("line.separator");
        String expectedLines = "$GPGGA,134012.000,4837.4374,N,00903.4036,E,1,8,,-48.8,M,,M,,*4F" + eol +
                "$GPWPL,4837.4374,N,00903.4036,E,*4C" + eol +
                "$GPRMC,134012.000,A,4837.4374,N,00903.4036,E,3.0,,260707,,A*69" + eol +
                "$GPZDA,134012.000,26,07,07,,*57" + eol +
                "$GPVTG,,T,,M,3.0,N,5.6,K,A*23" + eol;
        assertEquals(expectedLines, writer.getBuffer().toString());

        ParserContext<NmeaRoute> context2 = new ParserContextImpl<NmeaRoute>();
        format.read(new BufferedReader(new StringReader(writer.getBuffer().toString())), null, ISO_LATIN1_ENCODING, context2);
        List<NmeaRoute> routes2 = context2.getRoutes();
        assertEquals(1, routes2.size());
        NmeaRoute route2 = routes2.get(0);
        assertEquals(1, route2.getPositionCount());
        NmeaPosition position2 = route2.getPositions().get(0);
        assertDoubleEquals(9.0567266, position2.getLongitude());
        assertDoubleEquals(48.6239566, position2.getLatitude());
        assertDoubleEquals(-48.8, position2.getElevation());
        String actual2 = DateFormat.getDateTimeInstance().format(position2.getTime().getTime());
        assertEquals(expected, actual2);
        assertEquals(expectedCal, position2.getTime());
        assertNull(position2.getComment());
    }

    @Test
    public void testWestEastNorthSouthProblem() {
        NmeaPosition position = format.parsePosition("$GPRMC,062801.724,A,2608.6661,N,02758.8546,W,0.00,,160907,,,A*6B");
        assertDoubleEquals(-27.98091, position.getLongitude());
        assertDoubleEquals(26.1444349, position.getLatitude());

        position = format.parsePosition("$GPRMC,062801.724,A,2608.6661,N,02758.8546,E,0.00,,160907,,,A*6B");
        assertDoubleEquals(27.98091, position.getLongitude());
        assertDoubleEquals(26.1444349, position.getLatitude());

        position = format.parsePosition("$GPRMC,062801.724,A,2608.6661,S,02758.8546,W,0.00,,160907,,,A*6B");
        assertDoubleEquals(-27.98091, position.getLongitude());
        assertDoubleEquals(-26.1444349, position.getLatitude());

        position = format.parsePosition("$GPRMC,062801.724,A,2608.6661,S,02758.8546,E,0.00,,160907,,,A*6B");
        assertDoubleEquals(27.98091, position.getLongitude());
        assertDoubleEquals(-26.1444349, position.getLatitude());
    }

    @Test
    public void testSetLongitudeAndLatitudeAndElevation() {
        NmeaPosition position = format.parsePosition("$GPWPL,5334.169,N,01001.920,E,STATN1*22");
        assertDoubleEquals(1001.92, position.getLongitudeAsValueAndOrientation().getValue());
        assertDoubleEquals(5334.169, position.getLatitudeAsValueAndOrientation().getValue());
        assertDoubleEquals(10.0319999, position.getLongitude());
        assertDoubleEquals(53.5694833, position.getLatitude());
        assertNull(position.getElevation());
        position.setLongitude(19.02522);
        position.setLatitude(62.963395);
        position.setElevation(14.342);
        assertDoubleEquals(19.0252216, position.getLongitude());
        assertDoubleEquals(62.963395, position.getLatitude());
        assertNotNull(position.getElevation());
        assertDoubleEquals(14.342, position.getElevation());
        position.setLongitude(null);
        position.setLatitude(null);
        position.setElevation(null);
        assertNull(position.getLongitude());
        assertNull(position.getLatitude());
        assertNull(position.getElevation());
    }
}
