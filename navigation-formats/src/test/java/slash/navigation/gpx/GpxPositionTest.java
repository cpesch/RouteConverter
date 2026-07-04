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
package slash.navigation.gpx;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.*;

/**
 * Tests the constructors, accuracy fields and description handling of {@link GpxPosition}
 * without touching JAXB bindings.
 *
 * @author Christian Pesch
 */
public class GpxPositionTest {

    @Test
    public void plainConstructorExposesCoordinatesSpeedAndDescription() {
        GpxPosition position = new GpxPosition(13.5, 48.5, 100.0, 5.0, null, "Berlin");

        assertEquals(13.5, position.getLongitude(), 0.0);
        assertEquals(48.5, position.getLatitude(), 0.0);
        assertEquals(100.0, position.getElevation(), 0.0);
        assertEquals(5.0, position.getSpeed(), 0.0);
        assertEquals("Berlin", position.getCity());
    }

    @Test
    public void gpx10ConstructorPopulatesHeadingAndAccuracy() {
        GpxPosition position = new GpxPosition(BigDecimal.valueOf(13.5), BigDecimal.valueOf(48.5),
                BigDecimal.valueOf(100), 5.0, 90.0, null, "desc",
                BigDecimal.valueOf(1.2), BigDecimal.valueOf(2.3), BigDecimal.valueOf(3.4),
                BigInteger.valueOf(7), null);

        assertEquals(90.0, position.getHeading(), 0.0);
        assertEquals(1.2, position.getHdop(), 0.0);
        assertEquals(2.3, position.getPdop(), 0.0);
        assertEquals(3.4, position.getVdop(), 0.0);
        assertEquals(Integer.valueOf(7), position.getSatellites());
    }

    @Test
    public void setDescriptionPlainTextKeepsCityAndNoReason() {
        GpxPosition position = new GpxPosition(13.5, 48.5, null, null, null, null);
        position.setDescription("Berlin");

        assertEquals("Berlin", position.getCity());
        assertNull(position.getReason());
    }

    @Test
    public void headingAndSpeedRoundTripWithoutExtension() {
        GpxPosition position = new GpxPosition(13.5, 48.5, null, null, null, null);

        position.setHeading(45.0);
        position.setSpeed(12.0);

        assertEquals(45.0, position.getHeading(), 0.0);
        assertEquals(12.0, position.getSpeed(), 0.0);
    }

    @Test
    public void asGpxPositionReturnsItself() {
        GpxPosition position = new GpxPosition(13.5, 48.5, null, null, null, null);

        assertSame(position, position.asGpxPosition());
    }

    @Test
    public void equalPositionsAreEqual() {
        GpxPosition one = new GpxPosition(13.5, 48.5, 100.0, 5.0, null, "Berlin");
        GpxPosition same = new GpxPosition(13.5, 48.5, 100.0, 5.0, null, "Berlin");
        GpxPosition different = new GpxPosition(13.5, 48.6, 100.0, 5.0, null, "Berlin");

        assertEquals(one, same);
        assertEquals(one.hashCode(), same.hashCode());
        assertNotEquals(one, different);
    }
}
