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

import org.junit.AfterClass;
import org.junit.Test;
import slash.common.io.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static slash.navigation.base.NavigationTestCase.*;

public class ReadIT {
    private NavigationFileParser parser = new NavigationFileParser();
    private static Set<String> comments = new HashSet<String>();

    protected interface TestFileCallback {
        void test(File file) throws IOException;
    }

    private void readFiles(String extension, TestFileCallback callback) throws IOException {
        List<File> files = Files.collectFiles(new File(TEST_PATH), extension);
        for (File file : files)
            callback.test(file);
        files = Files.collectFiles(new File(SAMPLE_PATH), extension);
        for (File file : files)
            callback.test(file);
    }

    private static final List NO_NAME_DEFINED = Arrays.asList("alanwpr.gpx", "bcr_with_gpsbabel.gpx",
            "bcr_with_nhtoptrans.gpx", "expertgps.gpx", "Erzgebirge.gpx", "fells_loop.gpx",
            "garmin_symbols.gpx", "holux.gpx", "large10.gpx", "tm20070607.gpx");

    private void readFiles(String extension) throws IOException {
        readFiles(extension, new TestFileCallback() {
            @SuppressWarnings({"unchecked"})
            public void test(File file) throws IOException {
                assertTrue("Cannot read route from " + file, parser.read(file));
                assertNotNull(parser.getFormat());
                assertNotNull("Cannot get route from " + file, parser.getTheRoute());
                assertNotNull(parser.getAllRoutes());
                assertTrue(parser.getAllRoutes().size() > 0);
                for(BaseRoute route : parser.getAllRoutes()) {
                    List<BaseNavigationPosition> positions = route.getPositions();
                    for(BaseNavigationPosition position : positions) {
                        comments.add(position.getComment());
                    }
                }
                if (!NO_NAME_DEFINED.contains(file.getName()) && !file.getName().endsWith(".axe") &&
                        !file.getName().endsWith(".gdb") && !file.getName().endsWith(".nmea") &&
                        !file.getName().endsWith(".mps") && !file.getName().endsWith(".rte") &&
                        !file.getName().endsWith(".tef") && !file.getName().endsWith(".wpt"))
                    assertNotNull("Route has no name", parser.getTheRoute().getName());
                // a GoPal 3 track without positions which is not rejected because the following Nmn4Format would try to readSampleGpxFile if forever
                // a OziExplorer Route has a first route without a single position
                if (!file.getName().equals("dieter3-GoPal3Track.trk") && !file.getName().equals("ozi-condecourt.rte"))
                    assertTrue("Route " + file + " has no positions", parser.getTheRoute().getPositionCount() > 0);
            }
        });
    }

    @AfterClass
    public static void tearDown() throws IOException {
        PrintStream out = new PrintStream(new FileOutputStream(File.createTempFile("comments", ".csv")));
        String[] strings = comments.toArray(new String[comments.size()]);
        Arrays.sort(strings);
        for (String string : strings) {
            out.println(string);
        }
        out.flush();
        out.close();
    }

    @Test
    public void testAscFilesAreValid() throws IOException {
        readFiles(".asc");
    }

    @Test
    public void testMicrosoftAutoRouteFilesAreValid() throws IOException {
        readFiles(".axe");
    }

    @Test
    public void testMicrosoftMapPointFilesAreValid() throws IOException {
        readFiles(".ptm");
    }

    @Test
    public void testBcrFilesAreValid() throws IOException {
        readFiles(".bcr");
    }

    @Test
    public void testCsvFilesAreValid() throws IOException {
        // Columbus V900
        readFiles(".csv");
    }

    @Test
    public void testGarminPoiFilesAreValid() throws IOException {
        readFiles(".gpi");
    }

    @Test
    public void testGarminPoiDbFilesAreValid() throws IOException {
        readFiles(".xcsv");
    }

    @Test
    public void testGarminMapSource6FilesAreValid() throws IOException {
        readFiles(".gdb");
    }

    @Test
    public void testGeoCachingFilesAreValid() throws IOException {
        readFiles(".loc");
    }

    @Test
    public void testGpxFilesAreValid() throws IOException {
        readFiles(".gpx");
    }

    @Test
    public void testHoluxM241BinaryFilesAreValid() throws IOException {
        readFiles(".bin");
    }

    @Test
    public void testTomTomRouteFilesAreValid() throws IOException {
        readFiles(".itn");
    }

    @Test
    public void testKmlFilesAreValid() throws IOException {
        readFiles(".kml");
    }

    @Test
    public void testKmzFilesAreValid() throws IOException {
        readFiles(".kmz");
    }

    @Test
    public void testKlickTelRouteFilesAreValid() throws IOException {
        readFiles(".krt");
    }

    @Test
    public void testMagellanExploristFilesAreValid() throws IOException {
        readFiles(".log");
    }

    @Test
    public void testMagicMapsIktFilesAreValid() throws IOException {
        readFiles(".ikt");
    }

    @Test
    public void testNokiaLandmarkExchangeFilesAreValid() throws IOException {
        readFiles(".lmx");
    }

    @Test
    public void testNmeaFilesAreValid() throws IOException {
        readFiles(".nmea");
        readFiles(".pgl");
    }

    @Test
    public void testNmn7FilesAreValid() throws IOException {
        readFiles(".freshroute");
    }

    @Test
    public void testMagellanMapSendFilesAreValid() throws Exception {
        readFiles(".mps");
    }

    @Test
    public void testOv2FilesAreValid() throws IOException {
        readFiles(".ov2");
    }

    @Test
    public void testOvlFilesAreValid() throws IOException {
        readFiles(".ovl");
    }

    @Test
    public void testOziExplorerTrackFilesAreValid() throws IOException {
        readFiles(".plt");
    }

    @Test
    public void testMagicMapsPthFilesAreValid() throws IOException {
        readFiles(".pth");
    }

    @Test
    public void testRteFilesAreValid() throws IOException {
        // Magellan Route
        // Navigon Mobile Navigator
        // OziExplorerRoute
        readFiles(".rte");
    }

    @Test
    public void testNavilinkFilesAreValid() throws IOException {
        readFiles(".sbp");
    }

    @Test
    public void testTourExchangeFilesAreValid() throws IOException {
        readFiles(".tef");
    }

    @Test
    public void testTrainingCenterDatabaseFilesAreValid() throws IOException {
        readFiles(".crs");
        readFiles(".hst");
        readFiles(".tcx");
    }

    @Test
    public void testTourFilesAreValid() throws IOException {
        readFiles(".tour");
    }

    @Test
    public void testTrkFilesAreValid() throws IOException {
        readFiles(".trk");
    }

    @Test
    public void testTkFilesAreValid() throws IOException {
        // Glopus
        // Kompass
        readFiles(".tk");
    }

    @Test
    public void testNationalGeographicTopo3FilesAreValid() throws IOException {
        readFiles(".tpo");
    }

    @Test
    public void testTrpFilesAreValid() throws IOException {
        readFiles(".trp");
    }

    @Test
    public void testTextFilesAreValid() throws IOException {
        // KienzleGps
        // MagicMaps2Go
        // NmnUrl
        // Sygic
        readFiles(".txt");
    }

    @Test
    public void testAlanTracklogFilesAreValid() throws IOException {
        readFiles(".trl");
    }

    @Test
    public void testUrlFilesAreValid() throws IOException {
        // GoogleMapsUrl
        readFiles(".url");
    }

    @Test
    public void testWintecWbt202TesFilesAreValid() throws IOException {
        readFiles(".tes");
    }

    @Test
    public void testWintecWbt201Tk1FilesAreValid() throws IOException {
        readFiles(".tk1");
    }

    @Test
    public void testWintecWbt201Tk2FilesAreValid() throws IOException {
        readFiles(".tk2");
    }

    @Test
    public void testWprFilesAreValid() throws IOException {
        readFiles(".wpr");
    }

    @Test
    public void testOziExplorerWaypointFilesAreValid() throws IOException {
        readFiles(".wpt");
    }

    @Test
    public void testXmlFilesAreValid() throws IOException {
        // GoPal 3,5
        readFiles(".xml");
    }

    @Test
    public void testViaMichelinFilesAreValid() throws IOException {
        readFiles(".xvm");
    }


    private void dontReadFiles(String path, TestFileCallback callback) throws IOException {
        List<File> files = Files.collectFiles(new File(path), null);
        for (File file : files)
            if (!file.getPath().contains(".svn"))
                callback.test(file);
    }

    public static final String UNRECOGNIZED_PATH = ROUTE_PATH + "unrecognized\\";
    public static final String FALSE_DETECTS_PATH = ROUTE_PATH + "falsedetects\\";

    @Test
    public void testDontReadUnrecognizedFiles() throws IOException {
        dontReadFiles(UNRECOGNIZED_PATH, new TestFileCallback() {
            public void test(File file) throws IOException {
                boolean success = parser.read(file);
                NavigationFormat format = success ? parser.getFormat() : null;
                assertFalse("Can read route from " + file + " as " + format, success);
            }
        });
    }

    @Test
    public void testReadFalseDetectsFiles() throws IOException {
        dontReadFiles(FALSE_DETECTS_PATH, new TestFileCallback() {
            public void test(File file) throws IOException {
                boolean success = parser.read(file);
                NavigationFormat format = success ? parser.getFormat() : null;
                assertTrue("Cannot read route from " + file + " as " + format, success);
            }
        });
    }
}
