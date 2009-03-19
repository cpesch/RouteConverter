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

import slash.navigation.util.Conversion;

import java.io.IOException;

public class SplitTest extends ReadWriteBase {

    public void testSplitBcrReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large.bcr");
    }

    public void testSplitTomTomRouteReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large.itn", false, false);
        splitReadWriteRoundtrip(TEST_PATH + "large.itn", false, true);
    }

    public void testSplitGarminMapSource6ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large.gdb");
    }

    public void testSplitGpx10ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large10.gpx");
    }

    public void testSplitGpx11ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large11.gpx");
    }

    public void testSplitKml20ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large20.kml");
    }

    public void testSplitKml21ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large21.kml");
    }

    public void testSplitGarminMapSource5ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large.mps");
    }

    public void testSplitNmn4ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large-nmn4.rte");
    }

    public void testSplitNmn6ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large-nmn6.rte", false, false);
        splitReadWriteRoundtrip(TEST_PATH + "large-nmn6.rte", false, true);
        splitReadWriteRoundtrip(TEST_PATH + "large-nmn6.rte", true, false);
        splitReadWriteRoundtrip(TEST_PATH + "large-nmn6.rte", true, true);
    }

    public void testSplitGarminPcx5ReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "large-pcx5.wpt");
    }

    public void testSplitGpsTunerTrkReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "from-gpstuner.trk");
    }

    public void testSplitAlanWaypointsAndRoutesReadWriteRoundtrip() throws IOException {
        splitReadWriteRoundtrip(TEST_PATH + "from.wpr");
    }

    public void testCeiling() {
        assertEquals(3, Conversion.ceiling(184, 90, true));
        assertEquals(1, Conversion.ceiling(0, 1, true));
    }
}
