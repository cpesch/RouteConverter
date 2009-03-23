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

import slash.navigation.gopal.GoPalRouteFormat;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.mm.MagicMapsPthFormat;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmn.Nmn4Format;
import slash.navigation.nmn.Nmn5Format;
import slash.navigation.nmn.Nmn6Format;
import slash.navigation.simple.GpsTunerFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AppendTest extends NavigationTestCase {
    NavigationFileParser parser = new NavigationFileParser();

    static boolean isStoringRouteName(NavigationFormat format) {
        return !(format instanceof GoPalRouteFormat) && !(format instanceof GoPalTrackFormat) &&
                !(format instanceof GpsTunerFormat) && !(format instanceof TomTomRouteFormat) &&
                !(format instanceof NmeaFormat) && !(format instanceof Nmn4Format) &&
                !(format instanceof Nmn5Format) && !(format instanceof Nmn6Format) &&
                !(format instanceof MagicMapsPthFormat);
    }

    void append(String testFileName, String appendFileName) throws IOException {
        File appendFile = new File(appendFileName);
        assertTrue(parser.read(appendFile));
        assertNotNull(parser.getTheRoute());
        assertNotNull(parser.getFormat());
        assertNotNull(parser.getAllRoutes());
        assertTrue(parser.getAllRoutes().size() > 0);

        int appendPositionCount = parser.getTheRoute().getPositionCount();
        List<BaseNavigationPosition> appendPositions = parser.getTheRoute().getPositions();
        assertTrue(appendPositionCount > 0);

        File testFile = new File(testFileName);
        parser.read(testFile);
        BaseRoute testRoute = parser.getTheRoute();
        assertNotNull(testRoute);
        NavigationFormat testFormat = parser.getFormat();
        assertNotNull(testFormat);
        String testName = parser.getTheRoute().getName();
        List<String> testDescription = parser.getTheRoute().getDescription();
        int testPositionCount = parser.getTheRoute().getPositionCount();
        List<BaseNavigationPosition> testPositions = new ArrayList<BaseNavigationPosition>(parser.getTheRoute().getPositions());
        assertTrue(testPositionCount > 0);

        NavigationFileParser appendParser = new NavigationFileParser();
        appendParser.read(appendFile);

        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> appendRoute = NavigationFormats.asFormat(appendParser.getTheRoute(), parser.getFormat());
        parser.getTheRoute().getPositions().addAll(appendRoute.getPositions());

        BaseRoute route = parser.getTheRoute();
        assertEquals(testRoute, route);
        // since a lot of formats determine route names from the first
        // and the (here changing) last way point name
        if (isStoringRouteName(testFormat))
            assertEquals(testName, route.getName());
        assertEquals(testDescription, route.getDescription());
        assertEquals(testPositionCount + appendPositionCount, route.getPositionCount());

        List<BaseNavigationPosition> positions = route.getPositions();
        Class<? extends BaseNavigationPosition> positionClass = testPositions.get(0).getClass();
        for (int i = 0; i < testPositionCount; i++) {
            BaseNavigationPosition position = positions.get(i);
            assertEquals(positionClass, position.getClass());
            assertEquals(testPositions.get(i), position);
        }

        for (int i = testPositionCount; i < testPositionCount + appendPositionCount; i++) {
            BaseNavigationPosition position = positions.get(i);
            assertEquals(positionClass, position.getClass());
            BaseNavigationPosition expected = NavigationFormats.asFormat(appendPositions.get(i - testPositionCount), route.getFormat());
            assertEquals(expected, position);
        }
    }

    public void testAppendTomTomRouteToMTP0607() throws IOException {
        append(TEST_PATH + "from-mtp0607.bcr", TEST_PATH + "large.itn");
    }

    public void testAppendBcrToKml() throws IOException {
        append(TEST_PATH + "from20.kml", TEST_PATH + "large.bcr");
    }

    public void testAppendKmlToGpx() throws IOException {
        append(TEST_PATH + "large10.gpx", TEST_PATH + "from21.kml");
    }

    public void testAppendGpxToKml() throws IOException {
        append(TEST_PATH + "from20.kml", TEST_PATH + "large11.gpx");
    }

    public void testAppendKmlToTomTomRoute() throws IOException {
        append(TEST_PATH + "large.itn", TEST_PATH + "large20.kml");
    }

    public void testAppendGarminMapSource5ToGarminMapSource6() throws IOException {
        append(TEST_PATH + "from.gdb", TEST_PATH + "from.mps");
    }

    public void testAppendGarminMapSource6ToGarminMapSource5() throws IOException {
        append(TEST_PATH + "from.mps", TEST_PATH + "from.gdb");
    }

    public void testAppendMicrosoftAutoRouteToMagellanMapSend() throws IOException {
        append(TEST_PATH + "from.axe", TEST_PATH + "from-mapsend.wpt");
    }

    public void testAppendMicrosoftAutoRouteToGarminMapSource6() throws IOException {
        append(TEST_PATH + "from.gdb", TEST_PATH + "from.axe");
    }

    public void testAppendMagellanMapSendToGarminMapSource6() throws IOException {
        append(TEST_PATH + "from-mapsend.wpt", TEST_PATH + "from.axe");
    }

    public void testAppendTourExchangeToGarminMapSource6() throws IOException {
        append(TEST_PATH + "from.gdb", TEST_PATH + "from.tef");
    }

    public void testAppendGarminPcx5ToNmea() throws IOException {
        append(TEST_PATH + "from.nmea", TEST_PATH + "from-pcx5.wpt");
    }

    public void testAppendNmeaToNmn4() throws IOException {
        append(TEST_PATH + "from.nmea", TEST_PATH + "from-nmn4.rte");
    }

    public void testAppendNmeaToNmn5() throws IOException {
        append(TEST_PATH + "from.nmea", TEST_PATH + "from-nmn5.rte");
    }

    public void testAppendNmeaToNmn6() throws IOException {
        append(TEST_PATH + "from.nmea", TEST_PATH + "from-nmn6.rte");
    }

    public void testAppendNmn4ToNmea() throws IOException {
        append(TEST_PATH + "from-nmn4.rte", TEST_PATH + "from.nmea");
    }

    public void testAppendNmn5ToNmea() throws IOException {
        append(TEST_PATH + "from-nmn5.rte", TEST_PATH + "from.nmea");
    }

    public void testAppendNmn6ToNmea() throws IOException {
        append(TEST_PATH + "from-nmn6.rte", TEST_PATH + "from.nmea");
    }

    public void testAppendTrkToKml() throws IOException {
        append(TEST_PATH + "from21.kml", TEST_PATH + "from-gpstuner.trk");
    }

    // Wgs84Position & subclasses

    public void testAppendWgs84ToWgs84() throws IOException {
        append(TEST_PATH + "from-gpstuner.trk", TEST_PATH + "from-gpstuner.trk");
    }

    public void testAppendNmeaToWgs84() throws IOException {
        append(TEST_PATH + "from-gpstuner.trk", TEST_PATH + "from.nmea");
    }

    public void testAppendNmnToWgs84() throws IOException {
        append(TEST_PATH + "from-gpstuner.trk", TEST_PATH + "from-nmn5.rte");
    }

    public void testAppendWgs84ToNmea() throws IOException {
        append(TEST_PATH + "from.nmea", TEST_PATH + "from-gpstuner.trk");
    }

    public void testAppendWgs84ToNmn() throws IOException {
        append(TEST_PATH + "from-nmn5.rte", TEST_PATH + "from-gpstuner.trk");
    }

    public void testAppendGopalRouteToGopalTrack() throws IOException {
        append(TEST_PATH + "from-gopal.xml", TEST_PATH + "from-gopal.trk");
    }

    public void testAppendGopalTrackToGopalRoute() throws IOException {
        append(TEST_PATH + "from-gopal.trk", TEST_PATH + "from-gopal.xml");
    }

    // GkPosition

    public void testAppendWgs84ToGk() throws IOException {
        append(TEST_PATH + "from.pth", TEST_PATH + "from-gpstuner.trk");
    }

    public void testAppendGkToWgs84() throws IOException {
        append(TEST_PATH + "from-gpstuner.trk", TEST_PATH + "from.pth");
    }

    public void testAppendGkToNmea() throws IOException {
        append(TEST_PATH + "from-gpstuner.trk", TEST_PATH + "from.pth");
    }

    public void testAppendGkToNmn() throws IOException {
        append(TEST_PATH + "from-gpstuner.trk", TEST_PATH + "from.pth");
    }

    public void testAppendNmeaToGk() throws IOException {
        append(TEST_PATH + "from.pth", TEST_PATH + "from.nmea");
    }

    public void testAppendNmnToGk() throws IOException {
        append(TEST_PATH + "from.pth", TEST_PATH + "from-nmn4.rte");
    }
}
