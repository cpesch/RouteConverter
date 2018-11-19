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

package slash.navigation.tour;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;

public class TourFormatTest {
    private TourFormat format = new TourFormat();

    @Test
    public void testIsSectionTitle() {
        assertTrue(format.isSectionTitle("[0]"));
        assertTrue(format.isSectionTitle("[1]"));
        assertTrue(format.isSectionTitle("[1234]"));
        assertTrue(format.isSectionTitle("[TOUR]"));
        assertTrue(format.isSectionTitle("[HOME]"));

        assertFalse(format.isSectionTitle("[Tour]"));
        assertFalse(format.isSectionTitle("[Egal]"));
        assertFalse(format.isSectionTitle("[MapLage]"));
        assertFalse(format.isSectionTitle("[CLIENT]"));
        assertFalse(format.isSectionTitle("[COORDINATES]"));
        assertFalse(format.isSectionTitle("[DESCRIPTION]"));
        assertFalse(format.isSectionTitle("[ROUTE]"));
    }

    @Test
    public void testTourPosition() {
        Map<String, String> nameValues = new HashMap<>();
        nameValues.put("Visited", "0");
        TourPosition position = new TourPosition(1489415L, 6886471L, "10117", "Berlin", "Unter den Linden", "7", "Staatsoper unter den Linden", true, nameValues);
        position.put("Assembly", "FalkNavigator");
        assertEquals((Long) 1489415L, position.getX());
        assertEquals((Long) 6886471L, position.getY());
        assertDoubleEquals(13.39463, position.getLongitude());
        assertDoubleEquals(52.51718, position.getLatitude());
        assertEquals("10117 Berlin, Unter den Linden 7, Staatsoper unter den Linden", position.getDescription());
        assertNull(position.getElevation());
        assertNull(position.getTime());
        assertEquals("0", position.get("Visited"));
        assertEquals("FalkNavigator", position.get("Assembly"));
        assertTrue(position.isHome());
    }

    @Test
    public void testSetDescription() {
        TourPosition position = new TourPosition(null, null, "10117", "Berlin", "Unter den Linden", "7", "Staatsoper unter den Linden", false, new HashMap<String, String>());
        position.setDescription("ABC");
        assertEquals("ABC", position.getDescription());
    }
}
