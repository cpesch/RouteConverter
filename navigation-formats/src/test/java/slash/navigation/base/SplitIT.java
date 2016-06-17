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

package slash.navigation.base;

import org.junit.Test;
import slash.navigation.bcr.MTP0607Format;
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTom95RouteFormat;

import java.io.File;
import java.io.IOException;

import static java.io.File.createTempFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.base.BaseNavigationFormat.UNLIMITED_MAXIMUM_POSITION_COUNT;
import static slash.navigation.base.NavigationFormatParser.getNumberOfFilesToWriteFor;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.NavigationTestCase.compareSplitPositions;
import static slash.navigation.base.ReadWriteBase.readWriteRoundtrip;

public class SplitIT {

    private void splitReadWriteRoundtrip(String testFileName) throws IOException {
        splitReadWriteRoundtrip(testFileName, false);
    }

    private void splitReadWriteRoundtrip(String testFileName, boolean duplicateFirstPosition) throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

        File source = new File(testFileName);
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertNotNull(result.getFormat());
        BaseRoute sourceRoute = result.getTheRoute();
        assertNotNull(sourceRoute);
        assertNotNull(result.getAllRoutes());
        assertTrue(result.getAllRoutes().size() > 0);

        int maximumPositionCount = result.getFormat().getMaximumPositionCount();
        if (maximumPositionCount == UNLIMITED_MAXIMUM_POSITION_COUNT) {
            readWriteRoundtrip(testFileName);
        } else {
            int sourcePositionCount = result.getTheRoute().getPositionCount();
            int positionCount = result.getTheRoute().getPositionCount() + (duplicateFirstPosition ? 1 : 0);
            int fileCount = (int) Math.ceil((double) positionCount / maximumPositionCount);
            assertEquals(fileCount, getNumberOfFilesToWriteFor(sourceRoute, result.getFormat(), duplicateFirstPosition));

            File[] targets = new File[fileCount];
            for (int i = 0; i < targets.length; i++)
                targets[i] = createTempFile("target", ".test");
            parser.write(sourceRoute, result.getFormat(), duplicateFirstPosition, false, null, targets);

            ParserResult sourceResult = parser.read(source);
            int targetPositionCount = 0;
            for (int i = 0; i < targets.length; i++) {
                ParserResult targetResult = parser.read(targets[i]);

                NavigationFormat sourceFormat = sourceResult.getFormat();
                NavigationFormat targetFormat = targetResult.getFormat();
                assertEquals(sourceFormat, targetFormat);
                assertEquals(i != targets.length - 1 ? maximumPositionCount : (positionCount - i * maximumPositionCount),
                        targetResult.getTheRoute().getPositionCount());
                targetPositionCount += targetResult.getTheRoute().getPositionCount();

                compareSplitPositions(sourceResult.getTheRoute().getPositions(), sourceFormat,
                        targetResult.getTheRoute().getPositions(), targetFormat, i, maximumPositionCount,
                        duplicateFirstPosition, false, sourceResult.getTheRoute().getCharacteristics(),
                        targetResult.getTheRoute().getCharacteristics());
            }
            assertEquals(sourcePositionCount + (duplicateFirstPosition ? 1 : 0), targetPositionCount);
            assertEquals(positionCount, targetPositionCount);

            for (File target : targets)
                assertTrue(target.delete());
        }
    }

    @Test
    public void testSplitBcrReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large.bcr");
    }

    @Test
    public void testSplitTomTomRouteReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large.itn", false);
        splitReadWriteRoundtrip(TEST_PATH + "large.itn", false);
    }

    @Test
    public void testSplitGarminMapSource6ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large.gdb");
    }

    @Test
    public void testSplitGpx10ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large10.gpx");
    }

    @Test
    public void testSplitGpx11ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large11.gpx");
    }

    @Test
    public void testSplitKml20ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large20.kml");
    }

    @Test
    public void testSplitKml21ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large21.kml");
    }

    @Test
    public void testSplitKml22ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large22.kml");
    }

    @Test
    public void testSplitIgo8RouteReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large-igo8route.kml");
    }

    @Test
    public void testSplitGarminMapSource5ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large.mps");
    }

    @Test
    public void testSplitNmn4ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large-nmn4.rte");
    }

    @Test
    public void testSplitNmn6ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large-nmn6.rte", false);
        splitReadWriteRoundtrip(TEST_PATH + "large-nmn6.rte", false);
        splitReadWriteRoundtrip(TEST_PATH + "large-nmn6.rte", true);
        splitReadWriteRoundtrip(TEST_PATH + "large-nmn6.rte", true);
    }

    @Test
    public void testSplitGarminPcx5ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large-pcx5.wpt");
    }

    @Test
    public void testSplitGpsTunerTrkReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "from-gpstuner.trk");
    }

    @Test
    public void testSplitAlanWaypointsAndRoutesReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "from.wpr");
    }



    void convertSplitRoundtrip(String testFileName, BaseNavigationFormat sourceFormat, BaseNavigationFormat targetFormat) throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

        File source = new File(testFileName);
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertNotNull(result.getFormat());
        assertNotNull(result.getTheRoute());
        assertNotNull(result.getAllRoutes());
        assertTrue(result.getAllRoutes().size() > 0);

        BaseRoute sourceRoute = result.getTheRoute();
        int maximumPositionCount = targetFormat.getMaximumPositionCount();
        int positionCount = result.getTheRoute().getPositionCount();
        int fileCount = (int) Math.ceil((double) positionCount / maximumPositionCount);
        assertEquals(fileCount, NavigationFormatParser.getNumberOfFilesToWriteFor(sourceRoute, targetFormat, false));

        File[] targets = new File[fileCount];
        for (int i = 0; i < targets.length; i++)
            targets[i] = createTempFile("splittarget", targetFormat.getExtension());
        try {
            parser.write(sourceRoute, targetFormat, false, false, null, targets);

            ParserResult sourceResult = parser.read(source);
            for (int i = 0; i < targets.length; i++) {
                ParserResult targetResult = parser.read(targets[i]);
                assertEquals(sourceFormat.getClass(), sourceResult.getFormat().getClass());
                assertEquals(targetFormat.getClass(), targetResult.getFormat().getClass());
                assertEquals(sourceFormat.getName(), sourceResult.getFormat().getName());
                assertEquals(targetFormat.getName(), targetResult.getFormat().getName());
                assertEquals(i != targets.length - 1 ? maximumPositionCount : (positionCount - i * maximumPositionCount),
                        targetResult.getTheRoute().getPositionCount());

                compareSplitPositions(sourceResult.getTheRoute().getPositions(), sourceFormat,
                        targetResult.getTheRoute().getPositions(), targetFormat, i, maximumPositionCount, false, false,
                        sourceResult.getTheRoute().getCharacteristics(), targetResult.getTheRoute().getCharacteristics());
            }

            for (File target : targets) {
                assertTrue(target.exists());
                assertTrue(target.delete());
            }
        } finally {
            // avoid to clutter the temp directory
            for (File target : targets) {
                if (target.exists())
                    assertTrue(target.delete());
            }
        }
    }

    @Test
    public void testConvertLargeTomTomRouteToSeveralTomTomRoutes() throws IOException {
        convertSplitRoundtrip(TEST_PATH + "large.itn", new TomTom5RouteFormat(), new TomTom95RouteFormat());
    }

    @Test
    public void testConvertLargeTomTomRouteToSeveralMTP0607s() throws IOException {
        convertSplitRoundtrip(TEST_PATH + "large.itn", new TomTom5RouteFormat(), new MTP0607Format());
    }

    @Test
    public void testConvertLargeMTP0607ToSeveralTomTomRoutes() throws IOException {
        convertSplitRoundtrip(TEST_PATH + "large.bcr", new MTP0607Format(), new TomTom95RouteFormat());
    }

    @Test
    public void testConvertLargeMTP0607ToSeveralMTP0607s() throws IOException {
        convertSplitRoundtrip(TEST_PATH + "large.bcr", new MTP0607Format(), new MTP0607Format());
    }
}
