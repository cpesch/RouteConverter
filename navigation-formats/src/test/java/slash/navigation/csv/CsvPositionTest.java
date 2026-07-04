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
package slash.navigation.csv;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests the map-backed column lookup of {@link CsvPosition}, including primary/alternative
 * column names and value round-trips.
 *
 * @author Christian Pesch
 */
public class CsvPositionTest {

    private static CsvPosition position(String key, String value) {
        Map<String, String> row = new HashMap<>();
        row.put(key, value);
        return new CsvPosition(row);
    }

    @Test
    public void readsValueFromThePrimaryColumnName() {
        assertEquals(13.5, position("Longitude", "13.5").getLongitude(), 0.0);
    }

    @Test
    public void fallsBackToAnAlternativeColumnName() {
        // ColumnType.Longitude also accepts the German "Laengengrad"
        assertEquals(2.5, position("Längengrad", "2.5").getLongitude(), 0.0);
    }

    @Test
    public void missingColumnYieldsNull() {
        assertNull(new CsvPosition(new HashMap<>()).getLongitude());
    }

    @Test
    public void setterRoundTripsThroughTheMap() {
        Map<String, String> row = new HashMap<>();
        CsvPosition position = new CsvPosition(row);

        position.setLongitude(13.5);
        position.setLatitude(48.5);

        assertEquals(13.5, position.getLongitude(), 0.0);
        assertEquals(48.5, position.getLatitude(), 0.0);
        assertTrue(row.containsKey("Longitude"));
    }

    @Test
    public void extendedSensorValuesRoundTrip() {
        CsvPosition position = new CsvPosition(new HashMap<>());

        position.setTemperature(20.5);
        position.setHeading(90.0);

        assertEquals(20.5, position.getTemperature(), 0.0);
        assertEquals(90.0, position.getHeading(), 0.0);
    }

    @Test
    public void exposesTheBackingRow() {
        Map<String, String> row = new HashMap<>();
        row.put("Longitude", "1.0");

        assertSame(row, new CsvPosition(row).getRowAsMap());
    }
}
