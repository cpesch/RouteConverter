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
import slash.navigation.babel.OziExplorerRouteFormat;
import slash.navigation.babel.OziExplorerTrackFormat;
import slash.navigation.babel.OziExplorerWaypointFormat;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.nmn.NavigatingPoiWarnerFormat;
import slash.navigation.ovl.OvlFormat;

import java.io.IOException;

import static slash.navigation.base.ConvertBase.convertRoundtrip;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

public class OziExplorerConvertIT {

    @Test
    public void testConvertOziExplorerRouteToOziExplorerRoute() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.rte", new OziExplorerRouteFormat(), new OziExplorerRouteFormat());
    }

    @Test
    public void testConvertOziExplorerTrackToOziExplorerTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.plt", new OziExplorerTrackFormat(), new OziExplorerTrackFormat());
    }

    @Test
    public void testConvertOziExplorerWaypointToOziExplorerWaypoint() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerWaypointFormat(), new OziExplorerWaypointFormat());
    }

    @Test
    public void testConvertOziExplorerRouteToTop50() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.rte", new OziExplorerRouteFormat(), new OvlFormat());
    }

    @Test
    public void testConvertOziExplorerWaypointToNavigatingPoiWarner() throws IOException {
        convertRoundtrip(TEST_PATH + "from-ozi.wpt", new OziExplorerWaypointFormat(), new NavigatingPoiWarnerFormat());
    }

    @Test
    public void testConvertGpx10ToOziExplorerTrack() throws IOException {
        convertRoundtrip(TEST_PATH + "from10.gpx", new Gpx10Format(), new OziExplorerTrackFormat());
    }

    @Test
    public void testConvertGpx10ToOziExplorerWaypoints() throws IOException {
        convertRoundtrip(TEST_PATH + "from10trk.gpx", new Gpx10Format(), new OziExplorerWaypointFormat());
    }
}
