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
package slash.navigation.excel;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests {@link ExcelPosition} through its in-memory workbook constructor (no files).
 *
 * @author Christian Pesch
 */
public class ExcelPositionTest {

    @Test
    public void constructorStoresCoordinatesSpeedAndDescriptionInTheRow() {
        ExcelPosition position = new ExcelPosition(13.5, 48.5, 100.0, 5.0, null, "Berlin");

        assertEquals(13.5, position.getLongitude(), 0.0);
        assertEquals(48.5, position.getLatitude(), 0.0);
        assertEquals(100.0, position.getElevation(), 0.0);
        assertEquals(5.0, position.getSpeed(), 0.0);
        assertEquals("Berlin", position.getDescription());
    }

    @Test
    public void coordinateSettersRoundTripThroughTheRow() {
        ExcelPosition position = new ExcelPosition(0.0, 0.0, null, null, null, null);

        position.setLongitude(1.25);
        position.setLatitude(-2.5);

        assertEquals(1.25, position.getLongitude(), 0.0);
        assertEquals(-2.5, position.getLatitude(), 0.0);
    }

    @Test
    public void extendedSensorTemperatureRoundTrips() {
        ExcelPosition position = new ExcelPosition(0.0, 0.0, null, null, null, null);

        position.setTemperature(20.5);

        assertEquals(20.5, position.getTemperature(), 0.0);
    }

    @Test
    public void exposesItsBackingRow() {
        ExcelPosition position = new ExcelPosition(0.0, 0.0, null, null, null, null);

        assertNotNull(position.getRow());
    }
}
