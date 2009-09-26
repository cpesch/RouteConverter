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

import slash.navigation.NavigationTestCase;
import slash.navigation.RouteCharacteristics;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.util.CompactCalendar;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.List;

public class TripmasterTest extends NavigationTestCase {

    private void checkTripmaster1dot4Position(TomTomPosition position) {
        assertEquals("Richtung 316", position.getReason());
        assertEquals("Bahrenfeld", position.getCity());
        assertEquals(34.0, position.getElevation());
        assertEquals(316.0, position.getHeading());
        assertEquals(calendar(1970, 1, 1, 11, 32, 26).getTime(), position.getTime().getTime());
    }

    public void testTripmaster1dot4Position() {
        TomTomPosition position = new TomTomPosition(0, 0, "Richtung 316 - 11:32:26 - 34 m - Bahrenfeld");
        checkTripmaster1dot4Position(position);
    }

    public void testTripmaster1dot4PositionByConvertingFromOtherFormat() {
        TomTomPosition position = new TomTomPosition(null, null, null, null, null, "Richtung 316 - 11:32:26 - 34 m - Bahrenfeld");
        checkTripmaster1dot4Position(position);
    }

    public void testTripmasterShortPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "09:02:43 - 47.5 m");
        assertEquals("Waypoint", position.getReason());
        assertEquals(47.5, position.getElevation());
        assertEquals(calendar(1970, 1, 1, 9, 2, 43), position.getTime());
    }

    public void testTripmasterMiddlePosition() {
        TomTomPosition position1 = new TomTomPosition(0, 0, "Start : Noyal-Sur-Vilaine - 23/11/2006 - 08:50:26 - 37.2 m - 0.4 Km");
        assertEquals("Start : 23/11/2006 - 08:50:26", position1.getReason());
        assertEquals("Noyal-Sur-Vilaine", position1.getCity());
        assertEquals(37.2, position1.getElevation());
        assertNull(position1.getSpeed());
        assertEquals(calendar(2006, 11, 23, 8, 50, 26), position1.getTime());

        TomTomPosition position2a = new TomTomPosition(0, 0, "Finish : Cesson-Sévigné - 09:03:23 - 51.9 m - 8.6 Km");
        assertEquals("Finish : 09:03:23", position2a.getReason());
        assertEquals("Cesson-Sévigné", position2a.getCity());
        assertEquals(51.9, position2a.getElevation());
        assertNull(position2a.getSpeed());
        String actualStr = DateFormat.getDateTimeInstance().format(position2a.getTime().getTime());
        CompactCalendar expected = calendar(1970, 1, 1, 9, 3, 23);
        String expectedStr = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(expectedStr, actualStr);
        assertEquals(calendar(1970, 1, 1, 9, 3, 23), position2a.getTime());

        TomTomPosition position2b = new TomTomPosition(0, 0, "Ende : Herrenberg - 14:03:45 - 437.4 m - 25.5 km");
        assertEquals("Ende : 14:03:45", position2b.getReason());
        assertEquals("Herrenberg", position2b.getCity());
        assertEquals(437.4, position2b.getElevation());
        assertNull(position2b.getSpeed());
        actualStr = DateFormat.getDateTimeInstance().format(position2b.getTime().getTime());
        expected = calendar(1970, 1, 1, 14, 3, 45);
        expectedStr = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(expectedStr, actualStr);
        assertEquals(calendar(1970, 1, 1, 14, 3, 45), position2b.getTime());

        TomTomPosition position3a = new TomTomPosition(0, 0, "13:39:33 - Distanz 2 : Weil Der Stadt - 408.3 m - 2.0 km - 39 km/h");
        assertEquals("Distanz 2", position3a.getReason());
        assertEquals("Weil Der Stadt", position3a.getCity());
        assertEquals(39.0, position3a.getSpeed());
        assertEquals(408.3, position3a.getElevation());
        assertEquals(calendar(1970, 1, 1, 13, 39, 33), position3a.getTime());

        TomTomPosition position4 = new TomTomPosition(0, 0, "09:01:31 - Cape 125: Cesson-Sévigné - 62.0 m - 7.1 Km");
        assertEquals("Cape 125", position4.getReason());
        assertEquals("Cesson-Sévigné", position4.getCity());
        assertEquals(62.0, position4.getElevation());
        assertNull(position4.getSpeed());
        assertEquals(calendar(1970, 1, 1, 9, 1, 31), position4.getTime());

        TomTomPosition position5 = new TomTomPosition(0, 0, "18:51:45 - Hohenfelde (Hamburg) - 42.0 m - 0.2 Km - 2 Km/h - 5");
        assertEquals("Hohenfelde (Hamburg)", position5.getReason());
        assertEquals("Hohenfelde (Hamburg)", position5.getCity());
        assertEquals(42.0, position5.getElevation());
        assertEquals(2.0, position5.getSpeed());
        assertEquals(calendar(1970, 1, 1, 18, 51, 45), position5.getTime());
    }

    public void testTripmasterLongIntermediatePosition() {
        TomTomPosition position1 = new TomTomPosition(0, 0, "18:51:59 - Dur. 0:05:55 : Hohenfelde (Hamburg) - 41.0 m - 0.2 Km - 5 Km/h - 6");
        assertEquals("Dur. 0:05:55", position1.getReason());
        assertEquals("Hohenfelde (Hamburg)", position1.getCity());
        assertEquals(41.0, position1.getElevation());
        assertEquals(5.0, position1.getSpeed());
        assertEquals(calendar(1970, 1, 1, 18, 51, 59), position1.getTime());

        TomTomPosition position2 = new TomTomPosition(0, 0, "08:51:25 - Km 1.4: Acigné - 26.5 m - 1.4 km - 69 Km/h");
        assertEquals("Km 1.4", position2.getReason());
        assertEquals("Acigné", position2.getCity());
        assertEquals(26.5, position2.getElevation());
        assertEquals(69.0, position2.getSpeed());
        assertEquals(calendar(1970, 1, 1, 8, 51, 25), position2.getTime());
    }

    public void testTripmaster18LongStartPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "18:51:36 - Start : 21/07/2007 18:51:36 : Hohenfelde (Hamburg) - 1241.231 m - 0.2 Km - 12 Km/h - 6");
        assertEquals("Start : 21/07/2007 18:51:36", position.getReason());
        assertEquals("Hohenfelde (Hamburg)", position.getCity());
        assertEquals(1241.231, position.getElevation());
        assertEquals(12.0, position.getSpeed());
        assertEquals(calendar(2007, 7, 21, 18, 51, 36), position.getTime());
    }

    public void testTripmaster22LongStartPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "08:45:54 - Start : 04/04/2009 08:45:54 - 12.7 m - 0.0 Km - 5 Km/h - 11");
        assertEquals("Start : 04/04/2009 08:45:54", position.getReason());
        assertEquals("Start : 04/04/2009 08:45:54", position.getCity());
        assertEquals(12.7, position.getElevation());
        assertEquals(5.0, position.getSpeed());
        assertEquals(calendar(2009, 4, 4, 8, 45, 54), position.getTime());
    }

    private void checkTripmasterComment(String expectedComment, String expectedReason, String comment) {
        TomTomPosition position = new TomTomPosition(0.0, 0.0, 0.0, null, null, comment);
        assertEquals(expectedReason, position.getReason());
        assertEquals(expectedComment, position.getCity());
        assertEquals(expectedComment, position.getComment());
    }

    public void testTripmasterShortComments() {
        checkTripmasterComment(null, "Waypoint", "13:35:13 - 430.5 m");
        checkTripmasterComment(null, "Waypoint", "23:33:44 - -2.5 m");
    }

    public void testTripmasterMiddleComments() {
        checkTripmasterComment("Weil Der Stadt", "Start : 27/12/2006 - 13:35:13", "Start : Weil Der Stadt - 27/12/2006 - 13:35:13 - 430.5 m - 0.0 km");
        checkTripmasterComment("Weil Der Stadt", "Kurs 83", "13:35:50 - Kurs 83 : Weil Der Stadt - 411.4 m - 0.0 km");
        checkTripmasterComment("Weil Der Stadt", "Wpt", "13:36:13 - Wpt : Weil Der Stadt - 408.5 m - 0.1 km");
        checkTripmasterComment("Herrenberg", "Ende : 14:03:45", "Ende : Herrenberg - 14:03:45 - 437.4 m - 25.5 km");
    }

    public void testTripmasterLongComments() {
        checkTripmasterComment("Altona-Altstadt", "Start : 31/08/2007 19:57:24", "19:57:24 - Start : 31/08/2007 19:57:24 : Altona-Altstadt - 18.2 m - 0.0 Km - 0 Km/h - 9");
        checkTripmasterComment("Altona-Altstadt", "Dist. 8", "19:57:24 - Dist. 8 : Altona-Altstadt - 25.5 m - 8.0 Km - 50 Km/h - 9");
        checkTripmasterComment("Altona-Altstadt", "Dur. 0:23:18", "19:57:24 - Dur. 0:23:18 : Altona-Altstadt - 25.7 m - 8.3 Km - 50 Km/h - 10");
        checkTripmasterComment("Altona-Altstadt", "Course 327", "19:57:24 - Course 327 : Altona-Altstadt - 27.5 m - 8.4 Km - 25 Km/h - 11");
        checkTripmasterComment("Altona-Altstadt", "Finish : 31/08/2007 20:15:11", "20:15:11 - Finish : 31/08/2007 20:15:11 : Altona-Altstadt - 24.6 m - 10.3 Km - 7 Km/h - 7");

        checkTripmasterComment("Abtsgmünd", "Start : 30/09/2007 09:34:55", "09:34:55 - Start : 30/09/2007 09:34:55 : Abtsgmünd - 369.2 m - 0.0 km - 0 km/h - 6");
        checkTripmasterComment("Abtsgmünd", "Kurs 193", "09:36:43 - Kurs 193 : Abtsgmünd - 371.1 m - 0.1 km - 18 km/h - 6");
        checkTripmasterComment("Neuschmiede", "Distanz 2", "09:39:18 - Distanz 2 : Neuschmiede - 379.5 m - 2.0 km - 67 km/h - 6");
        checkTripmasterComment("Hüttlingen DE", "Dauer 0:10:04", "09:44:58 - Dauer 0:10:04 : Hüttlingen DE - 407.8 m - 8.6 km - 71 km/h - 7");
        checkTripmasterComment("Weil Der Stadt", "Kurs 83", "13:35:50 - Kurs 83 : Weil Der Stadt - 411.4 m - 0.0 km - 5 km/h");
        checkTripmasterComment("Weil Der Stadt", "Wpt", "13:36:13 - Wpt : Weil Der Stadt - 408.5 m - 0.1 km - 25 km/h");
        checkTripmasterComment("Acigné", "Cape 155", "08:52:25 - Cape 155 : Acigné - 39.4 m - 2.1 km - 54 Km/h");
    }

    private void checkTripmasterGpxComment(String expectedComment, String expectedReason, String comment) {
        GpxPosition position = new GpxPosition(0.0, 0.0, 0.0, null, null, comment);
        assertEquals(expectedReason, position.getReason());
        assertEquals(expectedComment, position.getCity());
        assertEquals(expectedComment, position.getComment());
    }

    public void testTripmaster1dot4GpxComments() {
        checkTripmasterGpxComment("Deven (Gross Plasten)", "Richtung 358", "Richtung 358 - Deven (Gross Plasten)");
        checkTripmasterGpxComment("Gross Gievitz", "Abstand 10", "Abstand 10 - Gross Gievitz");
        checkTripmasterGpxComment("Malchin", "Punkt", "Punkt - Malchin");
    }

    public void testTripmasterGpxComments() {
        checkTripmasterGpxComment("Blunk", "Dur. 2:11:13", "Dur. 2:11:13 : Blunk");
        checkTripmasterGpxComment("Blunk", "Dist. 107", "Dist. 107 : Blunk");
        checkTripmasterGpxComment("Blunk", "Course 35", "Course 35 : Blunk");
    }

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
