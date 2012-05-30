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
import static org.junit.Assert.assertNull;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;

public class LogposTest {

    @Test
    public void testLogpos1aPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "080629 07:33:00: + Eschelbach (Rhein-Neckar-Kreis, Baden-Wuerttemberg); L612 @196.9m (s=66 d=91)");
        assertEquals("s=66 d=91", position.getReason());
        assertEquals("Eschelbach (Rhein-Neckar-Kreis, Baden-Wuerttemberg); L612", position.getCity());
        assertDoubleEquals(66.0, position.getSpeed());
        assertDoubleEquals(91.0, position.getHeading());
        assertDoubleEquals(196.9, position.getElevation());
        CompactCalendar expectedCal = calendar(2008, 6, 29, 7, 33, 0);
        assertEquals(expectedCal, position.getTime());
    }

    @Test
    public void testLogpos1bPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "090314 08:05:55: * 1000462:4889518 @365.8m (s=1 d=193)");
        assertEquals("s=1 d=193", position.getReason());
        assertEquals("1000462:4889518", position.getCity());
        assertDoubleEquals(1.0, position.getSpeed());
        assertDoubleEquals(193.0, position.getHeading());
        assertDoubleEquals(365.8, position.getElevation());
        assertEquals(calendar(2009, 3, 14, 8, 5, 55), position.getTime());
    }

    @Test
    public void testLogpos1cPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "090314 07:36:52: = 1000466:4889529 (@365.8m 090314 07:36:52 - 090314 08:02:04)");
        assertEquals("090314 07:36:52 - 090314 08:02:04", position.getReason());
        assertEquals("1000466:4889529", position.getCity());
        assertNull(position.getSpeed());
        assertDoubleEquals(365.8, position.getElevation());
        assertEquals(calendar(2009, 3, 14, 7, 36, 52), position.getTime());
    }

    @Test
    public void testLogpos1dPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "081012 19:00:47: * 17159970:-4176126 @?m (s=12 d=170)");
        assertEquals("s=12 d=170", position.getReason());
        assertEquals("17159970:-4176126", position.getCity());
        assertDoubleEquals(12.0, position.getSpeed());
        assertDoubleEquals(170.0, position.getHeading());
        assertNull(position.getElevation());
        assertEquals(calendar(2008, 10, 12, 19, 0, 47), position.getTime());
    }

    @Test
    public void testLogpos2Position() {
        TomTomPosition position = new TomTomPosition(0, 0, "080530 08:11:44: + Neuhaus Im Solling (Holzminden); B497 In Der Fahrt; 3  (s=69 d=207)");
        assertEquals("s=69 d=207", position.getReason());
        assertEquals("Neuhaus Im Solling (Holzminden); B497 In Der Fahrt; 3", position.getCity());
        assertDoubleEquals(69.0, position.getSpeed());
        assertDoubleEquals(207.0, position.getHeading());
        assertNull(position.getElevation());
        assertEquals(calendar(2008, 5, 30, 8, 11, 44), position.getTime());
    }
}