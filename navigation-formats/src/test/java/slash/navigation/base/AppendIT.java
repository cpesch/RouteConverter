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
import slash.navigation.common.NavigationPosition;
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

import static org.junit.Assert.*;
import static slash.navigation.base.NavigationFormatConverter.asFormat;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.NavigationTestCase.assertRouteNameEquals;

public class AppendIT {
    private NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

    private static boolean isStoringRouteName(NavigationFormat format) {
        return !(format instanceof GoPalRouteFormat) && !(format instanceof GoPalTrackFormat) &&
                !(format instanceof GpsTunerFormat) && !(format instanceof TomTomRouteFormat) &&
                !(format instanceof NmeaFormat) && !(format instanceof Nmn4Format) &&
                !(format instanceof Nmn5Format) && !(format instanceof Nmn6Format) &&
                !(format instanceof MagicMapsPthFormat);
    }

    private void append(String testFileName, String appendFileName) throws IOException {
        File appendFile = new File(appendFileName);
        ParserResult appendResult = parser.read(appendFile);
        assertNotNull(appendResult);
        assertNotNull(appendResult.getTheRoute());
        assertNotNull(appendResult.getFormat());
        assertNotNull(appendResult.getAllRoutes());
        assertTrue(appendResult.getAllRoutes().size() > 0);

        int appendPositionCount = appendResult.getTheRoute().getPositionCount();
        List<BaseNavigationPosition> appendPositions = appendResult.getTheRoute().getPositions();
        assertTrue(appendPositionCount > 0);

        File testFile = new File(testFileName);
        ParserResult testResult = parser.read(testFile);
        assertNotNull(testResult);
        BaseRoute<?, ?> testRoute = testResult.getTheRoute();
        assertNotNull(testRoute);
        NavigationFormat<?> testFormat = testResult.getFormat();
        assertNotNull(testFormat);
        String testName = testResult.getTheRoute().getName();
        List<String> testDescription = testResult.getTheRoute().getDescription();
        int testPositionCount = testResult.getTheRoute().getPositionCount();
        List<NavigationPosition> testPositions = new ArrayList<NavigationPosition>(testResult.getTheRoute().getPositions());
        assertTrue(testPositionCount > 0);

        NavigationFormatParser appendParser = new NavigationFormatParser(new AllNavigationFormatRegistry());
        appendParser.read(appendFile);

        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> appendRoute = asFormat(appendResult.getTheRoute(), testResult.getFormat());
        testResult.getTheRoute().getPositions().addAll(appendRoute.getPositions());

        BaseRoute<BaseNavigationPosition, ?> route = testResult.getTheRoute();
        assertEquals(testRoute, route);
        // since a lot of formats determine route names from the first
        // and the (here changing) last way point name
        if (isStoringRouteName(testFormat))
            assertRouteNameEquals(testName, route.getName());
        assertEquals(testDescription, route.getDescription());
        assertEquals(testPositionCount + appendPositionCount, route.getPositionCount());

        List<BaseNavigationPosition> positions = route.getPositions();
        Class<? extends NavigationPosition> positionClass = testPositions.get(0).getClass();
        for (int i = 0; i < testPositionCount; i++) {
            NavigationPosition position = positions.get(i);
            assertEquals(positionClass, position.getClass());
            assertEquals(testPositions.get(i), position);
        }

        for (int i = testPositionCount; i < testPositionCount + appendPositionCount; i++) {
            NavigationPosition position = positions.get(i);
            assertEquals(positionClass, position.getClass());
            NavigationPosition expected = asFormat(appendPositions.get(i - testPositionCount), route.getFormat());
            assertEquals(expected, position);
        }
    }

    @Test
    public void testAppendTomTomRouteToMTP0607() throws IOException {
        append(TEST_PATH + "from-mtp0607.bcr", TEST_PATH + "large.itn");
    }

    @Test
    public void testAppendBcrToKml() throws IOException {
        append(TEST_PATH + "from20.kml", TEST_PATH + "large.bcr");
    }

    @Test
    public void testAppendKmlToGpx() throws IOException {
        append(TEST_PATH + "large10.gpx", TEST_PATH + "from21.kml");
    }

    @Test
    public void testAppendGpxToKml() throws IOException {
        append(TEST_PATH + "from20.kml", TEST_PATH + "large11.gpx");
    }

    @Test
    public void testAppendKmlToTomTomRoute() throws IOException {
        append(TEST_PATH + "large.itn", TEST_PATH + "large20.kml");
    }

    @Test
    public void testAppendGpxToExcel() throws IOException {
        append(TEST_PATH + "from10.gpx", TEST_PATH + "from.xls");
        append(TEST_PATH + "from11.gpx", TEST_PATH + "from.xlsx");
    }

    @Test
    public void testAppendGarminMapSource5ToGarminMapSource6() throws IOException {
        // writing Mapsource 5 seems limited to 1 position
        append(TEST_PATH + "from3.gdb", TEST_PATH + "from.mps");
    }

    @Test
    public void testAppendGarminMapSource6ToGarminMapSource5() throws IOException {
        // writing Mapsource 5 seems limited to 1 position
        append(TEST_PATH + "from.mps", TEST_PATH + "from2.gdb");
    }

    @Test
    public void testAppendTourExchangeToGarminMapSource6() throws IOException {
        append(TEST_PATH + "from2.gdb", TEST_PATH + "from.tef");
    }

    @Test
    public void testAppendGarminPcx5ToNmea() throws IOException {
        append(TEST_PATH + "from.nmea", TEST_PATH + "from-pcx5.wpt");
    }

    @Test
    public void testAppendNmeaToNmn4() throws IOException {
        append(TEST_PATH + "from.nmea", TEST_PATH + "from-nmn4.rte");
    }

    @Test
    public void testAppendNmeaToNmn5() throws IOException {
        append(TEST_PATH + "from.nmea", TEST_PATH + "from-nmn5.rte");
    }

    @Test
    public void testAppendNmeaToNmn6() throws IOException {
        append(TEST_PATH + "from.nmea", TEST_PATH + "from-nmn6.rte");
    }

    @Test
    public void testAppendNmn4ToNmea() throws IOException {
        append(TEST_PATH + "from-nmn4.rte", TEST_PATH + "from.nmea");
    }

    @Test
    public void testAppendNmn5ToNmea() throws IOException {
        append(TEST_PATH + "from-nmn5.rte", TEST_PATH + "from.nmea");
    }

    @Test
    public void testAppendNmn6ToNmea() throws IOException {
        append(TEST_PATH + "from-nmn6.rte", TEST_PATH + "from.nmea");
    }

    @Test
    public void testAppendTrkToKml() throws IOException {
        append(TEST_PATH + "from21.kml", TEST_PATH + "from-gpstuner.trk");
    }

    // Wgs84Position & subclasses

    @Test
    public void testAppendWgs84ToWgs84() throws IOException {
        append(TEST_PATH + "from-gpstuner.trk", TEST_PATH + "from-gpstuner.trk");
    }

    @Test
    public void testAppendNmeaToWgs84() throws IOException {
        append(TEST_PATH + "from-gpstuner.trk", TEST_PATH + "from.nmea");
    }

    @Test
    public void testAppendNmnToWgs84() throws IOException {
        append(TEST_PATH + "from-gpstuner.trk", TEST_PATH + "from-nmn5.rte");
    }

    @Test
    public void testAppendWgs84ToNmea() throws IOException {
        append(TEST_PATH + "from.nmea", TEST_PATH + "from-gpstuner.trk");
    }

    @Test
    public void testAppendWgs84ToNmn() throws IOException {
        append(TEST_PATH + "from-nmn5.rte", TEST_PATH + "from-gpstuner.trk");
    }

    @Test
    public void testAppendGopalRouteToGopalTrack() throws IOException {
        append(TEST_PATH + "from-gopal3.xml", TEST_PATH + "from-gopal.trk");
        append(TEST_PATH + "from-gopal5.xml", TEST_PATH + "from-gopal.trk");
    }

    @Test
    public void testAppendGopalTrackToGopalRoute() throws IOException {
        append(TEST_PATH + "from-gopal.trk", TEST_PATH + "from-gopal3.xml");
        append(TEST_PATH + "from-gopal.trk", TEST_PATH + "from-gopal5.xml");
    }

    // GkPosition

    @Test
    public void testAppendWgs84ToGk() throws IOException {
        append(TEST_PATH + "from.pth", TEST_PATH + "from-gpstuner.trk");
    }

    @Test
    public void testAppendGkToWgs84() throws IOException {
        append(TEST_PATH + "from-gpstuner.trk", TEST_PATH + "from.pth");
    }

    @Test
    public void testAppendGkToNmea() throws IOException {
        append(TEST_PATH + "from-gpstuner.trk", TEST_PATH + "from.pth");
    }

    @Test
    public void testAppendGkToNmn() throws IOException {
        append(TEST_PATH + "from-gpstuner.trk", TEST_PATH + "from.pth");
    }

    @Test
    public void testAppendNmeaToGk() throws IOException {
        append(TEST_PATH + "from.pth", TEST_PATH + "from.nmea");
    }

    @Test
    public void testAppendNmnToGk() throws IOException {
        append(TEST_PATH + "from.pth", TEST_PATH + "from-nmn4.rte");
    }

    // Wintec formats

    @Test
    public void testAppendTk1ToTes() throws IOException {
        append(TEST_PATH + "from.tes", TEST_PATH + "from.tk1");
    }

    @Test
    public void testAppendTesToTk1() throws IOException {
        append(TEST_PATH + "from.tk1", TEST_PATH + "from.tes");
    }

    @Test
    public void testAppendTesToTk2() throws IOException {
        append(TEST_PATH + "from.tk2", TEST_PATH + "from.tes");
    }
}
