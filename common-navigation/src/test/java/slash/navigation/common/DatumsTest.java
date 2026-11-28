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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.navigation.common.Datums.lookupIndex;
import static slash.navigation.common.Datums.toWgs84;

public class DatumsTest {
    private static final double TOLERANCE = 5e-6;

    private void assertConverts(String datum, double latitude, double longitude,
                                double expectedLatitude, double expectedLongitude) {
        int index = lookupIndex(datum);
        double[] result = toWgs84(latitude, longitude, 0.0, index);
        assertEquals(expectedLatitude, result[0], TOLERANCE, datum + " latitude");
        assertEquals(expectedLongitude, result[1], TOLERANCE, datum + " longitude");
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

    @Test
    public void testOziExplorerNamesUseTheSameDatum() {
        String[][] pairs = {
                {"Ord Srvy Grt Britn", "OSGB36"},
                {"Tokyo", "Tokyo mean"},
                {"Pulkovo 1942 (1)", "Pulkovo 1942"},
                {"Pulkovo 1942 (2)", "Pulkovo 1942"},
                {"S42", "S-42(Pulkovo1942)"},
                {"Johnston Island", "Johnston Island 61"},
                {"European 1979", "European 1979 mean"},
                {"Arc 1950", "ARC 1950 Mean"},
                {"Hartebeeshoek94", "WGS 84"},
        };
        for (String[] pair : pairs) {
            int alias = lookupIndex(pair[0]);
            int target = lookupIndex(pair[1]);
            assertTrue(target >= 0, pair[1]);
            assertEquals(target, alias, pair[0]);
            assertArrayEquals(toWgs84(51.5, -0.1, 0.0, target), toWgs84(51.5, -0.1, 0.0, alias), 0.0, pair[0]);
        }
        assertEquals(lookupIndex("S42"), lookupIndex("S42       "));
    }

    @Test
    public void testOziExplorerNamesOfMissingDatumsStayUnknown() {
        for (String name : new String[]{"Potsdam Rauenberg DHDN", "RT 90", "NTF France", "Rijksdriehoeksmeting",
                "Hermannskogel", "Old Hawaiian", "Australian Geodetic 1966"})
            assertEquals(-1, lookupIndex(name), name);
    }

    @Test
    public void testPolesKeepTheLongitude() {
        int datum = lookupIndex("European 1950");
        for (double latitude : new double[]{90.0, -90.0, 89.9999, -89.9999}) {
            double[] result = toWgs84(latitude, 10.0, 0.0, datum);
            assertEquals(10.0, result[1], 0.0, "latitude " + latitude);
            assertTrue(Math.abs(result[0]) <= 90.0 + 0.001, "latitude " + latitude);
        }
    }

    @Test
    public void testLongitudeIsWrappedAtTheAntimeridian() {
        double[] east = toWgs84(48.1, 179.9999, 0.0, lookupIndex("European 1950"));
        assertTrue(east[1] >= -180.0 && east[1] <= 180.0, "longitude " + east[1]);
        assertEquals(-179.9988, east[1], 0.0001);
        double[] west = toWgs84(-48.1, -179.9999, 0.0, lookupIndex("Tokyo mean"));
        assertTrue(west[1] >= -180.0 && west[1] <= 180.0, "longitude " + west[1]);
    }

    @Test
    public void testNonFiniteValuesPassThrough() {
        int datum = lookupIndex("European 1950");
        double[] result = toWgs84(Double.NaN, 11.0, 0.0, datum);
        assertTrue(Double.isNaN(result[0]));
        assertEquals(11.0, result[1], 0.0);
        result = toWgs84(48.0, Double.POSITIVE_INFINITY, 0.0, datum);
        assertEquals(48.0, result[0], 0.0);
        assertEquals(Double.POSITIVE_INFINITY, result[1], 0.0);
    }
}
