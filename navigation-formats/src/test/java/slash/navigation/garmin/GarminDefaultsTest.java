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

package slash.navigation.garmin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static slash.navigation.garmin.GarminDefaults.*;

public class GarminDefaultsTest {

    @Test
    public void testWaypointIconIsTheSameForAllFormats() {
        assertEquals("Waypoint", DEFAULT_ICON_DESCRIPTION);
        for (Format format : Format.values())
            assertEquals(18, waypointIconNumber(format));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWaypointIconWithoutFormat() {
        waypointIconNumber(null);
    }

    @Test
    public void testMapSourceWaypointIconPerVersion() {
        assertEquals(18, mapSourceWaypointIconNumber(3));
        assertEquals(18, mapSourceWaypointIconNumber(4));
        assertEquals(18, mapSourceWaypointIconNumber(5));
    }

    @Test
    public void testMapSourceConvertedIconNumber() {
        // version 5 knows every symbol
        assertEquals(139, mapSourceConvertedIconNumber(139, 5));
        assertEquals(120, mapSourceConvertedIconNumber(120, 5));
        assertEquals(117, mapSourceConvertedIconNumber(117, 5));
        // version 4 lacks the water hydrant
        assertEquals(18, mapSourceConvertedIconNumber(139, 4));
        assertEquals(120, mapSourceConvertedIconNumber(120, 4));
        assertEquals(117, mapSourceConvertedIconNumber(117, 4));
        // version 3 lacks contacts and draws geocaches as "Circle with X"
        assertEquals(18, mapSourceConvertedIconNumber(139, 3));
        assertEquals(18, mapSourceConvertedIconNumber(119, 3));
        assertEquals(18, mapSourceConvertedIconNumber(138, 3));
        assertEquals(65, mapSourceConvertedIconNumber(117, 3));
        assertEquals(65, mapSourceConvertedIconNumber(118, 3));
        assertEquals(116, mapSourceConvertedIconNumber(116, 3));
        assertEquals(140, mapSourceConvertedIconNumber(140, 3));
    }

    @Test
    public void testGdbWaypointIconPerVersion() {
        assertEquals(18, gdbWaypointIconNumber(1));
        assertEquals(18, gdbWaypointIconNumber(2));
        assertEquals(18, gdbWaypointIconNumber(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGdbWaypointIconForUnknownVersion() {
        gdbWaypointIconNumber(4);
    }

    @Test
    public void testGdbWaypointClassAndDisplay() {
        assertEquals(0, gdbWaypointClass(false));
        assertEquals(8, gdbWaypointClass(true));
        assertEquals(GDB_DISPLAY_MODE_SYMBOL_AND_NAME, gdbWaypointDisplayMode(gdbWaypointClass(false)));
        assertEquals(GDB_DISPLAY_MODE_SYMBOL, gdbWaypointDisplayMode(gdbWaypointClass(true)));
        assertEquals(1, GDB_DISPLAY_MODE_SYMBOL_AND_NAME);
        assertEquals(0, GDB_DISPLAY_MODE_SYMBOL);
        assertEquals(2, GDB_DISPLAY_MODE_SYMBOL_AND_COMMENT);
        assertEquals(0, GDB_WAYPOINT_COLOR);
    }

    @Test
    public void testMapSourceDefaults() {
        assertEquals(0, MAPSOURCE_DEFAULT_CLASS);
        assertEquals(8, MAPSOURCE_HIDDEN_ROUTE_WAYPOINT_CLASS);
        assertEquals(0, mapSourceWaypointClass(false));
        assertEquals(8, mapSourceWaypointClass(true));
        assertEquals(1, MAPSOURCE_WAYPOINT_DISPLAY);
        assertEquals(0, MAPSOURCE_WAYPOINT_COLOR);
        assertEquals(1, MAPSOURCE_TRACK_DISPLAY);
        assertEquals(0, MAPSOURCE_TRACK_COLOR);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapSourceWaypointIconForVersionBelow3() {
        mapSourceWaypointIconNumber(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapSourceWaypointIconForVersionAbove5() {
        mapSourceWaypointIconNumber(6);
    }

    @Test
    public void testColorTable() {
        assertEquals(UNKNOWN_COLOR, colorValue(0));
        assertEquals(0x000000, colorValue(1));
        assertEquals(0x0000FF, colorValue(10));
        assertEquals(0xFF00FF, colorValue(14));
        assertEquals(0xFFFFFF, colorValue(16));
        assertEquals(UNKNOWN_COLOR, colorValue(17));
        assertEquals(UNKNOWN_COLOR, colorValue(18));
        assertEquals(UNKNOWN_COLOR, colorValue(-1));
        assertEquals(UNKNOWN_COLOR, colorValue(0xFFFFFFFFL));
        assertEquals("Unknown", colorName(0));
        assertEquals("Magenta", colorName(14));
        assertEquals("Transparent", colorName(17));
        assertEquals("Unknown", colorName(99));
    }

    @Test
    public void testColorIndexByRgb() {
        assertEquals(0, colorIndexByRgb(UNKNOWN_COLOR));
        assertEquals(1, colorIndexByRgb(0x000000));
        assertEquals(14, colorIndexByRgb(0xFF00FF));
        assertEquals(0, colorIndexByRgb(0x123456));
        for (int i = 1; i < 17; i++)
            assertEquals(i, colorIndexByRgb(colorValue(i)));
    }

    @Test
    public void testGdbRouteAndTrackColorDefaults() {
        assertEquals(14, gdbRouteColorIndex(UNKNOWN_COLOR));
        assertEquals(10, gdbRouteColorIndex(0x0000FF));
        assertEquals(0, gdbTrackColorIndex(UNKNOWN_COLOR));
        assertEquals(10, gdbTrackColorIndex(0x0000FF));
    }
}
