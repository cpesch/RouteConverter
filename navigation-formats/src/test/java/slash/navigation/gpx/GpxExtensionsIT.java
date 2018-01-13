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
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormatParser;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.io.File.createTempFile;
import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;
import static slash.common.io.InputOutput.readFileToString;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.NavigationTestCase.readGpxFile;

public class GpxExtensionsIT {

    private GpxRoute readRoute(String fileName) throws Exception {
        List<GpxRoute> routes = readGpxFile(new Gpx11Format(), fileName);
        assertNotNull(routes);
        assertEquals(1, routes.size());
        return routes.get(0);
    }

    private GpxPosition readPosition(String fileName) throws Exception {
        GpxRoute route = readRoute(fileName);
        assertEquals(1, route.getPositionCount());
        return route.getPosition(0);
    }

    private void checkPositionBasics(GpxPosition position) {
        assertEquals("one", position.getDescription());
        assertDoubleEquals(50.8758450, position.getLatitude());
        assertDoubleEquals(4.6710150, position.getLongitude());
        assertDoubleEquals(60.0, position.getElevation());
        assertEquals(calendar(2014, 6, 15, 15, 45, 39), position.getTime());
    }

    @Test
    public void testReadGarminGpxExtensionv3() throws Exception {
        GpxPosition position = readPosition(TEST_PATH + "garmin-gpx-extension-v3.gpx");
        checkPositionBasics(position);
        assertDoubleEquals(25.0, position.getTemperature());
    }

    @Test
    public void testReadGarminTrackPointExtensionv1() throws Exception {
        GpxPosition position = readPosition(TEST_PATH + "garmin-track-point-extension-v1.gpx");
        checkPositionBasics(position);
        assertDoubleEquals(25.0, position.getTemperature());
    }

    @Test
    public void testReadGarminTrackPointExtensionv2() throws Exception {
        GpxPosition position = readPosition(TEST_PATH + "garmin-track-point-extension-v2.gpx");
        checkPositionBasics(position);
        assertDoubleEquals(25.0, position.getTemperature());
        assertDoubleEquals(43.2, position.getSpeed());
        assertDoubleEquals(98.0, position.getHeading());
    }

    @Test
    public void testReadTrekbuddyExtension1() throws Exception {
        GpxPosition position = readPosition(TEST_PATH + "trekbuddy-extension-1.gpx");
        checkPositionBasics(position);
        assertDoubleEquals(15.12, position.getSpeed());
        assertDoubleEquals(124.5, position.getHeading());
    }

    @Test
    public void testReadTrekbuddyExtension2() throws Exception {
        GpxPosition position = readPosition(TEST_PATH + "trekbuddy-extension-2.gpx");
        checkPositionBasics(position);
        assertDoubleEquals(15.12, position.getSpeed());
        assertDoubleEquals(124.5, position.getHeading());
    }

    private File writeRoute(GpxRoute route) throws IOException {
        File target = createTempFile("target", ".gpx");
        NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());
        parser.write(Collections.<BaseRoute>singletonList(route), new Gpx11Format(), target);
        return target;
    }

    @Test
    public void testWriteGarminGpxExtensionv3Temperature() throws Exception {
        GpxRoute route = readRoute(TEST_PATH + "garmin-gpx-extension-v3.gpx");
        GpxPosition position = route.getPosition(0);
        assertDoubleEquals(25.0, position.getTemperature());

        position.setTemperature(19.8);

        File file = writeRoute(route);

        String after = readFileToString(file);
        assertTrue(after.contains("<gpxx:Temperature>19.8</gpxx:Temperature>"));
    }
}
