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

package slash.navigation.columbus;

import org.junit.Test;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

/**
 * Read-only integration tests for the Columbus Fusion device sample files,
 * covering all five track-file column layouts (GNSS, GNSS+SAT, GNSS+SAT+FIX,
 * GNSS+IMU, IMU-only). Fixtures resolve via {@link slash.navigation.base.NavigationTestCase#TEST_PATH};
 * a test skips rather than fails when its fixture is absent, matching the
 * existing *IT convention so public CI without the private sample set stays green.
 */
public class ColumbusFusionReadIT {

    @SuppressWarnings("unchecked")
    private SimpleRoute readColumbusFusion(String fileName) throws IOException {
        File file = new File(TEST_PATH + fileName);
        assumeTrue("Sample file not found: " + file.getAbsolutePath(), file.exists());

        NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());
        ParserResult result = parser.read(file, Collections.<NavigationFormat<?>>singletonList(new ColumbusFusionFormat()));
        assertTrue("Could not read " + fileName, result.isSuccessful());
        return (SimpleRoute) result.getAllRoutes().get(0);
    }

    @SuppressWarnings("unchecked")
    private Map<WaypointType, Integer> countWaypointTypes(List<Wgs84Position> positions) {
        Map<WaypointType, Integer> counts = new EnumMap<>(WaypointType.class);
        for (Wgs84Position position : positions) {
            counts.merge(position.getWaypointType(), 1, Integer::sum);
        }
        return counts;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGnss() throws IOException {
        SimpleRoute route = readColumbusFusion("from-columbusfusion-gnss.csv");
        assertEquals(1052, route.getPositionCount());

        Map<WaypointType, Integer> counts = countWaypointTypes(route.getPositions());
        assertEquals(Integer.valueOf(2), counts.get(WaypointType.Parking));
        assertEquals(Integer.valueOf(4), counts.get(WaypointType.PointOfInterestC));
        assertEquals(Integer.valueOf(2), counts.get(WaypointType.PointOfInterestD));

        Wgs84Position first = (Wgs84Position) route.getPosition(0);
        assertDoubleEquals(26.0984118, first.getLatitude());
        assertDoubleEquals(119.2647755, first.getLongitude());
        assertDoubleEquals(60.9, first.getElevation());
        assertDoubleEquals(0.1, first.getSpeed());
        assertDoubleEquals(0.0, first.getHeading());
        assertNotNull(first.getTime());
    }

    @Test
    public void testGnss5m() throws IOException {
        SimpleRoute route = readColumbusFusion("from-columbusfusion-gnss-5m.csv");
        assertEquals(623, route.getPositionCount());
    }

    @Test
    public void testGnss5s() throws IOException {
        SimpleRoute route = readColumbusFusion("from-columbusfusion-gnss-5s.csv");
        assertEquals(247, route.getPositionCount());
    }

    @Test
    public void testGnssSat() throws IOException {
        SimpleRoute route = readColumbusFusion("from-columbusfusion-gnss-sat.csv");
        assertEquals(6, route.getPositionCount());

        Wgs84Position first = (Wgs84Position) route.getPosition(0);
        assertEquals(Integer.valueOf(12), first.getSatellites());
        assertDoubleEquals(1.75, first.getHdop());
    }

    @Test
    public void testGnssImu() throws IOException {
        SimpleRoute route = readColumbusFusion("from-columbusfusion-gnss-imu.csv");
        assertEquals(765, route.getPositionCount());

        Wgs84Position first = (Wgs84Position) route.getPosition(0);
        assertDoubleEquals(-0.30, first.getAccelerationX());
        assertDoubleEquals(0.05, first.getAccelerationY());
        assertDoubleEquals(1.08, first.getAccelerationZ());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testImu() throws IOException {
        SimpleRoute route = readColumbusFusion("from-columbusfusion-imu.csv");
        assertEquals(22, route.getPositionCount());

        for (Object positionObject : route.getPositions()) {
            Wgs84Position position = (Wgs84Position) positionObject;
            assertNull(position.getLongitude());
            assertNull(position.getLatitude());
            assertNull(position.getElevation());
            assertNotNull(position.getTime());
        }

        Wgs84Position first = (Wgs84Position) route.getPosition(0);
        assertDoubleEquals(-0.02, first.getAccelerationX());
        assertDoubleEquals(0.04, first.getAccelerationY());
        assertDoubleEquals(1.09, first.getAccelerationZ());
    }
}
