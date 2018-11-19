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

import junit.framework.AssertionFailedError;
import org.junit.Test;
import slash.navigation.babel.*;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.lmx.NokiaLandmarkExchangeFormat;
import slash.navigation.mm.MagicMapsPthFormat;
import slash.navigation.nmn.NavigatingPoiWarnerFormat;
import slash.navigation.ovl.OvlFormat;

import static slash.navigation.base.ConvertBase.convertRoundtrip;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.NavigationTestCase.assertException;

public class ConvertFailsIT {

    @Test
    public void testConvertXcsvToGpiFails() {
        // Garmin file contains 400 instead of expected 216 positions
        assertException(AssertionFailedError.class, new NavigationTestCaseThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.xcsv", new GarminPoiDbFormat(), new GarminPoiFormat());
            }
        });
    }

    @Test
    public void testConvertNokiaLandmarkExchangeUtf8ToNavigatingPoiWarnerIso88591Fails() {
        // source is UTF8 while target is only ISO8859-1
        assertException(AssertionFailedError.class, new NavigationTestCaseThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.lmx", new NokiaLandmarkExchangeFormat(), new NavigatingPoiWarnerFormat());
            }
        });
    }

    @Test
    public void testConvertTourExchangeToGarminMapSource5Fails() {
        // Mapsource file contains only 1 instead of expected 49 positions
        assertException(AssertionError.class, new NavigationTestCaseThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new GarminMapSource5Format());
            }
        });
    }

    @Test
    public void testConvertGarminMapSource5Fails() {
        // Mapsource file contains only 1 instead of expected 46 positions
        assertException(AssertionFailedError.class, new NavigationTestCaseThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.mps", new GarminMapSource5Format(), new GarminMapSource5Format());
                convertRoundtrip(TEST_PATH + "large.mps", new GarminMapSource5Format(), new GarminMapSource5Format());
                convertRoundtrip(TEST_PATH + "from3.gdb", new GarminMapSource6Format(), new GarminMapSource5Format());
                convertRoundtrip(TEST_PATH + "large.gdb", new GarminMapSource6Format(), new GarminMapSource5Format());
            }
        });
    }

    @Test
    public void testConvertGpx11ToGarminMapSource5Fails() {
        // Mapsource file contains only 1 instead of expected 46 positions
        assertException(AssertionError.class, new NavigationTestCaseThrowsException() {
            public void run() throws Exception {
        convertRoundtrip(TEST_PATH + "from11.gpx", new Gpx11Format(), new GarminMapSource5Format());
        convertRoundtrip(TEST_PATH + "from11trk.gpx", new Gpx11Format(), new GarminMapSource5Format());
            }
        });
    }

    @Test
    public void testConvertOziExplorerTrackToTop50Fails() {
        // differences in conversion: Target longitude 0 does not exist
        assertException(AssertionFailedError.class, new NavigationTestCaseThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerTrackFormat(), new OvlFormat());
            }
        });
    }

    @Test
    public void testConvertOziExplorerToMagicMapsPthFails() {
        // differences in conversion: 2.6141469644200224 is not within +5.0E-6 of -17.954639 to -17.954728773195
        assertException(AssertionFailedError.class, new NavigationTestCaseThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from-ozi.rte", new OziExplorerRouteFormat(), new MagicMapsPthFormat());
                convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerTrackFormat(), new MagicMapsPthFormat());
                convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerWaypointFormat(), new MagicMapsPthFormat());
            }
        });
    }

    @Test
    public void testConvertGpx10ToOziExplorerRoute() {
        // differences in conversion: Longitude 0 does not match expected:<-10.76617> but was:<-53.69928>
        assertException(AssertionFailedError.class, new NavigationTestCaseThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new OziExplorerRouteFormat());
            }
        });
    }

    @Test
    public void testConvertMagellanMapSendToMagellanMapSendFails() {
        // roundtrip fails since name and description are mangled
        assertException(AssertionFailedError.class, new NavigationTestCaseThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from-mapsend.wpt", new MagellanMapSendFormat(), new MagellanMapSendFormat());
            }
        });
    }

    @Test
    public void testConvertAlanWaypointsAndRoutesToGarminMapSource5() {
        // fails since the Garmin Mapsource seems to capture only tracks correctly
        // in routes positions with the same name have the same coordinates
        // in waypoint lists positions with the same coordinates are eliminated
        // Garmin file contains only 37 instead of expected 46 positions
        assertException(AssertionError.class, new NavigationTestCaseThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.wpr", new AlanWaypointsAndRoutesFormat(), new GarminMapSource5Format());
            }
        });
    }
}