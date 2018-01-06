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

import java.io.IOException;

import static slash.navigation.base.NavigationTestCase.SAMPLE_PATH;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.ReadWriteBase.readWriteRoundtrip;

public class ReadWriteRoundtripIT {

    @Test
    public void testAlanTrackLogWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.trl");
    }

    @Test
    public void testAlanWaypointsAndRoutesWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.wpr");
    }

    @Test
    public void testColumbusGpsReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-columbusv900-standard.csv");
        readWriteRoundtrip(TEST_PATH + "from-columbusv900-professional.csv");
    }

    @Test
    public void testCompeGPSReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-compegps.trk");
        readWriteRoundtrip(TEST_PATH + "from-compegps.wpt");
    }

    @Test
    public void testCoPilotReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-copilot6.trp");
        readWriteRoundtrip(TEST_PATH + "from-copilot7.trp");
        readWriteRoundtrip(TEST_PATH + "from-copilot8.trp");
    }

    @Test
    public void testFlightRecorderDataReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.igc");
    }

    @Test
    public void testGarminFlightPlanReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.fpl");
    }

    @Test
    public void testGarminMapSource5ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.mps");
        readWriteRoundtrip(TEST_PATH + "from10.mps");
        readWriteRoundtrip(TEST_PATH + "from10trk.mps");
    }

    @Test
    public void testGarminMapSource6ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from2.gdb");
        readWriteRoundtrip(TEST_PATH + "from3.gdb");
        readWriteRoundtrip(TEST_PATH + "from10.gdb");
        readWriteRoundtrip(TEST_PATH + "from10trk.gdb");
    }

    @Test
    public void testGarminPcx5ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-pcx5.wpt");
    }

    @Test
    public void testGarminPoiReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.gpi");
    }

    @Test
    public void testGeocachingDotComReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.loc");
    }

    @Test
    public void testGlopusReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-glopus.tk");
    }

    @Test
    public void testGoPalRouteReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-gopal3.xml");
        readWriteRoundtrip(TEST_PATH + "from-gopal5.xml");
    }

    @Test
    public void testGoPalTrackReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-gopal.trk");
    }

    @Test
    public void testGoogleMapsUrlReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-googlemaps.url");
    }

    @Test
    public void testGoRiderGpsReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-goridergps.rt");
    }

    @Test
    public void testGpsTunerReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-gpstuner.trk");
    }

    @Test
    public void testGpx11ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from11.gpx");
        readWriteRoundtrip(TEST_PATH + "from11trk.gpx");
    }

    @Test
    public void testGroundTrackReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-groundtrack.txt");
    }

    @Test
    public void testHaicomLoggerReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-haicomlogger.csv");
    }

    @Test
    public void testiBlue747ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-iblue747.csv");
    }

    @Test
    public void testKlickTelRouteReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.krt");
    }

    @Test
    public void testKmlReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from20.kml");
        readWriteRoundtrip(TEST_PATH + "from21.kml");
        readWriteRoundtrip(TEST_PATH + "from22beta.kml");
        readWriteRoundtrip(TEST_PATH + "from22.kml");
    }

    @Test
    public void testKmzReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from20.kmz");
        readWriteRoundtrip(TEST_PATH + "from21.kmz");
        readWriteRoundtrip(TEST_PATH + "from22beta.kmz");
        readWriteRoundtrip(TEST_PATH + "from22.kmz");
    }

    @Test
    public void testKompassReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-kompass.tk");
    }

    @Test
    public void testMagicMapsIktReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.ikt");
    }

    @Test
    public void testMagicMapsPthReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.pth");
    }

    @Test
    public void testMagicMaps2GoReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-magicmaps2go.txt");
    }

    @Test
    public void testMTP0607ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "large.bcr");
    }

    @Test
    public void testNavigatingPOIWarnerReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-navigating-poiwarner.asc");
    }

    @Test
    public void testNavigonCruiserReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.cruiser");
    }

    @Test
    public void testNmeaReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "fromgga.nmea");
        readWriteRoundtrip(TEST_PATH + "fromrmc.nmea");
        readWriteRoundtrip(TEST_PATH + "fromwpl.nmea");
        readWriteRoundtrip(TEST_PATH + "from.nmea");
    }

    @Test
    public void testNmn4ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-nmn4.rte");
        readWriteRoundtrip(TEST_PATH + "large-nmn4.rte");
    }

    @Test
    public void testNmn5ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-nmn5.rte");
    }

    @Test
    public void testNmn6ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-nmn6.rte");
    }

    @Test
    public void testNmn6FavoritesReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-nmn6favorites.storage");
    }

    @Test
    public void testNmn7ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-nmn7.freshroute");
    }

    @Test
    public void testNmnUrlReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-nmn.txt");
        readWriteRoundtrip(TEST_PATH + "from-nmn-plain.txt");
    }

    @Test
    public void testNokiaLandmarkExhangeReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.lmx");
    }

    @Test
    public void testMagellanExploristReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-magellan.log");
    }

    @Test
    public void testMagellanRouteReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-magellan.rte");
    }

    @Test
    public void testQstarzQ1000ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-qstarz-q1000.csv");
    }

    @Test
    public void testRoute66ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-route66poi.csv");
    }

    @Test
    public void testSygicUnicodeReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-sygic-unicode.txt");
    }

    @Test
    public void testTop50OvlReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.ovl");
    }

    @Test
    public void testTourReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.tour");
    }

    @Test
    public void testTomTomRouteReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.itn");
        readWriteRoundtrip(TEST_PATH + "large.itn");
    }

    @Test
    public void testTomTomTrackReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(SAMPLE_PATH + "tripmaster3.itn");
    }

    @Test
    public void testViaMichelinReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-poi.xvm");
        readWriteRoundtrip(TEST_PATH + "from-itinerary.xvm");
    }

    @Test
    public void testXcsvReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.xcsv");
    }
}
