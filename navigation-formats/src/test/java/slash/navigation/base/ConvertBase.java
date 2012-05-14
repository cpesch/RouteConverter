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

import slash.navigation.babel.CompeGPSDataFormat;
import slash.navigation.babel.CompeGPSDataRouteFormat;
import slash.navigation.babel.GarminMapSource6Format;
import slash.navigation.babel.MicrosoftAutoRouteFormat;
import slash.navigation.babel.OziExplorerReadFormat;
import slash.navigation.babel.OziExplorerWriteFormat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.io.File.createTempFile;
import static slash.common.io.Files.getExtension;
import static slash.navigation.base.NavigationFormats.asFormat;
import static slash.navigation.base.NavigationFormats.getReadFormatsPreferredByExtension;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public abstract class ConvertBase extends NavigationTestCase {
    private NavigationFormatParser parser = new NavigationFormatParser();

    void convertRoundtrip(String testFileName,
                          BaseNavigationFormat sourceFormat,
                          BaseNavigationFormat targetFormat) throws IOException {
        assertTrue(sourceFormat.isSupportsReading());
        assertTrue(targetFormat.isSupportsWriting());

        File source = new File(testFileName);
        ParserResult result = parser.read(source, getReadFormatsPreferredByExtension(getExtension(testFileName)));
        assertNotNull("Cannot read route from " + source, result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getFormat());
        assertNotNull(result.getTheRoute());
        assertNotNull(result.getAllRoutes());
        assertTrue(result.getAllRoutes().size() > 0);

        // check append
        BaseNavigationPosition sourcePosition = result.getTheRoute().getPositions().get(0);
        BaseNavigationPosition targetPosition = asFormat(sourcePosition, targetFormat);
        assertNotNull(targetPosition);

        convertSingleRouteRoundtrip(sourceFormat, targetFormat, source, result.getTheRoute());

        if (targetFormat.isSupportsMultipleRoutes()) {
            convertMultipleRouteRoundtrip(sourceFormat, targetFormat, source, Collections.<BaseRoute>singletonList(result.getTheRoute()));
            convertMultipleRouteRoundtrip(sourceFormat, targetFormat, source, result.getAllRoutes());
        }
    }

    private BaseNavigationFormat handleReadOnlyFormats(BaseNavigationFormat sourceFormat) {
        if (sourceFormat instanceof CompeGPSDataFormat)
            sourceFormat = new CompeGPSDataRouteFormat();
        return sourceFormat;
    }

    private BaseNavigationFormat handleWriteOnlyFormats(BaseNavigationFormat targetFormat) {
        if (targetFormat instanceof OziExplorerWriteFormat)
            targetFormat = new OziExplorerReadFormat();
        return targetFormat;
    }

    @SuppressWarnings("unchecked")
    private void convertSingleRouteRoundtrip(BaseNavigationFormat sourceFormat, BaseNavigationFormat targetFormat, File source, BaseRoute sourceRoute) throws IOException {
        File target = createTempFile("singletarget", targetFormat.getExtension());
        target.deleteOnExit();
        try {
            parser.write(sourceRoute, targetFormat, false, false, target);
            assertTrue(target.exists());

            ParserResult sourceResult = parser.read(source, getReadFormatsPreferredByExtension(getExtension(source)));
            assertNotNull(sourceResult);
            ParserResult targetResult = parser.read(target, getReadFormatsPreferredByExtension(getExtension(target)));
            assertNotNull(targetResult);

            targetFormat = handleWriteOnlyFormats(targetFormat);

            assertEquals(sourceFormat.getClass(), sourceResult.getFormat().getClass());
            assertEquals(targetFormat.getClass(), targetResult.getFormat().getClass());
            assertEquals(sourceFormat.getName(), sourceResult.getFormat().getName());
            assertEquals(targetFormat.getName(), targetResult.getFormat().getName());

            compareRouteMetaData(sourceRoute, targetResult.getTheRoute());
            comparePositions(sourceRoute, sourceFormat, targetResult.getTheRoute(), targetFormat, targetResult.getAllRoutes().size() > 0);

            for (BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute : targetResult.getAllRoutes()) {
                compareRouteMetaData(sourceRoute, targetRoute);
                comparePositions(sourceRoute, sourceFormat, targetRoute, targetFormat, targetResult.getAllRoutes().size() > 0);
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
        File target = createTempFile("multitarget", targetFormat.getExtension());
        target.deleteOnExit();
        try {
            parser.write(sourceRoutes, (MultipleRoutesFormat) targetFormat, target);
            assertTrue(target.exists());

            ParserResult sourceResult = parser.read(source);
            assertNotNull(sourceResult);
            assertTrue(sourceResult.isSuccessful());
            ParserResult targetResult = parser.read(target);
            assertNotNull(targetResult);
            assertTrue(targetResult.isSuccessful());

            sourceFormat = handleReadOnlyFormats(sourceFormat);
            targetFormat = handleWriteOnlyFormats(targetFormat);

            assertEquals(sourceFormat.getClass(), sourceResult.getFormat().getClass());
            assertEquals(targetFormat.getClass(), targetResult.getFormat().getClass());
            assertEquals(sourceFormat.getName(), sourceResult.getFormat().getName());
            assertEquals(targetFormat.getName(), targetResult.getFormat().getName());

            compareRouteMetaData(sourceResult.getTheRoute(), targetResult.getTheRoute());

            for (int i = 0; i < targetResult.getAllRoutes().size(); i++) {
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute = targetResult.getAllRoutes().get(i);
                BaseRoute sourceRoute = sourceResult.getAllRoutes().get(sourceFormat instanceof MicrosoftAutoRouteFormat ? 0 : i);
                // skip since first route is a list of all waypoints of all routes
                if (targetFormat instanceof GarminMapSource6Format && targetRoute.getCharacteristics().equals(Waypoints))
                    continue;
                compareRouteMetaData(sourceRoute, targetRoute);
                comparePositions(sourceRoute, sourceFormat, targetRoute, targetFormat, targetResult.getAllRoutes().size() > 1);
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
            parser.write(sourceRoute, targetFormat, false, false, targets);

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
                assert target.delete();
            }
        }
    }
}