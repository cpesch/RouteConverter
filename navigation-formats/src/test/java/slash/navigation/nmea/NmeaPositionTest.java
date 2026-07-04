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
import static slash.navigation.common.Orientation.East;
import static slash.navigation.common.Orientation.North;

/**
 * Tests the NMEA {@code ddmm.mmmm} <-> decimal-degree conversion of {@link NmeaPosition}.
 *
 * @author Christian Pesch
 */
public class NmeaPositionTest {
    private static final double DELTA = 1e-6;

    @Test
    public void degreesRoundTripThroughNmeaStorage() {
        NmeaPosition position = new NmeaPosition(13.5, 48.5, null, null, null, "x");

        assertEquals(13.5, position.getLongitude(), DELTA);
        assertEquals(48.5, position.getLatitude(), DELTA);
    }

    @Test
    public void settersStoreValueAndOrientationInNmeaForm() {
        NmeaPosition position = new NmeaPosition(13.5, 48.5, null, null, null, "x");

        assertEquals(1330.0, position.getLongitudeAsValueAndOrientation().value(), 1e-4);
        assertEquals(East, position.getLongitudeAsValueAndOrientation().orientation());
        assertEquals(4830.0, position.getLatitudeAsValueAndOrientation().value(), 1e-4);
        assertEquals(North, position.getLatitudeAsValueAndOrientation().orientation());
    }

    @Test
    public void rawNmeaConstructorConvertsToDegrees() {
        NmeaPosition position = new NmeaPosition(1330.0, "E", 4830.0, "N", null, null, null, null, "x");

        assertEquals(13.5, position.getLongitude(), DELTA);
        assertEquals(48.5, position.getLatitude(), DELTA);
    }

    @Test
    public void westAndSouthOrientationsYieldNegativeDegrees() {
        NmeaPosition position = new NmeaPosition(1330.0, "W", 4830.0, "S", null, null, null, null, "x");

        assertEquals(-13.5, position.getLongitude(), DELTA);
        assertEquals(-48.5, position.getLatitude(), DELTA);
    }

    @Test
    public void nullCoordinatesStayNull() {
        NmeaPosition position = new NmeaPosition(13.5, 48.5, null, null, null, "x");
        position.setLongitude(null);

        assertNull(position.getLongitudeAsValueAndOrientation());
        assertNull(position.getLongitude());
    }
}
