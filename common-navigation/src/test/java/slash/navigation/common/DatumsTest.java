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

package slash.navigation.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static slash.navigation.common.Datums.lookupIndex;
import static slash.navigation.common.Datums.toWgs84;

public class DatumsTest {
    private static final double TOLERANCE = 5e-6;

    private void assertConverts(String datum, double latitude, double longitude,
                                double expectedLatitude, double expectedLongitude) {
        int index = lookupIndex(datum);
        double[] result = toWgs84(latitude, longitude, 0.0, index);
        assertEquals(datum + " latitude", expectedLatitude, result[0], TOLERANCE);
        assertEquals(datum + " longitude", expectedLongitude, result[1], TOLERANCE);
    }

    @Test
    public void testEuropean1950() {
        assertConverts("European 1950", 48.0, 11.0, 47.999154, 10.998960);
    }

    @Test
    public void testOsgb36() {
        assertConverts("OSGB 36", 51.5, -0.1, 51.500458, -0.101589);
    }

    @Test
    public void testNad27Conus() {
        assertConverts("NAD27 CONUS", 40.0, -100.0, 40.000009, -100.000418);
    }

    @Test
    public void testTokyoMean() {
        assertConverts("Tokyo mean", 35.6, 139.7, 35.603289, 139.696865);
    }

    @Test
    public void testCh1903() {
        assertConverts("CH1903", 46.9, 7.4, 46.898683, 7.399056);
    }

    @Test
    public void testWgs84Unchanged() {
        double[] result = toWgs84(48.0, 11.0, 123.5, lookupIndex("WGS 84"));
        assertEquals(48.0, result[0], 0.0);
        assertEquals(11.0, result[1], 0.0);
        assertEquals(123.5, result[2], 0.0);
    }

    @Test
    public void testLookups() {
        assertEquals(118, lookupIndex("WGS84"));
        assertEquals(118, lookupIndex("wgs 84"));
        assertEquals(86, lookupIndex("OSGB-36"));
        assertEquals(123, lookupIndex("CH 1903"));
        assertEquals(38, lookupIndex("European 1950 (Spain and Portugal)"));
        assertEquals(-1, lookupIndex("Nowhere 2000"));
        assertEquals(-1, lookupIndex(null));
    }

    @Test
    public void testName() {
        assertEquals("WGS 84", Datums.name(118));
        assertEquals("Adindan", Datums.name(0));
    }
}
