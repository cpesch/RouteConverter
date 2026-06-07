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

package slash.navigation.base;

import org.junit.Test;

import static org.junit.Assert.*;

public class Wgs84PositionTest {

    @Test
    public void testConstructorSetsAllFields() {
        Wgs84Position pos = new Wgs84Position(10.5, 48.3, 520.0, 60.0, null, "Munich");
        assertEquals(10.5, pos.getLongitude(), 0.0001);
        assertEquals(48.3, pos.getLatitude(), 0.0001);
        assertEquals(520.0, pos.getElevation(), 0.0001);
        assertEquals(60.0, pos.getSpeed(), 0.0001);
        assertNull(pos.getTime());
        assertEquals("Munich", pos.getDescription());
    }

    @Test
    public void testSetAndGetLongitude() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        pos.setLongitude(13.404);
        assertEquals(13.404, pos.getLongitude(), 0.0001);
    }

    @Test
    public void testSetAndGetLatitude() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        pos.setLatitude(52.520);
        assertEquals(52.520, pos.getLatitude(), 0.0001);
    }

    @Test
    public void testSetAndGetElevation() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        pos.setElevation(200.0);
        assertEquals(200.0, pos.getElevation(), 0.0001);
    }

    @Test
    public void testSetAndGetSpeed() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        pos.setSpeed(80.0);
        assertEquals(80.0, pos.getSpeed(), 0.0001);
    }

    @Test
    public void testSetAndGetDescription() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        pos.setDescription("Frankfurt");
        assertEquals("Frankfurt", pos.getDescription());
    }

    @Test
    public void testSetDescriptionToNull() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, "Initial");
        pos.setDescription(null);
        assertNull(pos.getDescription());
    }

    @Test
    public void testSetAndGetWaypointType() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        pos.setWaypointType(WaypointType.Airport);
        assertEquals(WaypointType.Airport, pos.getWaypointType());
    }

    @Test
    public void testWaypointTypeDefaultIsNull() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertNull(pos.getWaypointType());
    }

    @Test
    public void testSetAndGetHeading() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        pos.setHeading(270.0);
        assertEquals(270.0, pos.getHeading(), 0.0001);
    }

    @Test
    public void testHeadingDefaultIsNull() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertNull(pos.getHeading());
    }

    @Test
    public void testSetAndGetPressure() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        pos.setPressure(1013.25);
        assertEquals(1013.25, pos.getPressure(), 0.01);
    }

    @Test
    public void testSetAndGetTemperature() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        pos.setTemperature(23.5);
        assertEquals(23.5, pos.getTemperature(), 0.01);
    }

    @Test
    public void testSetAndGetHeartBeat() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        pos.setHeartBeat((short) 75);
        assertEquals((short) 75, (short) pos.getHeartBeat());
    }

    @Test
    public void testAsWgs84PositionReturnsSelf() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, "Test");
        assertSame(pos, pos.asWgs84Position());
    }

    @Test
    public void testEqualsAndHashCodeSameValues() {
        Wgs84Position a = new Wgs84Position(10.0, 50.0, 200.0, null, null, "Munich");
        Wgs84Position b = new Wgs84Position(10.0, 50.0, 200.0, null, null, "Munich");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualDifferentLongitude() {
        Wgs84Position a = new Wgs84Position(10.0, 50.0, null, null, null, null);
        Wgs84Position b = new Wgs84Position(11.0, 50.0, null, null, null, null);
        assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentDescription() {
        Wgs84Position a = new Wgs84Position(10.0, 50.0, null, null, null, "Munich");
        Wgs84Position b = new Wgs84Position(10.0, 50.0, null, null, null, "Berlin");
        assertNotEquals(a, b);
    }

    @Test
    public void testHasCoordinatesTrue() {
        Wgs84Position pos = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertTrue(pos.hasCoordinates());
    }

    @Test
    public void testHasCoordinatesFalseWhenNull() {
        Wgs84Position pos = new Wgs84Position(null, null, null, null, null, null);
        assertFalse(pos.hasCoordinates());
    }
}

