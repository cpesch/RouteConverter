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

import slash.navigation.babel.AlanWaypointsAndRoutesFormat;
import slash.navigation.babel.GarminMapSource5Format;
import slash.navigation.babel.GarminPoiDbFormat;
import slash.navigation.babel.GarminPoiFormat;
import slash.navigation.babel.MagellanMapSendFormat;
import slash.navigation.babel.MicrosoftAutoRouteFormat;
import slash.navigation.babel.OziExplorerRouteFormat;
import slash.navigation.babel.OziExplorerTrackFormat;
import slash.navigation.babel.OziExplorerWaypointFormat;
import slash.navigation.babel.TourExchangeFormat;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.mm.MagicMapsIktFormat;
import slash.navigation.mm.MagicMapsPthFormat;
import slash.navigation.ovl.OvlFormat;

import java.io.IOException;

public class ConvertFailsIT extends ConvertBase {

    public void testConvertXcsvToGpiFails() throws IOException {
        // Garmin file contains 400 instead of expected 216 positions
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.xcsv", new GarminPoiDbFormat(), new GarminPoiFormat());
            }
        });
    }

    public void testConvertMicrosoftAutoRouteToGarminMapSource5() throws IOException {
        // Garmin file contains only 41 instead of expected 45 positions
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.axe", new MicrosoftAutoRouteFormat(), new GarminMapSource5Format());
            }
        });
    }

    public void testConvertTourExchangeToGarminMapSource5Fails() throws IOException {
        // Garmin file contains only 47 instead of expected 49 positions
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.tef", new TourExchangeFormat(), new GarminMapSource5Format());
            }
        });
    }

    public void testConvertOziExplorerTrackToTop50() throws IOException {
        // differences in conversion: Target longitude 0 does not exist
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerTrackFormat(), new OvlFormat());
            }
        });
    }

    public void testConvertOziExplorerWaypointToTop50() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerWaypointFormat(), new OvlFormat());
    }

    public void testConvertOziExplorerToMagicMaps() throws IOException {
        // differences in conversion: 2.6141469644200224 is not within +5.0E-6 of -17.954639 to -17.954728773195
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from-ozi.rte", new OziExplorerRouteFormat(), new MagicMapsIktFormat());
                convertRoundtrip(TEST_PATH + "from-ozi.rte", new OziExplorerRouteFormat(), new MagicMapsPthFormat());
                convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerTrackFormat(), new MagicMapsIktFormat());
                convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerTrackFormat(), new MagicMapsPthFormat());
                convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerWaypointFormat(), new MagicMapsIktFormat());
                convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerWaypointFormat(), new MagicMapsPthFormat());
            }
        });
    }

    public void testConvertGpx10ToOziExplorerRoute() throws IOException {
        // differences in conversion: Longitude 0 does not match expected:<-10.76617> but was:<-53.69928>
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new OziExplorerRouteFormat());
            }
        });
    }

    public void testConvertMagellanMapSendToMagellanMapSendFails() throws IOException {
        // roundtrip fails since name and description are mangled
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from-mapsend.wpt", new MagellanMapSendFormat(), new MagellanMapSendFormat());
            }
        });
    }

    public void testConvertAlanWaypointsAndRoutesToGarminMapSource5() throws IOException {
        // fails since the Garmin Mapsource seems to capture only tracks correctly
        // in routes positions with the same name have the same coordinates
        // in waypoint lists positions with the same coordinates are eliminated
        // Garmin file contains only 37 instead of expected 46 positions
        assertTestFails(new ThrowsException() {
            public void run() throws Exception {
                convertRoundtrip(TEST_PATH + "from.wpr", new AlanWaypointsAndRoutesFormat(), new GarminMapSource5Format());
            }
        });
    }
}