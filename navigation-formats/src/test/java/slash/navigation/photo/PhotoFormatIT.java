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

package slash.navigation.photo;

import org.apache.commons.imaging.common.RationalNumber;
import org.junit.Test;
import slash.navigation.base.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static java.io.File.createTempFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.*;
import static slash.common.io.InputOutput.copyAndClose;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

public class PhotoFormatIT {
    private NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

    public static void assertRationalNumberEquals(RationalNumber expected, RationalNumber was) {
        assertEquals(expected.numerator, was.numerator);
        assertEquals(expected.divisor, was.divisor);
    }


    @Test
    public void testIsJpgWithEmbeddedExifGPSMetadata() throws IOException {
        File source = new File(TEST_PATH + "from-gps.jpg");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(PhotoFormat.class, result.getFormat().getClass());
        BaseRoute theRoute = result.getTheRoute();
        assertEquals(1, theRoute.getPositionCount());
        Wgs84Route route = (Wgs84Route) theRoute;
        PhotoPosition position = (PhotoPosition) route.getPosition(0);
        assertEquals("NIKON CORPORATION NIKON D90 Photo from 2010-08-31T10:31:27Z", position.getDescription());
        assertDoubleEquals(135.0, position.getElevation());
        assertDoubleEquals(8.474513333333334, position.getLongitude());
        assertDoubleEquals(53.026513333333334, position.getLatitude());
        assertNull(position.getSpeed());
        assertEquals(calendar(2010, 8, 31, 8, 30, 58), position.getTime());
        assertEquals(Integer.valueOf(10), position.getSatellites());
        assertNull(position.getHdop());
        assertNull(position.getPdop());
        assertNull(position.getVdop());
        assertNull(position.getHeading());
        assertEquals("NIKON CORPORATION", position.getMake());
        assertEquals("NIKON D90", position.getModel());
        assertEquals(Integer.valueOf(1024), position.getWidth());
        assertEquals(Integer.valueOf(680), position.getHeight());
        assertRationalNumberEquals(new RationalNumber(50, 10), position.getfNumber());
        assertRationalNumberEquals(new RationalNumber(10, 8000), position.getExposure());
        assertEquals(Integer.valueOf(0), position.getFlash());
        assertRationalNumberEquals(new RationalNumber(700, 10), position.getFocal());
        assertEquals(Integer.valueOf(200), position.getPhotographicSensitivity());
    }

    @Test
    public void testIsJpgWithEmbeddedExifMetadata() throws IOException {
        File source = new File(TEST_PATH + "from-exif.jpg");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(PhotoFormat.class, result.getFormat().getClass());
        BaseRoute theRoute = result.getTheRoute();
        assertEquals(1, theRoute.getPositionCount());
        Wgs84Route route = (Wgs84Route) theRoute;
        PhotoPosition position = (PhotoPosition) route.getPosition(0);
        assertEquals("Palm Pre Photo from 2010-01-30T13:10:15Z", position.getDescription());
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
        assertEquals("Palm", position.getMake());
        assertEquals("Pre", position.getModel());
        assertEquals(Integer.valueOf(1520), position.getWidth());
        assertEquals(Integer.valueOf(2032), position.getHeight());
        assertRationalNumberEquals(new RationalNumber(24, 10), position.getfNumber());
        assertRationalNumberEquals(new RationalNumber(1, 65536000), position.getExposure());
        assertEquals(Integer.valueOf(24), position.getFlash());
        assertRationalNumberEquals(new RationalNumber(100, 41), position.getFocal());
        assertNull(position.getPhotographicSensitivity());
    }

    @Test
    public void testIsJpgWithoutMetadata() throws IOException {
        File source = new File(TEST_PATH + "from-plain.jpg");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        BaseRoute theRoute = result.getTheRoute();
        assertEquals(PhotoFormat.class, result.getFormat().getClass());
        assertEquals(1, theRoute.getPositionCount());
        Wgs84Route route = (Wgs84Route) theRoute;
        PhotoPosition position = (PhotoPosition) route.getPosition(0);
        assertEquals("No EXIF data", position.getDescription());
        assertNull(position.getElevation());
        assertNull(position.getLongitude());
        assertNull(position.getLatitude());
        assertNull(position.getSpeed());
        // due to writing of ExifWriter into orignal file this is now the current time
        // assertCalendarEquals(calendar(2016, 1, 27, 21, 1, 58), position.getTime());
        assertNull(position.getSatellites());
        assertNull(position.getHdop());
        assertNull(position.getPdop());
        assertNull(position.getVdop());
        assertNull(position.getHeading());
        assertNull(position.getMake());
        assertNull(position.getModel());
        assertNull(position.getWidth());
        assertNull(position.getHeight());
        assertNull(position.getfNumber());
        assertNull(position.getExposure());
        assertNull(position.getFlash());
        assertNull(position.getFocal());
        assertNull(position.getPhotographicSensitivity());
    }

    private void modifyImage(String path) throws IOException {
        File target = createTempFile("target", ".jpg");
        target.deleteOnExit();
        File bak = new File(target.getAbsolutePath() + ".bak");

        copyAndClose(new FileInputStream(path), new FileOutputStream(target));

        ParserResult result = parser.read(target);
        assertNotNull(result);
        BaseRoute theRoute = result.getTheRoute();
        PhotoFormat format = (PhotoFormat) result.getFormat();
        Wgs84Route route = (Wgs84Route) theRoute;
        PhotoPosition position = (PhotoPosition) route.getPosition(0);
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
        position.setMake("make");
        position.setModel("model");
        position.setWidth(1024);
        position.setHeight(512);
        position.setfNumber(new RationalNumber(53, 10));
        position.setExposure(new RationalNumber(11, 8000));
        position.setFlash(1);
        position.setFocal(new RationalNumber(35, 1));
        position.setPhotographicSensitivity(800);

        try {
            parser.write(route, format, false, false, null, target);
            assertTrue(target.exists());

            ParserResult result2 = parser.read(target);
            assertNotNull(result2);
            assertEquals(PhotoFormat.class, result2.getFormat().getClass());
            BaseRoute theRoute2 = result2.getTheRoute();
            Wgs84Route route2 = (Wgs84Route) theRoute2;
            assertEquals(1, route2.getPositionCount());
            PhotoPosition position2 = (PhotoPosition) route2.getPosition(0);
            assertEquals("description", position2.getDescription());
            assertDoubleEquals(222.0, position2.getElevation());
            assertDoubleEquals(10.0, position2.getLongitude());
            assertDoubleEquals(50.0, position2.getLatitude());
            assertDoubleEquals(40.0, position2.getSpeed());
            assertCalendarEquals(calendar(2014, 2, 11, 15, 3, 25), position2.getTime());
            assertEquals(Integer.valueOf(4), position2.getSatellites());
            assertNull(position2.getHdop());
            assertDoubleEquals(2.0, position2.getPdop());
            assertNull(position2.getVdop());
            assertDoubleEquals(111.0, position2.getHeading());
            assertEquals("make", position2.getMake());
            assertEquals("model", position2.getModel());
            assertEquals(Integer.valueOf(1024), position2.getWidth());
            assertEquals(Integer.valueOf(512), position2.getHeight());
            assertRationalNumberEquals(new RationalNumber(53, 10), position2.getfNumber());
            assertRationalNumberEquals(new RationalNumber(11, 8000), position2.getExposure());
            assertEquals(Integer.valueOf(1), position2.getFlash());
            assertRationalNumberEquals(new RationalNumber(35, 1), position2.getFocal());
            assertEquals(Integer.valueOf(800), position.getPhotographicSensitivity());

            assertTrue(target.delete());
            assertTrue(bak.delete());
        } finally {
            // avoid to clutter the temp directory
            if (target.exists())
                assertTrue(target.delete());
            if (bak.exists())
                assertTrue(bak.delete());
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

