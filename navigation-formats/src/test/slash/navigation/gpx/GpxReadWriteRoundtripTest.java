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

package slash.navigation.gpx;

import slash.navigation.NavigationFileParser;
import slash.navigation.ReadWriteBase;
import slash.navigation.RouteCharacteristics;
import slash.navigation.gpx.binding10.Gpx;

import java.io.IOException;
import java.math.BigInteger;

public class GpxReadWriteRoundtripTest extends ReadWriteBase {

    private void checkUnprocessed(Gpx gpx) {
        assertNotNull(gpx);
        assertEquals("Name", gpx.getName());
        assertEquals("Description", gpx.getDesc());
        assertEquals("Author", gpx.getAuthor());
        assertEquals("EMail@EMail.Email", gpx.getEmail());
        assertEquals("URL", gpx.getUrl());
        assertEquals("URLName", gpx.getUrlname());
    }

    private void checkUnprocessed(Gpx.Rte rte) {
        assertNotNull(rte);
        assertEquals("Route1 Name", rte.getName());
        assertEquals("Comment", rte.getCmt());
        assertEquals("Description", rte.getDesc());
        assertEquals("Source", rte.getSrc());
        assertEquals("URL", rte.getUrl());
        assertEquals("URLName", rte.getUrlname());
        assertEquals(new BigInteger("1"), rte.getNumber());
    }

    private void checkUnprocessed(Gpx.Trk trk) {
        assertNotNull(trk);
        assertEquals("Track1 Name", trk.getName());
        assertEquals("Comment", trk.getCmt());
        assertEquals("Description", trk.getDesc());
        assertEquals("Source", trk.getSrc());
        assertEquals("URL", trk.getUrl());
        assertEquals("URLName", trk.getUrlname());
        assertEquals(new BigInteger("1"), trk.getNumber());
    }

    private void checkUnprocessed(Gpx.Wpt wpt) {
        assertNotNull(wpt);
        assertEquals("Waypoint1 Name", wpt.getName());
        assertEquals("Comment", wpt.getCmt());
        assertEquals("Description", wpt.getDesc());
        assertEquals("Source", wpt.getSrc());
        assertEquals("URL", wpt.getUrl());
        assertEquals("URLName", wpt.getUrlname());
    }

    private void checkUnprocessed(Gpx.Rte.Rtept rtept) {
        assertNotNull(rtept);
        if (rtept.getName().endsWith(rtept.getDesc()))
            assertEquals("Route Point1 Name; Description", rtept.getName());
        else
            assertEquals("Route Point1 Name", rtept.getName());
        assertEquals("Comment", rtept.getCmt());
        assertEquals("Description", rtept.getDesc());
        assertEquals("Source", rtept.getSrc());
        assertEquals("URL", rtept.getUrl());
        assertEquals("URLName", rtept.getUrlname());
    }

    private void checkUnprocessed(Gpx.Trk.Trkseg.Trkpt trkpt) {
        assertNotNull(trkpt);
        if (trkpt.getName().endsWith(trkpt.getDesc()))
            assertEquals("Track Point1 Name; Description", trkpt.getName());
        else
            assertEquals("Track Point1 Name", trkpt.getName());
        assertEquals("Comment", trkpt.getCmt());
        assertEquals("Description", trkpt.getDesc());
        assertEquals("Source", trkpt.getSrc());
        assertEquals("URL", trkpt.getUrl());
        assertEquals("URLName", trkpt.getUrlname());
    }

    public void testGpx10ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from10.gpx", new NavigationFileParserCallback() {
            public void test(NavigationFileParser source, NavigationFileParser target) {
                GpxRoute sourceWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertEquals(RouteCharacteristics.Waypoints, sourceWaypoints.getCharacteristics());
                assertNotNull(sourceWaypoints.getOrigins());
                assertEquals(1, sourceWaypoints.getOrigins().size());
                checkUnprocessed(sourceWaypoints.getOrigin(Gpx.class));
                GpxPosition sourceWaypoint = sourceWaypoints.getPosition(0);
                assertNotNull(sourceWaypoint.getOrigin());
                checkUnprocessed(sourceWaypoint.getOrigin(Gpx.Wpt.class));

                GpxRoute sourceRoute = (GpxRoute) source.getAllRoutes().get(1);
                assertEquals(RouteCharacteristics.Route, sourceRoute.getCharacteristics());
                assertNotNull(sourceRoute.getOrigins());
                assertEquals(2, sourceRoute.getOrigins().size());
                checkUnprocessed(sourceRoute.getOrigin(Gpx.class));
                checkUnprocessed(sourceRoute.getOrigin(Gpx.Rte.class));
                GpxPosition sourceRoutePoint = sourceRoute.getPosition(0);
                assertNotNull(sourceRoutePoint.getOrigin());
                checkUnprocessed(sourceRoutePoint.getOrigin(Gpx.Rte.Rtept.class));

                GpxRoute targetWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertEquals(RouteCharacteristics.Waypoints, targetWaypoints.getCharacteristics());
                assertNotNull(targetWaypoints.getOrigins());
                assertEquals(1, targetWaypoints.getOrigins().size());
                checkUnprocessed(targetWaypoints.getOrigin(Gpx.class));
                GpxPosition targetWaypoint = targetWaypoints.getPosition(0);
                assertNotNull(targetWaypoint.getOrigin());
                checkUnprocessed(targetWaypoint.getOrigin(Gpx.Wpt.class));

                GpxRoute targetRoute = (GpxRoute) target.getAllRoutes().get(1);
                assertEquals(RouteCharacteristics.Route, targetRoute.getCharacteristics());
                assertNotNull(targetRoute.getOrigins());
                assertEquals(2, targetRoute.getOrigins().size());
                checkUnprocessed(targetRoute.getOrigin(Gpx.class));
                checkUnprocessed(targetRoute.getOrigin(Gpx.Rte.class));
                GpxPosition targetRoutePoint = targetRoute.getPosition(0);
                assertNotNull(targetRoutePoint.getOrigin());
                checkUnprocessed(targetRoutePoint.getOrigin(Gpx.Rte.Rtept.class));
            }
        });
    }

    public void testGpx10TrkReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from10trk.gpx", new NavigationFileParserCallback() {
            public void test(NavigationFileParser source, NavigationFileParser target) {
                GpxRoute sourceWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertNotNull(sourceWaypoints.getOrigins());
                assertEquals(1, sourceWaypoints.getOrigins().size());
                checkUnprocessed(sourceWaypoints.getOrigin(Gpx.class));
                GpxPosition sourceWaypoint = sourceWaypoints.getPosition(0);
                assertNotNull(sourceWaypoint.getOrigin());
                checkUnprocessed(sourceWaypoint.getOrigin(Gpx.Wpt.class));

                GpxRoute sourceTrack = (GpxRoute) source.getAllRoutes().get(1);
                assertNotNull(sourceTrack.getOrigins());
                assertEquals(2, sourceTrack.getOrigins().size());
                checkUnprocessed(sourceTrack.getOrigin(Gpx.class));
                checkUnprocessed(sourceTrack.getOrigin(Gpx.Trk.class));
                GpxPosition sourceTrackPoint = sourceTrack.getPosition(0);
                assertNotNull(sourceTrackPoint.getOrigin());
                checkUnprocessed(sourceTrackPoint.getOrigin(Gpx.Trk.Trkseg.Trkpt.class));

                GpxRoute targetWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertNotNull(targetWaypoints.getOrigins());
                assertEquals(1, targetWaypoints.getOrigins().size());
                checkUnprocessed(targetWaypoints.getOrigin(Gpx.class));
                GpxPosition targetWaypoint = targetWaypoints.getPosition(0);
                assertNotNull(targetWaypoint.getOrigin());
                checkUnprocessed(targetWaypoint.getOrigin(Gpx.Wpt.class));

                GpxRoute targetTrack = (GpxRoute) target.getAllRoutes().get(1);
                assertNotNull(targetTrack.getOrigins());
                assertEquals(2, targetTrack.getOrigins().size());
                checkUnprocessed(targetTrack.getOrigin(Gpx.class));
                checkUnprocessed(targetTrack.getOrigin(Gpx.Trk.class));
                GpxPosition targetTrackPoint = targetTrack.getPosition(0);
                assertNotNull(targetTrackPoint.getOrigin());
                checkUnprocessed(targetTrackPoint.getOrigin(Gpx.Trk.Trkseg.Trkpt.class));
            }
        });
    }

    public void testGpx11ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from11.gpx", new NavigationFileParserCallback() {
            public void test(NavigationFileParser source, NavigationFileParser target) {
                GpxRoute sourceRoute = (GpxRoute) source.getAllRoutes().get(0);
                // assertNotNull(sourceRoute.getOriginalData());
                // TODO
                GpxRoute targetRoute = (GpxRoute) target.getAllRoutes().get(0);
                // TODO
                assertTrue(false);
            }
        });
    }
}
