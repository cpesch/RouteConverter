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

package slash.navigation.image;

import org.junit.Test;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.base.Wgs84Position;

import java.io.File;
import java.io.IOException;

import static java.io.File.createTempFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertCalendarEquals;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

public class ImageFormatIT {
    private NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

    @Test
    public void testIsJpgWithEmbeddedExifGPSMetadata() throws IOException {
        File source = new File(TEST_PATH + "from-gps.jpg");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(ImageFormat.class, result.getFormat().getClass());
        BaseRoute theRoute = result.getTheRoute();
        assertEquals(1, theRoute.getPositionCount());
        ImageRoute route = (ImageRoute) theRoute;
        Wgs84Position position = route.getPosition(0);
        assertEquals("NIKON CORPORATION NIKON D90 Image from 2010-08-31T10:31:27Z", position.getDescription());
        assertDoubleEquals(135.0, position.getElevation());
        assertDoubleEquals(8.474513333333334, position.getLongitude());
        assertDoubleEquals(53.026513333333334, position.getLatitude());
        assertNull(position.getSpeed());
        assertEquals(calendar(2010, 8, 31, 8, 30, 58), position.getTime());
        assertEquals(new Integer(10), position.getSatellites());
        assertNull(position.getHdop());
        assertNull(position.getPdop());
        assertNull(position.getVdop());
        assertNull(position.getHeading());
    }

    @Test
    public void testIsJpgWithEmbeddedExifMetadata() throws IOException {
        File source = new File(TEST_PATH + "from-exif.jpg");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(ImageFormat.class, result.getFormat().getClass());
        BaseRoute theRoute = result.getTheRoute();
        assertEquals(1, theRoute.getPositionCount());
        ImageRoute route = (ImageRoute) theRoute;
        Wgs84Position position = route.getPosition(0);
        assertEquals("Palm Pre Image from 2010-01-30T13:10:15Z", position.getDescription());
        assertNull(position.getElevation());
        assertNull(position.getLongitude());
        assertNull(position.getLatitude());
        assertNull(position.getSpeed());
        assertCalendarEquals(calendar(2010, 1, 30, 13, 10, 15), position.getTime());
        assertNull(position.getSatellites());
        assertNull(position.getHdop());
        assertNull(position.getPdop());
        assertNull(position.getVdop());
        assertNull(position.getHeading());
    }

    @Test
    public void testIsJpgWithoutMetadata() throws IOException {
        File source = new File(TEST_PATH + "from-plain.jpg");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        BaseRoute theRoute = result.getTheRoute();
        assertEquals(ImageFormat.class, result.getFormat().getClass());
        assertEquals(1, theRoute.getPositionCount());
        ImageRoute route = (ImageRoute) theRoute;
        Wgs84Position position = route.getPosition(0);
        assertEquals("No EXIF data", position.getDescription());
        assertNull(position.getElevation());
        assertNull(position.getLongitude());
        assertNull(position.getLatitude());
        assertNull(position.getSpeed());
        assertCalendarEquals(calendar(2016, 1, 27, 21, 1, 58), position.getTime());
        assertNull(position.getSatellites());
        assertNull(position.getHdop());
        assertNull(position.getPdop());
        assertNull(position.getVdop());
        assertNull(position.getHeading());
    }

    private void modifyImage(String path) throws IOException {
        File source = new File(path);
        ParserResult result = parser.read(source);
        assertNotNull(result);
        BaseRoute theRoute = result.getTheRoute();
        ImageFormat format = (ImageFormat) result.getFormat();
        ImageRoute route = (ImageRoute) theRoute;
        Wgs84Position position = route.getPosition(0);
        position.setDescription("description");
        position.setElevation(222.0);
        position.setLongitude(10.0);
        position.setLatitude(50.0);
        position.setSpeed(40.0);
        position.setTime(calendar(2014, 2, 11, 15, 3, 25));
        position.setSatellites(4);
        position.setHdop(1.0);
        position.setPdop(2.0);
        position.setVdop(3.0);
        position.setHeading(111.0);

        File target = createTempFile("target", ".jpg");
        target.deleteOnExit();
        try {
            parser.write(route, format, false, false, null, target);
            assertTrue(target.exists());

            ParserResult result2 = parser.read(target);
            assertNotNull(result2);
            assertEquals(ImageFormat.class, result2.getFormat().getClass());
            BaseRoute theRoute2 = result2.getTheRoute();
            ImageRoute route2 = (ImageRoute) theRoute2;
            assertEquals(1, route2.getPositionCount());
            Wgs84Position position2 = route2.getPosition(0);
            assertEquals("description", position2.getDescription());
            assertDoubleEquals(222.0, position2.getElevation());
            assertDoubleEquals(10.0, position2.getLongitude());
            assertDoubleEquals(50.0, position2.getLatitude());
            assertDoubleEquals(40.0, position2.getSpeed());
            assertCalendarEquals(calendar(2014, 2, 11, 15, 3, 25), position2.getTime());
            assertEquals(new Integer(4), position2.getSatellites());
            assertNull(position2.getHdop());
            assertDoubleEquals(2.0, position2.getPdop());
            assertNull(position2.getVdop());
            assertDoubleEquals(111.0, position2.getHeading());

            assertTrue(target.delete());
        } finally {
            // avoid to clutter the temp directory
            if (target.exists())
                assertTrue(target.delete());
        }
    }

    @Test
    public void testModifyJpgWithEmbeddedExifGPSMetadata() throws IOException {
        modifyImage(TEST_PATH + "from-gps.jpg");
    }

    @Test
    public void testModifyJpgWithEmbeddedExifMetadata() throws IOException {
        modifyImage(TEST_PATH + "from-exif.jpg");
    }

    @Test
    public void testModifyJpgWithoutMetadata() throws IOException {
        modifyImage(TEST_PATH + "from-plain.jpg");
    }
}

