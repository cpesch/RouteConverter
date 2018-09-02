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

package slash.navigation.babel;

import org.junit.Test;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.base.RouteCharacteristics;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static slash.common.io.Files.getExtension;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class CompeGPSDataFormatIT {
    private NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

    private void checkFile(String testFileName, RouteCharacteristics characteristics, int positionCount) throws IOException {
        File source = new File(TEST_PATH + testFileName);
        ParserResult result = parser.read(source, parser.getNavigationFormatRegistry().
                getReadFormatsPreferredByExtension(getExtension(testFileName)));
        assertNotNull(result);
        List<BaseRoute> routes = result.getAllRoutes();
        assertEquals(1, routes.size());
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = result.getTheRoute();
        assertEquals(characteristics, route.getCharacteristics());
        assertEquals(positionCount, route.getPositionCount());
    }

    @Test
    public void testRoute() throws IOException {
        checkFile("from-compegps.rte", Route, 19);
    }

    @Test
    public void testTrack() throws IOException {
        checkFile("from-compegps.trk", Track, 3670);
    }

    @Test
    public void testWaypoints() throws IOException {
        checkFile("from-compegps.wpt", Waypoints, 31);
    }
}