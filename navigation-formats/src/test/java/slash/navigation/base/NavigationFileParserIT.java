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

import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFileParser;
import slash.navigation.base.RouteCharacteristics;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class NavigationFileParserIT extends NavigationTestCase {
    NavigationFileParser parser = new NavigationFileParser();

    void read(String testFileName) throws IOException {
        File source = new File(testFileName);
        assertTrue(parser.read(source));
        assertNotNull(parser.getFormat());
        assertNotNull(parser.getAllRoutes());
        assertTrue(parser.getAllRoutes().size() > 0);
        assertNotNull("Cannot read route from " + source, parser.getTheRoute());
        assertTrue(parser.getTheRoute().getPositionCount() > 0);
    }

    void readRouteCharacteristics(String testFileName, RouteCharacteristics characteristics,
                                  int characteristicsCount, int[] positionCount) throws IOException {
        read(testFileName);
        List<BaseRoute> routes = parser.getRouteCharacteristics(characteristics);
        if (characteristicsCount == 0) {
            assertNull(routes);
        } else {
            assertEquals(characteristicsCount, routes.size());
            for (int i = 0; i < routes.size(); i++) {
                BaseRoute route = routes.get(i);
                assertEquals(characteristics, route.getCharacteristics());
                assertEquals(positionCount[i], route.getPositionCount());
            }
        }
    }

    void readWaypoints(String testFileName, int positionCount) throws IOException {
        readRouteCharacteristics(testFileName, RouteCharacteristics.Waypoints, 1, new int[]{positionCount});
    }

    void readRoute(String testFileName, int routeCount, int... positionCount) throws IOException {
        readRouteCharacteristics(testFileName, RouteCharacteristics.Route, routeCount, positionCount);
    }

    void readTrack(String testFileName, int trackCount, int... positionCount) throws IOException {
        readRouteCharacteristics(testFileName, RouteCharacteristics.Track, trackCount, positionCount);
    }


    public void testIsValidMicrosoftAutoRoute() throws IOException {
        read(TEST_PATH + "from.axe");
        read(TEST_PATH + "large.axe");
    }

    public void testIsValidBcr() throws IOException {
        read(TEST_PATH + "from-mtp0607.bcr");
        read(TEST_PATH + "from-mtp0809.bcr");
        read(TEST_PATH + "large.bcr");
    }

    public void testIsValidGpx10() throws IOException {
        readWaypoints(TEST_PATH + "from10.gpx", 3);
        readRoute(TEST_PATH + "from10.gpx", 3, 1, 2, 3);
        readTrack(TEST_PATH + "from10.gpx", 0);

        readWaypoints(TEST_PATH + "from10trk.gpx", 3);
        readRoute(TEST_PATH + "from10trk.gpx", 0);
        readTrack(TEST_PATH + "from10trk.gpx", 4, 1, 2, 3, 4);

        read(TEST_PATH + "large10.gpx");
    }

    public void testIsValidGpx11() throws IOException {
        readWaypoints(TEST_PATH + "from11.gpx", 3);
        readRoute(TEST_PATH + "from11.gpx", 3, 1, 2, 3);
        readTrack(TEST_PATH + "from11.gpx", 0);

        readWaypoints(TEST_PATH + "from11trk.gpx", 3);
        readRoute(TEST_PATH + "from11trk.gpx", 0);
        readTrack(TEST_PATH + "from11trk.gpx", 4, 1, 2, 3, 4);

        read(TEST_PATH + "large11.gpx");
    }

    public void testIsValidGarminMapSource6() throws IOException {
        read(TEST_PATH + "from.gdb");

        readWaypoints(TEST_PATH + "from10.gdb", 6);
        readRoute(TEST_PATH + "from10.gdb", 3, 1, 2, 3);
        readTrack(TEST_PATH + "from10.gdb", 0);

        readWaypoints(TEST_PATH + "from10trk.gdb", 3);
        readRoute(TEST_PATH + "from10trk.gdb", 0);
        readTrack(TEST_PATH + "from10trk.gdb", 4, 1, 2, 3, 4);

        read(TEST_PATH + "large.gdb");
    }

    public void testIsValidTomTomRoute() throws IOException {
        read(TEST_PATH + "from.itn");
        read(TEST_PATH + "large.itn");
    }

    public void testIsValidKml20() throws IOException {
        read(TEST_PATH + "from20.kml");
        read(TEST_PATH + "large20.kml");
    }

    public void testIsValidKml21() throws IOException {
        read(TEST_PATH + "from21.kml");
        read(TEST_PATH + "large21.kml");
    }

    public void testIsValidMagellanMapSend() throws IOException {
        read(TEST_PATH + "from-mapsend.wpt");
    }

    public void testIsValidNmea() throws IOException {
        read(TEST_PATH + "from.nmea");
    }

    public void testIsValidNmn4() throws IOException {
        read(TEST_PATH + "from-nmn4.rte");
        read(TEST_PATH + "large-nmn4.rte");
    }

    public void testIsValidNmn5() throws IOException {
        read(TEST_PATH + "from-nmn5.rte");
        read(TEST_PATH + "large-nmn5.rte");
    }

    public void testIsValidNmn6() throws IOException {
        read(TEST_PATH + "from-nmn6.rte");
        read(TEST_PATH + "large-nmn6.rte");
    }

    public void testIsValidOvl() throws IOException {
        readRoute(TEST_PATH + "from-rte.ovl", 1, 100);
        readTrack(TEST_PATH + "from.ovl", 3, 476, 476, 476);
        read(TEST_PATH + "from.ovl");
    }

    public void testIsValidGarminPcx5() throws IOException {
        read(TEST_PATH + "from-pcx5.wpt");
        read(TEST_PATH + "large-pcx5.wpt");
    }

    public void testIsValidTourExchange() throws IOException {
        read(TEST_PATH + "from.tef");
        read(TEST_PATH + "large.tef");
    }

    public void testIsValidTomTomPoi() throws IOException {
        read(TEST_PATH + "from.ov2");
        read(TEST_PATH + "large.ov2");
    }

    public void testIsValidTrk() throws IOException {
        read(TEST_PATH + "from-gpstuner.trk");
    }

    public void testIsValidUrl() throws IOException {
        read(TEST_PATH + "from.url");
    }
}
