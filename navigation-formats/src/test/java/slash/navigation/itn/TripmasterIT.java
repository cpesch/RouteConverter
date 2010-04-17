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

import slash.common.io.CompactCalendar;
import slash.navigation.base.NavigationTestCase;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.List;

public class TripmasterIT extends NavigationTestCase {

    private void readFiles(String extension, int routeCount, boolean expectElevation, boolean expectTime, RouteCharacteristics... characteristics) throws IOException {
        readFiles("tripmaster", extension, routeCount, expectElevation, expectTime, characteristics);
    }

    public void testAllTripmasterGpxTracks() throws IOException {
        readFiles(".gpx", 1, true, true, RouteCharacteristics.Track);
        readFile(new File(SAMPLE_PATH, "tripmastr-with-3-routes.gpx"), 3, true, true, RouteCharacteristics.Waypoints, RouteCharacteristics.Route, RouteCharacteristics.Track);
    }

    public void testAllTripmasterTracks() throws IOException {
        readFiles(".itn", 1, true, true, RouteCharacteristics.Track);
    }

    public void testAllTripmasterKmlTracks() throws IOException {
        readFiles(".kml", 1, true, true, RouteCharacteristics.Track);
        readFile(new File(SAMPLE_PATH, "tripmastr-with-3-routes.kml"), 1, true, false, RouteCharacteristics.Track);
        readFile(new File(SAMPLE_PATH, "tripmastr-with-3-routes-2.kml"), 2, true, false, RouteCharacteristics.Waypoints, RouteCharacteristics.Waypoints);
        readFile(new File(SAMPLE_PATH, "tripmastr-with-3-routes-3.kml"), 3, true, false, RouteCharacteristics.Track, RouteCharacteristics.Waypoints, RouteCharacteristics.Waypoints);
    }

    public void testTripmaster1dot4GpxTrack() throws IOException {
        List<GpxRoute> routes = readSampleGpxFile(new Gpx10Format(), "tripmaster1.gpx");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(RouteCharacteristics.Track, route.getCharacteristics());
        assertEquals(881, route.getPositionCount());
        GpxPosition position1 = route.getPositions().get(441);
        assertEquals(53.9783, position1.getLatitude());
        assertEquals(11.148, position1.getLongitude());
        assertEquals(22.6, position1.getElevation());
        assertEquals("Klütz", position1.getComment());
        assertEquals("Klütz", position1.getCity());
        assertEquals("Richtung 248", position1.getReason());
        assertEquals(248.0, position1.getHeading());
        CompactCalendar actual = position1.getTime();
        String cal1 = DateFormat.getDateTimeInstance().format(actual.getTime());
        CompactCalendar expected = calendar(2007, 6, 23, 14, 57, 14);
        String cal2 = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(cal2, cal1);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());

        GpxPosition position2 = route.getPositions().get(442);
        assertEquals(53.978, position2.getLatitude());
        assertEquals(11.1451, position2.getLongitude());
        assertEquals(18.0, position2.getElevation());
        assertEquals("Klütz", position2.getComment());
        assertEquals("Klütz", position2.getCity());
        assertEquals("Punkt", position2.getReason());

        GpxPosition position3 = route.getPositions().get(443);
        assertEquals(53.9778, position3.getLatitude());
        assertEquals(11.1386, position3.getLongitude());
        assertEquals(20.3, position3.getElevation());
        assertEquals("Klütz", position3.getComment());
        assertEquals("Klütz", position3.getCity());
        assertEquals("Abstand 211", position3.getReason());
    }

    public void testTripmasterGpxTrack() throws IOException {
        List<GpxRoute> routes = readSampleGpxFile(new Gpx10Format(), "tripmaster2.gpx");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(RouteCharacteristics.Track, route.getCharacteristics());
        assertEquals(735, route.getPositionCount());
        GpxPosition position1 = route.getPositions().get(441);
        assertEquals(53.79967, position1.getLatitude());
        assertEquals(10.36535, position1.getLongitude());
        assertEquals(17.9, position1.getElevation());
        assertEquals(13.0, position1.getSpeed());
        assertEquals("Bad Oldesloe; 170.1 Km", position1.getComment());
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
        assertEquals(53.79544, position2.getLatitude());
        assertEquals(10.35700, position2.getLongitude());
        assertEquals(3.9, position2.getElevation());
        assertEquals(13.0, position1.getSpeed());
        assertEquals("Bad Oldesloe; 170.9 Km", position2.getComment());
        assertEquals("Bad Oldesloe; 170.9 Km", position2.getCity());
        assertEquals("Dist. 171", position2.getReason());

        GpxPosition position3 = route.getPositions().get(443);
        assertEquals(53.79446, position3.getLatitude());
        assertEquals(10.35603, position3.getLongitude());
        assertEquals(5.6, position3.getElevation());
        assertEquals("Bad Oldesloe; 171.0 Km", position3.getComment());
        assertEquals("Bad Oldesloe; 171.0 Km", position3.getCity());
        assertEquals("Dur. 3:49:31", position3.getReason());
    }

    public void testTripmaster1dot4Track() throws IOException {
        File file = new File(SAMPLE_PATH + "tripmaster1.itn");
        List<TomTomRoute> routes = readSampleTomTomRouteFile("tripmaster1.itn", true);
        assertNotNull(routes);
        assertEquals(1, routes.size());
        TomTomRoute route = routes.get(0);
        assertEquals(RouteCharacteristics.Track, route.getCharacteristics());
        assertEquals(369, route.getPositionCount());
        TomTomPosition position1 = route.getPositions().get(85);
        assertEquals(53.65066, position1.getLatitude());
        assertEquals(9.56348, position1.getLongitude());
        assertEquals(-5.4, position1.getElevation());
        assertEquals("Hohenhorst (Haselau)", position1.getComment());
        assertEquals("Hohenhorst (Haselau)", position1.getCity());
        assertEquals("Richtung 248", position1.getReason());
        CompactCalendar actual = position1.getTime();
        String cal1 = DateFormat.getDateTimeInstance().format(actual.getTime());
        CompactCalendar expected = calendar(file, 12, 12, 27);
        String cal2 = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(cal2, cal1);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());

        TomTomPosition position2 = route.getPositions().get(86);
        assertEquals(53.65074, position2.getLatitude());
        assertEquals(9.56224, position2.getLongitude());
        assertEquals(-3.3, position2.getElevation());
        assertEquals("Hohenhorst (Haselau)", position2.getComment());
        assertEquals("Hohenhorst (Haselau)", position2.getCity());
        assertEquals("Punkt", position2.getReason());

        TomTomPosition position3 = route.getPositions().get(97);
        assertEquals(53.6691, position3.getLatitude());
        assertEquals(9.57994, position3.getLongitude());
        assertEquals(-1.6, position3.getElevation());
        assertEquals("Audeich (Haselau)", position3.getComment());
        assertEquals("Audeich (Haselau)", position3.getCity());
        assertEquals("Abstand 46", position3.getReason());
    }

    public void testTripmaster1dot8Track() throws IOException {
        List<TomTomRoute> routes = readSampleTomTomRouteFile("tripmaster3.itn", true);
        assertNotNull(routes);
        assertEquals(1, routes.size());
        TomTomRoute route = routes.get(0);
        assertEquals(RouteCharacteristics.Track, route.getCharacteristics());
        assertEquals(6, route.getPositionCount());
        TomTomPosition position1 = route.getPositions().get(0);
        assertEquals(53.56963, position1.getLatitude());
        assertEquals(10.0294, position1.getLongitude());
        assertEquals(41.0, position1.getElevation());
        assertEquals("Hohenfelde (Hamburg)", position1.getComment());
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
        assertEquals(53.56963, position2.getLatitude());
        assertEquals(10.0294, position2.getLongitude());
        assertEquals(42.0, position2.getElevation());
        assertEquals("Hohenfelde (Hamburg)", position2.getComment());
        assertEquals("Hohenfelde (Hamburg)", position2.getCity());
        assertEquals("Hohenfelde (Hamburg)", position2.getReason());

        TomTomPosition position3 = route.getPositions().get(2);
        assertEquals(53.56963, position3.getLatitude());
        assertEquals(10.0294, position3.getLongitude());
        assertEquals(0.0, position3.getElevation());
        assertEquals("Hohenfelde (Hamburg)", position3.getComment());
        assertEquals("Hohenfelde (Hamburg)", position3.getCity());
        assertEquals("Dur. 0:05:55", position3.getReason());
    }
}