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

import java.io.IOException;

public class ReadWriteRoundtripIT extends ReadWriteBase {

    public void testAlanTrackLogWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.trl");
    }

    public void testAlanWaypointsAndRoutesWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.wpr");
    }

    public void testColumbusV900ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-columbusv900-standard.csv");
        readWriteRoundtrip(TEST_PATH + "from-columbusv900-professional.csv");
    }

    public void testCoPilotReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from6.trp");
        readWriteRoundtrip(TEST_PATH + "from7.trp");
    }

    public void testGeocachingDotComReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.loc");
    }

    public void testGlopusReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-glopus.tk");
    }

    public void testGoPalRouteReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-gopal.xml");
    }

    public void testGoPalTrackReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-gopal.trk");
    }

    public void testGpsTunerReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-gpstuner.trk");
    }

    public void testGpx11ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from11.gpx");
        readWriteRoundtrip(TEST_PATH + "from11trk.gpx");
    }

    public void testGarminMapSource5ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.mps");
        readWriteRoundtrip(TEST_PATH + "from10.mps");
        readWriteRoundtrip(TEST_PATH + "from10trk.mps");
    }

    public void testGarminMapSource6ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.gdb");
        readWriteRoundtrip(TEST_PATH + "from10.gdb");
        readWriteRoundtrip(TEST_PATH + "from10trk.gdb");
    }

    public void testGarminPcx5ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-pcx5.wpt");
    }

    public void testGarminPoiReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.gpi");
    }

    public void testHaicomLoggerReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-haicomlogger.csv");
    }

    public void testKlickTelRouteReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.krt");
    }

    public void testKmlReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from20.kml");
        readWriteRoundtrip(TEST_PATH + "from21.kml");
        readWriteRoundtrip(TEST_PATH + "from22beta.kml");
        readWriteRoundtrip(TEST_PATH + "from22.kml");
    }

    public void testKmzReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from20.kmz");
        readWriteRoundtrip(TEST_PATH + "from21.kmz");
        readWriteRoundtrip(TEST_PATH + "from22beta.kmz");
        readWriteRoundtrip(TEST_PATH + "from22.kmz");
    }

    public void testKompassReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-kompass.tk");
    }

    public void testMagicMapsIktReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.ikt");
    }

    public void testMagicMapsPthReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.pth");
    }

    public void testMagicMaps2GoReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-magicmaps2go.txt");
    }

    public void testMTP0607ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "large.bcr");
    }

    public void testNavigatingPOIWarnerReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-navigating-poiwarner.asc");
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

    public void testNmn6FavoritesReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-nmn6favorites.storage");
    }

    public void testNmn7ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-nmn7.freshroute");
    }

    public void testNokiaLandmarkExhangeReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.lmx");
    }

    public void testMagellanExploristReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-magellan.log");
    }

    public void testMagellanRouteReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-magellan.rte");
    }

    public void testRoute66ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-route66poi.csv");
    }

    public void testSygicUnicodeReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-sygic-unicode.txt");
    }

    public void testTop50OvlReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.ovl");
    }

    public void testTourReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.tour");
    }

    public void testTomTomRouteReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.itn");
        readWriteRoundtrip(TEST_PATH + "large.itn");
    }

    public void testTomTomTrackReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(SAMPLE_PATH + "tripmaster3.itn");
    }

    public void testViaMichelinReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-poi.xvm");
        readWriteRoundtrip(TEST_PATH + "from-itinerary.xvm");
    }

    public void testXcsvReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.xcsv");
    }
}
