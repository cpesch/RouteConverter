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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation;

import java.io.IOException;

public class ReadWriteRoundtripTest extends ReadWriteBase {

    public void testAlanTrlWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.trl");
    }

    public void testAlanWprWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.wpr");
    }

    public void testMTP0607ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "large.bcr");
    }

    public void testItnReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.itn");
        readWriteRoundtrip(TEST_PATH + "large.itn");
    }

    public void testKmlReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from20.kml");
        readWriteRoundtrip(TEST_PATH + "from21.kml");
        readWriteRoundtrip(TEST_PATH + "from22.kml");
    }

    public void testTourReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.tour");
    }

    public void testGeocachingDotDomReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.loc");
    }

    public void testGoPalRouteReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-gopal.xml");
    }

    public void testGoPalTrackReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-gopal.trk");
    }

    public void testGpx10ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from10.gpx");
        readWriteRoundtrip(TEST_PATH + "from10trk.gpx");
    }

    public void testGpx11ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from11.gpx");
        readWriteRoundtrip(TEST_PATH + "from11trk.gpx");
    }

    public void testGdbReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.gdb");
        readWriteRoundtrip(TEST_PATH + "from10.gdb");
        readWriteRoundtrip(TEST_PATH + "from10trk.gdb");
    }

    public void testGpiReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.gpi");
    }

    /*
    public void testHaicomLoggerReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-haicomlogger.csv");
    }
    */

    public void testMagellanExploristReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.log");
    }

    public void testNmeaReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "fromgga.nmea");
        readWriteRoundtrip(TEST_PATH + "fromrmc.nmea");
        readWriteRoundtrip(TEST_PATH + "fromwpl.nmea");
        readWriteRoundtrip(TEST_PATH + "from.nmea");
    }

    public void testNmn4ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-nmn4.rte");
        readWriteRoundtrip(TEST_PATH + "large-nmn4.rte");
    }

    public void testNmn5ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-nmn5.rte");
    }

    public void testNmn6ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-nmn6.rte");
    }

    public void testNavigatingPOIWarnerReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-navigating-poiwarner.asc");
    }

    public void testNmn6FavoritesReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-nmn6favorites.storage");
    }

    public void testMpsReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.mps");
        readWriteRoundtrip(TEST_PATH + "from10.mps");
        readWriteRoundtrip(TEST_PATH + "from10trk.mps");
    }

    public void testTop50OvlReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.ovl");
    }

    public void testMagicMapsIktReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.ikt");
    }

    public void testMagicMapsPthReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.pth");
    }

    public void testRoute66ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-route66poi.csv");
    }

    public void testCoPilotReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from6.trp");
        readWriteRoundtrip(TEST_PATH + "from7.trp");
    }

    public void testTkReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-glopus.tk");
    }

    public void testTrkReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-gpstuner.trk");
    }

    public void testPcx5ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-pcx5.wpt");
    }

    public void testViaMichelinReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-poi.xvm");
        readWriteRoundtrip(TEST_PATH + "from-itinerary.xvm");
    }

    public void testXcsvReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.xcsv");
    }
}
