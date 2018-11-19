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

package slash.navigation.columbus;

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.Wgs84Position;

import java.text.DateFormat;

import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;

public class ColumbusGpsType1FormatTest {
    private ColumbusGpsType1Format format = new ColumbusGpsType1Format();

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX"));
        assertTrue(format.isValidLine("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,ALTITUDE,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX"));
        assertTrue(format.isValidLine("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,VOX"));
        assertTrue(format.isValidLine("1150  ,T,090522,150532,48.206931N,016.372713E,-5   ,0   ,0  ,3D,SPS ,2.3  ,2.1  ,1.0  ,"));
        assertTrue(format.isValidLine("7     ,T,151216,084034,53.569869N,010.027401E,0    ,0   ,0  ,3D,SPS ,1.2  ,0.8  ,0.8  ,"));

        assertTrue(format.isValidLine("2852\u0000\u0000\u0000\u0000\u0000,T,120811,141223,50.149103N,008.570144E,196\u0000\u0000,0\u0000\u0000\u0000,0\u0000\u0000,3D,SPS ,1.6\u0000\u0000,1.3\u0000\u0000,0.9\u0000\u0000,\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"));
        assertTrue(format.isValidLine("1,T,130830,145806,50.636938N,008.141175E,643,0,0,,,,,,"));

        assertTrue(format.isValidLine("4     ,T,090421,061054,47.797283N,013.049748E,519  ,5   ,206,         "));
        assertTrue(format.isValidLine("7\u0000\u0000\u0000\u0000\u0000,V,090421,061109,47.797191N,013.049593E,500\u0000\u0000,0\u0000\u0000\u0000,206,VOX00014 "));
        assertTrue(format.isValidLine("297   ,G,130630,032828,23.644383N,113.650126E,14   ,0   ,0  ,"));

        assertFalse(format.isValidLine("422   ,G,150911,092432,47.456091N,010.992004E,1135 ,0   ,0  ,1D,SPS ,2.0  ,1.8  ,1.0  ,         "));
        assertFalse(format.isValidLine("422   ,G,150911,092432,47.456091N,010.992004E,1135 ,0   ,0  ,2D,S ,2.0  ,1.8  ,1.0  ,         "));

        assertFalse(format.isValidLine("4     ,T,090421,061054,-47.797283N,013.049748E,519  ,5   ,206,         "));
        assertFalse(format.isValidLine("4     ,T,090421,061054,47.797283N,-013.049748E,519  ,5   ,206,         "));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("2971  ,V,090508,084815,48.132451N,016.321871E,319  ,12  ,207,3D,SPS ,1.6  ,1.3  ,0.9  ,VOX02971"));
        assertTrue(format.isPosition("1,T,130830,145806,50.636938N,008.141175E,643,0,0,,,,,,"));
        assertTrue(format.isPosition("3     ,T,151216,084026,53.570485N,010.025716E,0    ,0   ,0  ,3D,SPS ,1.9  ,1.6  ,1.0  ,         "));
        assertTrue(format.isPosition("3\u0000\u0000\u0000\u0000\u0000,T,151216,084026,53.570485N,010.025716E,0\u0000\u0000\u0000\u0000,0\u0000\u0000\u0000,0\u0000\u0000,3D,SPS ,1.9\u0000\u0000,1.6\u0000\u0000,1.0\u0000\u0000,         "));

        assertTrue(format.isPosition("5     ,T,090421,061057,47.797281N,013.049743E,504  ,0   ,206,         "));
        assertTrue(format.isPosition("3434  ,T,121126,083100,09.941796N,076.267059E,0    ,14  ,0  ,         "));
        assertTrue(format.isPosition("3434  ,G,121126,083100,09.941796N,076.267059E,0    ,14  ,0  ,         "));
        assertTrue(format.isPosition("13699 ,T,160809,175228,37.120038N,097.401658W,288  ,2   ,359,         "));

        assertFalse(format.isPosition("13699 ,T,160809,175228,37.120038N,097.401658W,288  ,0 2 ,359,         "));
        assertFalse(format.isPosition("754   ,T,090527,110453,47.842436N,013.276313E,681  ,0 1 ,109,3D,SPS ,3.9  ,3.8  ,1.0  ,"));
        assertFalse(format.isPosition("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX"));
        assertFalse(format.isPosition("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,VOX"));
    }

    @Test
    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("2971  ,V,090508,084815,48.132451N,016.321871E,319  ,12  ,207,3D,SPS ,1.6  ,1.3  ,0.9  ,VOX02971", new ParserContextImpl());
        assertDoubleEquals(16.321871, position.getLongitude());
        assertDoubleEquals(48.132451, position.getLatitude());
        assertDoubleEquals(319.0, position.getElevation());
        assertDoubleEquals(12.0, position.getSpeed());
        assertDoubleEquals(207.0, position.getHeading());
        assertDoubleEquals(1.6, position.getPdop());
        assertDoubleEquals(1.3, position.getHdop());
        assertDoubleEquals(0.9, position.getVdop());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2009, 5, 8, 8, 48, 15);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertEquals("VOX02971.wav", position.getDescription());
    }

    @Test
    public void testParseTypeBPosition() {
        Wgs84Position position = format.parsePosition("6     ,T,090421,061058,47.797278N,013.049739E,502  ,8 ,206,VOX00006 ", new ParserContextImpl());
        assertDoubleEquals(13.049739, position.getLongitude());
        assertDoubleEquals(47.797278, position.getLatitude());
        assertDoubleEquals(502.0, position.getElevation());
        assertDoubleEquals(8.0, position.getSpeed());
        assertDoubleEquals(206.0, position.getHeading());
        assertNull(position.getHdop());
        assertNull(position.getSatellites());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2009, 4, 21, 6, 10, 58);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertEquals("VOX00006", position.getDescription());
    }

    @Test
    public void testParseTypeBSouthWestPosition() {
        Wgs84Position position = format.parsePosition("6     ,V,090421,061058,47.797278S,013.049739W,-102  ,8   ,206,", new ParserContextImpl());
        assertDoubleEquals(-13.049739, position.getLongitude());
        assertDoubleEquals(-47.797278, position.getLatitude());
        assertDoubleEquals(-102.0, position.getElevation());
        assertEquals("Voice 6.wav", position.getDescription());
    }

    @Test
    public void testParseTypeBPOICPosition() {
        Wgs84Position position = format.parsePosition("7     ,C,090421,061058,47.797278S,013.049739W,502  ,8   ,206,", new ParserContextImpl());
        assertEquals("PointOfInterestC 7", position.getDescription());
    }

    @Test
    public void testParseTypeBPOIDPosition() {
        Wgs84Position position = format.parsePosition("8     ,D,090421,061058,47.797278S,013.049739W,502  ,8   ,206,", new ParserContextImpl());
        assertEquals("PointOfInterestD 8", position.getDescription());
    }
}