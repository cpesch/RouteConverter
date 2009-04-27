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
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.binding11.WptType;
import slash.navigation.gpx.binding11.RteType;
import slash.navigation.gpx.binding11.TrkType;

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

    private void checkUnprocessed(GpxType gpxType) {
        assertNotNull(gpxType);
        assertEquals("Name", gpxType.getMetadata().getName());
        assertEquals("Description", gpxType.getMetadata().getDesc());
        assertEquals("Author", gpxType.getMetadata().getAuthor().getName());
        assertEquals("http://author", gpxType.getMetadata().getAuthor().getLink().getHref());
        assertEquals("Id", gpxType.getMetadata().getAuthor().getEmail().getId());
        assertEquals("Domain", gpxType.getMetadata().getAuthor().getEmail().getDomain());
        assertEquals("http://metadata", gpxType.getMetadata().getLink().get(0).getHref());
        assertEquals("Keywords", gpxType.getMetadata().getKeywords());
    }

    private void checkUnprocessed(RteType rteType) {
        assertNotNull(rteType);
        assertEquals("Route1 Name", rteType.getName());
        assertEquals("Comment", rteType.getCmt());
        assertEquals("Description", rteType.getDesc());
        assertEquals("Source", rteType.getSrc());
        assertEquals("http://rte", rteType.getLink().get(0).getHref());
        assertEquals(new BigInteger("1"), rteType.getNumber());
    }

    private void checkUnprocessed(TrkType trkType) {
        assertNotNull(trkType);
        assertEquals("Track1 Name", trkType.getName());
        assertEquals("Comment", trkType.getCmt());
        assertEquals("Description", trkType.getDesc());
        assertEquals("Source", trkType.getSrc());
        assertEquals("http://trk", trkType.getLink().get(0).getHref());
        assertEquals(new BigInteger("1"), trkType.getNumber());
    }

    private void checkUnprocessed(WptType wptType) {
        assertNotNull(wptType);
        if (wptType.getName().endsWith(wptType.getDesc()))
            assertEquals("Waypoint1 Name; Description", wptType.getName());
        else
            assertEquals("Waypoint1 Name", wptType.getName());
        assertEquals("Comment", wptType.getCmt());
        assertEquals("Description", wptType.getDesc());
        assertEquals("Source", wptType.getSrc());
        assertEquals("http://wpt", wptType.getLink().get(0).getHref());
    }

    public void testGpx11ReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from11.gpx", new NavigationFileParserCallback() {
            public void test(NavigationFileParser source, NavigationFileParser target) {
                GpxRoute sourceWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertEquals(RouteCharacteristics.Waypoints, sourceWaypoints.getCharacteristics());
                assertNotNull(sourceWaypoints.getOrigins());
                assertEquals(1, sourceWaypoints.getOrigins().size());
                checkUnprocessed(sourceWaypoints.getOrigin(GpxType.class));
                GpxPosition sourceWaypoint = sourceWaypoints.getPosition(0);
                assertNotNull(sourceWaypoint.getOrigin());
                checkUnprocessed(sourceWaypoint.getOrigin(WptType.class));

                GpxRoute sourceRoute = (GpxRoute) source.getAllRoutes().get(1);
                assertEquals(RouteCharacteristics.Route, sourceRoute.getCharacteristics());
                assertNotNull(sourceRoute.getOrigins());
                assertEquals(2, sourceRoute.getOrigins().size());
                checkUnprocessed(sourceRoute.getOrigin(GpxType.class));
                checkUnprocessed(sourceRoute.getOrigin(RteType.class));
                GpxPosition sourceRoutePoint = sourceRoute.getPosition(0);
                assertNotNull(sourceRoutePoint.getOrigin());
                checkUnprocessed(sourceRoutePoint.getOrigin(WptType.class));

                GpxRoute targetWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertEquals(RouteCharacteristics.Waypoints, targetWaypoints.getCharacteristics());
                assertNotNull(targetWaypoints.getOrigins());
                assertEquals(1, targetWaypoints.getOrigins().size());
                checkUnprocessed(targetWaypoints.getOrigin(GpxType.class));
                GpxPosition targetWaypoint = targetWaypoints.getPosition(0);
                assertNotNull(targetWaypoint.getOrigin());
                checkUnprocessed(targetWaypoint.getOrigin(WptType.class));

                GpxRoute targetRoute = (GpxRoute) target.getAllRoutes().get(1);
                assertEquals(RouteCharacteristics.Route, targetRoute.getCharacteristics());
                assertNotNull(targetRoute.getOrigins());
                assertEquals(2, targetRoute.getOrigins().size());
                checkUnprocessed(targetRoute.getOrigin(GpxType.class));
                checkUnprocessed(targetRoute.getOrigin(RteType.class));
                GpxPosition targetRoutePoint = targetRoute.getPosition(0);
                assertNotNull(targetRoutePoint.getOrigin());
                checkUnprocessed(targetRoutePoint.getOrigin(WptType.class));
            }
        });
    }

    public void testGpx11TrkReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from11trk.gpx", new NavigationFileParserCallback() {
            public void test(NavigationFileParser source, NavigationFileParser target) {
                GpxRoute sourceWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertNotNull(sourceWaypoints.getOrigins());
                assertEquals(1, sourceWaypoints.getOrigins().size());
                checkUnprocessed(sourceWaypoints.getOrigin(GpxType.class));
                GpxPosition sourceWaypoint = sourceWaypoints.getPosition(0);
                assertNotNull(sourceWaypoint.getOrigin());
                checkUnprocessed(sourceWaypoint.getOrigin(WptType.class));

                GpxRoute sourceTrack = (GpxRoute) source.getAllRoutes().get(1);
                assertNotNull(sourceTrack.getOrigins());
                assertEquals(2, sourceTrack.getOrigins().size());
                checkUnprocessed(sourceTrack.getOrigin(GpxType.class));
                checkUnprocessed(sourceTrack.getOrigin(TrkType.class));
                GpxPosition sourceTrackPoint = sourceTrack.getPosition(0);
                assertNotNull(sourceTrackPoint.getOrigin());
                checkUnprocessed(sourceTrackPoint.getOrigin(WptType.class));

                GpxRoute targetWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertNotNull(targetWaypoints.getOrigins());
                assertEquals(1, targetWaypoints.getOrigins().size());
                checkUnprocessed(targetWaypoints.getOrigin(GpxType.class));
                GpxPosition targetWaypoint = targetWaypoints.getPosition(0);
                assertNotNull(targetWaypoint.getOrigin());
                checkUnprocessed(targetWaypoint.getOrigin(WptType.class));

                GpxRoute targetTrack = (GpxRoute) target.getAllRoutes().get(1);
                assertNotNull(targetTrack.getOrigins());
                assertEquals(2, targetTrack.getOrigins().size());
                checkUnprocessed(targetTrack.getOrigin(GpxType.class));
                checkUnprocessed(targetTrack.getOrigin(TrkType.class));
                GpxPosition targetTrackPoint = targetTrack.getPosition(0);
                assertNotNull(targetTrackPoint.getOrigin());
                checkUnprocessed(targetTrackPoint.getOrigin(WptType.class));
            }
        });
    }
}
