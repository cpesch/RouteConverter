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

package slash.navigation;

import slash.navigation.babel.AlanTrackLogFormat;
import slash.navigation.babel.GarminPcx5Format;
import slash.navigation.babel.AlanWaypointsAndRoutesFormat;
import slash.common.util.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class ReadWriteBase extends NavigationTestCase {
    NavigationFileParser parser = new NavigationFileParser();

    protected void readWriteRoundtrip(String testFileName, NavigationFileParserCallback navigationFileParserCallback) throws IOException {
        File source = new File(testFileName);
        assertTrue("Could not read " + testFileName, parser.read(source));
        assertNotNull(parser.getFormat());
        assertNotNull(parser.getAllRoutes());
        assertTrue(parser.getAllRoutes().size() > 0);

        File target = File.createTempFile("target", Files.getExtension(source));
        // see AlanWaypointsAndRoutesFormat#isSupportsMultipleRoutes
        if (parser.getFormat().isSupportsMultipleRoutes() || parser.getFormat() instanceof AlanWaypointsAndRoutesFormat)
            parser.write(parser.getAllRoutes(), (MultipleRoutesFormat) parser.getFormat(), target);
        else
            parser.write(parser.getTheRoute(), parser.getFormat(), false, true, target);

        // NOT possible to determine if I add description lines while writing
        // assertEquals(source.length(), target.length());

        NavigationFileParser sourceParser = new NavigationFileParser();
        sourceParser.read(source);
        NavigationFileParser targetParser = new NavigationFileParser();
        targetParser.read(target);

        NavigationFormat sourceFormat = sourceParser.getFormat();
        NavigationFormat targetFormat = targetParser.getFormat();
        assertEquals(sourceFormat.getName(), targetFormat.getName());
        // see AlanWaypointsAndRoutesFormat#isSupportsMultipleRoutes
        if (sourceFormat.isSupportsMultipleRoutes() || sourceFormat instanceof AlanWaypointsAndRoutesFormat)
            assertEquals(sourceParser.getAllRoutes().size(), targetParser.getAllRoutes().size());
        else
            assertEquals(1, targetParser.getAllRoutes().size());

        List<BaseRoute> sourceRoutes = sourceParser.getAllRoutes();
        List<BaseRoute> targetRoutes = targetParser.getAllRoutes();
        // GPSBabel creates a route and a track out of a simple GarminPcx5 track if called with -r and -t
        // and out of a simple AlanTrk track if called with -t
        int count = targetFormat instanceof GarminPcx5Format || targetFormat instanceof AlanTrackLogFormat ?
                targetRoutes.size() : sourceRoutes.size();
        for (int i = 0; i < count; i++) {
            BaseRoute sourceRoute = sourceRoutes.get(i);
            BaseRoute targetRoute = targetRoutes.get(i);
            compareRouteMetaData(sourceRoute, targetRoute);
            comparePositions(sourceRoute, sourceFormat, targetRoute, targetFormat, targetRoutes.size() > 0);
        }

        if (navigationFileParserCallback != null)
            navigationFileParserCallback.test(sourceParser, targetParser);

        assertTrue(target.exists());
        assertTrue(target.delete());
    }

    protected void readWriteRoundtrip(String testFileName) throws IOException {
        readWriteRoundtrip(testFileName, null);
    }

    void splitReadWriteRoundtrip(String testFileName) throws IOException {
        splitReadWriteRoundtrip(testFileName, false);
    }

    void splitReadWriteRoundtrip(String testFileName, boolean duplicateFirstPosition) throws IOException {
        File source = new File(testFileName);
        assertTrue(parser.read(source));
        assertNotNull(parser.getFormat());
        BaseRoute sourceRoute = parser.getTheRoute();
        assertNotNull(sourceRoute);
        assertNotNull(parser.getAllRoutes());
        assertTrue(parser.getAllRoutes().size() > 0);

        int maximumPositionCount = parser.getFormat().getMaximumPositionCount();
        if (maximumPositionCount == BaseNavigationFormat.UNLIMITED_MAXIMUM_POSITION_COUNT) {
            readWriteRoundtrip(testFileName);
        } else {
            int sourcePositionCount = parser.getTheRoute().getPositionCount();
            int positionCount = parser.getTheRoute().getPositionCount() + (duplicateFirstPosition ? 1 : 0);
            int fileCount = (int) Math.ceil((double) positionCount / maximumPositionCount);
            assertEquals(fileCount, NavigationFileParser.getNumberOfFilesToWriteFor(sourceRoute, parser.getFormat(), duplicateFirstPosition));

            File[] targets = new File[fileCount];
            for (int i = 0; i < targets.length; i++)
                targets[i] = File.createTempFile("target", ".test");
            parser.write(sourceRoute, parser.getFormat(), duplicateFirstPosition, false, targets);

            NavigationFileParser sourceParser = new NavigationFileParser();
            sourceParser.read(source);

            int targetPositionCount = 0;
            for (int i = 0; i < targets.length; i++) {
                NavigationFileParser targetParser = new NavigationFileParser();
                targetParser.read(targets[i]);

                NavigationFormat sourceFormat = sourceParser.getFormat();
                NavigationFormat targetFormat = targetParser.getFormat();
                assertNotEquals(sourceFormat, targetFormat);
                assertEquals(sourceFormat.getName(), targetFormat.getName());
                assertEquals(i != targets.length - 1 ? maximumPositionCount : (positionCount - i * maximumPositionCount),
                        targetParser.getTheRoute().getPositionCount());
                targetPositionCount += targetParser.getTheRoute().getPositionCount();

                compareSplitPositions(sourceParser.getTheRoute().getPositions(), sourceFormat,
                        targetParser.getTheRoute().getPositions(), targetFormat, i, maximumPositionCount,
                        duplicateFirstPosition, false, sourceParser.getTheRoute().getCharacteristics(),
                        targetParser.getTheRoute().getCharacteristics());
            }
            assertEquals(sourcePositionCount + (duplicateFirstPosition ? 1 : 0), targetPositionCount);
            assertEquals(positionCount, targetPositionCount);

            for (File target : targets)
                assertTrue(target.delete());
        }
    }

    protected interface NavigationFileParserCallback {
        void test(NavigationFileParser source, NavigationFileParser target);
    }
}
