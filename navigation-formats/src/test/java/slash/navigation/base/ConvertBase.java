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

import slash.navigation.babel.MicrosoftAutoRouteFormat;
import slash.navigation.babel.OziExplorerReadFormat;
import slash.navigation.babel.OziExplorerWriteFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ConvertBase extends NavigationTestCase {
    NavigationFileParser parser = new NavigationFileParser();

    void convertRoundtrip(String testFileName,
                          BaseNavigationFormat sourceFormat,
                          BaseNavigationFormat targetFormat) throws IOException {
        assertTrue(sourceFormat.isSupportsReading());
        assertTrue(targetFormat.isSupportsWriting());

        File source = new File(testFileName);
        assertTrue("Cannot read route from " + source, parser.read(source));
        assertNotNull(parser.getFormat());
        assertNotNull(parser.getTheRoute());
        assertNotNull(parser.getAllRoutes());
        assertTrue(parser.getAllRoutes().size() > 0);

        // check append
        BaseNavigationPosition sourcePosition = parser.getTheRoute().getPositions().get(0);
        BaseNavigationPosition targetPosition = NavigationFormats.asFormat(sourcePosition, targetFormat);
        assertNotNull(targetPosition);

        convertSingleRouteRoundtrip(sourceFormat, targetFormat, source, parser.getTheRoute());

        if (targetFormat.isSupportsMultipleRoutes()) {
            convertMultipleRouteRoundtrip(sourceFormat, targetFormat, source, new ArrayList<BaseRoute>(Arrays.<BaseRoute>asList(parser.getTheRoute())));
            convertMultipleRouteRoundtrip(sourceFormat, targetFormat, source, parser.getAllRoutes());
        }
    }

    private BaseNavigationFormat handleWriteOnlyFormats(BaseNavigationFormat targetFormat) {
        if (targetFormat instanceof OziExplorerWriteFormat)
            targetFormat = new OziExplorerReadFormat();
        return targetFormat;
    }

    @SuppressWarnings("unchecked")
    private void convertSingleRouteRoundtrip(BaseNavigationFormat sourceFormat, BaseNavigationFormat targetFormat, File source, BaseRoute sourceRoute) throws IOException {
        File target = File.createTempFile("singletarget", targetFormat.getExtension());
        target.deleteOnExit();
        try {
            parser.write(sourceRoute, targetFormat, false, false, target);
            assertTrue(target.exists());

            NavigationFileParser sourceParser = new NavigationFileParser();
            assertTrue(sourceParser.read(source));
            NavigationFileParser targetParser = new NavigationFileParser();
            assertTrue(targetParser.read(target));

            targetFormat = handleWriteOnlyFormats(targetFormat);

            assertEquals(sourceFormat.getClass(), sourceParser.getFormat().getClass());
            assertEquals(targetFormat.getClass(), targetParser.getFormat().getClass());
            assertEquals(sourceFormat.getName(), sourceParser.getFormat().getName());
            assertEquals(targetFormat.getName(), targetParser.getFormat().getName());

            BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute = targetParser.getTheRoute();
            compareRouteMetaData(sourceRoute, targetRoute);
            comparePositions(sourceRoute, sourceFormat, targetRoute, targetFormat, targetParser.getAllRoutes().size() > 0);

            for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route : targetParser.getAllRoutes()) {
                compareRouteMetaData(sourceRoute, route);
                comparePositions(sourceRoute, sourceFormat, route, targetFormat, targetParser.getAllRoutes().size() > 0);
            }

            assertTrue(target.exists());
            assertTrue(target.delete());
        } finally {
            // avoid to clutter the temp directory
            assert target.delete();
        }
    }

    @SuppressWarnings("unchecked")
    private void convertMultipleRouteRoundtrip(BaseNavigationFormat sourceFormat, BaseNavigationFormat targetFormat, File source, List<BaseRoute> sourceRoutes) throws IOException {
        File target = File.createTempFile("multitarget", targetFormat.getExtension());
        target.deleteOnExit();
        try {
            parser.write(sourceRoutes, (MultipleRoutesFormat) targetFormat, target);
            assertTrue(target.exists());

            NavigationFileParser sourceParser = new NavigationFileParser();
            assertTrue(sourceParser.read(source));
            NavigationFileParser targetParser = new NavigationFileParser();
            assertTrue(targetParser.read(target));

            targetFormat = handleWriteOnlyFormats(targetFormat);

            assertEquals(sourceFormat.getClass(), sourceParser.getFormat().getClass());
            assertEquals(targetFormat.getClass(), targetParser.getFormat().getClass());
            assertEquals(sourceFormat.getName(), sourceParser.getFormat().getName());
            assertEquals(targetFormat.getName(), targetParser.getFormat().getName());

            BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute = targetParser.getTheRoute();
            compareRouteMetaData(sourceParser.getTheRoute(), targetRoute);

            for (int i = 0; i < targetParser.getAllRoutes().size(); i++) {
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = targetParser.getAllRoutes().get(i);
                compareRouteMetaData(sourceParser.getTheRoute(), route);
                BaseRoute sourceRoute = sourceFormat instanceof MicrosoftAutoRouteFormat ? sourceRoutes.get(0) : sourceRoutes.get(i);
                comparePositions(sourceRoute, sourceFormat, route, targetFormat, targetParser.getAllRoutes().size() > 1);
            }

            assertTrue(target.exists());
            assertTrue(target.delete());
        } finally {
            // avoid to clutter the temp directory
            assert target.delete();
        }
    }

    void convertSplitRoundtrip(String testFileName, BaseNavigationFormat sourceFormat, BaseNavigationFormat targetFormat) throws IOException {
        File source = new File(testFileName);
        assertTrue(parser.read(source));
        assertNotNull(parser.getFormat());
        assertNotNull(parser.getTheRoute());
        assertNotNull(parser.getAllRoutes());
        assertTrue(parser.getAllRoutes().size() > 0);

        BaseRoute sourceRoute = parser.getTheRoute();
        int maximumPositionCount = targetFormat.getMaximumPositionCount();
        int positionCount = parser.getTheRoute().getPositionCount();
        int fileCount = (int) Math.ceil((double) positionCount / maximumPositionCount);
        assertEquals(fileCount, NavigationFileParser.getNumberOfFilesToWriteFor(sourceRoute, targetFormat, false));

        File[] targets = new File[fileCount];
        for (int i = 0; i < targets.length; i++)
            targets[i] = File.createTempFile("splittarget", targetFormat.getExtension());
        try {
            parser.write(sourceRoute, targetFormat, false, false, targets);

            NavigationFileParser sourceParser = new NavigationFileParser();
            sourceParser.read(source);

            for (int i = 0; i < targets.length; i++) {
                NavigationFileParser targetParser = new NavigationFileParser();
                targetParser.read(targets[i]);
                assertEquals(sourceFormat.getClass(), sourceParser.getFormat().getClass());
                assertEquals(targetFormat.getClass(), targetParser.getFormat().getClass());
                assertEquals(sourceFormat.getName(), sourceParser.getFormat().getName());
                assertEquals(targetFormat.getName(), targetParser.getFormat().getName());
                assertEquals(i != targets.length - 1 ? maximumPositionCount : (positionCount - i * maximumPositionCount),
                        targetParser.getTheRoute().getPositionCount());

                compareSplitPositions(sourceParser.getTheRoute().getPositions(), sourceFormat,
                        targetParser.getTheRoute().getPositions(), targetFormat, i, maximumPositionCount, false, false,
                        sourceParser.getTheRoute().getCharacteristics(), targetParser.getTheRoute().getCharacteristics());
            }

            for (File target : targets) {
                assertTrue(target.exists());
                assertTrue(target.delete());
            }
        } finally {
            // avoid to clutter the temp directory
            for (File target : targets) {
                assert target.delete();
            }
        }
    }
}