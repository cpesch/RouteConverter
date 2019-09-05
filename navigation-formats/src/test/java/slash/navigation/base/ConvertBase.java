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
import slash.navigation.babel.GarminMapSource6Format;
import slash.navigation.common.NavigationPosition;
import slash.navigation.tcx.TcxFormat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.io.File.createTempFile;
import static org.junit.Assert.*;
import static slash.common.io.Files.getExtension;
import static slash.navigation.base.NavigationFormatConverter.asFormat;
import static slash.navigation.base.NavigationTestCase.comparePositions;
import static slash.navigation.base.NavigationTestCase.compareRouteMetaData;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.columbus.ColumbusV1000Device.getUseLocalTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.setUseLocalTimeZone;

public abstract class ConvertBase {

    @Test
    public static void convertRoundtrip(String testFileName,
                                        BaseNavigationFormat sourceFormat,
                                        BaseNavigationFormat targetFormat) throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

        assertTrue(sourceFormat.isSupportsReading());
        assertTrue(targetFormat.isSupportsWriting());

        File source = new File(testFileName);
        ParserResult result = parser.read(source, parser.getNavigationFormatRegistry().getReadFormatsPreferredByExtension(getExtension(testFileName)));
        assertNotNull("Cannot read route from " + source, result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getFormat());
        assertNotNull(result.getTheRoute());
        assertNotNull(result.getAllRoutes());
        assertTrue(result.getAllRoutes().size() > 0);

        // check append
        NavigationPosition sourcePosition = result.getTheRoute().getPositions().get(0);
        NavigationPosition targetPosition = asFormat(sourcePosition, targetFormat);
        assertNotNull(targetPosition);

        convertSingleRouteRoundtrip(sourceFormat, targetFormat, source, result.getTheRoute());

        if (targetFormat.isSupportsMultipleRoutes()) {
            convertMultipleRouteRoundtrip(sourceFormat, targetFormat, source, Collections.singletonList(result.getTheRoute()));
            convertMultipleRouteRoundtrip(sourceFormat, targetFormat, source, result.getAllRoutes());
        }
    }

    private static void removeTcxLap(BaseNavigationFormat targetFormat, ParserResult targetResult) {
        if (!(targetFormat instanceof TcxFormat))
            return;

        Set<BaseRoute> remove = new HashSet<>();
        for(BaseRoute route : targetResult.getAllRoutes()) {
            Wgs84Position first = (Wgs84Position) route.getPosition(0);
            if(first.getOrigin().getClass().getName().contains("CourseLap"))
                remove.add(route);
        }

        // remove 2 element <Lap>s with start and end from TcxFormat
        targetResult.getAllRoutes().removeAll(remove);
    }

    @SuppressWarnings("unchecked")
    private static void convertSingleRouteRoundtrip(BaseNavigationFormat sourceFormat, BaseNavigationFormat targetFormat, File source, BaseRoute sourceRoute) throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

        File target = createTempFile("singletarget", targetFormat.getExtension());
        target.deleteOnExit();
        try {
            parser.write(sourceRoute, targetFormat, false, false, null, target);
            assertTrue(target.exists());

            ParserResult sourceResult = parser.read(source, parser.getNavigationFormatRegistry().getReadFormatsPreferredByExtension(getExtension(source)));
            assertNotNull(sourceResult);
            ParserResult targetResult = parser.read(target, parser.getNavigationFormatRegistry().getReadFormatsPreferredByExtension(getExtension(target)));
            assertNotNull(targetResult);

            assertEquals(sourceFormat.getClass(), sourceResult.getFormat().getClass());
            assertEquals(targetFormat.getClass(), targetResult.getFormat().getClass());
            assertEquals(sourceFormat.getName(), sourceResult.getFormat().getName());
            assertEquals(targetFormat.getName(), targetResult.getFormat().getName());

            removeTcxLap(targetFormat, targetResult);

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
            if (target.exists())
                assertTrue(target.delete());
        }
    }

    @SuppressWarnings("unchecked")
    private static void convertMultipleRouteRoundtrip(BaseNavigationFormat sourceFormat, BaseNavigationFormat targetFormat, File source, List<BaseRoute> sourceRoutes) throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

        File target = createTempFile("multitarget", targetFormat.getExtension());
        target.deleteOnExit();
        try {
            parser.write(sourceRoutes, (MultipleRoutesFormat) targetFormat, target);
            assertTrue(target.exists());

            ParserResult sourceResult = parser.read(source, parser.getNavigationFormatRegistry().getReadFormatsPreferredByExtension(getExtension(source)));
            assertNotNull(sourceResult);
            assertTrue(sourceResult.isSuccessful());
            ParserResult targetResult = parser.read(target, parser.getNavigationFormatRegistry().getReadFormatsPreferredByExtension(getExtension(target)));
            assertNotNull(targetResult);
            assertTrue(targetResult.isSuccessful());

            assertEquals(sourceFormat.getClass(), sourceResult.getFormat().getClass());
            assertEquals(targetFormat.getClass(), targetResult.getFormat().getClass());
            assertEquals(sourceFormat.getName(), sourceResult.getFormat().getName());
            assertEquals(targetFormat.getName(), targetResult.getFormat().getName());

            removeTcxLap(targetFormat, targetResult);

            compareRouteMetaData(sourceResult.getTheRoute(), targetResult.getTheRoute());

            for (int i = 0; i < targetResult.getAllRoutes().size(); i++) {
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute = targetResult.getAllRoutes().get(i);
                BaseRoute sourceRoute = sourceResult.getAllRoutes().get(i);
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
            if (target.exists())
                assertTrue(target.delete());
        }
    }

    public interface RunnableThrowsException {
        void run() throws Exception;
    }

    public static void ignoreLocalTimeZone(RunnableThrowsException runnable) throws Exception {
        boolean localTimeZone = getUseLocalTimeZone();
        try {
            setUseLocalTimeZone(false);

            runnable.run();
        } finally {
            setUseLocalTimeZone(localTimeZone);
        }
    }
}