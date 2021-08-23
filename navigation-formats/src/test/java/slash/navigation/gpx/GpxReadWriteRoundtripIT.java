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

import org.junit.Test;
import slash.navigation.base.ParserResult;
import slash.navigation.base.ReadWriteTestCallback;
import slash.navigation.base.Wgs84Position;
import slash.navigation.gpx.binding10.Gpx;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.binding11.RteType;
import slash.navigation.gpx.binding11.TrkType;
import slash.navigation.gpx.binding11.WptType;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.ReadWriteBase.readWriteRoundtrip;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class GpxReadWriteRoundtripIT {

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
        assertEquals("Waypoint1 13:29:25", wpt.getName());
        assertEquals("Comment", wpt.getCmt());
        assertEquals("Description1", wpt.getDesc());
        assertEquals("Source", wpt.getSrc());
        assertEquals("URL", wpt.getUrl());
        assertEquals("URLName", wpt.getUrlname());
        assertEquals(new BigInteger("3"), wpt.getSat());
        assertEquals(new BigDecimal("1.2"), wpt.getHdop());
        assertEquals(new BigDecimal("1.1"), wpt.getVdop());
        assertEquals(new BigDecimal("1.4"), wpt.getPdop());
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
        assertEquals(new BigInteger("4"), rtept.getSat());
        assertEquals(new BigDecimal("1.5"), rtept.getHdop());
        assertEquals(new BigDecimal("1.2"), rtept.getVdop());
        assertEquals(new BigDecimal("1.7"), rtept.getPdop());
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
        assertEquals(new BigDecimal("22.4"), trkpt.getCourse());
        assertEquals(new BigDecimal("15.5"), trkpt.getSpeed());
        assertEquals(new BigInteger("5"), trkpt.getSat());
        assertEquals(new BigDecimal("1.5"), trkpt.getHdop());
        assertEquals(new BigDecimal("1.2"), trkpt.getVdop());
        assertEquals(new BigDecimal("1.7"), trkpt.getPdop());
    }

    private void checkHeadingAndAccuracy(GpxRoute sourceTrack, GpxRoute targetTrack) {
        for (int i = 0; i < sourceTrack.getPositionCount(); i++) {
            Wgs84Position sourcePosition = sourceTrack.getPosition(i);
            Wgs84Position targetPosition = targetTrack.getPosition(i);
            assertEquals(targetPosition.getHeading(), sourcePosition.getHeading());
            assertEquals(targetPosition.getHdop(), sourcePosition.getHdop());
            assertEquals(targetPosition.getVdop(), sourcePosition.getVdop());
            assertEquals(targetPosition.getPdop(), sourcePosition.getPdop());
        }
    }

    @Test
    public void testGpx10Roundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from10.gpx", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                GpxRoute sourceWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertEquals(Waypoints, sourceWaypoints.getCharacteristics());
                assertNotNull(sourceWaypoints.getOrigins());
                assertEquals(1, sourceWaypoints.getOrigins().size());
                checkUnprocessed(sourceWaypoints.getOrigin(Gpx.class));
                GpxPosition sourceWaypoint = sourceWaypoints.getPosition(0);
                assertNotNull(sourceWaypoint.getOrigin());
                checkUnprocessed(sourceWaypoint.getOrigin(Gpx.Wpt.class));

                GpxRoute sourceRoute = (GpxRoute) source.getAllRoutes().get(1);
                assertEquals(Route, sourceRoute.getCharacteristics());
                assertNotNull(sourceRoute.getOrigins());
                assertEquals(2, sourceRoute.getOrigins().size());
                checkUnprocessed(sourceRoute.getOrigin(Gpx.class));
                checkUnprocessed(sourceRoute.getOrigin(Gpx.Rte.class));
                GpxPosition sourceRoutePoint = sourceRoute.getPosition(0);
                assertNotNull(sourceRoutePoint.getOrigin());
                checkUnprocessed(sourceRoutePoint.getOrigin(Gpx.Rte.Rtept.class));

                GpxRoute targetWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertEquals(Waypoints, targetWaypoints.getCharacteristics());
                assertNotNull(targetWaypoints.getOrigins());
                assertEquals(1, targetWaypoints.getOrigins().size());
                checkUnprocessed(targetWaypoints.getOrigin(Gpx.class));
                GpxPosition targetWaypoint = targetWaypoints.getPosition(0);
                assertNotNull(targetWaypoint.getOrigin());
                checkUnprocessed(targetWaypoint.getOrigin(Gpx.Wpt.class));

                GpxRoute targetRoute = (GpxRoute) target.getAllRoutes().get(1);
                assertEquals(Route, targetRoute.getCharacteristics());
                assertNotNull(targetRoute.getOrigins());
                assertEquals(2, targetRoute.getOrigins().size());
                checkUnprocessed(targetRoute.getOrigin(Gpx.class));
                checkUnprocessed(targetRoute.getOrigin(Gpx.Rte.class));
                GpxPosition targetRoutePoint = targetRoute.getPosition(0);
                assertNotNull(targetRoutePoint.getOrigin());
                checkUnprocessed(targetRoutePoint.getOrigin(Gpx.Rte.Rtept.class));

                checkHeadingAndAccuracy(sourceWaypoints, targetWaypoints);
                checkHeadingAndAccuracy(sourceRoute, targetRoute);
            }
        });
    }

    @Test
    public void testGpx10TrkRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from10trk.gpx", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
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

                checkHeadingAndAccuracy(sourceWaypoints, targetWaypoints);
                checkHeadingAndAccuracy(sourceTrack, targetTrack);
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

    private void checkHeading(GpxPosition position) {
        assertDoubleEquals(89.9, position.getHeading());
    }

    private void checkSpeed(GpxPosition position) {
        assertDoubleEquals(44.28, position.getSpeed());
    }

    private void checkTemperature(GpxPosition position) {
        assertDoubleEquals(26.0, position.getTemperature());
    }

    private void checkHeartBeat(GpxPosition position) {
        assertEquals(Short.valueOf("107"), position.getHeartBeat());
    }

    private void checkUnprocessed(WptType wptType) {
        assertNotNull(wptType);
        if (wptType.getName().endsWith(wptType.getDesc()))
            assertEquals("Waypoint1 Name; Description", wptType.getName());
        else if ("Waypoint".equals(wptType.getSym()))
            assertEquals("WP1 Name", wptType.getName());
        else
            assertEquals("Route Point1 Name", wptType.getName());
        assertEquals("Comment", wptType.getCmt());
        assertEquals("Description", wptType.getDesc());
        assertEquals("Source", wptType.getSrc());
        assertEquals("http://wpt", wptType.getLink().get(0).getHref());
    }

    @Test
    public void testGpx11Roundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from11.gpx", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                GpxRoute sourceWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertEquals(Waypoints, sourceWaypoints.getCharacteristics());
                assertNotNull(sourceWaypoints.getOrigins());
                assertEquals(1, sourceWaypoints.getOrigins().size());
                checkUnprocessed(sourceWaypoints.getOrigin(GpxType.class));
                GpxPosition sourceWaypoint = sourceWaypoints.getPosition(0);
                assertNotNull(sourceWaypoint.getOrigin());
                checkUnprocessed(sourceWaypoint.getOrigin(WptType.class));

                GpxRoute sourceRoute = (GpxRoute) source.getAllRoutes().get(1);
                assertEquals(Route, sourceRoute.getCharacteristics());
                assertNotNull(sourceRoute.getOrigins());
                assertEquals(2, sourceRoute.getOrigins().size());
                checkUnprocessed(sourceRoute.getOrigin(GpxType.class));
                checkUnprocessed(sourceRoute.getOrigin(RteType.class));
                GpxPosition sourceRoutePoint = sourceRoute.getPosition(0);
                assertNotNull(sourceRoutePoint.getOrigin());
                checkUnprocessed(sourceRoutePoint.getOrigin(WptType.class));

                GpxRoute targetWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertEquals(Waypoints, targetWaypoints.getCharacteristics());
                assertNotNull(targetWaypoints.getOrigins());
                assertEquals(1, targetWaypoints.getOrigins().size());
                checkUnprocessed(targetWaypoints.getOrigin(GpxType.class));
                GpxPosition targetWaypoint = targetWaypoints.getPosition(0);
                assertNotNull(targetWaypoint.getOrigin());
                checkUnprocessed(targetWaypoint.getOrigin(WptType.class));

                GpxRoute targetRoute = (GpxRoute) target.getAllRoutes().get(1);
                assertEquals(Route, targetRoute.getCharacteristics());
                assertNotNull(targetRoute.getOrigins());
                assertEquals(2, targetRoute.getOrigins().size());
                checkUnprocessed(targetRoute.getOrigin(GpxType.class));
                checkUnprocessed(targetRoute.getOrigin(RteType.class));
                GpxPosition targetRoutePoint = targetRoute.getPosition(0);
                assertNotNull(targetRoutePoint.getOrigin());
                checkUnprocessed(targetRoutePoint.getOrigin(WptType.class));

                checkHeadingAndAccuracy(sourceWaypoints, targetWaypoints);
                checkHeadingAndAccuracy(sourceRoute, targetRoute);
            }
        });
    }

    @Test
    public void testGpx11TrkRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from11trk.gpx", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                GpxRoute sourceWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertNotNull(sourceWaypoints.getOrigins());
                assertEquals(1, sourceWaypoints.getOrigins().size());
                checkUnprocessed(sourceWaypoints.getOrigin(GpxType.class));
                GpxPosition sourceWaypoint = sourceWaypoints.getPosition(0);
                assertNotNull(sourceWaypoint.getOrigin());
                checkUnprocessed(sourceWaypoint.getOrigin(WptType.class));

                GpxRoute sourceTrack = (GpxRoute) source.getAllRoutes().get(1);
                assertNotNull(sourceTrack.getOrigins());
                assertEquals(3, sourceTrack.getOrigins().size());
                checkUnprocessed(sourceTrack.getOrigin(GpxType.class));
                checkUnprocessed(sourceTrack.getOrigin(TrkType.class));

                GpxPosition sourceTrackPoint1 = sourceTrack.getPosition(0);
                assertNotNull(sourceTrackPoint1.getOrigin());
                checkUnprocessed(sourceTrackPoint1.getOrigin(WptType.class));
                checkHeading(sourceTrackPoint1);
                checkSpeed(sourceTrackPoint1);
                checkTemperature(sourceTrackPoint1);
                checkHeartBeat(sourceTrackPoint1);

                GpxPosition sourceTrackPoint2 = sourceTrack.getPosition(1);
                assertNotNull(sourceTrackPoint2.getOrigin());
                checkHeading(sourceTrackPoint2);
                checkSpeed(sourceTrackPoint2);
                checkTemperature(sourceTrackPoint2);
                checkHeartBeat(sourceTrackPoint2);

                GpxPosition sourceTrackPoint3 = sourceTrack.getPosition(2);
                assertNotNull(sourceTrackPoint3.getOrigin());
                assertNull(sourceTrackPoint3.getHeading());
                assertNull(sourceTrackPoint3.getSpeed());
                checkTemperature(sourceTrackPoint3);

                GpxPosition sourceTrackPoint4 = sourceTrack.getPosition(3);
                assertNotNull(sourceTrackPoint4.getOrigin());
                assertNull(sourceTrackPoint4.getHeading());
                checkSpeed(sourceTrackPoint4);
                assertNull(sourceTrackPoint4.getTemperature());
                assertNull(sourceTrackPoint4.getHeartBeat());

                GpxPosition sourceTrackPoint5 = sourceTrack.getPosition(4);
                assertNotNull(sourceTrackPoint5.getOrigin());
                checkHeading(sourceTrackPoint5);
                assertNull(sourceTrackPoint5.getSpeed());
                assertNull(sourceTrackPoint5.getTemperature());
                assertNull(sourceTrackPoint5.getHeartBeat());

                GpxPosition sourceTrackPoint6 = sourceTrack.getPosition(5);
                assertNotNull(sourceTrackPoint6.getOrigin());
                assertNull(sourceTrackPoint6.getHeading());
                assertNull(sourceTrackPoint6.getSpeed());
                assertNull(sourceTrackPoint4.getTemperature());
                checkHeartBeat(sourceTrackPoint6);

                GpxRoute targetWaypoints = (GpxRoute) source.getAllRoutes().get(0);
                assertNotNull(targetWaypoints.getOrigins());
                assertEquals(1, targetWaypoints.getOrigins().size());
                checkUnprocessed(targetWaypoints.getOrigin(GpxType.class));
                GpxPosition targetWaypoint = targetWaypoints.getPosition(0);
                assertNotNull(targetWaypoint.getOrigin());
                checkUnprocessed(targetWaypoint.getOrigin(WptType.class));

                GpxRoute targetTrack = (GpxRoute) target.getAllRoutes().get(1);
                assertNotNull(targetTrack.getOrigins());
                assertEquals(3, targetTrack.getOrigins().size());
                checkUnprocessed(targetTrack.getOrigin(GpxType.class));
                checkUnprocessed(targetTrack.getOrigin(TrkType.class));
                GpxPosition targetTrackPoint1 = targetTrack.getPosition(0);
                assertNotNull(targetTrackPoint1.getOrigin());
                checkUnprocessed(targetTrackPoint1.getOrigin(WptType.class));
                checkSpeed(targetTrackPoint1);

                GpxPosition targetTrackPoint2 = targetTrack.getPosition(1);
                assertNotNull(targetTrackPoint2.getOrigin());
                checkSpeed(targetTrackPoint2);

                checkHeadingAndAccuracy(sourceTrack, targetTrack);
                checkHeadingAndAccuracy(sourceWaypoints, targetWaypoints);
            }
        });
    }
}
