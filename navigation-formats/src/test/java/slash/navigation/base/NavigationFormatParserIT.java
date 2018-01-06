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
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTom8RouteFormat;
import slash.navigation.nmn.NmnUrlFormat;
import slash.navigation.url.GoogleMapsUrlFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static slash.common.TestCase.assertEquals;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.RouteCharacteristics.*;

public class NavigationFormatParserIT {
    private NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

    ParserResult read(String testFileName) throws IOException {
        File source = new File(testFileName);
        ParserResult result = parser.read(source);
        assertNotNull(result.getFormat());
        assertNotNull(result.getAllRoutes());
        assertTrue(result.getAllRoutes().size() > 0);
        assertNotNull("Cannot read route from " + source, result.getTheRoute());
        assertTrue(result.getTheRoute().getPositionCount() > 0);
        return result;
    }

    private List<BaseRoute> getRouteCharacteristics(List<BaseRoute> routes, RouteCharacteristics characteristics) {
        List<BaseRoute> result = new ArrayList<>();
        for (BaseRoute route : routes) {
            if (route.getCharacteristics().equals(characteristics))
                result.add(route);
        }
        return result.size() > 0 ? result : null;
    }

    void readRouteCharacteristics(String testFileName, RouteCharacteristics characteristics,
                                  int characteristicsCount, int[] positionCount) throws IOException {
        ParserResult result = read(testFileName);
        List<BaseRoute> routes = getRouteCharacteristics(result.getAllRoutes(), characteristics);
        if (routes == null || characteristicsCount == 0) {
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
        readRouteCharacteristics(testFileName, Waypoints, 1, new int[]{positionCount});
    }

    void readRoute(String testFileName, int routeCount, int... positionCount) throws IOException {
        readRouteCharacteristics(testFileName, Route, routeCount, positionCount);
    }

    void readTrack(String testFileName, int trackCount, int... positionCount) throws IOException {
        readRouteCharacteristics(testFileName, Track, trackCount, positionCount);
    }

    @Test
    public void testNavigationFileParserListener() throws IOException {
        final NavigationFormat[] found = new NavigationFormat[1];
        found[0] = null;
        NavigationFormatParserListener listener = new NavigationFormatParserListener() {
            public void reading(NavigationFormat<BaseRoute> format) {
                found[0] = format;
            }
        };
        try {
            parser.addNavigationFileParserListener(listener);
            read(TEST_PATH + "from.itn");
            assertEquals(TomTom5RouteFormat.class, found[0].getClass());
            found[0] = null;
            parser.removeNavigationFileParserListener(listener);
            read(TEST_PATH + "from.itn");
            assertNull(found[0]);
        } finally {
            parser.removeNavigationFileParserListener(listener);
        }
    }

    @Test
    public void testReadWithFormatList() throws IOException {
        List<NavigationFormat> formats = new ArrayList<>();
        ParserResult result1 = parser.read(new File(TEST_PATH + "from.itn"), formats);
        assertFalse(result1.isSuccessful());

        formats.add(new TomTom8RouteFormat());
        ParserResult result2 = parser.read(new File(TEST_PATH + "from.itn"), formats);
        assertTrue(result2.isSuccessful());
        assertEquals(0, result2.getTheRoute().getPositions().size());
        assertEquals(1, result2.getAllRoutes().size());
        assertEquals(result2.getFormat().getClass(), TomTom8RouteFormat.class);

        formats.add(new TomTom5RouteFormat());
        ParserResult result3 = parser.read(new File(TEST_PATH + "from.itn"), formats);
        assertTrue(result3.isSuccessful());
        assertEquals(46, result3.getTheRoute().getPositions().size());
        assertEquals(1, result3.getAllRoutes().size());
        assertEquals(result3.getFormat().getClass(), TomTom5RouteFormat.class);
    }

    @Test
    public void testIsValidBcr() throws IOException {
        read(TEST_PATH + "from-mtp0607.bcr");
        read(TEST_PATH + "from-mtp0809.bcr");
        read(TEST_PATH + "large.bcr");
    }

    @Test
    public void testIsValidGpx10() throws IOException {
        readWaypoints(TEST_PATH + "from10.gpx", 3);
        readRoute(TEST_PATH + "from10.gpx", 3, 1, 2, 3);
        readTrack(TEST_PATH + "from10.gpx", 0);

        readWaypoints(TEST_PATH + "from10trk.gpx", 3);
        readRoute(TEST_PATH + "from10trk.gpx", 0);
        readTrack(TEST_PATH + "from10trk.gpx", 4, 1, 2, 3, 4);

        read(TEST_PATH + "large10.gpx");
    }

    @Test
    public void testIsValidGpx11() throws IOException {
        readWaypoints(TEST_PATH + "from11.gpx", 3);
        readRoute(TEST_PATH + "from11.gpx", 3, 1, 2, 3);
        readTrack(TEST_PATH + "from11.gpx", 0);

        readWaypoints(TEST_PATH + "from11trk.gpx", 3);
        readRoute(TEST_PATH + "from11trk.gpx", 0);
        readTrack(TEST_PATH + "from11trk.gpx", 4, 5, 2, 3, 4);

        read(TEST_PATH + "large11.gpx");
    }

    @Test
    public void testIsValidGarminMapSource6() throws IOException {
        read(TEST_PATH + "from2.gdb");
        read(TEST_PATH + "from3.gdb");

        readWaypoints(TEST_PATH + "from10.gdb", 6);
        readRoute(TEST_PATH + "from10.gdb", 3, 1, 2, 3);
        readTrack(TEST_PATH + "from10.gdb", 0);

        readWaypoints(TEST_PATH + "from10trk.gdb", 3);
        readRoute(TEST_PATH + "from10trk.gdb", 0);
        readTrack(TEST_PATH + "from10trk.gdb", 4, 1, 2, 3, 4);

        read(TEST_PATH + "large.gdb");
    }

    @Test
    public void testIsValidTomTomRoute() throws IOException {
        read(TEST_PATH + "from.itn");
        read(TEST_PATH + "large.itn");
    }

    @Test
    public void testIsValidKml20() throws IOException {
        read(TEST_PATH + "from20.kml");
        read(TEST_PATH + "large20.kml");
    }

    @Test
    public void testIsValidKml21() throws IOException {
        read(TEST_PATH + "from21.kml");
        read(TEST_PATH + "large21.kml");
    }

    @Test
    public void testIsValidMagellanMapSend() throws IOException {
        read(TEST_PATH + "from-mapsend.wpt");
    }

    @Test
    public void testIsValidNmea() throws IOException {
        read(TEST_PATH + "from.nmea");
    }

    @Test
    public void testIsValidNmn4() throws IOException {
        read(TEST_PATH + "from-nmn4.rte");
        read(TEST_PATH + "large-nmn4.rte");
    }

    @Test
    public void testIsValidNmn5() throws IOException {
        read(TEST_PATH + "from-nmn5.rte");
        read(TEST_PATH + "large-nmn5.rte");
    }

    @Test
    public void testIsValidNmn6() throws IOException {
        read(TEST_PATH + "from-nmn6.rte");
        read(TEST_PATH + "large-nmn6.rte");
    }

    @Test
    public void testIsValidOvl() throws IOException {
        readRoute(TEST_PATH + "from-rte.ovl", 1, 100);
        readTrack(TEST_PATH + "from.ovl", 3, 476, 476, 476);
        read(TEST_PATH + "from.ovl");
    }

    @Test
    public void testIsValidGarminPcx5() throws IOException {
        read(TEST_PATH + "from-pcx5.wpt");
        read(TEST_PATH + "large-pcx5.wpt");
    }

    @Test
    public void testIsValidTourExchange() throws IOException {
        read(TEST_PATH + "from.tef");
        read(TEST_PATH + "large.tef");
    }

    @Test
    public void testIsValidTomTomPoi() throws IOException {
        read(TEST_PATH + "from.ov2");
        read(TEST_PATH + "large.ov2");
    }

    @Test
    public void testIsValidTrk() throws IOException {
        read(TEST_PATH + "from-gpstuner.trk");
    }

    @Test
    public void testIsValidGoogleMapsUrl() throws IOException {
        ParserResult result = read(TEST_PATH + "from-googlemaps.url");
        assertEquals(GoogleMapsUrlFormat.class, result.getFormat().getClass());
    }

    @Test
    public void testIsValidNavigonUrl() throws IOException {
        ParserResult result = read(TEST_PATH + "from-nmn.txt");
        assertEquals(NmnUrlFormat.class, result.getFormat().getClass());
        ParserResult plainResult = read(TEST_PATH + "from-nmn-plain.txt");
        assertEquals(NmnUrlFormat.class, plainResult.getFormat().getClass());
    }
}
