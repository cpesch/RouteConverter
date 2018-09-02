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

import slash.navigation.babel.AlanTrackLogFormat;
import slash.navigation.babel.AlanWaypointsAndRoutesFormat;
import slash.navigation.babel.GarminPcx5Format;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.io.File.createTempFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static slash.common.io.Files.getExtension;
import static slash.navigation.base.NavigationTestCase.comparePositions;
import static slash.navigation.base.NavigationTestCase.compareRouteMetaData;

public abstract class ReadWriteBase {
    @SuppressWarnings("unchecked")
    public static void readWriteRoundtrip(String testFileName, ReadWriteTestCallback parserCallback) throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

        File source = new File(testFileName);
        ParserResult result = parser.read(source);
        assertNotNull("Could not read " + testFileName, result);
        assertNotNull(result);
        assertNotNull(result.getFormat());
        assertNotNull(result.getAllRoutes());
        assertTrue(result.getAllRoutes().size() > 0);

        File target = createTempFile("target", getExtension(source));
        // see AlanWaypointsAndRoutesFormat#isSupportsMultipleRoutes
        if (result.getFormat().isSupportsMultipleRoutes() || result.getFormat() instanceof AlanWaypointsAndRoutesFormat)
            parser.write(result.getAllRoutes(), (MultipleRoutesFormat) result.getFormat(), target);
        else
            parser.write(result.getTheRoute(), result.getFormat(), false, true, null, target);

        // NOT possible to determine if I add description lines while writing
        // assertEquals(source.length(), target.length());

        ParserResult sourceResult = parser.read(source);
        ParserResult targetResult = parser.read(target);

        NavigationFormat sourceFormat = sourceResult.getFormat();
        NavigationFormat targetFormat = targetResult.getFormat();
        assertEquals(sourceFormat.getName(), targetFormat.getName());
        // see AlanWaypointsAndRoutesFormat#isSupportsMultipleRoutes
        if (sourceFormat.isSupportsMultipleRoutes() || sourceFormat instanceof AlanWaypointsAndRoutesFormat)
            assertEquals(sourceResult.getAllRoutes().size(), targetResult.getAllRoutes().size());
        else
            assertEquals(1, targetResult.getAllRoutes().size());

        List<BaseRoute> sourceRoutes = sourceResult.getAllRoutes();
        List<BaseRoute> targetRoutes = targetResult.getAllRoutes();
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

        if (parserCallback != null)
            parserCallback.test(sourceResult, targetResult);

        assertTrue(target.exists());
        assertTrue(target.delete());
    }

    public static void readWriteRoundtrip(String testFileName) throws IOException {
        readWriteRoundtrip(testFileName, null);
    }
}
