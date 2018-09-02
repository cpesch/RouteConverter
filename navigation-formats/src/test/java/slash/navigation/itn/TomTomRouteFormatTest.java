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

package slash.navigation.itn;

import org.junit.Test;
import slash.common.type.CompactCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;
import static slash.navigation.base.RouteComments.parseDescription;

public class TomTomRouteFormatTest {
    private TomTomRouteFormat format = new TomTom5RouteFormat();

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("1046348|5364352|Linau|1|"));
        assertTrue(format.isPosition("+1046348|+5364352|Linau|1|"));
        assertTrue(format.isPosition("-1046348|5364352|Linau|1|"));
        assertTrue(format.isPosition("-1046348|+5364352|Linau|1|"));
        assertTrue(format.isPosition("1046348|-5364352|Linau|1|"));
        assertTrue(format.isPosition("+1046348|-5364352|Linau|1|"));
        assertTrue(format.isPosition("-7342221|4111437||4|"));
        assertTrue(format.isPosition("980401|4894505|TC-Rp,27,|5|"));
        assertTrue(format.isPosition("980401|4894505|TC-Rp,27,|7|"));
        assertTrue(format.isPosition("718697|5334397|Borkum - Anleger|0|"));
        assertTrue(format.isPosition("991830|5755430|12:23:10 Start (#1)|0"));

        assertFalse(format.isPosition("1046348|5364352|Linau"));
        assertFalse(format.isPosition("1046348|5364352|Linau|"));
        assertFalse(format.isPosition("-+1046348|5364352|Linau|1"));
        assertFalse(format.isPosition("+-1046348|5364352|Linau|1"));
        assertFalse(format.isPosition("1046348|+-5364352|Linau|1"));
        assertFalse(format.isPosition("1046348|-+5364352|Linau|1"));

        assertFalse(format.isPosition("A|5364352|Linau|1|"));
        assertFalse(format.isPosition("1046348|B|Linau|1|"));
        assertFalse(format.isPosition("1046348|5364352|Linau|A|"));
        assertFalse(format.isPosition("1046348|5364352|Linau|10|"));
    }

    @Test
    public void testParsePosition() {
        TomTomPosition position = format.parsePosition("1003200|5356948|Hamburg/Uhlenhorst|4|");
        assertEquals(1003200, position.getLongitudeAsInt().intValue());
        assertEquals(5356948, position.getLatitudeAsInt().intValue());
        assertEquals("Hamburg/Uhlenhorst", position.getDescription());
    }

    @Test
    public void testParsePositionFromITNConv() {
        TomTomPosition position = format.parsePosition("+1003200|+5356948|Hamburg/Uhlenhorst|4|");
        assertEquals(1003200, position.getLongitudeAsInt().intValue());
        assertEquals(5356948, position.getLatitudeAsInt().intValue());
        assertEquals("Hamburg/Uhlenhorst", position.getDescription());
    }

    @Test
    public void testParsePositionWithNegativeNumbers() {
        TomTomPosition position = format.parsePosition("-4253127|-3910293|Nirgendwo|3|");
        assertEquals(-4253127, position.getLongitudeAsInt().intValue());
        assertEquals(-3910293, position.getLatitudeAsInt().intValue());
        assertEquals("Nirgendwo", position.getDescription());
    }

    @Test
    public void testParsePositionFromMotorradTourenplaner() {
        TomTomPosition position = format.parsePosition("1003200|5356949|Finkenau, Hamburg, Uhlenhorst (Hamburg) |2|");
        assertEquals(1003200, position.getLongitudeAsInt().intValue());
        assertEquals(5356949, position.getLatitudeAsInt().intValue());
        assertEquals("Finkenau, Hamburg, Uhlenhorst (Hamburg)", position.getDescription());
    }

    @Test
    public void testParsePositionFromTripmaster() {
        TomTomPosition position = format.parsePosition("992001|5356396|Abstand 6 - 11:32:26 - 34 m - Bahrenfeld|0|");
        assertEquals(992001, position.getLongitudeAsInt().intValue());
        assertEquals(5356396, position.getLatitudeAsInt().intValue());
        assertEquals("Bahrenfeld", position.getDescription());
        assertEquals("Bahrenfeld", position.getCity());
        assertDoubleEquals(34.0, position.getElevation());
        assertEquals("Abstand 6", position.getReason());
        assertEquals(calendar(1970, 1, 1, 11, 32, 26), position.getTime());
    }

    @Test
    public void testParsePositionFromTripmasterWithStrangeNullPointerException() {
        TomTomPosition position = format.parsePosition("967193|5362179|Punkt - 12:01:38 - 10.9 m - Holm DE (Pinneberg)|0|");
        assertEquals(967193, position.getLongitudeAsInt().intValue());
        assertEquals(5362179, position.getLatitudeAsInt().intValue());
        assertEquals("Holm DE (Pinneberg)", position.getDescription());
        assertDoubleEquals(10.9, position.getElevation());
        assertEquals("Punkt", position.getReason());
        assertEquals(calendar(1970, 1, 1, 12, 1, 38), position.getTime());
    }

    @Test
    public void testIsName() {
        assertTrue(format.isName("\"\""));
        assertTrue(format.isName("\"a\""));
        assertTrue(format.isName("\"abc\""));
        assertFalse(format.isName("\"\"\""));
    }

    @Test
    public void testParseName() {
        assertEquals("abc", format.parseName("\"abc\""));
    }

    @Test
    public void testSinglePositionFile() {
        TomTomPosition position = format.parsePosition("883644|4939999|kommandantenhaus|2|");
        assertEquals(883644, position.getLongitudeAsInt().intValue());
        assertEquals(4939999, position.getLatitudeAsInt().intValue());
        assertEquals("kommandantenhaus", position.getDescription());
    }

    @Test
    public void testSetLongitudeAndLatitudeAndElevation() {
        TomTomPosition position = format.parsePosition("992001|5356396|Abstand 6 - 11:32:26 - 34 m - Bahrenfeld|0|");
        assertEquals(992001, position.getLongitudeAsInt().intValue());
        assertEquals(5356396, position.getLatitudeAsInt().intValue());
        assertDoubleEquals(34.0, position.getElevation());
        position.setLongitude(19.02522);
        position.setLatitude(62.963395);
        position.setElevation(67.42);
        assertEquals(1902522, position.getLongitudeAsInt().intValue());
        assertEquals(6296339, position.getLatitudeAsInt().intValue());
        assertDoubleEquals(19.02522, position.getLongitude());
        assertDoubleEquals(62.96339, position.getLatitude());
        assertDoubleEquals(67.42, position.getElevation());
        position.setLongitude(null);
        position.setLatitude(null);
        position.setElevation(null);
        assertNull(position.getLongitudeAsInt());
        assertNull(position.getLatitudeAsInt());
        assertNull(position.getLongitude());
        assertNull(position.getLatitude());
        assertNull(position.getElevation());
    }

    @Test
    public void testFormatFirstName() {
        TomTomPosition position = format.parsePosition("883644|4939999|Los|2|");
        String description = format.formatFirstOrLastName(position, "Start", null);
        assertEquals("Los", description);
        position.setDescription(description);
        parseDescription(position, description);
        assertEquals("Los", position.getDescription());
    }

    private static final CompactCalendar DATE = calendar(2004, 8, 7, 3, 29, 10, 542);
    private static final CompactCalendar TIME = calendar(1970, 1, 1, 3, 29, 10, 542);

    @Test
    public void testFormatFirstNameWithDate() {
        TomTomPosition position = format.parsePosition("883644|4939999|Los|2|");
        position.setTime(DATE);
        String description = format.formatFirstOrLastName(position, "Start", null);
        assertEquals("Start : Los : 07/08/2004 03:29:10 - 0.0 m - 0.0 Km/h - 0.0 deg", description);
        position.setTime(null);
        position.setDescription(description);
        assertEquals("Los", position.getDescription());
        assertEquals("Start", position.getReason());
        assertEquals(DATE.getTimeInMillis() / 1000, position.getTime().getTimeInMillis() / 1000);

        position.setElevation(47.4);
        description = format.formatFirstOrLastName(position, "Start", null);
        assertEquals("Start : Los : 07/08/2004 03:29:10 - 47.4 m - 0.0 Km/h - 0.0 deg", description);
        position.setElevation(null);
        position.setDescription(description);
        assertDoubleEquals(47.4, position.getElevation());

        description = format.formatFirstOrLastName(position, "Start", 10.0);
        assertEquals("Start : Los : 07/08/2004 03:29:10 - 47.4 m - 0.0 Km/h - 0.0 deg - 10 Km", description);
        position.setElevation(null);
        position.setDescription(description);
        assertDoubleEquals(47.4, position.getElevation());
    }

    @Test
    public void testFormatLastName() {
        TomTomPosition position = format.parsePosition("883644|4939999|Los|2|");
        position.setTime(DATE);
        position.setElevation(82.4);
        position.setHeading(248.9);
        position.setSpeed(61.3);
        String description = format.formatFirstOrLastName(position, "Finish", 1354.4);
        assertEquals("Finish : Los : 07/08/2004 03:29:10 - 82.4 m - 61.3 Km/h - 248.9 deg - 1354 Km", description);
        position.setElevation(null);
        position.setTime(null);
        position.setDescription(description);
        assertDoubleEquals(82.4, position.getElevation());
        assertDoubleEquals(248.9, position.getHeading());
        assertDoubleEquals(61.3, position.getSpeed());
    }

    @Test
    public void testFormatIntermediateName() {
        TomTomPosition position = format.parsePosition("883644|4939999|Weiter|2|");
        String description = format.formatIntermediateName(position, null);
        assertEquals("Weiter", description);
        position.setDescription(description);
        parseDescription(position, description);
        assertEquals("Weiter", position.getDescription());
    }

    @Test
    public void testFormatIntermediateNameWithDateElevationSpeedAndHeading() {
        TomTomPosition position = format.parsePosition("883644|4939999|Weiter|2|");
        position.setTime(TIME);
        String description = format.formatIntermediateName(position, null);
        assertEquals("Weiter : 03:29:10 - 0.0 m - 0.0 Km/h - 0.0 deg", description);
        position.setTime(null);
        position.setDescription(description);
        assertEquals("Weiter", position.getDescription());
        assertNull(position.getReason());
        assertEquals(TIME.getTimeInMillis() / 1000, position.getTime().getTimeInMillis() / 1000);

        position.setElevation(47.4);
        position.setHeading(248.9);
        position.setSpeed(61.3);
        description = format.formatIntermediateName(position, 5.0);
        assertEquals("Weiter : 03:29:10 - 47.4 m - 61.3 Km/h - 248.9 deg - 5 Km", description);
        position.setElevation(null);
        position.setHeading(null);
        position.setSpeed(null);
        position.setDescription(description);
        assertEquals("Weiter", position.getDescription());
        assertDoubleEquals(47.4, position.getElevation());
        assertDoubleEquals(248.9, position.getHeading());
        assertDoubleEquals(61.3, position.getSpeed());
    }
}

