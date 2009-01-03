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

import junit.framework.Assert;
import slash.navigation.NavigationFileParser;
import slash.navigation.NavigationTestCase;
import slash.navigation.RouteCharacteristics;

import java.io.File;
import java.io.IOException;

public class ItnFormatTest extends NavigationTestCase {
    ItnFormat format = new ItnFormat();

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

        assertFalse(format.isPosition("1046348|5364352|Linau|1"));
        assertFalse(format.isPosition("-+1046348|5364352|Linau|1"));
        assertFalse(format.isPosition("+-1046348|5364352|Linau|1"));
        assertFalse(format.isPosition("1046348|+-5364352|Linau|1"));
        assertFalse(format.isPosition("1046348|-+5364352|Linau|1"));

        assertFalse(format.isPosition("A|5364352|Linau|1|"));
        assertFalse(format.isPosition("1046348|B|Linau|1|"));
        assertFalse(format.isPosition("1046348|5364352|Linau|A|"));
        assertFalse(format.isPosition("1046348|5364352|Linau|10|"));
    }

    public void testParsePosition() {
        ItnPosition position = format.parsePosition("1003200|5356948|Hamburg/Uhlenhorst|4|");
        assertEquals(1003200, (int)position.getLongitudeAsInt());
        assertEquals(5356948, (int)position.getLatitudeAsInt());
        assertEquals("Hamburg/Uhlenhorst", position.getComment());
    }

    public void testParsePositionFromITNConv() {
        ItnPosition position = format.parsePosition("+1003200|+5356948|Hamburg/Uhlenhorst|4|");
        assertEquals(1003200, (int)position.getLongitudeAsInt());
        assertEquals(5356948, (int)position.getLatitudeAsInt());
        assertEquals("Hamburg/Uhlenhorst", position.getComment());
    }

    public void testParsePositionWithNegativeNumbers() {
        ItnPosition position = format.parsePosition("-4253127|-3910293|Nirgendwo|3|");
        assertEquals(-4253127, (int)position.getLongitudeAsInt());
        assertEquals(-3910293, (int)position.getLatitudeAsInt());
        assertEquals("Nirgendwo", position.getComment());
    }

    public void testParsePositionFromMotorradTourenplaner() {
        ItnPosition position = format.parsePosition("1003200|5356949|Finkenau, Hamburg, Uhlenhorst (Hamburg) |2|");
        assertEquals(1003200, (int)position.getLongitudeAsInt());
        assertEquals(5356949, (int)position.getLatitudeAsInt());
        assertEquals("Finkenau, Hamburg, Uhlenhorst (Hamburg)", position.getComment());
    }

    public void testParsePositionFromTripmaster() {
        ItnPosition position = format.parsePosition("992001|5356396|Abstand 6 - 11:32:26 - 34 m - Bahrenfeld|0|");
        assertEquals(992001, (int)position.getLongitudeAsInt());
        assertEquals(5356396, (int)position.getLatitudeAsInt());
        assertEquals("Bahrenfeld", position.getComment());
        assertEquals("Bahrenfeld", position.getCity());
        assertEquals(34.0, position.getElevation());
        assertEquals("Abstand 6", position.getReason());
        assertEquals(calendar(1970, 1, 1, 11, 32, 26), position.getTime());
    }

    public void testParsePositionFromTripmasterWithStrangeNullPointerException() {
        ItnPosition position = format.parsePosition("967193|5362179|Punkt - 12:01:38 - 10.9 m - Holm DE (Pinneberg)|0|");
        assertEquals(967193, (int)position.getLongitudeAsInt());
        assertEquals(5362179, (int)position.getLatitudeAsInt());
        assertEquals("Holm DE (Pinneberg)", position.getComment());
        assertEquals(10.9, position.getElevation());
        assertEquals("Punkt", position.getReason());
        assertEquals(calendar(1970, 1, 1, 12, 1, 38), position.getTime());
    }

    public void testIsName() {
        assertTrue(format.isName("\"\""));
        assertTrue(format.isName("\"a\""));
        assertTrue(format.isName("\"abc\""));
        assertFalse(format.isName("\"\"\""));
    }

    public void testParseName() {
        assertEquals("abc", format.parseName("\"abc\""));
    }

    public void testIsPlainRouteRouteCharacteristics() throws IOException {
        File source = new File(SAMPLE_PATH + "bcr_with_itnconv.itn");
        NavigationFileParser parser = new NavigationFileParser();
        assertTrue(parser.read(source));
        Assert.assertEquals(RouteCharacteristics.Route, parser.getTheRoute().getCharacteristics());
    }

    public void testIsTripmasterTrackRouteCharacteristics() throws IOException {
        File source = new File(SAMPLE_PATH + "tripmaster2.itn");
        NavigationFileParser parser = new NavigationFileParser();
        assertTrue(parser.read(source));
        assertEquals(RouteCharacteristics.Track, parser.getTheRoute().getCharacteristics());
    }

    public void testSinglePositionFile() throws IOException {
        ItnPosition position = format.parsePosition("883644|4939999|kommandantenhaus|2|");
        assertEquals(883644, (int)position.getLongitudeAsInt());
        assertEquals(4939999, (int)position.getLatitudeAsInt());
        assertEquals("kommandantenhaus", position.getComment());

        File source = new File(SAMPLE_PATH + "dilsberg kommandantenhaus.itn");
        NavigationFileParser parser = new NavigationFileParser();
        assertTrue(parser.read(source));
        assertEquals(RouteCharacteristics.Route, parser.getTheRoute().getCharacteristics());
        assertEquals(1, parser.getTheRoute().getPositionCount());
    }

    public void testIsNamedByTyre() throws IOException {
        File source = new File(SAMPLE_PATH + "itn_with_tyre.itn");
        NavigationFileParser parser = new NavigationFileParser();
        assertTrue(parser.read(source));
        Assert.assertEquals("Eis essen in Ratzeburg", parser.getTheRoute().getName());
    }

    public void testSetLongitudeAndLatitudeAndElevation() {
        ItnPosition position = format.parsePosition("992001|5356396|Abstand 6 - 11:32:26 - 34 m - Bahrenfeld|0|");
        assertEquals(992001, (int)position.getLongitudeAsInt());
        assertEquals(5356396, (int)position.getLatitudeAsInt());
        assertEquals(34.0, position.getElevation());
        position.setLongitude(19.02522);
        position.setLatitude(62.963395);
        position.setElevation(67.42);
        assertEquals(1902522, (int)position.getLongitudeAsInt());
        assertEquals(6296339, (int)position.getLatitudeAsInt());
        assertEquals(19.02522, position.getLongitude());
        assertEquals(62.96339, position.getLatitude());
        assertEquals(67.42, position.getElevation());
        position.setLongitude(null);
        position.setLatitude(null);
        position.setElevation(null);
        assertNull(position.getLongitudeAsInt());
        assertNull(position.getLatitudeAsInt());
        assertNull(position.getLongitude());
        assertNull(position.getLatitude());
        assertNull(position.getElevation());
    }
}
