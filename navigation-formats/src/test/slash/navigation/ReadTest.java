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

import slash.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ReadTest extends NavigationTestCase {
    NavigationFileParser parser = new NavigationFileParser();

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
            public void test(File file) throws IOException {
                assertTrue("Cannot read route from " + file, parser.read(file));
                assertNotNull(parser.getFormat());
                assertNotNull("Cannot get route from " + file, parser.getTheRoute());
                assertNotNull(parser.getAllRoutes());
                assertTrue(parser.getAllRoutes().size() > 0);
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

    public void testAscFilesAreValid() throws IOException {
        readFiles(".asc");
    }

    public void testMicrosoftAutoRouteFilesAreValid() throws IOException {
        readFiles(".axe");
    }

    public void testMicrosoftMapPointFilesAreValid() throws IOException {
        readFiles(".ptm");
    }

    public void testBcrFilesAreValid() throws IOException {
        readFiles(".bcr");
    }

    public void testCsvFilesAreValid() throws IOException {
        // Columbus V900
        readFiles(".csv");
    }

    public void testGarminPoiFilesAreValid() throws IOException {
        readFiles(".gpi");
    }

    public void testGarminPoiDbFilesAreValid() throws IOException {
        readFiles(".xcsv");
    }

    public void testGarminMapSource6FilesAreValid() throws IOException {
        readFiles(".gdb");
    }

    public void testGeoCachingFilesAreValid() throws IOException {
        readFiles(".loc");
    }

    public void testGpxFilesAreValid() throws IOException {
        readFiles(".gpx");
    }

    public void testHoluxM241BinaryFilesAreValid() throws IOException {
        readFiles(".bin");
    }

    public void testTomTomRouteFilesAreValid() throws IOException {
        readFiles(".itn");
    }

    public void testKmlFilesAreValid() throws IOException {
        readFiles(".kml");
    }

    public void testKmzFilesAreValid() throws IOException {
        readFiles(".kmz");
    }

    public void testKlickTelRouteFilesAreValid() throws IOException {
        readFiles(".krt");
    }

    public void testMagellanExploristFilesAreValid() throws IOException {
        readFiles(".log");
    }

    public void testMagicMapsIktFilesAreValid() throws IOException {
        readFiles(".ikt");
    }

    public void testNokiaLandmarkExchangeFilesAreValid() throws IOException {
        readFiles(".lmx");
    }

    public void testNmeaFilesAreValid() throws IOException {
        readFiles(".nmea");
        readFiles(".pgl");
    }

    public void testNmn7FilesAreValid() throws IOException {
        readFiles(".freshroute");
    }

    public void testMagellanMapSendFilesAreValid() throws Exception {
        Thread.sleep(5000); // this seems to help against the errors that only show up on complete runs
        readFiles(".mps");
    }

    public void testOv2FilesAreValid() throws IOException {
        readFiles(".ov2");
    }

    public void testOvlFilesAreValid() throws IOException {
        readFiles(".ovl");
    }

    public void testOziExplorerTrackFilesAreValid() throws IOException {
        readFiles(".plt");
    }

    public void testMagicMapsPthFilesAreValid() throws IOException {
        readFiles(".pth");
    }

    public void testRteFilesAreValid() throws IOException {
        // Magellan Route
        // Navigon Mobile Navigator
        // OziExplorerRoute
        readFiles(".rte");
    }

    public void testTourExchangeFilesAreValid() throws IOException {
        readFiles(".tef");
    }

    public void testTrainingCenterDatabaseFilesAreValid() throws IOException {
        readFiles(".crs");
        readFiles(".hst");
        readFiles(".tcx");
    }

    public void testTourFilesAreValid() throws IOException {
        readFiles(".tour");
    }

    public void testTrkFilesAreValid() throws IOException {
        readFiles(".trk");
    }

    public void testTkFilesAreValid() throws IOException {
        // Glopus
        // Kompass
        readFiles(".tk");
    }

    public void testNationalGeographicTopo3FilesAreValid() throws IOException {
        readFiles(".tpo");
    }

    public void testTrpFilesAreValid() throws IOException {
        readFiles(".trp");
    }

    public void testTextFilesAreValid() throws IOException {
        // KienzleGps
        // MagicMaps2Go
        // Sygic
        readFiles(".txt");
    }

    public void testAlanTracklogFilesAreValid() throws IOException {
        readFiles(".trl");
    }

    public void testUrlFilesAreValid() throws IOException {
        readFiles(".url");
    }

    public void testWintecWbt201FilesAreValid() throws IOException {
        readFiles(".tk1");
        readFiles(".tk2");
    }

    public void testWprFilesAreValid() throws IOException {
        readFiles(".wpr");
    }

    public void testOziExplorerWaypointFilesAreValid() throws IOException {
        readFiles(".wpt");
    }

    public void testXmlFilesAreValid() throws IOException {
        readFiles(".xml");
    }

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

    public void testDontReadUnrecognizedFiles() throws IOException {
        dontReadFiles(UNRECOGNIZED_PATH, new TestFileCallback() {
            public void test(File file) throws IOException {
                assertFalse("Can read route from " + file, parser.read(file));
            }
        });
    }

    public void testReadFalseDetectsFiles() throws IOException {
        dontReadFiles(FALSE_DETECTS_PATH, new TestFileCallback() {
            public void test(File file) throws IOException {
                assertTrue("Cannot read route from " + file, parser.read(file));
            }
        });
    }
}
