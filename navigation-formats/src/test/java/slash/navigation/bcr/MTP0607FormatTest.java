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

package slash.navigation.bcr;

import org.junit.Test;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;

import java.io.*;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.io.Transfer.ISO_LATIN1_ENCODING;
import static slash.navigation.bcr.BcrPosition.NO_ALTITUDE_DEFINED;
import static slash.navigation.bcr.BcrPosition.STREET_DEFINES_CENTER_NAME;

public class MTP0607FormatTest {
    private MTP0607Format format = new MTP0607Format();
    private BcrRoute route = new BcrRoute(format, "RouteName", asList("Description1", "Description2"), asList(new BcrPosition(1, 2, 3, "Start"), new BcrPosition(3, 4, 5, "End")));

    @Test
    public void testIsSectionTitle() {
        assertTrue(format.isSectionTitle("[CLIENT]"));
        assertTrue(format.isSectionTitle("[COORDINATES]"));
        assertTrue(format.isSectionTitle("[DESCRIPTION]"));
        assertTrue(format.isSectionTitle("[ROUTE]"));

        assertFalse(format.isSectionTitle(" [COORDINATES]"));
        assertFalse(format.isSectionTitle("[DESCRIPTION] "));
        assertFalse(format.isSectionTitle(" [ROUTE] "));
        assertFalse(format.isSectionTitle("[[ROUTE]"));
        assertFalse(format.isSectionTitle("ROUTE]"));

        assertFalse(format.isSectionTitle("[Egal]"));
        assertFalse(format.isSectionTitle("[Symbol 1]"));
        assertFalse(format.isSectionTitle("[Symbol 12]"));
        assertFalse(format.isSectionTitle("[Symbol 123]"));
        assertFalse(format.isSectionTitle("[Symbol 1234]"));
        assertFalse(format.isSectionTitle("[Overlay]"));
        assertFalse(format.isSectionTitle("[MapLage]"));
    }

    @Test
    public void testParsePositionWithStreet() {
        BcrPosition position = format.parsePosition("TOWN,210945415755", "1115508,7081108", "D 22081,Hamburg/Uhlenhorst,Finkenau,0,");
        assertEquals(210945415755L, position.getAltitude());
        assertEquals(1115508, (long) position.getX());
        assertEquals(7081108, (long) position.getY());
        assertEquals("D 22081", position.getZipCode());
        assertEquals("Hamburg/Uhlenhorst", position.getCity());
        assertEquals("Finkenau", position.getStreet());
        assertEquals("0", position.getType());
        assertFalse(position.isUnstructured());
    }

    @Test
    public void testParsePositionFromMTP20082009() {
        BcrPosition position = format.parsePosition("TOWN,210945415755,1", "1115508,7081108", "D 22081,Hamburg/Uhlenhorst,Finkenau,0,");
        assertEquals(210945415755L, position.getAltitude());
        assertEquals(1115508, (long) position.getX());
        assertEquals(7081108, (long) position.getY());
        assertEquals("D 22081", position.getZipCode());
        assertEquals("Hamburg/Uhlenhorst", position.getCity());
        assertEquals("Finkenau", position.getStreet());
        assertEquals("0", position.getType());
        assertFalse(position.isUnstructured());
    }

    @Test
    public void testParsePosition() {
        BcrPosition position = format.parsePosition("Standort,999999999", "1139093,7081574", "bei D 22885,Barsbüttel/Stemwarde,,0,");
        assertEquals(999999999, position.getAltitude());
        assertEquals(1139093, (long) position.getX());
        assertEquals(7081574, (long) position.getY());
        assertEquals("bei D 22885", position.getZipCode());
        assertEquals("Barsbüttel/Stemwarde", position.getCity());
        assertNull(position.getStreet());
        assertEquals("0", position.getType());
        assertFalse(position.isUnstructured());
    }

    @Test
    public void testParsePositionFromMotorradTourenplaner() {
        BcrPosition position = format.parsePosition("Standort,999999999", "1115508,7081108", "Großensee/Schwarzeka,,@,0,");
        assertNull(position.getZipCode());
        assertEquals("Großensee/Schwarzeka", position.getCity());
        assertEquals(STREET_DEFINES_CENTER_NAME, position.getStreet());
        assertEquals("0", position.getType());
        assertFalse(position.isUnstructured());
    }

    @Test
    public void testParsePositionFromITNConv() {
        BcrPosition position = format.parsePosition("Standort,999999999", "1115508,7081108", "Hamburg/Uhlenhorst");
        assertEquals(999999999, position.getAltitude());
        assertEquals(1115508, (long) position.getX());
        assertEquals(7081108, (long) position.getY());
        assertNull(position.getZipCode());
        assertEquals("Hamburg/Uhlenhorst", position.getCity());
        assertNull(position.getStreet());
        assertNull(position.getType());
        assertTrue(position.isUnstructured());
    }

    @Test
    public void testParseNegativePosition() {
        BcrPosition position = format.parsePosition("Standort,999999999", "-449242,6182322", "bei F 29400,Lampaul Guimiliau,@,0,");
        assertEquals("bei F 29400", position.getZipCode());
        assertEquals("Lampaul Guimiliau", position.getCity());
        assertEquals(STREET_DEFINES_CENTER_NAME, position.getStreet());
        assertEquals("0", position.getType());
        assertFalse(position.isUnstructured());
        assertEquals(NO_ALTITUDE_DEFINED, position.getAltitude());
        assertEquals(-449242, (long) position.getX());
        assertEquals(6182322, (long) position.getY());
    }

    @Test
    public void testSetdescription() {
        BcrPosition position = format.parsePosition("TOWN,210845415855", "2115508,9081108", null);
        assertEquals(210845415855L, position.getAltitude());
        assertEquals(2115508, (long) position.getX());
        assertEquals(9081108, (long) position.getY());
        assertNull(position.getZipCode());
        assertNull(position.getCity());
        assertNull(position.getStreet());
        assertNull(position.getType());
        assertNull(position.getDescription());
        assertTrue(position.isUnstructured());
        position.setDescription(null);
        assertNull(position.getDescription());
    }

    @Test
    public void testSetLongitudeAndLatitudeAndElevation() {
        BcrPosition position = format.parsePosition("TOWN,210945416161", "2115508,9081108", null);
        assertEquals(2115508, (long) position.getX());
        assertEquals(9081108, (long) position.getY());
        assertDoubleEquals(55.52, position.getElevation());
        position.setLongitude(19.02522);
        position.setLatitude(62.963395);
        position.setElevation(14.42);
        assertEquals(2115508, (long) position.getX());
        assertEquals(9081108, (long) position.getY());
        assertDoubleEquals(19.02522, position.getLongitude());
        assertDoubleEquals(62.96339, position.getLatitude());
        assertDoubleEquals(14.42, position.getElevation());
        position.setLongitude(null);
        position.setLatitude(null);
        position.setElevation(null);
        assertNull(position.getX());
        assertNull(position.getY());
        assertNull(position.getLongitude());
        assertNull(position.getLatitude());
        assertNull(position.getElevation());
    }

    @Test
    public void testSetdescriptionForMTPFirstAndLastPosition() {
        BcrPosition position = new BcrPosition(1, 2, 3, ",Hamburg/Uhlenhorst,,0,");
        assertNull(position.getZipCode());
        assertEquals("Hamburg/Uhlenhorst", position.getCity());
        assertNull(position.getStreet());
        assertEquals("0", position.getType());
        assertFalse(position.isUnstructured());
    }

    @Test
    public void testReaddescription() throws IOException {
        StringWriter writer = new StringWriter();
        format.write(route, new PrintWriter(writer), 0, 2);
        ParserContext<BcrRoute> context = new ParserContextImpl<>();
        format.read(new BufferedReader(new StringReader(writer.toString())), ISO_LATIN1_ENCODING, context);
        List<BcrRoute> routes = context.getRoutes();
        assertEquals(1, routes.size());
        BcrRoute route = routes.get(0);
        List<BcrPosition> positions = route.getPositions();
        assertEquals(2, positions.size());
        BcrPosition position1 = positions.get(0);
        assertEquals("Start", position1.getDescription());
        BcrPosition position2 = positions.get(1);
        assertEquals("End", position2.getDescription());
    }

    @Test
    public void testWritedescription() {
        StringWriter writer = new StringWriter();
        format.write(route, new PrintWriter(writer), 0, 2);
        assertTrue(writer.toString().contains("STATION1=Start"));
        assertTrue(writer.toString().contains("STATION2=End"));
    }
}
