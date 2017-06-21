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

package slash.navigation.itn;

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.NavigationTestCase;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static slash.common.TestCase.calendar;
import static slash.navigation.base.NavigationTestCase.*;
import static slash.navigation.base.RouteCharacteristics.*;

public class TripmasterIT {
    @Test
    public void testAllTripmasterGpxTracks() throws IOException {
        readFiles("tripmaster", ".gpx", 1, true, true, Track);
        readFile(new File(SAMPLE_PATH, "tripmastr-with-3-routes.gpx"), 3, true, true, Waypoints, Route, Track);
    }

    @Test
    public void testAllTripmasterTracks() throws IOException {
        readFiles("tripmaster", ".itn", 1, true, true, Track);
    }

    @Test
    public void testAllTripmasterKmlTracks() throws IOException {
        readFiles("tripmaster", ".kml", 1, true, true, Track);
        readFile(new File(SAMPLE_PATH, "tripmastr-with-3-routes.kml"), 1, true, false, Route);
        readFile(new File(SAMPLE_PATH, "tripmastr-with-3-routes-2.kml"), 2, true, false, Track, Waypoints);
        readFile(new File(SAMPLE_PATH, "tripmastr-with-3-routes-3.kml"), 3, true, false, Track, Waypoints, Route);
    }

    @Test
    public void testTripmaster1dot4GpxTrack() throws Exception {
        List<GpxRoute> routes = readGpxFile(new Gpx10Format(), SAMPLE_PATH + "tripmaster1.gpx");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(Track, route.getCharacteristics());
        assertEquals(881, route.getPositionCount());
        GpxPosition position1 = route.getPositions().get(441);
        assertDoubleEquals(53.9783, position1.getLatitude());
        assertDoubleEquals(11.148, position1.getLongitude());
        assertDoubleEquals(22.6, position1.getElevation());
        assertEquals("Kl\u00fctz", position1.getDescription());
        assertEquals("Kl\u00fctz", position1.getCity());
        assertEquals("Richtung 248", position1.getReason());
        assertDoubleEquals(248.0, position1.getHeading());
        CompactCalendar actual = position1.getTime();
        String cal1 = DateFormat.getDateTimeInstance().format(actual.getTime());
        CompactCalendar expected = calendar(2007, 6, 23, 14, 57, 14);
        String cal2 = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(cal2, cal1);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());

        GpxPosition position2 = route.getPositions().get(442);
        assertDoubleEquals(53.978, position2.getLatitude());
        assertDoubleEquals(11.1451, position2.getLongitude());
        assertDoubleEquals(18.0, position2.getElevation());
        assertEquals("Kl\u00fctz", position2.getDescription());
        assertEquals("Kl\u00fctz", position2.getCity());
        assertEquals("Punkt", position2.getReason());

        GpxPosition position3 = route.getPositions().get(443);
        assertDoubleEquals(53.9778, position3.getLatitude());
        assertDoubleEquals(11.1386, position3.getLongitude());
        assertDoubleEquals(20.3, position3.getElevation());
        assertEquals("Kl\u00fctz", position3.getDescription());
        assertEquals("Kl\u00fctz", position3.getCity());
        assertEquals("Abstand 211", position3.getReason());
    }

    @Test
    public void testTripmasterGpxTrack() throws Exception {
        List<GpxRoute> routes = readGpxFile(new Gpx10Format(), SAMPLE_PATH + "tripmaster2.gpx");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(Track, route.getCharacteristics());
        assertEquals(735, route.getPositionCount());
        GpxPosition position1 = route.getPositions().get(441);
        assertDoubleEquals(53.79967, position1.getLatitude());
        assertDoubleEquals(10.36535, position1.getLongitude());
        assertDoubleEquals(17.9, position1.getElevation());
        assertDoubleEquals(13.0, position1.getSpeed());
        assertEquals("Bad Oldesloe; 170.1 Km", position1.getDescription());
        assertEquals("Bad Oldesloe; 170.1 Km", position1.getCity());
        assertEquals("Course 184", position1.getReason());
        CompactCalendar actual = position1.getTime();
        String cal1 = DateFormat.getDateTimeInstance().format(actual.getTime());
        CompactCalendar expected = calendar(2007, 7, 15, 15, 2, 53);
        String cal2 = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(cal2, cal1);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());

        GpxPosition position2 = route.getPositions().get(442);
        assertDoubleEquals(53.79544, position2.getLatitude());
        assertDoubleEquals(10.35700, position2.getLongitude());
        assertDoubleEquals(3.9, position2.getElevation());
        assertDoubleEquals(13.0, position1.getSpeed());
        assertEquals("Bad Oldesloe; 170.9 Km", position2.getDescription());
        assertEquals("Bad Oldesloe; 170.9 Km", position2.getCity());
        assertEquals("Dist. 171", position2.getReason());

        GpxPosition position3 = route.getPositions().get(443);
        assertDoubleEquals(53.79446, position3.getLatitude());
        assertDoubleEquals(10.35603, position3.getLongitude());
        assertDoubleEquals(5.6, position3.getElevation());
        assertEquals("Bad Oldesloe; 171.0 Km", position3.getDescription());
        assertEquals("Bad Oldesloe; 171.0 Km", position3.getCity());
        assertEquals("Dur. 3:49:31", position3.getReason());
    }

    @Test
    public void testTripmaster1dot4Track() throws Exception {
        File file = new File(SAMPLE_PATH + "tripmaster1.itn");
        List<TomTomRoute> routes = readSampleTomTomRouteFile("tripmaster1.itn", true);
        assertNotNull(routes);
        assertEquals(1, routes.size());
        TomTomRoute route = routes.get(0);
        assertEquals(Track, route.getCharacteristics());
        assertEquals(369, route.getPositionCount());
        TomTomPosition position1 = route.getPositions().get(85);
        assertDoubleEquals(53.65066, position1.getLatitude());
        assertDoubleEquals(9.56348, position1.getLongitude());
        assertDoubleEquals(-5.4, position1.getElevation());
        assertEquals("Hohenhorst (Haselau)", position1.getDescription());
        assertEquals("Hohenhorst (Haselau)", position1.getCity());
        assertEquals("Richtung 248", position1.getReason());
        CompactCalendar actual = position1.getTime();
        String cal1 = DateFormat.getDateTimeInstance().format(actual.getTime());
        CompactCalendar expected = NavigationTestCase.calendar(file, 12, 12, 27);
        String cal2 = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(cal2, cal1);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());

        TomTomPosition position2 = route.getPositions().get(86);
        assertDoubleEquals(53.65074, position2.getLatitude());
        assertDoubleEquals(9.56224, position2.getLongitude());
        assertDoubleEquals(-3.3, position2.getElevation());
        assertEquals("Hohenhorst (Haselau)", position2.getDescription());
        assertEquals("Hohenhorst (Haselau)", position2.getCity());
        assertEquals("Punkt", position2.getReason());

        TomTomPosition position3 = route.getPositions().get(97);
        assertDoubleEquals(53.6691, position3.getLatitude());
        assertDoubleEquals(9.57994, position3.getLongitude());
        assertDoubleEquals(-1.6, position3.getElevation());
        assertEquals("Audeich (Haselau)", position3.getDescription());
        assertEquals("Audeich (Haselau)", position3.getCity());
        assertEquals("Abstand 46", position3.getReason());
    }

    @Test
    public void testTripmaster1dot8Track() throws Exception {
        List<TomTomRoute> routes = readSampleTomTomRouteFile("tripmaster3.itn", true);
        assertNotNull(routes);
        assertEquals(1, routes.size());
        TomTomRoute route = routes.get(0);
        assertEquals(Track, route.getCharacteristics());
        assertEquals(6, route.getPositionCount());
        TomTomPosition position1 = route.getPositions().get(0);
        assertDoubleEquals(53.56963, position1.getLatitude());
        assertDoubleEquals(10.0294, position1.getLongitude());
        assertDoubleEquals(41.0, position1.getElevation());
        assertEquals("Hohenfelde (Hamburg)", position1.getDescription());
        assertEquals("Hohenfelde (Hamburg)", position1.getCity());
        assertEquals("Start : 21/07/2007 18:51:36", position1.getReason());
        CompactCalendar actual = position1.getTime();
        String cal1 = DateFormat.getDateTimeInstance().format(actual.getTime());
        CompactCalendar expected = calendar(2007, 7, 21, 18, 51, 36);
        String cal2 = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(cal2, cal1);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());

        TomTomPosition position2 = route.getPositions().get(1);
        assertDoubleEquals(53.56963, position2.getLatitude());
        assertDoubleEquals(10.0294, position2.getLongitude());
        assertDoubleEquals(42.0, position2.getElevation());
        assertEquals("Hohenfelde (Hamburg)", position2.getDescription());
        assertEquals("Hohenfelde (Hamburg)", position2.getCity());
        assertEquals("Hohenfelde (Hamburg)", position2.getReason());

        TomTomPosition position3 = route.getPositions().get(2);
        assertDoubleEquals(53.56963, position3.getLatitude());
        assertDoubleEquals(10.0294, position3.getLongitude());
        assertDoubleEquals(43.21, position3.getElevation());
        assertEquals("Hohenfelde (Hamburg)", position3.getDescription());
        assertEquals("Hohenfelde (Hamburg)", position3.getCity());
        assertEquals("Dur. 0:05:55", position3.getReason());
    }
}