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

import static org.junit.Assert.*;

public class NmeaPositionTest {
    private static final double EPSILON = 0.0000001;

    @Test
    public void degreesRoundTripThroughNmeaStorage() {
        NmeaPosition position = new NmeaPosition(903.4036, "E", 4837.4374, "S", 16.76, null, null, null, null);
        assertEquals(9.0567266667, position.getLongitude(), EPSILON);
        assertEquals(-48.6239566667, position.getLatitude(), EPSILON);
    }

    @Test
    public void settersStoreValueAndOrientationInNmeaForm() {
        NmeaPosition position = new NmeaPosition(null, null, null, null, null, null, null, null, null);
        position.setLongitude(9.0567266667);
        position.setLatitude(-48.6239566667);
        assertEquals(9.0567266667, position.getLongitude(), EPSILON);
        assertEquals(-48.6239566667, position.getLatitude(), EPSILON);
    }

    @Test
    public void rawNmeaConstructorConvertsToDegrees() {
        NmeaPosition position = new NmeaPosition(903.4036, "E", 4837.4374, "S", 16.76, null, null, null, null);
        assertEquals(9.0567266667, position.getLongitude(), EPSILON);
        assertEquals(-48.6239566667, position.getLatitude(), EPSILON);
    }

    @Test
    public void westAndSouthOrientationsYieldNegativeDegrees() {
        NmeaPosition position = new NmeaPosition(903.4036, "W", 4837.4374, "S", 16.76, null, null, null, null);
        assertTrue(position.getLongitude() < 0.0);
        assertTrue(position.getLatitude() < 0.0);
    }

    @Test
    public void nullCoordinatesStayNull() {
        NmeaPosition position = new NmeaPosition(null, null, null, null, null, null, null, null, null);
        assertNull(position.getLongitude());
        assertNull(position.getLatitude());
    }

    @Test
    public void fixQualityAccessorStoresAndReturnsValue() {
        NmeaPosition position = new NmeaPosition(903.4036, "E", 4837.4374, "S", 16.76, null, null, null, null);
        assertNull(position.getFixQuality());
        position.setFixQuality(4);
        assertEquals(Integer.valueOf(4), position.getFixQuality());
    }

    @Test
    public void equalsAndHashCodeConsiderFixQuality() {
        NmeaPosition position1 = new NmeaPosition(903.4036, "E", 4837.4374, "S", 16.76, null, null, null, null);
        position1.setFixQuality(4);
        NmeaPosition position2 = new NmeaPosition(903.4036, "E", 4837.4374, "S", 16.76, null, null, null, null);
        position2.setFixQuality(4);
        assertEquals(position1, position2);
        assertEquals(position1.hashCode(), position2.hashCode());

        position2.setFixQuality(5);
        assertFalse(position1.equals(position2));
    }

    @Test
    public void asGpxPositionCopiesFixQuality() {
        NmeaPosition position = new NmeaPosition(903.4036, "E", 4837.4374, "S", 16.76, null, null, null, null);
        position.setFixQuality(2);
        assertEquals(Integer.valueOf(2), position.asGpxPosition().getFixQuality());
    }
}
