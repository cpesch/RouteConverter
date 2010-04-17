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

import slash.navigation.base.NavigationTestCase;

public class TTTracklogTest extends NavigationTestCase {

    public void testTTTracklogStartPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "12:23:10 Start (#1)");
        assertEquals("Start", position.getReason());
        assertEquals("12:23:10 Start (#1)", position.getCity());
        assertNull(position.getSpeed());
        assertNull(position.getElevation());
        assertEquals(calendar(1970, 1, 1, 12, 23, 10), position.getTime());
    }

    public void testTTTracklogSpeedAndElevationPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "13:31 v=54.5 alt=79 (#3634)");
        assertEquals("v=54.5 alt=79", position.getReason());
        assertEquals("13:31 v=54.5 alt=79 (#3634)", position.getCity());
        assertEquals(54.5, position.getSpeed());
        assertEquals(79.0, position.getElevation());
        assertEquals(calendar(1970, 1, 1, 13, 31, 0), position.getTime());
    }

    public void testTTTracklogPausePosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "13:58 0.7 min Pause (#5444)");
        assertEquals("0.7 min Pause", position.getReason());
        assertEquals("13:58 0.7 min Pause (#5444)", position.getCity());
        assertNull(position.getSpeed());
        assertNull(position.getElevation());
        assertEquals(calendar(1970, 1, 1, 13, 58, 0), position.getTime());
    }

    public void testTTTracklogPauseWithElevationPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "15:04 0.7 min Pause 48.2m (#1377)");
        assertEquals("0.7 min Pause 48.2m", position.getReason());
        assertEquals("15:04 0.7 min Pause 48.2m (#1377)", position.getCity());
        assertNull(position.getSpeed());
        assertEquals(48.2, position.getElevation());
        assertEquals(calendar(1970, 1, 1, 15, 4, 0), position.getTime());
    }
}