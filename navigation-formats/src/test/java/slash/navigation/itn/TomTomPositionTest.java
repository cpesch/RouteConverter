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

import static org.junit.Assert.*;

/**
 * Tests the integer-scaled coordinate handling of {@link TomTomPosition}.
 *
 * @author Christian Pesch
 */
public class TomTomPositionTest {

    @Test
    public void setLongitudeScalesByHundredThousand() {
        TomTomPosition position = new TomTomPosition(0, 0, null);
        position.setLongitude(13.5);

        assertEquals(Integer.valueOf(1350000), position.getLongitudeAsInt());
        assertEquals(13.5, position.getLongitude(), 1e-9);
    }

    @Test
    public void setLatitudeScalesByHundredThousand() {
        TomTomPosition position = new TomTomPosition(0, 0, null);
        position.setLatitude(52.5);

        assertEquals(Integer.valueOf(5250000), position.getLatitudeAsInt());
        assertEquals(52.5, position.getLatitude(), 1e-9);
    }

    @Test
    public void setLongitudeTruncatesTowardZero() {
        TomTomPosition position = new TomTomPosition(0, 0, null);
        position.setLongitude(1.234567);

        assertEquals(Integer.valueOf(123456), position.getLongitudeAsInt());
        assertEquals(1.23456, position.getLongitude(), 1e-9);
    }

    @Test
    public void integerConstructorTakesRawScaledCoordinates() {
        TomTomPosition position = new TomTomPosition(1350000, 5250000, "somewhere");

        assertEquals(13.5, position.getLongitude(), 1e-9);
        assertEquals(52.5, position.getLatitude(), 1e-9);
    }

    @Test
    public void nullCoordinatesStayNull() {
        TomTomPosition position = new TomTomPosition(0, 0, null);
        position.setLongitude(null);

        assertNull(position.getLongitudeAsInt());
        assertNull(position.getLongitude());
    }

    @Test
    public void equalCoordinatesAndDescriptionAreEqual() {
        TomTomPosition one = new TomTomPosition(1350000, 5250000, "Berlin");
        TomTomPosition same = new TomTomPosition(1350000, 5250000, "Berlin");
        TomTomPosition different = new TomTomPosition(1350001, 5250000, "Berlin");

        assertEquals(one, same);
        assertEquals(one.hashCode(), same.hashCode());
        assertNotEquals(one, different);
    }
}
